//
// Created by 86187 on 2026/2/6.
//

#ifndef WKUWKU_VULKAN_VKCONTEXT_H
#define WKUWKU_VULKAN_VKCONTEXT_H
#include <vector>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <android/native_window_jni.h>

struct surface_details_t {
    VkSurfaceCapabilitiesKHR capabilities;
    std::vector<VkSurfaceFormatKHR> formats;
    std::vector<VkPresentModeKHR> modes;
};

struct swap_chain_format_t {
    VkSurfaceFormatKHR image_format;
    VkPresentModeKHR mode;
    VkExtent2D extent;
    u_int32_t min_image_count;
};

struct queue_info_t {
    uint32_t index;
    VkQueue queue;
};

enum class queue_type_t {
    GRAPHICS,
    PRESENT
};

class VkContext {
private:
    std::string name;
    ANativeWindow* window;
    VkInstance instance{};
    VkSurfaceKHR surface{};
    VkPhysicalDevice GPU{};
    surface_details_t surface_details{};
    queue_info_t graphics_queue_info{};
    queue_info_t present_queue_info{};
    VkPhysicalDevice choose_device();
    bool is_suitable(VkPhysicalDevice gpu);
    bool choose_queue_families(VkPhysicalDevice gpu);
    void query_surface_details(VkPhysicalDevice gpu);
    swap_chain_format_t choose_swap_chain_format();
    void create_instance();
    void create_surface();
public:
    explicit VkContext(std::string, ANativeWindow* _window);
    virtual ~VkContext();
    void create_logic_device(const std::vector<const char*>& /*required_extensions*/, VkDevice &);
    void create_swap_chain(VkDevice &dev, VkSwapchainKHR &, swap_chain_format_t&, const VkSwapchainKHR&);
    VkPhysicalDevice get_device();
    VkSurfaceTransformFlagBitsKHR get_surface_transform();
    queue_info_t get_queue_info(const queue_type_t& type);
};


#endif //WKUWKU_VULKAN_VKCONTEXT_H
