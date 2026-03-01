//
// Created by 86187 on 2026/2/6.
//

#include "VkContext.h"

#include <utility>

VkContext::VkContext(std::string _name, ANativeWindow *_window): window(_window), name(std::move(_name)) {
    create_instance();
    create_surface();
    GPU = choose_device();
}

VkContext::~VkContext() {
    vkDestroySurfaceKHR(instance, surface, nullptr);
    vkDestroyInstance(instance, nullptr);
}

VkPhysicalDevice VkContext::choose_device() {
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
                 || !choose_queue_families(gpu)) {
        return false;
    }
    query_surface_details(gpu);
    if (surface_details.formats.empty() || surface_details.modes.empty()) {
        return false;
    }
    return true;
}

bool VkContext::choose_queue_families(VkPhysicalDevice gpu) {
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

void VkContext::query_surface_details(VkPhysicalDevice gpu) {
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu, surface, &surface_details.capabilities);
    uint32_t count = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(gpu, surface, &count, nullptr);
    surface_details.formats.resize(count);
    vkGetPhysicalDeviceSurfaceFormatsKHR(gpu, surface, &count, surface_details.formats.data());
    vkGetPhysicalDeviceSurfacePresentModesKHR(gpu, surface, &count, nullptr);
    surface_details.modes.resize(count);
    vkGetPhysicalDeviceSurfacePresentModesKHR(gpu, surface, &count, surface_details.modes.data());
}

swap_chain_format_t VkContext::choose_swap_chain_format() {
    swap_chain_format_t format{};
    /*Choose surface format*/
    auto fmt = std::find_if(surface_details.formats.begin(), surface_details.formats.end(), [](const VkSurfaceFormatKHR& it){
        return it.format == VK_FORMAT_B8G8R8A8_SRGB && it.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    });
    format.image_format = fmt != surface_details.formats.end() ? *fmt : surface_details.formats[0];

    /*Choose present mode*/
    auto mod = std::find_if(surface_details.modes.begin(), surface_details.modes.end(), [](const VkPresentModeKHR& it) {
        return it == VK_PRESENT_MODE_MAILBOX_KHR;
    });
    format.mode = mod != surface_details.modes.end() ? *mod : VK_PRESENT_MODE_FIFO_KHR;

    /*Choose extent*/
    if (surface_details.capabilities.currentExtent.width != std::numeric_limits<uint32_t>::max()) {
        format.extent = surface_details.capabilities.currentExtent;
    } else {
        const uint32_t width = ANativeWindow_getWidth(window);
        const uint32_t height = ANativeWindow_getHeight(window);
        format.extent = {};
        format.extent.width = std::clamp(width, surface_details.capabilities.minImageExtent.width, surface_details.capabilities.maxImageExtent.width);
        format.extent.height = std::clamp(height, surface_details.capabilities.minImageExtent.height, surface_details.capabilities.maxImageExtent.height);
    }

    /*Choose min image count*/
    format.min_image_count = surface_details.capabilities.minImageCount + 1;
    if (surface_details.capabilities.maxImageCount > 0 && format.min_image_count > surface_details.capabilities.maxImageCount) {
        format.min_image_count = surface_details.capabilities.maxImageCount;
    }
    return format;
}

void VkContext::create_instance() {
    /*Create instance*/
    VkApplicationInfo applicationInfo{};
    applicationInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    applicationInfo.pApplicationName = name.c_str();
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

void VkContext::create_logic_device(const std::vector<const char*>& enabled_extensions, VkDevice &dev) {
    /*Create logic device*/
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
//    deviceFeatures.samplerAnisotropy = VK_TRUE;
    const std::vector<const char*> enabledDeviceLayerNames = {};
    deviceCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    deviceCreateInfo.pQueueCreateInfos = &queueCreateInfo;
    deviceCreateInfo.queueCreateInfoCount = 1;
    deviceCreateInfo.pEnabledFeatures = &deviceFeatures;
    deviceCreateInfo.enabledLayerCount = enabledDeviceLayerNames.size();
    deviceCreateInfo.ppEnabledLayerNames = enabledDeviceLayerNames.data();
    deviceCreateInfo.enabledExtensionCount = enabled_extensions.size();
    deviceCreateInfo.ppEnabledExtensionNames = enabled_extensions.data();
    if (vkCreateDevice(GPU, &deviceCreateInfo, nullptr, &dev) != VK_SUCCESS) {
        throw std::runtime_error("Unable to create vkDevice!");
    }
    vkGetDeviceQueue(dev, graphics_queue_info.index, 0, &graphics_queue_info.queue);
    vkGetDeviceQueue(dev, present_queue_info.index, 0, &present_queue_info.queue);
}
#include "Log.h"
void VkContext::create_swap_chain(VkDevice &dev, VkSwapchainKHR &chain, swap_chain_format_t& format, const VkSwapchainKHR &old_chain) {
    /*Create swap chain*/
    format = choose_swap_chain_format();
    VkSwapchainCreateInfoKHR createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    createInfo.surface = surface;
    createInfo.presentMode = format.mode;
    createInfo.minImageCount = format.min_image_count;
    createInfo.imageFormat = format.image_format.format;
    createInfo.imageColorSpace = format.image_format.colorSpace;
    createInfo.imageExtent = format.extent;
    createInfo.imageArrayLayers = 1;
    createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    createInfo.queueFamilyIndexCount = 0;
    createInfo.pQueueFamilyIndices = nullptr;
    createInfo.preTransform = surface_details.capabilities.currentTransform;
    createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    createInfo.clipped = VK_TRUE;
    createInfo.oldSwapchain = old_chain;
    if (vkCreateSwapchainKHR(dev, &createInfo, nullptr, &chain) != VK_SUCCESS) {
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

VkPhysicalDevice VkContext::get_device() {
    return GPU;
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

VkSurfaceTransformFlagBitsKHR VkContext::get_surface_transform() {
    return surface_details.capabilities.currentTransform;
}