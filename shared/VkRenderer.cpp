//
// Created by wn123 on 2026-02-14.
//

#include "vk_shaders_spv.h"
#include "VkRenderer.h"
#include <swappy/swappyVk.h>
#include "Log.h"

static const char *TAG = "VkRenderer";

static const float vertexes[] = {
        1.f, 1.f, 1.f, 1.f,
        -1.f, 1.f, 0.f, 1.f,
        -1.f, -1.f, 0.f, 0.f,
        1.f, -1.f, 1.f, 0.f
};

static const uint16_t indices[] = {
        0, 1, 3,
        1, 2, 3
};

struct UBO {
    glm::mat4 model;
};

VkRenderer::VkRenderer(JNIEnv *env, jobject activity, jobject surface) {
    window = ANativeWindow_fromSurface(env, surface);
    context = std::make_unique<VkContext>(window);
    std::vector<const char *> extensions_names;
    GPU = context->get_GPU();
    choose_device_extensions(activity);
    for (const auto &it: required_extensions) {
        extensions_names.emplace_back(it.c_str());
    }
    context->create_logic_device(extensions_names, device);
    graphics_queue_info = context->get_queue_info(queue_type_t::GRAPHICS);
    present_queue_info = context->get_queue_info(queue_type_t::PRESENT);
    context->create_swap_chain(device, swap_chain, format);

    enable_swappy(env, activity, true);
    create_image_views();
    create_render_pass();
    create_layout_descriptor();
    create_graphics_pipeline();
    create_framebuffers();
    create_command_pool();
    create_buffers();
    create_descriptor_pool();
    create_command_buffers();
    create_sync_objects();
    state = renderer_state_t::PREPARED;
}

bool VkRenderer::request_start() {
    if (state != renderer_state_t::PREPARED) return false;
    vk_thread = std::thread([this]() {
        vk_thread_running = true;
        LOGI(TAG, "VkThread started, tid=%d", gettid());
        on_begin();
        for (;;) {
            if (state == renderer_state_t::RUNNING) {
                on_draw();
            } else if (state == renderer_state_t::PAUSED) {
                state.wait(renderer_state_t::PAUSED);
            }
            if (state == renderer_state_t::INVALID) {
                break;
            }
        }
        on_end();
        vk_thread_running.store(false);
        vk_thread_running.notify_one();
        LOGI(TAG, "VkThread exited, tid=%d", gettid());
    });
    state = renderer_state_t::RUNNING;
    vk_thread.detach();
    return true;
}

void VkRenderer::request_pause() {
    if (state == renderer_state_t::RUNNING) {
        state = renderer_state_t::PAUSED;
    }
}

void VkRenderer::request_resume() {
    if (state == renderer_state_t::PAUSED) {
        state = renderer_state_t::RUNNING;
        state.notify_one();
    }
}

void VkRenderer::release() {
    if (state == renderer_state_t::INVALID) return;
    state = renderer_state_t::INVALID;
    state.notify_one();
    vk_thread_running.wait(true);
    vkDeviceWaitIdle(device);
    for (size_t i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
        vkDestroySemaphore(device, image_available_semaphores[i], nullptr);
        vkDestroySemaphore(device, render_finished_semaphores[i], nullptr);
        vkDestroyFence(device, in_flight_fences[i], nullptr);
    }
    vkFreeCommandBuffers(device, command_pool, command_buffers.size(), command_buffers.data());
    vkDestroyCommandPool(device, command_pool, nullptr);
    vkDestroyBuffer(device, VBO, nullptr);
    vkDestroyBuffer(device, EBO, nullptr);
    vkFreeMemory(device, VBO_mem, nullptr);
    vkFreeMemory(device, EBO_mem, nullptr);
    for (size_t i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
        vkDestroyBuffer(device, UBOs[i], nullptr);
        vkFreeMemory(device, UBO_mems[i], nullptr);
    }
    vkDestroyDescriptorPool(device, descriptor_pool, nullptr);
    vkDestroyDescriptorSetLayout(device, descriptor_layout, nullptr);
    vkDestroyPipeline(device, pipeline, nullptr);
    vkDestroyPipelineLayout(device, pipeline_layout, nullptr);
    vkDestroyRenderPass(device, render_pass, nullptr);
    clean_swap_chain();
    vkDestroyDevice(device, nullptr);
    context = nullptr;
    ANativeWindow_release(window);
}

void VkRenderer::create_render_pass() {
    VkAttachmentDescription colorAttachment{};
    colorAttachment.format = format.image_format.format;
    colorAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
    colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    colorAttachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    colorAttachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentReference colorAttachmentRef{};
    colorAttachmentRef.attachment = 0;
    colorAttachmentRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorAttachmentRef;

    VkSubpassDependency dependency{};
    dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
    dependency.dstSubpass = 0;
    dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.srcAccessMask = 0;
    dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;

    VkRenderPassCreateInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    renderPassInfo.attachmentCount = 1;
    renderPassInfo.pAttachments = &colorAttachment;
    renderPassInfo.subpassCount = 1;
    renderPassInfo.pSubpasses = &subpass;
    renderPassInfo.dependencyCount = 1;
    renderPassInfo.pDependencies = &dependency;

    if (vkCreateRenderPass(device, &renderPassInfo, nullptr, &render_pass) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkRenderPass!");
    }
}

void VkRenderer::create_layout_descriptor() {
    VkDescriptorSetLayoutBinding uboLayoutBinding{};
    uboLayoutBinding.binding = 0;
    uboLayoutBinding.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    uboLayoutBinding.descriptorCount = 1;
    uboLayoutBinding.stageFlags = VK_SHADER_STAGE_VERTEX_BIT;
    uboLayoutBinding.pImmutableSamplers = nullptr;

    VkDescriptorSetLayoutBinding samplerLayoutBinding{};
    samplerLayoutBinding.binding = 1;
    samplerLayoutBinding.descriptorCount = 1;
    samplerLayoutBinding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    samplerLayoutBinding.pImmutableSamplers = nullptr;
    samplerLayoutBinding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;

    std::array<VkDescriptorSetLayoutBinding, 2> bindings = {uboLayoutBinding, samplerLayoutBinding};
    VkDescriptorSetLayoutCreateInfo layoutInfo{};
    layoutInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    layoutInfo.bindingCount = static_cast<uint32_t>(bindings.size());
    layoutInfo.pBindings = bindings.data();

    if (vkCreateDescriptorSetLayout(device, &layoutInfo, nullptr, &descriptor_layout) !=
        VK_SUCCESS) {
        throw std::runtime_error("Failed to create descriptor set layout!");
    }
}

void VkRenderer::create_descriptor_pool() {
    std::array<VkDescriptorPoolSize, 2> poolSizes{};
    poolSizes[0].type = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    poolSizes[0].descriptorCount = static_cast<uint32_t>(MAX_FRAMES_IN_FLIGHT);
    poolSizes[1].type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    poolSizes[1].descriptorCount = static_cast<uint32_t>(MAX_FRAMES_IN_FLIGHT);

    VkDescriptorPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    poolInfo.poolSizeCount = poolSizes.size();
    poolInfo.pPoolSizes = poolSizes.data();
    poolInfo.maxSets = static_cast<uint32_t>(MAX_FRAMES_IN_FLIGHT);
    if (vkCreateDescriptorPool(device, &poolInfo, nullptr, &descriptor_pool) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create descriptor pool!");
    }
}

void VkRenderer::create_graphics_pipeline() {
    VkShaderModule vert_shader_module = create_shader_mode(simple_vert_spv, simple_vert_spv_len);
    VkShaderModule frag_shader_module = create_shader_mode(simple_frag_spv, simple_frag_spv_len);

    VkPipelineShaderStageCreateInfo vertShaderStageCreateInfo{};
    vertShaderStageCreateInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    vertShaderStageCreateInfo.stage = VK_SHADER_STAGE_VERTEX_BIT;
    vertShaderStageCreateInfo.module = vert_shader_module;
    vertShaderStageCreateInfo.pName = "main";

    VkPipelineShaderStageCreateInfo fragShaderStageCreateInfo{};
    fragShaderStageCreateInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    fragShaderStageCreateInfo.stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    fragShaderStageCreateInfo.module = frag_shader_module;
    fragShaderStageCreateInfo.pName = "main";

    VkPipelineShaderStageCreateInfo shaderStages[] = {vertShaderStageCreateInfo,
                                                      fragShaderStageCreateInfo};

    std::vector<VkDynamicState> dynamicStates = {
            VK_DYNAMIC_STATE_VIEWPORT,
            VK_DYNAMIC_STATE_SCISSOR
    };

    VkPipelineDynamicStateCreateInfo dynamicState{};
    dynamicState.sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
    dynamicState.dynamicStateCount = static_cast<uint32_t>(dynamicStates.size());
    dynamicState.pDynamicStates = dynamicStates.data();

    VkPipelineVertexInputStateCreateInfo vertexInputInfo{};
    VkVertexInputBindingDescription bindingDescription{};

    bindingDescription.binding = 0;
    bindingDescription.stride = sizeof(float) * 4;
    bindingDescription.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;

    std::array<VkVertexInputAttributeDescription, 2> attributeDescriptions{};

    attributeDescriptions[0].binding = 0;
    attributeDescriptions[0].location = 0;
    attributeDescriptions[0].format = VK_FORMAT_R32G32_SFLOAT;
    attributeDescriptions[0].offset = 0;

    attributeDescriptions[1].binding = 0;
    attributeDescriptions[1].location = 1;
    attributeDescriptions[1].format = VK_FORMAT_R32G32_SFLOAT;
    attributeDescriptions[1].offset = sizeof(float) * 2;

    vertexInputInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    vertexInputInfo.vertexBindingDescriptionCount = 1;
    vertexInputInfo.pVertexBindingDescriptions = &bindingDescription;
    vertexInputInfo.vertexAttributeDescriptionCount = attributeDescriptions.size();
    vertexInputInfo.pVertexAttributeDescriptions = attributeDescriptions.data();

    VkPipelineInputAssemblyStateCreateInfo inputAssembly{};
    inputAssembly.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    inputAssembly.primitiveRestartEnable = VK_FALSE;

    VkViewport viewport{};
    viewport.x = 0.0f;
    viewport.y = 0.0f;
    viewport.width = static_cast<float>(format.extent.width);
    viewport.height = static_cast<float>(format.extent.height);
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;

    VkRect2D scissor{};
    scissor.offset = {0, 0};
    scissor.extent = format.extent;

    VkPipelineViewportStateCreateInfo viewportState{};
    viewportState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    viewportState.viewportCount = 1;
    viewportState.scissorCount = 1;

    VkPipelineRasterizationStateCreateInfo rasterizer{};
    rasterizer.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rasterizer.depthClampEnable = VK_FALSE;
    rasterizer.rasterizerDiscardEnable = VK_FALSE;
    rasterizer.polygonMode = VK_POLYGON_MODE_FILL;
    rasterizer.lineWidth = 1.0f;
    rasterizer.cullMode = VK_CULL_MODE_NONE;
    rasterizer.frontFace = VK_FRONT_FACE_CLOCKWISE;
    rasterizer.depthBiasEnable = VK_FALSE;
    rasterizer.depthBiasConstantFactor = 0.0f; // Optional
    rasterizer.depthBiasClamp = 0.0f; // Optional
    rasterizer.depthBiasSlopeFactor = 0.0f; // Optional

    VkPipelineMultisampleStateCreateInfo multisampling{};
    multisampling.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    multisampling.sampleShadingEnable = VK_FALSE;
    multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    multisampling.minSampleShading = 1.0f; // Optional
    multisampling.pSampleMask = nullptr; // Optional
    multisampling.alphaToCoverageEnable = VK_FALSE; // Optional
    multisampling.alphaToOneEnable = VK_FALSE; // Optional

    VkPipelineColorBlendAttachmentState colorBlendAttachment{};
    colorBlendAttachment.colorWriteMask =
            VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT |
            VK_COLOR_COMPONENT_A_BIT;
    colorBlendAttachment.blendEnable = VK_FALSE;
    colorBlendAttachment.srcColorBlendFactor = VK_BLEND_FACTOR_ONE; // Optional
    colorBlendAttachment.dstColorBlendFactor = VK_BLEND_FACTOR_ZERO; // Optional
    colorBlendAttachment.colorBlendOp = VK_BLEND_OP_ADD; // Optional
    colorBlendAttachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE; // Optional
    colorBlendAttachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ZERO; // Optional
    colorBlendAttachment.alphaBlendOp = VK_BLEND_OP_ADD; // Optional

    VkPipelineColorBlendStateCreateInfo colorBlending{};
    colorBlending.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    colorBlending.logicOpEnable = VK_FALSE;
    colorBlending.logicOp = VK_LOGIC_OP_COPY; // Optional
    colorBlending.attachmentCount = 1;
    colorBlending.pAttachments = &colorBlendAttachment;
    colorBlending.blendConstants[0] = 0.0f; // Optional
    colorBlending.blendConstants[1] = 0.0f; // Optional
    colorBlending.blendConstants[2] = 0.0f; // Optional
    colorBlending.blendConstants[3] = 0.0f; // Optional

    VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
    pipelineLayoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pipelineLayoutInfo.setLayoutCount = 1; // Optional
    pipelineLayoutInfo.pSetLayouts = &descriptor_layout; // Optional
    pipelineLayoutInfo.pushConstantRangeCount = 0; // Optional
    pipelineLayoutInfo.pPushConstantRanges = nullptr; // Optional

    if (vkCreatePipelineLayout(device, &pipelineLayoutInfo, nullptr, &pipeline_layout) !=
        VK_SUCCESS) {
        throw std::runtime_error("Unable to create pipeline layout!");
    }

    VkGraphicsPipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pipelineInfo.stageCount = 2;
    pipelineInfo.pStages = shaderStages;
    pipelineInfo.pVertexInputState = &vertexInputInfo;
    pipelineInfo.pInputAssemblyState = &inputAssembly;
    pipelineInfo.pViewportState = &viewportState;
    pipelineInfo.pRasterizationState = &rasterizer;
    pipelineInfo.pMultisampleState = &multisampling;
    pipelineInfo.pDepthStencilState = nullptr; // Optional
    pipelineInfo.pColorBlendState = &colorBlending;
    pipelineInfo.pDynamicState = &dynamicState;
    pipelineInfo.layout = pipeline_layout;
    pipelineInfo.renderPass = render_pass;
    pipelineInfo.subpass = 0;
    pipelineInfo.basePipelineHandle = VK_NULL_HANDLE; // Optional
    pipelineInfo.basePipelineIndex = -1; // Optional

    if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &pipelineInfo, nullptr, &pipeline) !=
        VK_SUCCESS) {
        throw std::runtime_error("Unable to create graphics pipeline!");
    }

    vkDestroyShaderModule(device, vert_shader_module, nullptr);
    vkDestroyShaderModule(device, frag_shader_module, nullptr);
}

void VkRenderer::create_framebuffers() {
    framebuffers.resize(image_views.size());
    for (int i = 0; i < framebuffers.size(); ++i) {
        VkImageView attachments[] = {
                image_views[i]
        };
        VkFramebufferCreateInfo createInfo{};
        createInfo.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        createInfo.pAttachments = attachments;
        createInfo.renderPass = render_pass;
        createInfo.attachmentCount = 1;
        createInfo.layers = 1;
        createInfo.width = format.extent.width;
        createInfo.height = format.extent.height;
        if (vkCreateFramebuffer(device, &createInfo, nullptr, &framebuffers[i]) != VK_SUCCESS) {
            throw std::runtime_error("Unable to create vkFramebuffer!");
        }
    }
}

void VkRenderer::create_command_pool() {
    VkCommandPoolCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    createInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    createInfo.queueFamilyIndex = graphics_queue_info.index;
    if (vkCreateCommandPool(device, &createInfo, nullptr, &command_pool) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkCommandPool!");
    }
}

void VkRenderer::create_command_buffers() {
    command_buffers.resize(MAX_FRAMES_IN_FLIGHT);
    VkCommandBufferAllocateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    createInfo.commandPool = command_pool;
    createInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    createInfo.commandBufferCount = command_buffers.size();
    if (vkAllocateCommandBuffers(device, &createInfo, command_buffers.data()) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create VkCommandBuffer!");
    }
}

void VkRenderer::create_texture() {

    uint32_t bytes_per_pixel;
    if (config->format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
    } else {
        bytes_per_pixel = 2;
    }

    VkDeviceSize imageSize = config->max_width * config->max_height * bytes_per_pixel;
    for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {

        create_buffer(imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                      tex_staging_buffer.buf, tex_staging_buffer.mem);
        vkMapMemory(device, tex_staging_buffer.mem, 0, imageSize, 0,
                    &tex_staging_buffer.data);
    }
    VkFormat imageFormat = VK_FORMAT_R8G8B8A8_SRGB;
    if (config->format == RETRO_PIXEL_FORMAT_RGB565) {
        imageFormat = VK_FORMAT_R5G6B5_UNORM_PACK16;
    }
    create_image(config->max_width, config->max_height, imageFormat,
                 VK_IMAGE_TILING_OPTIMAL,
                 VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                 VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, tex, tex_mem);
    /*Texture image view*/
    VkImageViewCreateInfo viewInfo{};
    viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    viewInfo.image = tex;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = imageFormat;
    viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    viewInfo.subresourceRange.baseMipLevel = 0;
    viewInfo.subresourceRange.levelCount = 1;
    viewInfo.subresourceRange.baseArrayLayer = 0;
    viewInfo.subresourceRange.layerCount = 1;
    if (vkCreateImageView(device, &viewInfo, nullptr, &tex_view) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create texture image view!");
    }
}

void VkRenderer::create_texture_sampler() {
    VkSamplerCreateInfo samplerInfo{};
    samplerInfo.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    samplerInfo.magFilter = VK_FILTER_NEAREST;
    samplerInfo.minFilter = VK_FILTER_NEAREST;
    samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    VkPhysicalDeviceProperties properties{};
    vkGetPhysicalDeviceProperties(GPU, &properties);
    samplerInfo.anisotropyEnable = VK_FALSE;
//    samplerInfo.maxAnisotropy = properties.limits.maxSamplerAnisotropy;
    samplerInfo.borderColor = VK_BORDER_COLOR_INT_OPAQUE_BLACK;
    samplerInfo.unnormalizedCoordinates = VK_FALSE;
    samplerInfo.compareEnable = VK_FALSE;
    samplerInfo.compareOp = VK_COMPARE_OP_ALWAYS;
    samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    samplerInfo.mipLodBias = 0.0f;
    samplerInfo.minLod = 0.0f;
    samplerInfo.maxLod = 0.0f;
    if (vkCreateSampler(device, &samplerInfo, nullptr, &tex_sampler) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create texture sampler!");
    }
}

void VkRenderer::create_buffers() {
    /*VAO*/
    size_t buffer_size = std::max(sizeof(vertexes), sizeof(indices));
    VkBuffer stagingBuffer;
    VkDeviceMemory stagingBufferMemory;
    create_buffer(buffer_size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                  VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                  stagingBuffer, stagingBufferMemory);

    void *data;
    vkMapMemory(device, stagingBufferMemory, 0, buffer_size, 0, &data);
    memcpy(data, vertexes, sizeof(vertexes));
    create_buffer(sizeof(vertexes),
                  VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                  VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VBO, VBO_mem);
    copy_buffer(stagingBuffer, VBO, sizeof(vertexes));

    /*EBO*/
    create_buffer(sizeof(indices),
                  VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                  VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, EBO, EBO_mem);
    memcpy(data, indices, sizeof(indices));
    vkUnmapMemory(device, stagingBufferMemory);
    copy_buffer(stagingBuffer, EBO, sizeof(indices));
    vkDestroyBuffer(device, stagingBuffer, nullptr);
    vkFreeMemory(device, stagingBufferMemory, nullptr);

    /*UBOs*/
    buffer_size = sizeof(UBO);
    UBOs.resize(MAX_FRAMES_IN_FLIGHT);
    UBO_mems.resize(MAX_FRAMES_IN_FLIGHT);
    for (size_t i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
        create_buffer(buffer_size, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                      VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                      UBOs[i], UBO_mems[i]);
    }
}

void VkRenderer::create_descriptor_sets() {
    std::vector<VkDescriptorSetLayout> layouts(MAX_FRAMES_IN_FLIGHT, descriptor_layout);
    VkDescriptorSetAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    allocInfo.descriptorPool = descriptor_pool;
    allocInfo.descriptorSetCount = static_cast<uint32_t>(MAX_FRAMES_IN_FLIGHT);
    allocInfo.pSetLayouts = layouts.data();

    descriptor_sets.resize(MAX_FRAMES_IN_FLIGHT);
    if (vkAllocateDescriptorSets(device, &allocInfo, descriptor_sets.data()) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate descriptor sets!");
    }

    for (size_t i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
        VkDescriptorBufferInfo bufferInfo{};
        bufferInfo.buffer = UBOs[i];
        bufferInfo.offset = 0;
        bufferInfo.range = sizeof(UBO);

        VkDescriptorImageInfo imageInfo{};
        imageInfo.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        imageInfo.imageView = tex_view;
        imageInfo.sampler = tex_sampler;

        std::array<VkWriteDescriptorSet, 2> descriptorWrites{};

        descriptorWrites[0].sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        descriptorWrites[0].dstSet = descriptor_sets[i];
        descriptorWrites[0].dstBinding = 0;
        descriptorWrites[0].dstArrayElement = 0;
        descriptorWrites[0].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        descriptorWrites[0].descriptorCount = 1;
        descriptorWrites[0].pBufferInfo = &bufferInfo;

        descriptorWrites[1].sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        descriptorWrites[1].dstSet = descriptor_sets[i];
        descriptorWrites[1].dstBinding = 1;
        descriptorWrites[1].dstArrayElement = 0;
        descriptorWrites[1].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        descriptorWrites[1].descriptorCount = 1;
        descriptorWrites[1].pImageInfo = &imageInfo;
        vkUpdateDescriptorSets(device, static_cast<uint32_t>(descriptorWrites.size()),
                               descriptorWrites.data(), 0, nullptr);
    }
}

void VkRenderer::create_sync_objects() {
    image_available_semaphores.resize(MAX_FRAMES_IN_FLIGHT);
    render_finished_semaphores.resize(MAX_FRAMES_IN_FLIGHT);
    in_flight_fences.resize(MAX_FRAMES_IN_FLIGHT);
    VkSemaphoreCreateInfo semaphoreInfo{};
    semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;
    for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
        if (vkCreateSemaphore(device, &semaphoreInfo, nullptr, &image_available_semaphores[i]) !=
            VK_SUCCESS ||
            vkCreateSemaphore(device, &semaphoreInfo, nullptr, &render_finished_semaphores[i]) !=
            VK_SUCCESS ||
            vkCreateFence(device, &fenceInfo, nullptr, &in_flight_fences[i]) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create semaphores!");
        }
    }
}

VkShaderModule VkRenderer::create_shader_mode(const u_int8_t *bytes, size_t size_in_bytes) {
    VkShaderModuleCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = size_in_bytes;
    createInfo.pCode = reinterpret_cast<const uint32_t *>(bytes);
    VkShaderModule module;
    if (vkCreateShaderModule(device, &createInfo, nullptr, &module) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkShaderModule!");
    }
    return module;
}

void
VkRenderer::create_buffer(VkDeviceSize size, VkBufferUsageFlags usage, VkMemoryPropertyFlags props,
                          VkBuffer &buffer,
                          VkDeviceMemory &mem) {
    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    if (vkCreateBuffer(device, &bufferInfo, nullptr, &buffer) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create vertex buffer!");
    }

    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(device, buffer, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = find_mem_type(memRequirements.memoryTypeBits, props);
    if (vkAllocateMemory(device, &allocInfo, nullptr, &mem) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate vertex buffer memory!");
    }
    vkBindBufferMemory(device, buffer, mem, 0);
}

uint32_t VkRenderer::find_mem_type(uint32_t filter_type, VkMemoryPropertyFlags properties) {
    VkPhysicalDeviceMemoryProperties memProperties;
    vkGetPhysicalDeviceMemoryProperties(GPU, &memProperties);
    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        if (filter_type & (1 << i) &&
            (memProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }

    throw std::runtime_error("failed to find suitable memory type!");
}

void VkRenderer::create_image(uint32_t w, uint32_t h, VkFormat fmt, VkImageTiling tiling,
                              VkImageUsageFlags usageFlags, VkMemoryPropertyFlags props,
                              VkImage &img, VkDeviceMemory &img_mem) {
    VkImageCreateInfo imageInfo{};
    imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.extent.width = w;
    imageInfo.extent.height = h;
    imageInfo.extent.depth = 1;
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.format = fmt;
    imageInfo.tiling = tiling;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    imageInfo.usage = usageFlags;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.flags = 0; // Optional

    if (vkCreateImage(device, &imageInfo, nullptr, &img) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create image!");
    }

    VkMemoryRequirements memRequirements;
    vkGetImageMemoryRequirements(device, img, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = find_mem_type(memRequirements.memoryTypeBits, props);

    if (vkAllocateMemory(device, &allocInfo, nullptr, &img_mem) != VK_SUCCESS) {
        throw std::runtime_error("failed to allocate image memory!");
    }

    vkBindImageMemory(device, img, img_mem, 0);
}

void VkRenderer::transition_layout(VkCommandBuffer command_buffer, VkImage img, VkFormat format,
                                   VkImageLayout old_layout,
                                   VkImageLayout new_layout) {
    VkImageMemoryBarrier barrier{};
    barrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
    barrier.oldLayout = old_layout;
    barrier.newLayout = new_layout;
    barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.image = img;
    barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    barrier.subresourceRange.baseMipLevel = 0;
    barrier.subresourceRange.levelCount = 1;
    barrier.subresourceRange.baseArrayLayer = 0;
    barrier.subresourceRange.layerCount = 1;
    VkPipelineStageFlags sourceStage;
    VkPipelineStageFlags destinationStage;

    if (old_layout == VK_IMAGE_LAYOUT_UNDEFINED &&
        new_layout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
        barrier.srcAccessMask = 0;
        barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;

        sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
    } else if (old_layout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
               new_layout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
        barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

        sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
    } else {
        throw std::invalid_argument("Unsupported layout transition!");
    }
    vkCmdPipelineBarrier(
            command_buffer,
            sourceStage, destinationStage,
            0,
            0, nullptr,
            0, nullptr,
            1, &barrier
    );
}

void VkRenderer::begin_single_time_commands(VkCommandBuffer &command_buffer) {
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = command_pool;
    allocInfo.commandBufferCount = 1;

    vkAllocateCommandBuffers(device, &allocInfo, &command_buffer);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(command_buffer, &beginInfo);
}

void VkRenderer::end_single_time_commands(VkCommandBuffer command_buffer) {
    vkEndCommandBuffer(command_buffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &command_buffer;

    vkQueueSubmit(graphics_queue_info.queue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(graphics_queue_info.queue);

    vkFreeCommandBuffers(device, command_pool, 1, &command_buffer);
}

void VkRenderer::on_begin() {
    create_texture();
    create_texture_sampler();
    create_descriptor_sets();
    cur_frame = 0;
}

void VkRenderer::on_draw() {
    uint32_t idx;
    vkWaitForFences(device, 1, &in_flight_fences[cur_frame], VK_TRUE, UINT64_MAX);
    vkResetFences(device, 1, &in_flight_fences[cur_frame]);
    if (vkAcquireNextImageKHR(device, swap_chain, UINT64_MAX,
                              image_available_semaphores[cur_frame],
                              VK_NULL_HANDLE, &idx) == VK_ERROR_OUT_OF_DATE_KHR) {
        recreate_swap_chain();
        return;
    }
    vkResetCommandBuffer(command_buffers[cur_frame], 0);

    update_uniform_buffer();
    record_command_buffer(command_buffers[cur_frame], idx);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;

    VkSemaphore waitSemaphores[] = {image_available_semaphores[cur_frame]};
    VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
    submitInfo.waitSemaphoreCount = 1;
    submitInfo.pWaitSemaphores = waitSemaphores;
    submitInfo.pWaitDstStageMask = waitStages;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &command_buffers[cur_frame];
    VkSemaphore signalSemaphores[] = {render_finished_semaphores[cur_frame]};
    submitInfo.signalSemaphoreCount = 1;
    submitInfo.pSignalSemaphores = signalSemaphores;
    if (vkQueueSubmit(graphics_queue_info.queue, 1, &submitInfo, in_flight_fences[cur_frame]) !=
        VK_SUCCESS) {
        throw std::runtime_error("Failed to submit command buffer!");
    }

    VkPresentInfoKHR presentInfo{};
    presentInfo.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;

    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = signalSemaphores;

    VkSwapchainKHR swapChains[] = {swap_chain};
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = swapChains;
    presentInfo.pImageIndices = &idx;
    presentInfo.pResults = nullptr; // Optional
    if (swappy_enabled) {
        SwappyVk_queuePresent(present_queue_info.queue, &presentInfo);
    } else {
        vkQueuePresentKHR(present_queue_info.queue, &presentInfo);
    }
    cur_frame = (cur_frame + 1) % MAX_FRAMES_IN_FLIGHT;
    frame_time_helper.next_frame();
}

void VkRenderer::on_end() {
    vkDestroyImage(device, tex, nullptr);
    vkFreeMemory(device, tex_mem, nullptr);
    for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
        vkUnmapMemory(device, tex_staging_buffer.mem);
        vkFreeMemory(device, tex_staging_buffer.mem, nullptr);
        vkDestroyBuffer(device, tex_staging_buffer.buf, nullptr);
    }
    vkDestroyImageView(device, tex_view, nullptr);
    vkDestroySampler(device, tex_sampler, nullptr);
}

void
VkRenderer::copy_image_buffer(VkCommandBuffer command_buffer, VkBuffer buffer, VkImage image,
                              uint32_t width, uint32_t height) {
    VkBufferImageCopy region{};
    region.bufferOffset = 0;
    region.bufferRowLength = 0;
    region.bufferImageHeight = 0;

    region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    region.imageSubresource.mipLevel = 0;
    region.imageSubresource.baseArrayLayer = 0;
    region.imageSubresource.layerCount = 1;

    region.imageOffset = {0, 0, 0};
    region.imageExtent = {
            width,
            height,
            1
    };
    vkCmdCopyBufferToImage(
            command_buffer,
            buffer,
            image,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            1,
            &region
    );
}

void VkRenderer::copy_buffer(VkBuffer src, VkBuffer dst, VkDeviceSize size) {
    VkCommandBuffer command_buffer;
    begin_single_time_commands(command_buffer);
    VkBufferCopy copyRegion{};
    copyRegion.size = size;
    vkCmdCopyBuffer(command_buffer, src, dst, 1, &copyRegion);
    end_single_time_commands(command_buffer);
}

void VkRenderer::update_uniform_buffer() {
    UBO ubo{};
    ubo.model = glm::mat4(1.0);
    void *data;
    vkMapMemory(device, UBO_mems[cur_frame], 0, sizeof(UBO), 0, &data);
    memcpy(data, &ubo, sizeof(UBO));
    vkUnmapMemory(device, UBO_mems[cur_frame]);
}

void VkRenderer::record_command_buffer(VkCommandBuffer command_buffer, u_int32_t index) {
    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = 0; // Optional
    beginInfo.pInheritanceInfo = nullptr; // Optional

    if (vkBeginCommandBuffer(command_buffer, &beginInfo) != VK_SUCCESS) {
        throw std::runtime_error("Unable to submit VkCommandBuffer!");
    }

    update_texture(command_buffer);
    VkRenderPassBeginInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    renderPassInfo.renderPass = render_pass;
    renderPassInfo.framebuffer = framebuffers[index];
    renderPassInfo.renderArea.offset = {0, 0};
    renderPassInfo.renderArea.extent = format.extent;
    VkClearValue clearColor = {{{0.0f, 0.0f, 0.0f, 1.0f}}};
    renderPassInfo.clearValueCount = 1;
    renderPassInfo.pClearValues = &clearColor;
    vkCmdBeginRenderPass(command_buffer, &renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
    vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

    VkViewport viewport{};
    viewport.x = 0.0f;
    viewport.y = 0.0f;
    viewport.width = static_cast<float>(format.extent.width);
    viewport.height = static_cast<float>(format.extent.height);
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;
    vkCmdSetViewport(command_buffer, 0, 1, &viewport);

    VkRect2D scissor{};
    scissor.offset = {0, 0};
    scissor.extent = format.extent;
    vkCmdSetScissor(command_buffer, 0, 1, &scissor);

    VkBuffer vertexBuffers[] = {VBO};
    VkDeviceSize offsets[] = {0};
    vkCmdBindVertexBuffers(command_buffer, 0, 1, vertexBuffers, offsets);
    vkCmdBindIndexBuffer(command_buffer, EBO, 0, VK_INDEX_TYPE_UINT16);
    vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout, 0, 1,
                            &descriptor_sets[cur_frame], 0, nullptr);
    vkCmdDrawIndexed(command_buffer, 6, 1, 0, 0, 0);
    vkCmdEndRenderPass(command_buffer);
    if (vkEndCommandBuffer(command_buffer) != VK_SUCCESS) {
        throw std::runtime_error("Failed to record command buffer!");
    }
}

void VkRenderer::update_texture(VkCommandBuffer command_buffer) {
    if (!tex_updated) return;
    VkFormat imageFormat = VK_FORMAT_R8G8B8A8_SRGB;
    if (config->format == RETRO_PIXEL_FORMAT_RGB565) {
        imageFormat = VK_FORMAT_R5G6B5_UNORM_PACK16;
    }
    transition_layout(command_buffer, tex, imageFormat,
                      VK_IMAGE_LAYOUT_UNDEFINED,
                      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    copy_image_buffer(command_buffer, tex_staging_buffer.buf, tex,
                      static_cast<uint32_t>(config->width),
                      static_cast<uint32_t>(config->height));
    transition_layout(command_buffer, tex, imageFormat, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                      VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
//    tex_updated.store(false);
}

void VkRenderer::create_image_views() {
    uint32_t count = 0;
    std::vector<VkImage> images;
    vkGetSwapchainImagesKHR(device, swap_chain, &count, nullptr);
    images.resize(count);
    image_views.resize(count);
    vkGetSwapchainImagesKHR(device, swap_chain, &count, images.data());
    for (int i = 0; i < images.size(); ++i) {
        const VkImage &image = images[i];
        VkImageViewCreateInfo imageViewCreateInfo{};
        imageViewCreateInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        imageViewCreateInfo.image = image;
        imageViewCreateInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        imageViewCreateInfo.format = format.image_format.format;
        imageViewCreateInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        imageViewCreateInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        imageViewCreateInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        imageViewCreateInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        imageViewCreateInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        imageViewCreateInfo.subresourceRange.baseMipLevel = 0;
        imageViewCreateInfo.subresourceRange.levelCount = 1;
        imageViewCreateInfo.subresourceRange.baseArrayLayer = 0;
        imageViewCreateInfo.subresourceRange.layerCount = 1;
        if (vkCreateImageView(device, &imageViewCreateInfo, nullptr, &image_views[i]) !=
            VK_SUCCESS) {
            throw std::runtime_error("Unable to create VkImageView!");
        }
    }
}

void VkRenderer::resize_viewport(uint32_t w, uint32_t h) {
    /*Do nothing*/
}

void VkRenderer::submit(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (state != renderer_state_t::RUNNING) return;
    uint8_t bytes_per_pixel = 2;
    if (config->format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
    }
    if (width * bytes_per_pixel == pitch) {
        memcpy(tex_staging_buffer.data, data, height * pitch);
    } else {
        for (int i = 0; i < height; ++i) {
            memcpy((void *) (static_cast<const char *>(tex_staging_buffer.data) +
                             i * width * bytes_per_pixel),
                   static_cast<const char *>(data) + i * pitch,
                   width * bytes_per_pixel);
        }
    }
    tex_updated.store(true);
}

int VkRenderer::get_frame_rate() {
    return frame_time_helper.frame_rate();
}

std::unique_ptr<image_t> VkRenderer::read_pixels() {
    return nullptr;
}

void VkRenderer::choose_device_extensions(bool use_swappy) {
    required_extensions.insert(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
    if (use_swappy) {
        uint32_t available_extension_count = 0;
        std::vector<VkExtensionProperties> available_extensions;
        vkEnumerateDeviceExtensionProperties(GPU, nullptr, &available_extension_count,
                                             nullptr);
        available_extensions.resize(available_extension_count);
        vkEnumerateDeviceExtensionProperties(GPU, nullptr, &available_extension_count,
                                             available_extensions.data());
        /*Frame pacing*/
        uint32_t count = 0;
        char **extensions;
        SwappyVk_determineDeviceExtensions(GPU, available_extension_count,
                                           available_extensions.data(), &count, nullptr);
        extensions = (char **) malloc(count * sizeof(char *));
        char *names = (char *) malloc(count * (VK_MAX_EXTENSION_NAME_SIZE + 1));
        for (uint32_t i = 0; i < count; i++) {
            extensions[i] = &names[i * (VK_MAX_EXTENSION_NAME_SIZE + 1)];
        }
        SwappyVk_determineDeviceExtensions(GPU, available_extension_count,
                                           available_extensions.data(), &count, extensions);
        for (uint32_t i = 0; i < count; i++) {
            required_extensions.insert(extensions[i]);
        }
        free(names);
        free(extensions);
    }
}

void VkRenderer::enable_swappy(JNIEnv *env, jobject activity, bool init_first) {
    if (init_first) {
        if (!activity) return;
        uint64_t refresh_duration;
        SwappyVk_setQueueFamilyIndex(device, present_queue_info.queue, present_queue_info.index);
        SwappyVk_initAndGetRefreshCycleDuration(env, activity, GPU, device, swap_chain,
                                                &refresh_duration);
    }
    int count = SwappyVk_getSupportedRefreshPeriodsNS(nullptr, 0, swap_chain);
    uint64_t all_swap_ns[count];
    SwappyVk_getSupportedRefreshPeriodsNS(all_swap_ns, count, swap_chain);
    uint64_t min_swap_ns = all_swap_ns[0];
    for (int i = 1; i < count; ++i) {
        min_swap_ns = std::min(all_swap_ns[i], min_swap_ns);
    }
    SwappyVk_setSwapIntervalNS(device, swap_chain, min_swap_ns);
    SwappyVk_setAutoSwapInterval(true);
    SwappyVk_setAutoPipelineMode(true);
    SwappyVk_setWindow(device, swap_chain, window);
    if (SwappyVk_isEnabled(swap_chain, &swappy_enabled) && swappy_enabled) {
        LOGI(TAG, "Frame pacing enabled, Set preference to %d fps.",
             static_cast<int32_t>(1000 * kNanosPerMillisecond / min_swap_ns));
    } else {
        swappy_enabled = false;
    }
}

void VkRenderer::recreate_swap_chain() {
    LOGD(TAG, "recreate_swap_chain called!");
    vkDeviceWaitIdle(device);
    clean_swap_chain();
    context->create_swap_chain(device, swap_chain, format);
    if (swappy_enabled) {
        enable_swappy(nullptr, nullptr, false);
    }
    create_image_views();
    create_framebuffers();
}

void VkRenderer::clean_swap_chain() {
    SwappyVk_destroySwapchain(device, swap_chain);
    for (int i = 0; i < framebuffers.size(); ++i) {
        vkDestroyFramebuffer(device, framebuffers[i], nullptr);
        vkDestroyImageView(device, image_views[i], nullptr);
    }
    vkDestroySwapchainKHR(device, swap_chain, nullptr);
    swap_chain = VK_NULL_HANDLE;
}
