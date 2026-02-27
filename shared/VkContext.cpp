//
// Created by 86187 on 2026/2/6.
//

#include <chrono>
#include "VkContext.h"
#include "Log.h"

static const char* TAG = "VkContext";

VkContext::VkContext(ANativeWindow *_window): window(_window) {
    create_instance();
    create_surface();
    create_logic_device();
    create_swap_chain();
}

VkContext::~VkContext() {
    vkDeviceWaitIdle(dev);
    vkDestroySwapchainKHR(dev, swap_chain, nullptr);
    vkDestroyDevice(dev, nullptr);
    vkDestroySurfaceKHR(instance, surface, nullptr);
    vkDestroyInstance(instance, nullptr);
}

VkPhysicalDevice VkContext::find_GPU() {
    uint32_t count = 0;
    std::vector<VkPhysicalDevice> devices;
    vkEnumeratePhysicalDevices(instance, &count, nullptr);
    devices.resize(count);
    vkEnumeratePhysicalDevices(instance, &count, devices.data());
    for (const VkPhysicalDevice& gpu: devices) {
        if (is_suitable(gpu)) {
            return gpu;
        }
    }
    return nullptr;
}

bool VkContext::is_suitable(VkPhysicalDevice gpu) {
    VkPhysicalDeviceProperties properties{};
    VkPhysicalDeviceFeatures features{};
    vkGetPhysicalDeviceProperties(gpu, &properties);
    vkGetPhysicalDeviceFeatures(gpu, &features);
    if ((properties.deviceType != VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
                 && properties.deviceType != VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                 || !find_queue_families(gpu)) {
        return false;
    }
    query_swap_chain_details(gpu);
    if (swap_chain_details.formats.empty() || swap_chain_details.modes.empty()) {
        return false;
    }
    return true;
}

bool VkContext::find_queue_families(VkPhysicalDevice gpu) {
    uint32_t count = 0;
    std::vector<VkQueueFamilyProperties> families;
    vkGetPhysicalDeviceQueueFamilyProperties(gpu, &count, nullptr);
    families.resize(count);
    vkGetPhysicalDeviceQueueFamilyProperties(gpu, &count, families.data());
    for (int i = 0; i < families.size(); ++i) {
        VkBool32 presentSupport = false;
        vkGetPhysicalDeviceSurfaceSupportKHR(gpu, i, surface, &presentSupport);
        if ((families[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && presentSupport) {
            graphics_queue_info.index = i;
            present_queue_info.index = i;
            return true;
        }
    }
    return false;
}

void VkContext::query_swap_chain_details(VkPhysicalDevice gpu) {
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu, surface, &swap_chain_details.capabilities);
    uint32_t count = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(gpu, surface, &count, nullptr);
    swap_chain_details.formats.resize(count);
    vkGetPhysicalDeviceSurfaceFormatsKHR(gpu, surface, &count, swap_chain_details.formats.data());
    vkGetPhysicalDeviceSurfacePresentModesKHR(gpu, surface, &count, nullptr);
    swap_chain_details.modes.resize(count);
    vkGetPhysicalDeviceSurfacePresentModesKHR(gpu, surface, &count, swap_chain_details.modes.data());
}

swap_chain_format_t VkContext::choose_swap_chain_format() {
    /*Choose surface format*/
    auto fmt = std::find_if(swap_chain_details.formats.begin(), swap_chain_details.formats.end(), [](const VkSurfaceFormatKHR& it){
        return it.format == VK_FORMAT_B8G8R8A8_SRGB && it.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    });
    swap_chain_format.image_format = fmt != swap_chain_details.formats.end() ? *fmt : swap_chain_details.formats[0];

    /*Choose present mode*/
    auto mod = std::find_if(swap_chain_details.modes.begin(), swap_chain_details.modes.end(), [](const VkPresentModeKHR& it) {
        return it == VK_PRESENT_MODE_MAILBOX_KHR;
    });
    swap_chain_format.mode = mod != swap_chain_details.modes.end() ? *mod : VK_PRESENT_MODE_FIFO_KHR;

    /*Choose extent*/
    if (swap_chain_details.capabilities.currentExtent.width != std::numeric_limits<uint32_t>::max()) {
        swap_chain_format.extent = swap_chain_details.capabilities.currentExtent;
    } else {
        const uint32_t width = ANativeWindow_getWidth(window);
        const uint32_t height = ANativeWindow_getHeight(window);
        swap_chain_format.extent = {};
        swap_chain_format.extent.width = std::clamp(width, swap_chain_details.capabilities.minImageExtent.width, swap_chain_details.capabilities.maxImageExtent.width);
        swap_chain_format.extent.height = std::clamp(height, swap_chain_details.capabilities.minImageExtent.height, swap_chain_details.capabilities.maxImageExtent.height);
    }

    /*Choose min image count*/
    swap_chain_format.min_image_count = swap_chain_details.capabilities.minImageCount + 1;
    if (swap_chain_details.capabilities.maxImageCount > 0 && swap_chain_format.min_image_count > swap_chain_details.capabilities.maxImageCount) {
        swap_chain_format.min_image_count = swap_chain_details.capabilities.maxImageCount;
    }
    return swap_chain_format;
}

void VkContext::create_instance() {
    /*Create instance*/
    VkApplicationInfo applicationInfo{};
    applicationInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    applicationInfo.pApplicationName = "Hello Vulkan";
    applicationInfo.pEngineName = "No Name";
    applicationInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    applicationInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    applicationInfo.apiVersion = VK_API_VERSION_1_3;

    const std::vector<const char*> enabledLayerNames = {
//            "VK_LAYER_KHRONOS_validation"
    };
    const std::vector<const char*> enabledExtensionNames = {
            VK_KHR_SURFACE_EXTENSION_NAME,
            VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    VkInstanceCreateInfo instanceCreateInfo{};
    instanceCreateInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    instanceCreateInfo.pApplicationInfo = &applicationInfo;
    instanceCreateInfo.enabledExtensionCount = enabledExtensionNames.size();
    instanceCreateInfo.ppEnabledExtensionNames = enabledExtensionNames.data();
    instanceCreateInfo.enabledLayerCount = enabledLayerNames.size();
    instanceCreateInfo.ppEnabledLayerNames = enabledLayerNames.data();

    if (vkCreateInstance(&instanceCreateInfo, nullptr, &instance) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkInstance!");
    }
}

void VkContext::create_logic_device() {
    /*Create logic device*/
    GPU = find_GPU();
    if (GPU == nullptr) {
        throw std::runtime_error("GPU not found!");
    }

    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.queueFamilyIndex = graphics_queue_info.index;
    float priority = 1.0f;
    queueCreateInfo.pQueuePriorities = &priority;

    VkDeviceCreateInfo deviceCreateInfo{};
    VkPhysicalDeviceFeatures deviceFeatures{};
    deviceFeatures.samplerAnisotropy = VK_TRUE;
    const std::vector<const char*> enabledDeviceLayerNames = {

    };
    const std::vector<const char*> enabledDeviceExtensionNames = {
            VK_KHR_SWAPCHAIN_EXTENSION_NAME
    };
    deviceCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    deviceCreateInfo.pQueueCreateInfos = &queueCreateInfo;
    deviceCreateInfo.queueCreateInfoCount = 1;
    deviceCreateInfo.pEnabledFeatures = &deviceFeatures;
    deviceCreateInfo.enabledLayerCount = enabledDeviceLayerNames.size();
    deviceCreateInfo.ppEnabledLayerNames = enabledDeviceLayerNames.data();
    deviceCreateInfo.enabledExtensionCount = enabledDeviceExtensionNames.size();
    deviceCreateInfo.ppEnabledExtensionNames = enabledDeviceExtensionNames.data();
    if (vkCreateDevice(GPU, &deviceCreateInfo, nullptr, &dev) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkDevice!");
    }
    vkGetDeviceQueue(dev, graphics_queue_info.index, 0, &graphics_queue_info.queue);
    vkGetDeviceQueue(dev, present_queue_info.index, 0, &present_queue_info.queue);
}

void VkContext::create_swap_chain() {
    /*Create swap chain*/
    choose_swap_chain_format();
    VkSwapchainCreateInfoKHR swapchainCreateInfo{};
    swapchainCreateInfo.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    swapchainCreateInfo.surface = surface;
    swapchainCreateInfo.presentMode = swap_chain_format.mode;
    swapchainCreateInfo.minImageCount = swap_chain_format.min_image_count;
    swapchainCreateInfo.imageFormat = swap_chain_format.image_format.format;
    swapchainCreateInfo.imageColorSpace = swap_chain_format.image_format.colorSpace;
    swapchainCreateInfo.imageExtent = swap_chain_format.extent;
    swapchainCreateInfo.imageArrayLayers = 1;
    swapchainCreateInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    swapchainCreateInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    swapchainCreateInfo.queueFamilyIndexCount = 0;
    swapchainCreateInfo.pQueueFamilyIndices = nullptr;
    swapchainCreateInfo.preTransform = swap_chain_details.capabilities.currentTransform;
    swapchainCreateInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    swapchainCreateInfo.clipped = VK_TRUE;
    swapchainCreateInfo.oldSwapchain = VK_NULL_HANDLE;
    if (vkCreateSwapchainKHR(dev, &swapchainCreateInfo, nullptr, &swap_chain) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkSwapChainKHR!");
    }
}

void VkContext::create_surface() {
    /*Create surface*/
    VkAndroidSurfaceCreateInfoKHR surfaceCreateInfo{};
    surfaceCreateInfo.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surfaceCreateInfo.window = window;

    if (vkCreateAndroidSurfaceKHR(instance, &surfaceCreateInfo, nullptr, &surface) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkSurface!");
    }
}

swap_chain_format_t VkContext::get_swap_chain_format() {
    return swap_chain_format;
}

VkDevice VkContext::get_device() {
    return dev;
}

VkPhysicalDevice VkContext::get_physical_device() {
    return GPU;
}

VkSwapchainKHR VkContext::get_swap_chain() {
    return swap_chain;
}

queue_info_t VkContext::get_queue_info(const queue_type_t& type) {
    if (type == queue_type_t::GRAPHICS) {
        return graphics_queue_info;
    } else if (type == queue_type_t::PRESENT) {
        return present_queue_info;
    } else {
        throw std::invalid_argument("Unknown queue type");
    }
}
