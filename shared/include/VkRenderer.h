//
// Created by wn123 on 2026-02-14.
//

#ifndef WKUWKU_VULKAN_VKRENDERER_H
#define WKUWKU_VULKAN_VKRENDERER_H
#include <jni.h>
#include <thread>
#include <set>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include "VkShader.h"
#include "Renderer.h"
#include "VkContext.h"
#include "Utils.h"

class VkRenderer: public Renderer {
private:
    const int MAX_FRAMES_IN_FLIGHT = 2;
    std::unique_ptr<VkContext> context;
    ANativeWindow* window;
    std::thread vk_thread;
    std::atomic<bool> vk_thread_running = false;
    std::atomic<renderer_state_t> state = renderer_state_t::INVALID;
    bool use_swappy = false;
    util::frame_time_helper_t frame_time_helper;
    VkDevice device{};
    VkPhysicalDevice phy_device;
    swap_chain_format_t format{};
    VkRenderPass render_pass{};
    VkDescriptorSetLayout descriptor_layout{};
    VkDescriptorPool descriptor_pool{};
    std::vector<VkDescriptorSet> descriptor_sets;
    VkPipelineLayout pipeline_layout{};
    VkPipeline pipeline{};
    VkCommandPool command_pool{};
    VkImage tex{};
    VkImageView tex_view{};
    VkDeviceMemory tex_mem{};
    VkSampler tex_sampler{};
    VkSwapchainKHR swap_chain{};
    queue_info_t graphics_queue_info{};
    queue_info_t present_queue_info{};
    VkBuffer VBO{}, EBO{};
    VkDeviceMemory VBO_mem{}, EBO_mem{};
    std::vector<VkBuffer> UBOs;
    std::vector<VkDeviceMemory> UBO_mems;
    std::vector<VkImageView> image_views;
    std::vector<VkFramebuffer> framebuffers;
    std::vector<VkCommandBuffer> command_buffers;
    std::vector<VkSemaphore> image_available_semaphores;
    std::vector<VkSemaphore> render_finished_semaphores;
    std::vector<VkFence> in_flight_fences;
    std::shared_ptr<video_config_t> config;
    std::set<std::string> required_extensions{};
    uint32_t cur_frame = 0;
    void create_image_views();
    void create_framebuffers();
    void create_render_pass();
    void create_layout_descriptor();
    void create_descriptor_pool();
    void create_descriptor_sets();
    void create_graphics_pipeline();
    void create_command_pool();
    void create_command_buffers();
    void create_texture();
    void create_texture_sampler();
    void create_buffers();
    void create_sync_objects();
    void create_buffer(VkDeviceSize, VkBufferUsageFlags, VkMemoryPropertyFlags, VkBuffer&, VkDeviceMemory&);
    void recreate_swap_chain();
    void clean_swap_chain();
    void copy_buffer(VkBuffer /*src*/, VkBuffer /*dst*/, VkDeviceSize /*size*/);
    void copy_image_buffer(VkBuffer /*buffer*/, VkImage /*image*/, uint32_t /*width*/, uint32_t /*height*/);
    uint32_t find_mem_type(uint32_t filter, VkMemoryPropertyFlags properties);
    VkShaderModule create_shader_mode(const u_int8_t* bytes, size_t size_in_bytes);
    void create_image(uint32_t, uint32_t, VkFormat, VkImageTiling, VkImageUsageFlags, VkMemoryPropertyFlags, VkImage&, VkDeviceMemory&);
    void transition_layout(VkImage, VkFormat, VkImageLayout /*old_layout*/, VkImageLayout /*new_layout*/);
    void begin_single_time_commands(VkCommandBuffer&);
    void end_single_time_commands(VkCommandBuffer);
    void update_uniform_buffer();
    void record_command_buffer(VkCommandBuffer /*buffer*/, u_int32_t /*image index*/);
    void on_begin();
    void on_draw();
    void on_end();
    void choose_device_extensions();
    void init_swappy(JNIEnv*, jobject);
    void enable_swappy();
public:
    explicit VkRenderer(JNIEnv */*env*/, jobject /*activity*/, jobject /*surface*/);
    void resize_viewport(uint32_t w, uint32_t h) override;
    bool request_start() override;
    void request_pause() override;
    void request_resume() override;
    void submit(const void *, unsigned, unsigned, size_t) override;
    std::unique_ptr<image_t> read_pixels() override;
    int get_frame_rate() override;
    void release() override;
};


#endif //WKUWKU_VULKAN_VKRENDERER_H
