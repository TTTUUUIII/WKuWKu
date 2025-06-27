//
// Created by wn123 on 2025-06-26.
//

#ifndef WKUWKU_RETROCORE_H
#define WKUWKU_RETROCORE_H

#include <string>
#include "libretro.h"

class RetroCore {
private:
    void* handle;
public:
    decltype(&retro_set_environment) set_environment_cb;
    decltype(&retro_set_video_refresh) set_video_refresh_cb;
    decltype(&retro_set_audio_sample) set_audio_sample_cb;
    decltype(&retro_set_audio_sample_batch) set_audio_sample_batch_cb;
    decltype(&retro_set_input_poll) set_input_poll_cb;
    decltype(&retro_set_input_state) set_input_state_cb;
    decltype(&retro_init) init;
    decltype(&retro_deinit) deinit;
    decltype(&retro_api_version) get_api_version;
    decltype(&retro_get_system_info) get_system_info;
    decltype(&retro_get_system_av_info) get_system_av_info;
    decltype(&retro_set_controller_port_device) set_controller_port_device;
    decltype(&retro_reset) reset;
    decltype(&retro_run) run;
    decltype(&retro_serialize_size) get_serialize_size;
    decltype(&retro_serialize) get_serialize_data;
    decltype(&retro_unserialize) set_serialize_data;
    decltype(&retro_cheat_reset) reset_cheat;
    decltype(&retro_cheat_set) set_cheat;
    decltype(&retro_load_game) load_game;
    decltype(&retro_load_game_special) load_game_special;
    decltype(&retro_unload_game) unload_game;
    decltype(&retro_get_region) get_region;
    decltype(&retro_get_memory_data) get_memory_data;
    decltype(&retro_get_memory_size) get_memory_size;
    explicit RetroCore(const std::string &lib, bool *status);
    ~RetroCore();
};


#endif //WKUWKU_RETROCORE_H
