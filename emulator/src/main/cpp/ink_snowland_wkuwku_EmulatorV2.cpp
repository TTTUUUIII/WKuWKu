#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fstream>
#include <queue>
#include <swappy/swappyGL_extra.h>
#include <swappy/swappyGL.h>
#include "GLRenderer.h"
#include "GLUtils.h"
#include "AudioOutputStream.h"
#include "ink_snowland_wkuwku_EmulatorV2.h"

#define STB_IMAGE_WRITE_IMPLEMENTATION

#include "stb_image_write.h"

#ifndef TAG
#define TAG "Fceumm_Native"
#endif

std::mutex mtx;
std::atomic<unsigned> current_state = STATE_INVALID;
static std::shared_ptr<GLRenderer> renderer = nullptr;
static retro_hw_render_callback *hw_render_cb = nullptr;
static video_state_t current_video{0, 0, 0, RETRO_PIXEL_FORMAT_RGB565};
static std::unique_ptr<buffer_t> framebuffer = nullptr;
static std::unique_ptr<buffer_t> serialize_buffer = nullptr;
static std::queue<std::unique_ptr<message_t>> message_queue;
static std::shared_ptr<AudioOutputStream> audio_stream_out;
static std::atomic<bool> env_attached = false;

#define ARRAY_SIZE(arr) sizeof(arr) / sizeof(arr[0])
typedef struct {
    JavaVM *jvm;
    jclass input_descriptor_clazz;
    jclass message_ext_clazz;
    jclass array_list_clazz;
    jobject emulator_obj;
    jmethodID message_ext_constructor;
    jmethodID input_descriptor_constructor;
    jmethodID array_list_constructor;
    jmethodID array_list_add_method;
    jmethodID environment_method;
    jmethodID video_size_cb_method;
    jmethodID input_cb_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} em_context_t;

static em_context_t ctx{};
static jobject variable_object;
static jobject variable_entry_object;
static int need_fullpath = true;
static struct retro_disk_control_callback *disk_control;
static struct retro_disk_control_ext_callback *dis_control_ext;

static void set_variable_value(JNIEnv *env, jobject value) {
    env->SetObjectField(variable_object, ctx.variable_value_field, value);
}

static void set_variable_value(JNIEnv *env, jint value) {
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_value(env, val);
}

static void set_variable_value(JNIEnv *env, const char *value) {
    set_variable_value(env, env->NewStringUTF(value));
}

static jobject get_variable_value(JNIEnv *env) {
    return env->GetObjectField(variable_object, ctx.variable_value_field);
}

static jint get_variable_int_value(JNIEnv *env) {
    jobject integer_obj = env->GetObjectField(variable_object, ctx.variable_value_field);
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID int_value_method = env->GetMethodID(clazz, "intValue", "()I");
    return env->CallIntMethod(integer_obj, int_value_method);
}

static void set_variable_entry(JNIEnv *env, const char *key, jobject value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
                        env->NewStringUTF(key));
    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
}

static void set_variable_entry(JNIEnv *env, const char *key, jint value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
                        env->NewStringUTF(key));
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_entry(env, key, val);
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
            LOGE(TAG, "%s", buffer);
            break;
        case RETRO_LOG_INFO:
            LOGI(TAG, "%s", buffer);
            break;
        case RETRO_LOG_WARN:
            LOGW(TAG, "%s", buffer);
            break;
        default:
            LOGD(TAG, "%s", buffer);
    }
}

static void video_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (current_state != STATE_RUNNING) return;
    if (!hw_render_cb && data) {
        fill_framebuffer(data, width, height, pitch);
        if (current_video.pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
            texture(GL_RGB, (int) width, (int) height, current_video.rotation, framebuffer->data);
        } else {
            texture(GL_RGBA, (int) width, (int) height, current_video.rotation, framebuffer->data);
        }
    }
    renderer->swap_buffers();
    if (current_video.rotation == 1 || current_video.rotation == 3) {
        unsigned origin_width;
        origin_width = width;
        width = height;
        height = origin_width;
    }
    if (current_video.width != width || current_video.height != height) {
        current_video.width = width;
        current_video.height = height;
        notify_video_size_changed();
    }
}

static void fill_framebuffer(const void *data, unsigned width, unsigned height, size_t pitch) {
    std::lock_guard<std::mutex> lock(mtx);
    size_t bytes_per_pixel = 2;
    if (current_video.pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
        uint8_t *fb;
        fb = (uint8_t *) data;
        for (int i = 0; i < height * pitch; i += 4) {
            std::swap(fb[i], fb[i + 2]);
            fb[i + 3] = 0xFF;
        }
    }
    size_t size = width * height * bytes_per_pixel;
    if (!framebuffer || framebuffer->size != size) {
        framebuffer = std::make_unique<buffer_t>(size);
    }
    for (int i = 0; i < height; ++i) {
        memcpy((void *) (static_cast<const char *>(framebuffer->data) +
                         i * width * bytes_per_pixel), static_cast<const char *>(data) + i * pitch,
               width * bytes_per_pixel);
    }
}

static void notify_video_size_changed() {
    JNIEnv *env;
    if (attach_env(&env)) {
        env->CallVoidMethod(ctx.emulator_obj, ctx.video_size_cb_method, current_video.width,
                            current_video.height);
        detach_env();
    }
}

static void audio_buffer_state_callback(bool active, unsigned occupancy, bool underrun_likely) {
    LOGI(TAG, "Audio buffer state: active=%d, occupancy=%d, underrun=%d", active, occupancy,
         underrun_likely);
}

static size_t audio_cb(const int16_t *data, size_t frames) {
    if (current_state == STATE_RUNNING) {
        audio_stream_out->write(data, (int) frames, 50 * kNanosPerMillisecond);
    }
    return frames;
}

static int16_t input_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    JNIEnv *env;
    if (!ctx.emulator_obj || !attach_env(&env)) return 0;
    int16_t input_state;
    input_state = (int16_t) env->CallIntMethod(ctx.emulator_obj, ctx.input_cb_method, port,
                                              device,
                                              index,
                                              id);
    detach_env();
    return input_state;
}

static void input_poll_cb() {
    /*Ignored*/
}

static bool
set_rumble_state_callback(unsigned port, enum retro_rumble_effect effect, uint16_t strength) {
    return true;
}

static bool environment_cb(unsigned cmd, void *data) {
    JNIEnv *env;
    bool supported = false;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
        case RETRO_ENVIRONMENT_GET_FASTFORWARDING:
            if (attach_env(&env)) {
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   nullptr);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback *) data;
            log_cb->log = log_print_callback;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:
            *(unsigned *) data = RETRO_HW_CONTEXT_OPENGLES3;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_ROTATION:
            current_video.rotation = *(unsigned *) data;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
            if (data != nullptr) {
                auto *audio_buffer_state = (struct retro_audio_buffer_status_callback *) data;
                audio_buffer_state->callback = audio_buffer_state_callback;
            }
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            hw_render_cb = reinterpret_cast<struct retro_hw_render_callback *>(data);
            hw_render_cb->get_proc_address = get_hw_proc_address;
            hw_render_cb->get_current_framebuffer = get_hw_framebuffer;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE:
            struct retro_rumble_interface *interface;
            interface = (struct retro_rumble_interface *) data;
            interface->set_rumble_state = set_rumble_state_callback;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY:
            if (attach_env(&env)) {
                set_variable_value(env, (jint) (*(unsigned *) data));
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if (attach_env(&env)) {
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                if (supported) {
                    auto path = (jstring) get_variable_value(env);
                    *((const char **) data) = env->GetStringUTFChars(path, JNI_FALSE);
                }
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE:
            if (attach_env(&env)) {
                jobject jmsg_ext;
                struct retro_message *msg;
                msg = (struct retro_message *) data;
                jmsg_ext = env->NewObject(ctx.message_ext_clazz,
                                          ctx.message_ext_constructor,
                                          env->NewStringUTF(msg->msg),
                                          0,
                                          RETRO_LOG_INFO,
                                          RETRO_MESSAGE_TARGET_LOG,
                                          RETRO_MESSAGE_TYPE_NOTIFICATION,
                                          -1,
                                          300);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   jmsg_ext);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
            if (attach_env(&env)) {
                jobject jmsg_ext;
                struct retro_message_ext *msg_ext;
                msg_ext = (struct retro_message_ext *) data;
                jmsg_ext = env->NewObject(ctx.message_ext_clazz,
                                          ctx.message_ext_constructor,
                                          env->NewStringUTF(msg_ext->msg),
                                          msg_ext->priority,
                                          msg_ext->level,
                                          msg_ext->target,
                                          msg_ext->type,
                                          msg_ext->progress,
                                          msg_ext->duration
                );
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   jmsg_ext);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            if (attach_env(&env)) {
                struct retro_variable *variable;
                variable = (struct retro_variable *) data;
                set_variable_entry(env, variable->key, nullptr);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_entry_object);
                if (supported) {
                    auto value = (jstring) get_variable_entry_value(env);
                    variable->value = env->GetStringUTFChars(value, JNI_FALSE);
                }
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            if (attach_env(&env)) {
                struct retro_variable *variable;
                if (data != nullptr) {
                    variable = (struct retro_variable *) data;
                    set_variable_entry(env, variable->key, env->NewStringUTF(variable->value));
                }
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_entry_object);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            if (attach_env(&env)) {
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   nullptr);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            if (attach_env(&env)) {
                set_variable_value(env, (jint) *(unsigned *) data);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION:
            *(unsigned *) data = 2;
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            current_video.pixel_format = *((retro_pixel_format *) data);
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
            if (attach_env(&env)) {
                jobject array_list;
                jclass controller_desc_clazz;
                jmethodID constructor;
                struct retro_controller_info *controller_info;
                controller_info = (struct retro_controller_info *) data;
                array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
                controller_desc_clazz = env->FindClass(
                        "ink/snowland/wkuwku/common/ControllerDescription");
                constructor = env->GetMethodID(controller_desc_clazz, "<init>",
                                               "(Ljava/lang/String;I)V");
                for (int i = 0; i < controller_info->num_types; ++i) {
                    jobject controller_desc = env->NewObject(controller_desc_clazz, constructor,
                                                             env->NewStringUTF(
                                                                     controller_info->types[i].desc),
                                                             (jint) controller_info->types[i].id);
                    env->CallVoidMethod(array_list, ctx.array_list_add_method, i, controller_desc);
                }
                env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                       array_list);
                detach_env();
            }
            supported = true;
            break;
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
        case RETRO_ENVIRONMENT_GET_VFS_INTERFACE:
        case RETRO_ENVIRONMENT_GET_LED_INTERFACE:
        case RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER:
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
        case RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO:
            break;
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            supported = true;
            break;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE:
            if (data) {
                dis_control_ext = (struct retro_disk_control_ext_callback *) data;
                supported = true;
            }
            break;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE:
            if (data) {
                disk_control = (struct retro_disk_control_callback *) data;
                supported = true;
            }
            break;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            if (attach_env(&env)) {
                set_variable_value(env, RETRO_LANGUAGE_DUMMY);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                if (supported) {
                    auto language = get_variable_int_value(env);
                    *(unsigned *) data = language;
                }
            }
            break;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
            if (attach_env(&env)) {
                jint index = 0;
                jobject array_list;
                struct retro_input_descriptor *desc;
                desc = (struct retro_input_descriptor *) data;
                array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
                while (desc->description != nullptr) {
                    jobject it = env->NewObject(
                            ctx.input_descriptor_clazz,
                            ctx.input_descriptor_constructor,
                            desc->port,
                            desc->device,
                            desc->index,
                            desc->id,
                            env->NewStringUTF(desc->description));
                    env->CallVoidMethod(array_list, ctx.array_list_add_method, index, it);
                    desc++;
                    index++;
                }
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   array_list);
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            if (attach_env(&env)) {
                set_variable_value(env, 0);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                if (supported) {
                    jint val = get_variable_int_value(env);
                    *(int *) data = val;
                }
                detach_env();
            }
            break;
        case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
            if (attach_env(&env)) {
                set_variable_value(env, 0);
                supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                   variable_object);
                if (supported) {
                    jint version = (jint) get_variable_int_value(env);
                    *(int *) data = version;
                }
                detach_env();
            }
            break;
        default:
            LOGW(TAG, "Environment: %d ignored.", cmd);
            return false;
    }
    return supported;
}

static bool initialized = false;

static void em_attach_surface(JNIEnv *env, jobject thiz, _Nullable jobject activity, jobject surface) {
    LOGD(TAG, "em_attach_surface called.");
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (activity != nullptr && !SwappyGL_isEnabled()) {
        SwappyGL_init(env, activity);
        SwappyGL_setSwapIntervalNS(SWAPPY_SWAP_60FPS);
        SwappyGL_setAutoSwapInterval(true);
        SwappyGL_setAutoPipelineMode(true);
        SwappyGL_setWindow(window);
    }
    renderer = std::make_unique<GLRenderer>(window);
    GLRendererInterface *interface = renderer->get_renderer_interface();
    interface->on_create = on_create;
    interface->on_draw = on_draw;
    interface->on_destroy = on_destroy;
    if (current_state == STATE_RUNNING) {
        renderer->start();
    }
}

static void em_adjust_surface(JNIEnv *env, jobject thiz, jint vw, int vh) {
    LOGD(TAG, "em_adjust_surface called.");
    renderer->adjust_viewport(vw, vh);
}

static void em_detach_surface(JNIEnv *env, jobject thiz) {
    LOGD(TAG, "em_detach_surface called.");
    renderer->stop();
}

static jboolean em_start(JNIEnv *env, jobject thiz, jstring path) {
    LOGD(TAG, "em_start called.");
    if (ctx.emulator_obj == nullptr) {
        ctx.emulator_obj = env->NewGlobalRef(thiz);
    }
    if (current_state == INVALID) {
        retro_set_environment(environment_cb);
        retro_init();
        retro_set_video_refresh(video_cb);
        retro_set_audio_sample_batch(audio_cb);
        retro_set_input_state(input_cb);
        retro_set_input_poll(input_poll_cb);
        current_state = STATE_IDLE;
    }
    const char *rom_path = env->GetStringUTFChars(path, JNI_FALSE);
    struct retro_system_info system_info{};
    retro_get_system_info(&system_info);
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
        current_state = STATE_RUNNING;
        open_audio_stream();
        if (renderer) {
            renderer->start();
        }
    }
    env->ReleaseStringUTFChars(path, rom_path);
    return no_error;
}

static void em_pause(JNIEnv *env, jobject thiz) {
    LOGD(TAG, "em_pause called.");
    if (current_state == STATE_RUNNING) {
        current_state = STATE_PAUSED;
        audio_stream_out->request_pause();
    }
}

static void em_resume(JNIEnv *env, jobject thiz) {
    LOGD(TAG, "em_resume called.");
    if (current_state == STATE_PAUSED) {
        current_state = STATE_RUNNING;
        audio_stream_out->request_start();
    }
}

static void em_reset(JNIEnv *env, jobject thiz) {
    LOGD(TAG, "em_reset called.");
    message_queue.push(std::make_unique<message_t>(MSG_RESET_EMULATOR));
}

static void em_stop(JNIEnv *env, jobject thiz) {
    LOGD(TAG, "em_stop called.");
    current_state = STATE_IDLE;
    retro_unload_game();
    close_audio_stream();
    while (!message_queue.empty()) {
        message_queue.pop();
    }
}

static jobject em_get_system_av_info(JNIEnv *env, jobject thiz) {
    struct retro_system_av_info av_info = {0};
    retro_get_system_av_info(&av_info);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemTiming");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(DD)V");
    jobject o0 = env->NewObject(clazz, constructor, av_info.timing.fps,
                                av_info.timing.sample_rate);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
    jobject o1 = env->NewObject(clazz, constructor, (jint) av_info.geometry.base_width,
                                (jint) av_info.geometry.base_height,
                                (jint) av_info.geometry.max_width,
                                (jint) av_info.geometry.max_height, av_info.geometry.aspect_ratio);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemAvInfo");
    constructor = env->GetMethodID(clazz, "<init>",
                                   "(Link/snowland/wkuwku/common/EmGameGeometry;Link/snowland/wkuwku/common/EmSystemTiming;)V");
    return env->NewObject(clazz, constructor, o1, o0);
}

static jobject em_get_system_info(JNIEnv *env, jobject thiz) {
    struct retro_system_info system_info = {};
    retro_get_system_info(&system_info);
    need_fullpath = system_info.need_fullpath;
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
    size_t size;
    size = retro_serialize_size();
    if (size == 0) return nullptr;
    if (!serialize_buffer || serialize_buffer->size != size) {
        serialize_buffer = std::make_unique<buffer_t>(size);
    }
    serialize_buffer->state = BS_W;
    message_queue.push(std::make_unique<message_t>(MSG_GET_SERIALIZE_DATA));
    while (!(serialize_buffer->state & BS_R)) {}
    jbyteArray snapshot = env->NewByteArray((jint) size);
    env->SetByteArrayRegion(snapshot, 0, (jint) size,
                            reinterpret_cast<jbyte *>(serialize_buffer->data));
    return snapshot;
};

static void em_set_serialize_data(JNIEnv *env, jobject thiz, jbyteArray jdata) {
    const size_t &size = retro_serialize_size();
    if (env->GetArrayLength(jdata) == size) {
        jbyte *data = env->GetByteArrayElements(jdata, JNI_FALSE);
        if (!serialize_buffer || serialize_buffer->size != size) {
            serialize_buffer = std::make_unique<buffer_t>(size);
        }
        if (serialize_buffer->state & BS_W) {
            memcpy(serialize_buffer->data, data, size);
            serialize_buffer->state = BS_R;
            message_queue.push(std::make_unique<message_t>(MSG_SET_SERIALIZE_DATA));
        }
        env->ReleaseByteArrayElements(jdata, data, JNI_ABORT);
    }
}

static jbyteArray em_get_memory_data(JNIEnv *env, jobject thiz, jint id) {
    size_t len = retro_get_memory_size(id);
    if (len == 0) return nullptr;
    void *data = retro_get_memory_data(id);
    if (data) {
        jbyteArray mem_data = env->NewByteArray((jint) len);
        env->SetByteArrayRegion(mem_data, 0, (jint) len, reinterpret_cast<jbyte *>(data));
        return mem_data;
    }
    return nullptr;
};

static void em_set_memory_data(JNIEnv *env, jobject thiz, jint id, jbyteArray mem_data) {
    const size_t &len = retro_get_memory_size(id);
    if (env->GetArrayLength(mem_data) == len) {
        jbyte *data = env->GetByteArrayElements(mem_data, JNI_FALSE);
        void *mem = retro_get_memory_data(id);
        memcpy(mem, data, sizeof(jbyte) * len);
        env->ReleaseByteArrayElements(mem_data, data, 0);
    }
}

static jboolean em_capture_screen(JNIEnv *env, jobject thiz, jstring path) {
    bool no_error = true;
    std::lock_guard<std::mutex> lock(mtx);
    if (framebuffer) {
        const char *file_path = env->GetStringUTFChars(path, JNI_FALSE);
        if (current_video.pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
            stbi_write_png(file_path, current_video.width, current_video.height, 4, framebuffer->data,
                           current_video.width * 4);
        } else if (current_video.pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
            unsigned char data[current_video.width * current_video.height * 3];
            auto *origin = reinterpret_cast<uint16_t *>(framebuffer->data);
            for (int i = 0; i < current_video.width * current_video.height; ++i) {
                uint16_t pixel = origin[i];
                data[i * 3 + 0] = ((pixel >> 11) & 0x1F) << 3;
                data[i * 3 + 1] = ((pixel >> 5) & 0x3F) << 2;
                data[i * 3 + 2] = (pixel & 0x1F) << 3;
            }
            stbi_write_png(file_path, current_video.width, current_video.height, 3, data,
                           current_video.width * 3);
        } else {
            no_error = false;
        }
    } else {
        no_error = false;
    }
    return no_error;
}

static void em_set_controller_port_device(JNIEnv *env, jobject thiz, jint port, jint device) {
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
        {"nativeCaptureScreen",           "(Ljava/lang/String;)Z",                           (void *) em_capture_screen}
};

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
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
#ifndef EM_CLASS
    clazz = env->FindClass("ink/snowland/wkuwku/emulator/Fceumm");
#else
    clazz = env->FindClass(EM_CLASS);
#endif
    ctx.video_size_cb_method = env->GetMethodID(clazz, "onNativeVideoSizeChanged", "(II)V");
    ctx.environment_method = env->GetMethodID(clazz, "onNativeEnvironment",
                                              "(ILjava/lang/Object;)Z");
    ctx.input_cb_method = env->GetMethodID(clazz, "onNativePollInput", "(IIII)I");
    env->RegisterNatives(clazz, methods, ARRAY_SIZE(methods));
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(ctx.input_descriptor_clazz);
    env->DeleteGlobalRef(ctx.array_list_clazz);
    env->DeleteGlobalRef(ctx.message_ext_clazz);
    if (ctx.emulator_obj) {
        env->DeleteGlobalRef(ctx.emulator_obj);
        ctx.emulator_obj = nullptr;
    }
    framebuffer = nullptr;
    serialize_buffer = nullptr;
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    if (SwappyGL_isEnabled())
        SwappyGL_destroy();
    ctx.jvm = nullptr;
}

static void on_create(EGLDisplay dyp, EGLSurface sr) {
    if (hw_render_cb) {
        hw_render_cb->context_reset();
    } else {
        begin_texture();
    }
}

static void on_draw() {
    if (current_state == RUNNING) {
        retro_run();
        handle_message();
    }
}

static void on_destroy() {
    if (hw_render_cb != nullptr) {
        hw_render_cb->context_destroy();
    } else {
        end_texture();
    }
}

static retro_proc_address_t get_hw_proc_address(const char *sym) {
    return eglGetProcAddress(sym);
}

static uintptr_t get_hw_framebuffer() {
    GLint fb;
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &fb);
    return fb;
}

static void handle_message() {
    if (message_queue.empty()) return;
    std::unique_ptr<message_t> &msg = message_queue.front();
    bool no_error;
    switch (msg->what) {
        case MSG_SET_SERIALIZE_DATA:
            if (serialize_buffer->state & BS_R) {
                no_error = retro_unserialize(serialize_buffer->data, serialize_buffer->size);
                if (!no_error) {
                    LOGE(TAG, "Failed to unserialize data!");
                }
                serialize_buffer->state = BS_RW;
            }
            break;
        case MSG_GET_SERIALIZE_DATA:
            if (serialize_buffer->state & BS_W) {
                retro_serialize(serialize_buffer->data, serialize_buffer->size);
                serialize_buffer->state = BS_RW;
            }
            break;
        case MSG_RESET_EMULATOR:
            retro_reset();
            break;
        default:
    }
    message_queue.pop();
}

static bool attach_env(JNIEnv **env) {
    bool no_error = true;
    if (ctx.jvm->GetEnv((void **) env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(env, nullptr) != JNI_OK) {
            LOGE(TAG, "Unable attach env thread!");
            no_error = false;
        } else {
            env_attached = true;
        }
    }
    return no_error;
}

static void detach_env() {
    if (env_attached) {
        ctx.jvm->DetachCurrentThread();
        env_attached = false;
    }
}

static void open_audio_stream() {
    struct retro_system_av_info av_info{};
    retro_get_system_av_info(&av_info);
    audio_stream_out = std::make_shared<AudioOutputStream>(av_info.timing.sample_rate);
    audio_stream_out->request_open();
    audio_stream_out->request_start();
}

static void close_audio_stream() {
    audio_stream_out = nullptr;
}