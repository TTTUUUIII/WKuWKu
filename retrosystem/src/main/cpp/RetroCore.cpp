//
// Created by wn123 on 2025-06-26.
//

#include <dlfcn.h>
#include "Log.h"
#include "RetroCore.h"

RetroCore::RetroCore(const std::string alias, const std::string &lib, bool* status) {
    handle = dlopen(lib.c_str(), RTLD_LOCAL | RTLD_LAZY);
    if (!handle) {
        *status = false;
        return;
    }
    this->alias = std::string(alias);
    set_environment_cb = reinterpret_cast<decltype(set_environment_cb)>(dlsym(handle, "retro_set_environment"));
    set_video_refresh_cb = reinterpret_cast<decltype(set_video_refresh_cb)>(dlsym(handle, "retro_set_video_refresh"));
    set_audio_sample_cb = reinterpret_cast<decltype(set_audio_sample_cb)>(dlsym(handle, "retro_set_audio_sample"));
    set_audio_sample_batch_cb = reinterpret_cast<decltype(set_audio_sample_batch_cb)>(dlsym(handle, "retro_set_audio_sample_batch"));
    set_input_poll_cb = reinterpret_cast<decltype(set_input_poll_cb)>(dlsym(handle, "retro_set_input_poll"));
    set_input_state_cb = reinterpret_cast<decltype(set_input_state_cb)>(dlsym(handle, "retro_set_input_state"));
    init = reinterpret_cast<decltype(init)>(dlsym(handle, "retro_init"));
    deinit = reinterpret_cast<decltype(deinit)>(dlsym(handle, "retro_deinit"));
    get_api_version = reinterpret_cast<decltype(get_api_version)>(dlsym(handle, "retro_api_version"));
    get_system_info = reinterpret_cast<decltype(get_system_info)>(dlsym(handle, "retro_get_system_info"));
    get_system_av_info = reinterpret_cast<decltype(get_system_av_info)>(dlsym(handle, "retro_get_system_av_info"));
    set_controller_port_device = reinterpret_cast<decltype(set_controller_port_device)>(dlsym(handle, "retro_set_controller_port_device"));
    reset = reinterpret_cast<decltype(reset)>(dlsym(handle, "retro_reset"));
    run = reinterpret_cast<decltype(run)>(dlsym(handle, "retro_run"));
    get_serialize_size = reinterpret_cast<decltype(get_serialize_size)>(dlsym(handle, "retro_serialize_size"));
    get_serialize_data = reinterpret_cast<decltype(get_serialize_data)>(dlsym(handle, "retro_serialize"));
    set_serialize_data = reinterpret_cast<decltype(set_serialize_data)>(dlsym(handle, "retro_unserialize"));
    reset_cheat = reinterpret_cast<decltype(reset_cheat)>(dlsym(handle, "retro_cheat_reset"));
    set_cheat = reinterpret_cast<decltype(set_cheat)>(dlsym(handle, "retro_cheat_set"));
    load_game = reinterpret_cast<decltype(load_game)>(dlsym(handle, "retro_load_game"));
    unload_game = reinterpret_cast<decltype(unload_game)>(dlsym(handle, "retro_unload_game"));
    load_game_special = reinterpret_cast<decltype(load_game_special)>(dlsym(handle, "retro_load_game_special"));
    unload_game = reinterpret_cast<decltype(unload_game)>(dlsym(handle, "retro_unload_game"));
    get_region = reinterpret_cast<decltype(get_region)>(dlsym(handle, "retro_get_region"));
    get_memory_data = reinterpret_cast<decltype(get_memory_data)>(dlsym(handle, "retro_get_memory_data"));
    get_memory_size = reinterpret_cast<decltype(get_memory_size)>(dlsym(handle, "retro_get_memory_size"));
    *status = true;
    LOGI(__FILE_NAME__, "[LOADED] => %s", lib.c_str());
}

RetroCore::~RetroCore() {
    if (handle) {
        dlclose(handle);
        set_environment_cb          = nullptr;
        set_video_refresh_cb        = nullptr;
        set_audio_sample_cb         = nullptr;
        set_audio_sample_batch_cb   = nullptr;
        set_input_poll_cb           = nullptr;
        set_input_state_cb          = nullptr;
        init                        = nullptr;
        deinit                      = nullptr;
        get_api_version             = nullptr;
        get_system_info             = nullptr;
        get_system_av_info          = nullptr;
        set_controller_port_device  = nullptr;
        reset                       = nullptr;
        run                         = nullptr;
        get_serialize_size          = nullptr;
        get_serialize_data          = nullptr;
        set_serialize_data          = nullptr;
        reset_cheat                 = nullptr;
        set_cheat                   = nullptr;
        load_game                   = nullptr;
        unload_game                 = nullptr;
        load_game_special           = nullptr;
        unload_game                 = nullptr;
        get_region                  = nullptr;
        get_memory_data             = nullptr;
        get_memory_size             = nullptr;
    }
}
