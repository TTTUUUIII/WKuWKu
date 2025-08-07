#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fstream>
#include <sys/resource.h>
#include <unordered_map>
#include <queue>
#include <swappy/swappyGL_extra.h>
#include <swappy/swappyGL.h>
#include "GLRenderer.h"
#include "GLUtils.h"
#include "AudioOutputStream.h"
#include "ink_snowland_wkuwku_EmulatorV2.h"

#define STB_IMAGE_WRITE_IMPLEMENTATION

#include "stb_image_write.h"

#define TAG "EmulatorV2"

static std::mutex mtx;
static std::condition_variable looper_cv;
static retro_system_info system_info{};
static retro_system_av_info system_av_info{};
static std::atomic<unsigned> current_state = STATE_INVALID;
static std::shared_ptr<GLRenderer> renderer = nullptr;
static GLuint hw_texture, hw_fbo, hw_rbo;
static retro_hw_render_callback *hw_render_cb = nullptr;
static jshortArray audio_buffer = nullptr;
static buffer_t *framebuffers[2];
static std::atomic<int> draw_index = 0;
static rotation_t video_rotation = ROTATION_0;
static uint16_t video_width = 0;
static uint16_t video_height = 0;
static retro_pixel_format origin_pixel_format = RETRO_PIXEL_FORMAT_RGB565;
static retro_pixel_format pixel_format = RETRO_PIXEL_FORMAT_RGB565;
static std::queue<std::shared_ptr<message_t>> message_queue;
static std::unordered_map<int32_t, std::any> props;
static std::shared_ptr<AudioOutputStream> audio_stream_out;
static bool env_attached = false;

static em_context_t ctx{};
static std::unique_ptr<GLContext> shared_context;
static jobject variable_object;
static jobject variable_entry_object;
static struct retro_disk_control_callback *disk_control;
static struct retro_disk_control_ext_callback *dis_control_ext;

static void set_variable_value(JNIEnv *env, jobject value) {
    env->SetObjectField(variable_object, ctx.variable_value_field, value);
}

static jobject get_variable_value(JNIEnv *env) {
    return env->GetObjectField(variable_object, ctx.variable_value_field);
}

static void set_variable_entry(JNIEnv *env, const char *key, jobject value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
                        env->NewStringUTF(key));
    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
}

static jobject get_variable_entry_value(JNIEnv *env) {
    return env->GetObjectField(variable_entry_object, ctx.variable_entry_value_field);
}

static void log_print_callback(enum retro_log_level level, const char *fmt, ...) {
    char buffer[512];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);
    switch (level) {
        case RETRO_LOG_ERROR:
            LOGE(system_info.library_name, "%s", buffer);
            break;
        case RETRO_LOG_INFO:
            LOGI(system_info.library_name, "%s", buffer);
            break;
        case RETRO_LOG_WARN:
            LOGW(system_info.library_name, "%s", buffer);
            break;
        default:
            LOGD(system_info.library_name, "%s", buffer);
    }
}

static void video_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (current_state != STATE_RUNNING) return;
    if (!hw_render_cb && data) {
        if (pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
            XRGB8888_PATCH((void*)data, height * pitch);
        } else if (origin_pixel_format == RETRO_PIXEL_FORMAT_0RGB1555) {
            XRGB1555_TO_RGB565((void*)data, height * pitch);
        }
        fill_frame_buffer(data, width, height, pitch);
    }
    if (video_width != width || video_height != height) {
        video_width = width;
        video_height = height;
        notify_video_size_changed();
    }
}

static void alloc_frame_buffers() {
    retro_system_av_info av_info{};
    uint16_t bytes_per_pixels;
    if (pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixels = 4;
    } else {
        bytes_per_pixels = 2;
    }
    retro_get_system_av_info(&av_info);
    framebuffers[0] = new buffer_t(
            av_info.geometry.max_width * av_info.geometry.max_height * bytes_per_pixels);
    framebuffers[1] = new buffer_t(
            av_info.geometry.max_width * av_info.geometry.max_height * bytes_per_pixels);
    draw_index.store(0);
    LOGD(TAG, "Alloc frame buffers %zu bytes * 2.", framebuffers[0]->capacity);
}

static void free_frame_buffers() {
    delete framebuffers[0];
    delete framebuffers[1];
    framebuffers[0] = nullptr;
    framebuffers[1] = nullptr;
    LOGD(TAG, "Free frame buffers.");
}

static void fill_frame_buffer(const void *data, unsigned width, unsigned height, size_t pitch) {
    uint8_t bytes_per_pixel = 2;
    if (pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
    }

    int write_index = 1 - draw_index.load();
    if (width * bytes_per_pixel == pitch) {
        memcpy(framebuffers[write_index]->data, data, height * pitch);
    } else {
        for (int i = 0; i < height; ++i) {
            memcpy((void *) (static_cast<const char *>(framebuffers[write_index]->data) +
                             i * width * bytes_per_pixel),
                   static_cast<const char *>(data) + i * pitch,
                   width * bytes_per_pixel);
        }
    }
    draw_index.store(write_index);
}

static void notify_video_size_changed() {
    ctx.env->CallVoidMethod(ctx.emulator_obj, ctx.video_size_cb_method, video_width, video_height,
                            video_rotation);
}

static void audio_buffer_state_cb(bool active, unsigned occupancy, bool underrun_likely) {
    LOGI(TAG, "Audio buffer state: active=%d, occupancy=%d, underrun=%d", active, occupancy,
         underrun_likely);
}

static size_t audio_cb(const int16_t *data, size_t frames) {
    if (data && current_state == STATE_RUNNING) {
        if (audio_stream_out) {
            return audio_stream_out->write(data, (int) frames, 20 * kNanosPerMillisecond);
        } else {
            if (ctx.env->GetArrayLength(audio_buffer) < frames * 2) {
                ctx.env->DeleteGlobalRef(audio_buffer);
                audio_buffer = (jshortArray) ctx.env->NewGlobalRef(
                        ctx.env->NewShortArray((int) frames * 2));
            }
            ctx.env->SetShortArrayRegion(audio_buffer, 0, (int) frames * 2, data);
            return ctx.env->CallIntMethod(ctx.emulator_obj, ctx.audio_buffer_method, audio_buffer,
                                          frames);
        }
    }
    return frames;
}

static int16_t input_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (!ctx.emulator_obj) return 0;
    int16_t input_state;
    input_state = (int16_t) ctx.env->CallIntMethod(ctx.emulator_obj, ctx.input_cb_method, port,
                                                   device,
                                                   index,
                                                   id);
    return input_state;
}

static void input_poll_cb() {
    /*Ignored*/
}

static bool rumble_state_cb(unsigned port, enum retro_rumble_effect effect, uint16_t strength) {
    LOGW(TAG, "Rumble state event ignored, port=%d, effect=%d, strength=%d", port, effect,
         strength);
    return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.rumble_cb_method, port, effect,
                                      strength);
}

static bool environment_cb(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
        case RETRO_ENVIRONMENT_GET_FASTFORWARDING:
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              nullptr);
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback *) data;
            log_cb->log = log_print_callback;
            return true;
        case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:
            *(unsigned *) data = RETRO_HW_CONTEXT_OPENGLES3;
            return true;
        case RETRO_ENVIRONMENT_SET_ROTATION:
            video_rotation = *static_cast<rotation_t *>(data);
            return true;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
            if (data) {
                auto *audio_buffer_state = (struct retro_audio_buffer_status_callback *) data;
                audio_buffer_state->callback = audio_buffer_state_cb;
            }
            return true;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            hw_render_cb = reinterpret_cast<struct retro_hw_render_callback *>(data);
            if (hw_render_cb->context_type == RETRO_HW_CONTEXT_OPENGLES2
                || hw_render_cb->context_type == RETRO_HW_CONTEXT_OPENGLES3) {
                hw_render_cb->get_proc_address = get_hw_proc_address;
                hw_render_cb->get_current_framebuffer = get_hw_framebuffer;
                LOGI(TAG, "Hardware render attached, type=%d", hw_render_cb->context_type);
                return true;
            }
            break;
        case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE:
            struct retro_rumble_interface *interface;
            interface = (struct retro_rumble_interface *) data;
            interface->set_rumble_state = rumble_state_cb;
            return true;
        case RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY:
            set_variable_value(ctx.env, new_int(ctx.env, (jint) (*(unsigned *) data)));
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              variable_object);
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            std::string path = get_prop(static_cast<int>(cmd), std::string());
            if (!path.empty()) {
                *(const char **)data = path.c_str();
                return true;
            } else {
                return false;
            }
        }
        case RETRO_ENVIRONMENT_SET_MESSAGE:
            jobject jmsg_ext;
            struct retro_message *msg;
            msg = (struct retro_message *) data;
            jmsg_ext = ctx.env->NewObject(ctx.message_ext_clazz,
                                          ctx.message_ext_constructor,
                                          ctx.env->NewStringUTF(msg->msg),
                                          0,
                                          RETRO_LOG_INFO,
                                          RETRO_MESSAGE_TARGET_LOG,
                                          RETRO_MESSAGE_TYPE_NOTIFICATION,
                                          -1,
                                          300);
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              jmsg_ext);
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
            struct retro_message_ext *msg_ext;
            msg_ext = (struct retro_message_ext *) data;
            jmsg_ext = ctx.env->NewObject(ctx.message_ext_clazz,
                                          ctx.message_ext_constructor,
                                          ctx.env->NewStringUTF(msg_ext->msg),
                                          msg_ext->priority,
                                          msg_ext->level,
                                          msg_ext->target,
                                          msg_ext->type,
                                          msg_ext->progress,
                                          msg_ext->duration
            );
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              jmsg_ext);
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            struct retro_variable *variable;
            variable = (struct retro_variable *) data;
            set_variable_entry(ctx.env, variable->key, nullptr);
            if (ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                           variable_entry_object)) {
                auto value = (jstring) get_variable_entry_value(ctx.env);
                variable->value = ctx.env->GetStringUTFChars(value, JNI_FALSE);
                return true;
            }
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            if (data != nullptr) {
                variable = (struct retro_variable *) data;
                set_variable_entry(ctx.env, variable->key, ctx.env->NewStringUTF(variable->value));
            }
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              variable_entry_object);
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              nullptr);
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
        case RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION:
            *(unsigned *) data = 1;
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            origin_pixel_format = *((retro_pixel_format *) data);
            if (origin_pixel_format == RETRO_PIXEL_FORMAT_0RGB1555) {
                pixel_format = RETRO_PIXEL_FORMAT_RGB565;
            } else {
                pixel_format = origin_pixel_format;
            }
            return true;
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
            jobject array_list;
            jclass controller_desc_clazz;
            jmethodID constructor;
            struct retro_controller_info *controller_info;
            controller_info = (struct retro_controller_info *) data;
            array_list = ctx.env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
            controller_desc_clazz = ctx.env->FindClass(
                    "ink/snowland/wkuwku/common/ControllerDescription");
            constructor = ctx.env->GetMethodID(controller_desc_clazz, "<init>",
                                               "(Ljava/lang/String;I)V");
            for (int i = 0; i < controller_info->num_types; ++i) {
                jobject controller_desc = ctx.env->NewObject(controller_desc_clazz, constructor,
                                                             ctx.env->NewStringUTF(
                                                                     controller_info->types[i].desc),
                                                             (int) controller_info->types[i].id);
                ctx.env->CallVoidMethod(array_list, ctx.array_list_add_method, i, controller_desc);
            }
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              array_list);
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
        case RETRO_ENVIRONMENT_GET_VFS_INTERFACE:
        case RETRO_ENVIRONMENT_GET_LED_INTERFACE:
        case RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER:
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
        case RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO:
        case RETRO_ENVIRONMENT_GET_PERF_INTERFACE:
            break;
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
            return true;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE:
            if (data) {
                dis_control_ext = (struct retro_disk_control_ext_callback *) data;
            }
            return true;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE:
            if (data) {
                disk_control = (struct retro_disk_control_callback *) data;
            }
            return true;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            *(unsigned *) data = RETRO_LANGUAGE_ENGLISH;
            return true;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
            jint index = 0;
            struct retro_input_descriptor *desc;
            desc = (struct retro_input_descriptor *) data;
            array_list = ctx.env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
            while (desc->description != nullptr) {
                jobject it = ctx.env->NewObject(
                        ctx.input_descriptor_clazz,
                        ctx.input_descriptor_constructor,
                        desc->port,
                        desc->device,
                        desc->index,
                        desc->id,
                        ctx.env->NewStringUTF(desc->description));
                ctx.env->CallVoidMethod(array_list, ctx.array_list_add_method, index, it);
                desc++;
                index++;
            }
            return ctx.env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              array_list);
        }
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            *(unsigned *) data =
                    RETRO_AV_ENABLE_AUDIO | RETRO_AV_ENABLE_VIDEO | RETRO_AV_ENABLE_FAST_SAVESTATES;
            return true;
        case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
            *(unsigned *) data = 1;
            return true;
        default:
            LOGW(TAG, "Environment: %d ignored.", cmd);
    }
    return false;
}

static void
em_attach_surface(JNIEnv *env, jobject thiz, _Nullable jobject activity, jobject surface) {
    UNUSED(thiz);
    renderer = std::make_unique<GLRenderer>(env, activity, surface);
    renderer->set_renderer_callback(std::make_unique<renderer_callback_t<EGLDisplay, EGLSurface >>(
            on_surface_create,
            on_draw_frame,
            on_surface_destroy));
    send_empty_message(MSG_START_RENDERER);
}

static void em_adjust_surface(JNIEnv *env, jobject thiz, jint vw, int vh) {
    UNUSED(env);
    UNUSED(thiz);
    renderer->adjust_viewport(vw, vh);
}

static void em_detach_surface(JNIEnv *env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    renderer->release();
}

static jboolean em_start(JNIEnv *env, jobject thiz, jstring path) {
    if (ctx.emulator_obj == nullptr) {
        ctx.emulator_obj = env->NewGlobalRef(thiz);
    }
    ctx.env = env;
    if (current_state == STATE_INVALID) {
        retro_set_environment(environment_cb);
        retro_init();
        retro_set_video_refresh(video_cb);
        retro_set_audio_sample_batch(audio_cb);
        retro_set_input_state(input_cb);
        retro_set_input_poll(input_poll_cb);
        current_state = STATE_IDLE;
    }
    const char *rom_path = env->GetStringUTFChars(path, JNI_FALSE);
    struct retro_game_info info{rom_path, nullptr, 0, nullptr};
    if (!system_info.need_fullpath) {
        int fd = open(rom_path, O_RDONLY);
        if (fd == -1) return false;
        struct stat sb = {0};
        if (fstat(fd, &sb) == -1) {
            close(fd);
            return false;
        }
        info.size = sb.st_size;
        info.data = mmap(nullptr, sb.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    }
    bool no_error = retro_load_game(&info);
    if (info.data != nullptr) {
        munmap((void *) info.data, info.size);
    }
    if (no_error) {
        retro_get_system_av_info(&system_av_info);
        current_state = STATE_RUNNING;
        if (get_prop(PROP_OBOE_ENABLED, true)) {
            open_audio_stream();
        } else {
            audio_buffer = (jshortArray) env->NewGlobalRef(env->NewShortArray(0));
        }
        if (!hw_render_cb) {
            alloc_frame_buffers();
        }
        std::thread main_thread(entry_main_loop);
        main_thread.detach();
    } else {
        LOGE(TAG, "Unable load the game, file=%s", rom_path);
    }
    env->ReleaseStringUTFChars(path, rom_path);
    return no_error;
}

static void em_pause(JNIEnv *env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    if (current_state == STATE_RUNNING) {
        current_state = STATE_PAUSED;
        if (audio_stream_out) {
            audio_stream_out->request_pause();
        }
    }
    if (renderer) {
        renderer->request_pause();
    }
}

static void em_resume(JNIEnv *env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    if (current_state == STATE_PAUSED) {
        current_state = STATE_RUNNING;
        if (audio_stream_out) {
            audio_stream_out->request_start();
        }
        looper_cv.notify_one();
    }
    if (renderer) {
        renderer->request_resume();
    }
}

static void em_reset(JNIEnv *env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    send_empty_message(MSG_RESET_EMULATOR);
}

static void em_stop(JNIEnv *env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    current_state = STATE_IDLE;
    LOGI(TAG, "Killing main loop...");
    send_message(MSG_KILL, nullptr)->get_future().get();
    ctx.env = env;
    retro_unload_game();
#ifdef DEINIT_AFTER_UNLOAD
    retro_deinit();
    current_state = STATE_INVALID;
#endif
    clear_message();
    close_audio_stream();
    video_width = 0;
    video_height = 0;
    video_rotation = ROTATION_0;
    free_frame_buffers();
    if (audio_buffer) {
        env->DeleteGlobalRef(audio_buffer);
        audio_buffer = nullptr;
    }
}

static jobject em_get_system_av_info(JNIEnv *env, jobject thiz) {
    UNUSED(thiz);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemTiming");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(DD)V");
    jobject o0 = env->NewObject(clazz, constructor, system_av_info.timing.fps,
                                system_av_info.timing.sample_rate);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
    jobject o1 = env->NewObject(clazz, constructor, (int) system_av_info.geometry.base_width,
                                (int) system_av_info.geometry.base_height,
                                (int) system_av_info.geometry.max_width,
                                (int) system_av_info.geometry.max_height, system_av_info.geometry.aspect_ratio);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemAvInfo");
    constructor = env->GetMethodID(clazz, "<init>",
                                   "(Link/snowland/wkuwku/common/EmGameGeometry;Link/snowland/wkuwku/common/EmSystemTiming;)V");
    return env->NewObject(clazz, constructor, o1, o0);
}

static jobject em_get_system_info(JNIEnv *env, jobject thiz) {
    UNUSED(thiz);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemInfo");
    jmethodID constructor = env->GetMethodID(clazz, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    jobject obj = env->NewObject(clazz, constructor, env->NewStringUTF(system_info.library_name),
                                 env->NewStringUTF(system_info.library_version),
                                 env->NewStringUTF(system_info.valid_extensions));
    jfieldID need_full_path_filed = env->GetFieldID(clazz, "needFullpath", "Z");
    env->SetBooleanField(obj, need_full_path_filed, system_info.need_fullpath);
    jfieldID block_extract = env->GetFieldID(clazz, "blockExtract", "Z");
    env->SetBooleanField(obj, block_extract, system_info.block_extract);
    return obj;
}

static jbyteArray em_get_serialize_data(JNIEnv *env, jobject thiz) {
    UNUSED(thiz);
    std::shared_ptr<std::promise<result_t>> promise = send_message(MSG_GET_SERIALIZE_DATA, nullptr);
    const result_t &result = promise->get_future().get();
    if (result.state == NO_ERROR) {
        auto buffer = std::any_cast<std::shared_ptr<buffer_t>>(result.data);
        jbyteArray data = env->NewByteArray((jint) buffer->capacity);
        env->SetByteArrayRegion(data, 0, (jint) buffer->capacity,
                                reinterpret_cast<jbyte *>(buffer->data));
        return data;
    } else {
        LOGE(TAG, "Failed to get serialize data.");
    }
    return nullptr;
}

static void em_set_serialize_data(JNIEnv *env, jobject thiz, jbyteArray jdata) {
    UNUSED(thiz);
    const size_t size = env->GetArrayLength(jdata);
    jbyte *data = env->GetByteArrayElements(jdata, JNI_FALSE);
    std::shared_ptr<buffer_t> buffer = std::make_shared<buffer_t>(size);
    memcpy(buffer->data, data, buffer->capacity);
    env->ReleaseByteArrayElements(jdata, data, JNI_ABORT);
    send_message(MSG_SET_SERIALIZE_DATA, buffer);
}

static jbyteArray em_get_memory_data(JNIEnv *env, jobject thiz, jint id) {
    UNUSED(thiz);
    size_t len = retro_get_memory_size(id);
    if (len == 0) return nullptr;
    void *data = retro_get_memory_data(id);
    if (data) {
        jbyteArray mem_data = env->NewByteArray((jint) len);
        env->SetByteArrayRegion(mem_data, 0, (jint) len, reinterpret_cast<jbyte *>(data));
        return mem_data;
    }
    return nullptr;
}

static void em_set_memory_data(JNIEnv *env, jobject thiz, jint id, jbyteArray mem_data) {
    UNUSED(thiz);
    const size_t &len = retro_get_memory_size(id);
    if (env->GetArrayLength(mem_data) == len) {
        jbyte *data = env->GetByteArrayElements(mem_data, JNI_FALSE);
        void *mem = retro_get_memory_data(id);
        memcpy(mem, data, sizeof(jbyte) * len);
        env->ReleaseByteArrayElements(mem_data, data, 0);
    }
}

static void em_set_prop(JNIEnv *env, jobject thiz, jint prop, jobject val) {
    UNUSED(thiz);
    switch (prop) {
        case PROP_OBOE_ENABLED:
        case PROP_LOW_LATENCY_AUDIO_ENABLE:
        case PROP_AUDIO_UNDERRUN_OPTIMIZATION:
            props[prop] = as_bool(env, val);
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
            props[prop] = as_string(env, (jstring)val);
            break;
        default:;
    }
}

static jboolean em_capture_screen(JNIEnv *env, jobject thiz, jstring path) {
    UNUSED(thiz);
    bool no_error = false;
    if (current_state == STATE_RUNNING || current_state == STATE_PAUSED) {
        const char *file_path = env->GetStringUTFChars(path, JNI_FALSE);
        if (hw_render_cb) {
            std::shared_ptr<std::promise<result_t>> promise = send_message(MSG_READ_PIXELS,
                                                                                  nullptr);
            result_t result = promise->get_future().get();
            if (result.state == NO_ERROR) {
                auto pixels = std::any_cast<std::shared_ptr<buffer_t>>(result.data);
                no_error = stbi_write_png(file_path, video_width, video_height, 4,
                                          pixels->data,
                                          video_width * 4);
            }
        } else {
            if (pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
                no_error = stbi_write_png(file_path, video_width, video_height, 4,
                                          framebuffers[draw_index]->data,
                                          video_width * 4);
            } else if (pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
                std::unique_ptr<buffer_t> buffer = RGB565_TO_RGB888(framebuffers[draw_index]->data, video_width * video_height * 2);
                no_error = stbi_write_png(file_path, video_width, video_height, 3, buffer->data,
                                          video_width * 3);
            }
        }
        env->ReleaseStringUTFChars(path, file_path);
    }
    return no_error;
}

static void em_set_controller_port_device(JNIEnv *env, jobject thiz, jint port, jint device) {
    UNUSED(env);
    UNUSED(thiz);
    retro_set_controller_port_device(port, device);
}

static const JNINativeMethod methods[] = {
        {"nativeAttachSurface",           "(Landroid/app/Activity;Landroid/view/Surface;)V", (void *) em_attach_surface},
        {"nativeAdjustSurface",           "(II)V",                                           (void *) em_adjust_surface},
        {"nativeDetachSurface",           "()V",                                             (void *) em_detach_surface},
        {"nativeStart",                   "(Ljava/lang/String;)Z",                           (void *) em_start},
        {"nativePause",                   "()V",                                             (void *) em_pause},
        {"nativeResume",                  "()V",                                             (void *) em_resume},
        {"nativeReset",                   "()V",                                             (void *) em_reset},
        {"nativeStop",                    "()V",                                             (void *) em_stop},
        {"nativeGetSerializeData",        "()[B",                                            (void *) em_get_serialize_data},
        {"nativeSetSerializeData",        "([B)V",                                           (void *) em_set_serialize_data},
        {"nativeGetSystemInfo",           "()Link/snowland/wkuwku/common/EmSystemInfo;",
                                                                                             (void *) em_get_system_info},
        {"nativeGetSystemAvInfo",         "()Link/snowland/wkuwku/common/EmSystemAvInfo;",
                                                                                             (void *) em_get_system_av_info},
        {"nativeGetMemoryData",           "(I)[B",                                           (void *) em_get_memory_data},
        {"nativeSetMemoryData",           "(I[B)V",                                          (void *) em_set_memory_data},
        {"nativeSetControllerPortDevice", "(II)V",                                           (void *) em_set_controller_port_device},
        {"nativeCaptureScreen",           "(Ljava/lang/String;)Z",                           (void *) em_capture_screen},
        {"nativeSetProp",                 "(ILjava/lang/Object;)V",                          (void *) em_set_prop}
};

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    UNUSED(reserved);
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "JNI load failed!");
        return JNI_ERR;
    }
    ctx.jvm = vm;
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
    ctx.variable_value_field = env->GetFieldID(clazz, "value", "Ljava/lang/Object;");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    variable_object = env->NewGlobalRef(env->NewObject(clazz, constructor));
    clazz = env->FindClass("ink/snowland/wkuwku/common/VariableEntry");
    ctx.variable_entry_value_field = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
    ctx.variable_entry_key_field = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
    constructor = env->GetMethodID(clazz, "<init>", "()V");
    variable_entry_object = env->NewGlobalRef(env->NewObject(clazz, constructor));
    clazz = env->FindClass("ink/snowland/wkuwku/common/InputDescriptor");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIILjava/lang/String;)V");
    ctx.input_descriptor_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.input_descriptor_constructor = constructor;
    clazz = env->FindClass("java/util/ArrayList");
    constructor = env->GetMethodID(clazz, "<init>", "()V");
    ctx.array_list_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.array_list_constructor = constructor;
    ctx.array_list_add_method = env->GetMethodID(clazz, "add", "(ILjava/lang/Object;)V");
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmMessageExt");
    constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;IIIIII)V");
    ctx.message_ext_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.message_ext_constructor = constructor;
#ifndef MAIN_CLASS
    clazz = env->FindClass("ink/snowland/wkuwku/emulator/Fceumm");
#else
    clazz = env->FindClass(MAIN_CLASS);
#endif
    ctx.audio_buffer_method = env->GetMethodID(clazz, "onNativeAudioBuffer", "([SI)I");
    ctx.video_size_cb_method = env->GetMethodID(clazz, "onNativeVideoSizeChanged", "(III)V");
    ctx.environment_method = env->GetMethodID(clazz, "onNativeEnvironment",
                                              "(ILjava/lang/Object;)Z");
    ctx.input_cb_method = env->GetMethodID(clazz, "onNativePollInput", "(IIII)I");
    ctx.rumble_cb_method = env->GetMethodID(clazz, "onNativeRumbleState", "(III)Z");
    env->RegisterNatives(clazz, methods, ARRAY_SIZE(methods));
    retro_get_system_info(&system_info);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    UNUSED(reserved);
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
#ifndef DEINIT_AFTER_UNLOAD
    retro_deinit();
#endif
    env->DeleteGlobalRef(ctx.input_descriptor_clazz);
    env->DeleteGlobalRef(ctx.array_list_clazz);
    env->DeleteGlobalRef(ctx.message_ext_clazz);
    if (ctx.emulator_obj) {
        env->DeleteGlobalRef(ctx.emulator_obj);
        ctx.emulator_obj = nullptr;
    }
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    if (SwappyGL_isEnabled())
        SwappyGL_destroy();
    ctx.jvm = nullptr;
}

static void entry_main_loop() {
    if (!attach_env()) {
        LOGE(TAG, "Unable start main thread, attach env failed!");
        return;
    }
    set_thread_priority(THREAD_PRIORITY_AUDIO);
    if (hw_render_cb) {
        struct retro_system_av_info av_info{};
        retro_get_system_av_info(&av_info);
        int max_width = static_cast<int>(av_info.geometry.max_width);
        int max_height = static_cast<int>(av_info.geometry.max_height);
        shared_context = std::make_unique<GLContext>(max_width, max_height);
        shared_context->make();
        glGenTextures(1, &hw_texture);
        glBindTexture(GL_TEXTURE_2D, hw_texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, max_width, max_height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                     nullptr);

        glGenFramebuffers(1, &hw_fbo);
        glBindFramebuffer(GL_FRAMEBUFFER, hw_fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, hw_texture, 0);
        glGenRenderbuffers(1, &hw_rbo);
        glBindRenderbuffer(GL_RENDERBUFFER, hw_rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, max_width, max_height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, hw_rbo);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            __android_log_assert("glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE", TAG, "Failed to create framebuffer!");
        }
        hw_render_cb->context_reset();
    }
    for (;;) {
        std::shared_ptr<message_t> msg = obtain_message();
        if (current_state == STATE_RUNNING) {
            retro_run();
        } else if (current_state == STATE_PAUSED && !msg) {
            std::unique_lock<std::mutex> lock(mtx);
            looper_cv.wait(lock, []() {
                return current_state != STATE_PAUSED
                       || !message_queue.empty();
            });
        }
        if (handle_message(msg)) continue;
        if (msg->what == MSG_KILL) {
            if (hw_render_cb) {
                hw_render_cb->context_destroy();
                glDeleteRenderbuffers(1, &hw_rbo);
                glDeleteFramebuffers(1, &hw_fbo);
                glDeleteTextures(1, &hw_texture);
                shared_context = nullptr;
            }
            msg->promise->set_value({NO_ERROR, nullptr});
            break;
        }
    }
    detach_env();
}

static void on_surface_create(EGLDisplay dyp, EGLSurface sr) {
    UNUSED(dyp);
    UNUSED(sr);
    set_thread_priority(THREAD_PRIORITY_DISPLAY);
    begin_texture(pixel_format,
                  static_cast<int>(system_av_info.geometry.max_width),
                  static_cast<int>(system_av_info.geometry.max_height),
                  video_rotation, !hw_render_cb);
}

static void on_draw_frame() {
    if (current_state == STATE_RUNNING) {
        if (shared_context) {
            texture_hw(video_width,
                    video_height,
                    hw_texture);
        } else {
            int index = draw_index.load();
            texture(video_width,
                    video_height,
                    framebuffers[index]->data);
        }
        renderer->swap_buffers();
    }
}

static void on_surface_destroy() {
    end_texture();
}

static retro_proc_address_t get_hw_proc_address(const char *sym) {
    return eglGetProcAddress(sym);
}

static uintptr_t get_hw_framebuffer() {
    return hw_fbo;
}

static bool attach_env() {
    bool no_error = false;
    jint state = ctx.jvm->GetEnv((void **) &ctx.env, JNI_VERSION_1_6);
    if (state == JNI_OK
        || (state == JNI_EDETACHED && ctx.jvm->AttachCurrentThread(&ctx.env, nullptr) == JNI_OK)) {
        if (state == JNI_EDETACHED) {
            env_attached = true;
        }
        LOGD(TAG, "Attached to env thread! tid=%d", gettid());
        no_error = true;
    } else {
        LOGE(TAG, "Failed to attach env thread! state=%d", state);
    }
    return no_error;
}

static void detach_env() {
    if (env_attached) {
        ctx.jvm->DetachCurrentThread();
        env_attached = false;
    }
    ctx.env = nullptr;
    LOGD(TAG, "Detached from env thread! tid=%d", gettid());
}

static void open_audio_stream() {
    audio_stream_out = std::make_shared<AudioOutputStream>();
    audio_stream_out->set_sample_rate(static_cast<uint16_t>(system_av_info.timing.sample_rate));
    audio_stream_out->set_sharing_mode(oboe::SharingMode::Shared);
    audio_stream_out->set_channel_count(oboe::ChannelCount::Stereo);
    if (get_prop(PROP_LOW_LATENCY_AUDIO_ENABLE, true)) {
        audio_stream_out->set_performance_mode(oboe::PerformanceMode::LowLatency);
    }
    if (get_prop(PROP_AUDIO_UNDERRUN_OPTIMIZATION, true)) {
        audio_stream_out->set_check_underrun(true);
    }
    audio_stream_out->request_open();
    audio_stream_out->request_start();
}

static void close_audio_stream() {
    audio_stream_out = nullptr;
}

static std::shared_ptr<std::promise<result_t>> send_message(int what, const std::any &usr) {
    std::shared_ptr<std::promise<result_t>> promise = std::make_shared<std::promise<result_t>>();
    message_queue.push(std::make_shared<message_t>(what, promise, usr));
    looper_cv.notify_one();
    return promise;
}

static void send_empty_message(int what) {
    std::lock_guard<std::mutex> lock(mtx);
    message_queue.push(std::make_shared<message_t>(what, nullptr, nullptr));
    looper_cv.notify_one();
}

static void XRGB8888_PATCH(void* data, size_t len) {
    auto *fb = (uint8_t *) data;
    for (int i = 0; i < len; i += 4) {
        std::swap(fb[i], fb[i + 2]);
        fb[i + 3] = 0xFF;
    }
}

static void XRGB1555_TO_RGB565(void* data, size_t len) {
    auto *fb = (uint16_t *) data;
    for (int i = 0; i < len / 2; i++) {
        uint16_t g = fb[i] >> 5 & 0x1F;
        fb[i] = ((fb[i] >> 10 & 0x1F) << 11) | ((g << 1 | g >> 4) << 5) | fb[i] & 0x1F;
    }
}

static std::unique_ptr<buffer_t> RGB565_TO_RGB888(void* pixels, size_t len) {
    std::unique_ptr<buffer_t> buffer = std::make_unique<buffer_t>(len / 2 * 3);
    auto dest = static_cast<uint8_t*>(buffer->data);
    auto *data = reinterpret_cast<uint16_t *>(pixels);
    for (int i = 0; i < video_width * video_height; ++i) {
        uint16_t pixel = data[i];
        dest[i * 3 + 0] = ((pixel >> 11) & 0x1F) << 3;
        dest[i * 3 + 1] = ((pixel >> 5) & 0x3F) << 2;
        dest[i * 3 + 2] = (pixel & 0x1F) << 3;
    }
    return std::move(buffer);
}

static bool handle_message(const std::shared_ptr<message_t> &msg) {
    if (!msg) return true;
    size_t size;
    bool handled = true;
    bool no_error = false;
    std::shared_ptr<buffer_t> buffer;
    switch (msg->what) {
        case MSG_START_RENDERER:
            renderer->set_shared_context(shared_context);
            renderer->request_start();
            break;
        case MSG_SET_SERIALIZE_DATA:
            size = retro_serialize_size();
            buffer = std::any_cast<std::shared_ptr<buffer_t>>(msg->usr);
            if (size > 0 && size == buffer->capacity) {
                retro_unserialize(buffer->data, size);
            }
            break;
        case MSG_GET_SERIALIZE_DATA:
            size = retro_serialize_size();
            if (size > 0) {
                buffer = std::make_shared<buffer_t>(size);
                no_error = retro_serialize(buffer->data, buffer->capacity);
                if (no_error) {
                    msg->promise->set_value({NO_ERROR, buffer});
                }
            }
            if (!no_error) {
                msg->promise->set_value({ERROR, nullptr});
            }
            break;
        case MSG_RESET_EMULATOR:
            retro_reset();
            break;
        case MSG_READ_PIXELS:
            buffer = std::make_shared<buffer_t>(video_width * video_height * 4);
            glBindFramebuffer(GL_FRAMEBUFFER, hw_fbo);
            glReadPixels(0, 0, video_width, video_height, GL_RGBA, GL_UNSIGNED_BYTE, buffer->data);
            msg->promise->set_value({NO_ERROR, buffer});
            break;
        default:
            handled = false;
    }
    return handled;
}

static std::shared_ptr<message_t> obtain_message() {
    std::lock_guard<std::mutex> lock(mtx);
    if (message_queue.empty()) return nullptr;
    std::shared_ptr<message_t> result = std::move(message_queue.front());
    message_queue.pop();
    return result;
}

static void clear_message() {
    std::lock_guard<std::mutex> lock(mtx);
    while (!message_queue.empty())
        message_queue.pop();
}

static void set_thread_priority(int priority) {
    pid_t tid = gettid();
    setpriority(PRIO_PROCESS, tid, priority);
    LOGI(TAG, "Set thread priority tid=%d, priority=%d", tid, priority);
}

template<typename T>
static T get_prop(int32_t prop, const T &default_value) {
    if (props.count(prop)) {
        return std::any_cast<T>(props[prop]);
    }
    return default_value;
}

static jobject new_int(JNIEnv *env, int32_t value) {
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID method = env->GetMethodID(clazz, "<init>", "(I)V");
    return env->NewObject(clazz, method, value);
}

static std::string as_string(JNIEnv *env, jobject obj) {
    jclass clazz = env->FindClass("java/lang/String");
    jstring src = nullptr;
    if (env->IsInstanceOf(obj, clazz)) {
        src = (jstring) obj;
        goto to_string;
    }
    clazz = env->FindClass("java/io/File");
    if (env->IsInstanceOf(obj, clazz)) {
        jmethodID method = env->GetMethodID(clazz, "getAbsolutePath", "()Ljava/lang/String;");
        src = (jstring) env->CallObjectMethod(obj, method);
        goto to_string;
    }
    clazz = env->FindClass("java/nio/file/Path");
    if (env->IsInstanceOf(obj, clazz)) {
        jmethodID method = env->GetMethodID(clazz, "toString", "()Ljava/lang/String;");
        src = (jstring) env->CallObjectMethod(obj, method);
        goto to_string;
    }
    __android_log_assert("obj type != string", TAG, "The object is not a string type!");

    to_string:
    const char *chars = env->GetStringUTFChars(src, JNI_FALSE);
    std::string res(chars);
    env->ReleaseStringUTFChars(src, chars);
    return res;
}

static int32_t as_int(JNIEnv *env, jobject obj) {
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID method = env->GetMethodID(clazz, "intValue", "()I");
    return env->CallIntMethod(obj, method);
}

static bool as_bool(JNIEnv *env, jobject obj) {
    jclass clazz = env->FindClass("java/lang/Boolean");
    jmethodID method = env->GetMethodID(clazz, "booleanValue", "()Z");
    return env->CallBooleanMethod(obj, method);
}