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
#include <utility>
#include "Renderer.h"
#include "VkContext.h"
#include "Utils.h"


class vk_buffer_t {
private:
    VkDevice dev;
    VkBuffer buf{};
    VkDeviceMemory mem{};
    void *data_ptr = nullptr;
public:
    vk_buffer_t(VkDevice, VkPhysicalDevice, VkBufferUsageFlags, VkMemoryPropertyFlags,
                         VkDeviceSize);
    ~vk_buffer_t();
    void* data();
    [[nodiscard]] VkBuffer buffer() const;
    static uint32_t find_memory_type_index(VkPhysicalDevice, uint32_t /*type_filter*/, VkMemoryPropertyFlags /*properties*/);
};

class vk_swap_chain_t {
private:
    std::mutex mtx;
    std::vector<std::shared_ptr<vk_buffer_t>> buffers;
    std::queue<int> free_idxs;
    std::queue<int> full_idxs;
    int cur_read_idx = -1;
public:
    explicit vk_swap_chain_t(VkDevice,
                             VkPhysicalDevice,
                             VkBufferUsageFlags,
                             VkMemoryPropertyFlags,
                             VkDeviceSize,
                             uint32_t);

    int acquire_write_idx();

    void submit(int idx);

    std::shared_ptr<vk_buffer_t> get(int idx);

    int acquire_read_idx();

    virtual ~vk_swap_chain_t();
};

class VkRenderer : public Renderer {
private:
    const int MAX_FRAMES_IN_FLIGHT = 3;
    std::unique_ptr<VkContext> context;
    ANativeWindow *window;
    std::thread vk_thread;
    std::atomic<bool> vk_thread_running = false;
    std::atomic<renderer_state_t> state = renderer_state_t::INVALID;
    bool swappy_enabled = false;
    float cur_aspect_ratio;
    uint32_t next_image_idx{};
    utils::frame_time_helper_t frame_time_helper;
    VkDevice device{};
    VkPhysicalDevice GPU;
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
    std::unique_ptr<vk_swap_chain_t> tex_swap_chain;
    VkSwapchainKHR swap_chain{};
    queue_info_t graphics_queue_info{};
    queue_info_t present_queue_info{};
    std::shared_ptr<vk_buffer_t> VBO;
    std::shared_ptr<vk_buffer_t> EBO;
    std::vector<std::shared_ptr<vk_buffer_t>> UBOs;
    std::vector<VkImage> images;
    std::vector<VkImageView> image_views;
    std::vector<VkFramebuffer> framebuffers;
    std::vector<VkCommandBuffer> command_buffers;
    std::vector<VkSemaphore> image_available_semaphores;
    std::vector<VkSemaphore> render_finished_semaphores;
    std::vector<VkFence> in_flight_fences;
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

    void update_texture(VkCommandBuffer);

    void create_texture_sampler();

    void create_buffers();

    void create_sync_objects();

    void recreate_swap_chain();

    void clean_swap_chain();

//    void copy_buffer(vk_buffer_t& /*src*/, uint32_t /*src_offset*/, vk_buffer_t& /*dst*/,
//                     uint32_t /*dst_offset*/, VkDeviceSize /*len*/);

    VkShaderModule create_shader_mode(const u_int8_t *bytes, size_t size_in_bytes);

    void create_image(uint32_t, uint32_t, VkFormat, VkImageTiling, VkImageUsageFlags,
                      VkMemoryPropertyFlags, VkImage &, VkDeviceMemory &);

    void begin_single_time_commands(VkCommandBuffer &);

    void end_single_time_commands(VkCommandBuffer);

    void update_uniform_buffer();

    void update_texcoords();

    void record_command_buffer(VkCommandBuffer /*buffer*/, u_int32_t /*image index*/);

    void on_begin();

    void on_draw();

    void on_end();

    void choose_device_extensions(bool /*use_swappy*/);

    void enable_swappy(JNIEnv *env, jobject activity);

    static void
    upload_image(VkCommandBuffer, VkBuffer /*buffer*/, VkImage /*image*/,
                 uint32_t /*width*/,
                 uint32_t /*height*/);

    static void
    download_image(VkCommandBuffer, VkImage, uint32_t, uint32_t, VkBuffer);

    static void
    transition_image_layout(VkCommandBuffer, VkImage, VkFormat, VkImageLayout /*old_layout*/,
                            VkImageLayout /*new_layout*/);

public:
    explicit VkRenderer(JNIEnv */*env*/, jobject /*activity*/, jobject /*surface*/);

    void resize_viewport(uint32_t w, uint32_t h) override;

    bool request_start() override;

    void request_pause() override;

    void request_resume() override;

    void render(const void *, unsigned, unsigned, size_t) override;

    std::unique_ptr<image_t> read_pixels() override;

    int get_frame_rate() override;

    void release() override;
};


#endif //WKUWKU_VULKAN_VKRENDERER_H
