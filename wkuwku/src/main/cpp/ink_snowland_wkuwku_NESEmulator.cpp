#include <jni.h>
#include <libretro/libretro.h>
#include <string>
#include <fstream>
#include "log.h"

#define TAG "FceummEmulator_Native"
#define ARRAY_SIZE(arr) sizeof(arr) / sizeof(arr[0])

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("wkuwku");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("wkuwku")
//      }
//    }
typedef struct {
    JavaVM *jvm;
    jclass input_descriptor_clazz;
    jclass array_list_clazz;
    jobject fceumm_obj;
    jmethodID input_descriptor_constructor;
    jmethodID array_list_constructor;
    jmethodID array_list_add_method;
    jmethodID environment_method;
    jmethodID video_refresh_method;
    jmethodID audio_sample_batch_method;
    jmethodID input_state_method;
    jmethodID input_poll_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} JAVA_INTERFACE;

static JAVA_INTERFACE ctx = {nullptr};
static jobject variable_object;
static jobject variable_entry_object;

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

static void set_variable_entry(JNIEnv *env, const char* key , jobject value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field, env->NewStringUTF(key));
    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
}

static void set_variable_entry(JNIEnv *env, const char* key , jint value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field, env->NewStringUTF(key));
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_entry(env, key, val);
}

static jobject get_variable_entry_value(JNIEnv *env) {
    return env->GetObjectField(variable_entry_object, ctx.variable_entry_value_field);
}

static void log_print_callback(enum retro_log_level level, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    switch (level) {
        case RETRO_LOG_ERROR:
            LOGE(TAG, fmt, args);
            break;
        case RETRO_LOG_INFO:
            LOGI(TAG, fmt, args);
            break;
        case RETRO_LOG_WARN:
            LOGW(TAG, fmt, args);
            break;
        default:
            LOGD(TAG, fmt, args);
    }
    va_end(args);
}

static bool environment_callback(unsigned cmd, void *data) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return false;
    }
    switch (cmd)
    {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *(bool*)data = env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, nullptr);
            break;
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MEMORY_MAPS");
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            return env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            return env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *variable;
            variable = (struct retro_variable*)data;
            set_variable_entry(env, variable->key, nullptr);
            env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, variable_entry_object);
            jstring value = (jstring) get_variable_entry_value(env);
            variable->value = env->GetStringUTFChars(value, JNI_FALSE);
            }
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_LOG_INTERFACE");
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback*) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            set_variable_value(env, *((jint *)data));
            return env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, variable_object);
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            set_variable_value(env, "sss");
            env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, variable_object);
            jstring path = (jstring) get_variable_value(env);
            *((const char**)data) = env->GetStringUTFChars(path, JNI_FALSE);
            }
            break;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
            jint index = 0;
            struct retro_input_descriptor* desc;
            desc = (struct retro_input_descriptor*) data;
            jobject array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
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
            env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method,cmd, array_list);
            }
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
        case RETRO_ENVIRONMENT_SET_HW_RENDER | RETRO_ENVIRONMENT_EXPERIMENTAL:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MESSAGE_EXT");
            break;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_HW_RENDER");
            break;
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
//            LOGD(TAG, "RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            break;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
            break;
        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_GET_LANGUAGE: {
            set_variable_value(env, RETRO_LANGUAGE_DUMMY);
            env->CallBooleanMethod(ctx.fceumm_obj, ctx.environment_method, cmd, variable_object);
            jobject language = (jobject) get_variable_value(env);
            jclass integer_clazz = env->FindClass("java/lang/Integer");
            jmethodID int_value_method = env->GetMethodID(integer_clazz, "intValue", "()I");
            *(unsigned*) data = (unsigned int) env->CallIntMethod(language, int_value_method);
        }
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK");
            break;
        default:
//            LOGW(TAG, "WARN: cmd %d ignored!", cmd);
            return false;
    }
    return true;
}

static void video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    jbyteArray framebuffer = env->NewByteArray(height * pitch);
    env->SetByteArrayRegion(framebuffer, 0, height * pitch, (jbyte*) data);
    env->CallVoidMethod(ctx.fceumm_obj, ctx.video_refresh_method, framebuffer, (jint) width, (jint) height, (jint) pitch);
}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return frames;
    }
    jshortArray samples = env->NewShortArray(frames * 2);
    env->SetShortArrayRegion(samples, 0, frames * 2, data);
    env->CallVoidMethod(ctx.fceumm_obj, ctx.audio_sample_batch_method, samples, frames);
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return 0;
    }
    auto state = (int16_t) env->CallIntMethod(ctx.fceumm_obj, ctx.input_state_method, port, device, index,
                                          id);
    return state;
}

static void input_poll_callback()
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    env->CallVoidMethod(ctx.fceumm_obj, ctx.input_poll_method);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeReset(JNIEnv *env, jobject thiz) {
    retro_reset();
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativePowerOn(JNIEnv *env, jobject thiz) {
    ctx.fceumm_obj = env->NewGlobalRef(thiz);
    retro_set_environment(environment_callback);
    retro_init();
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_state(input_state_callback);
    retro_set_input_poll(input_poll_callback);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativePowerOff(JNIEnv *env, jobject thiz) {
    retro_deinit();
    env->DeleteGlobalRef(ctx.fceumm_obj);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeLoad(JNIEnv *env, jobject thiz, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    struct retro_game_info info = { path, nullptr, 0, nullptr };
    bool state = retro_load_game(&info);
    env->ReleaseStringUTFChars(jpath, path);
    return state;
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeRun(JNIEnv *env, jobject thiz) {
    retro_run();
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: jni load failed!");
        return JNI_ERR;
    }
    ctx.jvm = vm;
    jclass clazz = env->FindClass("ink/snowland/wkuwku/emulator/Fceumm");
    ctx.video_refresh_method = env->GetMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetMethodID(clazz, "onInputPoll", "()V");
    clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
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
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(ctx.fceumm_obj);
    env->DeleteGlobalRef(ctx.input_descriptor_clazz);
    env->DeleteGlobalRef(ctx.array_list_clazz);
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    ctx.jvm = nullptr;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeGetSystemAvInfo(JNIEnv *env, jobject thiz) {
    struct retro_system_av_info av_info = {0};
    retro_get_system_av_info(&av_info);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemTiming");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(DD)V");
    jobject o0 = env->NewObject(clazz, constructor, av_info.timing.fps,
                                av_info.timing.sample_rate);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
    jobject o1 = env->NewObject(clazz, constructor, (jint) av_info.geometry.base_width,
                                (jint) av_info.geometry.base_height, (jint) av_info.geometry.max_width,
                                (jint) av_info.geometry.max_height, av_info.geometry.aspect_ratio);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemAvInfo");
    constructor = env->GetMethodID(clazz, "<init>", "(Link/snowland/wkuwku/common/EmGameGeometry;Link/snowland/wkuwku/common/EmSystemTiming;)V");
    return env->NewObject(clazz, constructor, o1, o0);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeGetSystemInfo(JNIEnv *env, jobject thiz) {
    struct retro_system_info system_info = {};
    retro_get_system_info(&system_info);
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
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeSaveMemoryRam(JNIEnv *env, jobject thiz, jstring path) {
    size_t len = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    if (len == 0) return false;
    void *mem = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (mem == nullptr) return false;
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ofstream fp(_path, std::ios::out | std::ios::binary);
    if (!fp.is_open()) return false;
    fp.write(reinterpret_cast<char*>(mem), (int) len).flush();
    fp.close();
    env->ReleaseStringUTFChars(path, _path);
    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeSaveState(JNIEnv *env, jobject thiz, jstring path) {
    size_t len = retro_serialize_size();
    if (len == 0) return false;
    void* mem = calloc(1, len);
    if (mem == nullptr) return false;
    if (!retro_serialize(mem, len)) {
        free(mem);
        return false;
    }
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ofstream fp(_path, std::ios::out | std::ios::binary);
    if (!fp.is_open()) {
        free(mem);
        env->ReleaseStringUTFChars(path, _path);
        return false;
    }
    env->ReleaseStringUTFChars(path, _path);
    fp.write(reinterpret_cast<char*>(mem), (int) len).flush();
    fp.close();
    
    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeLoadMemoryRam(JNIEnv *env, jobject thiz, jstring path) {
    bool no_error = false;
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ifstream fp(_path, std::ios::in|std::ios::binary);
    if (fp.is_open()) {
        size_t len = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
        void *mem = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
        if (mem != nullptr && len != 0) {
            fp.read(reinterpret_cast<char*>(mem), (int) len);
            no_error = true;
        }
        fp.close();
    }
    env->ReleaseStringUTFChars(path, _path);
    return no_error;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeLoadState(JNIEnv *env, jobject thiz, jstring path) {
    bool no_error = false;
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    size_t len = retro_serialize_size();
    std::ifstream fp(_path);
    if (fp.is_open() && len != 0) {
        void *mem = calloc(1, len);
        if (mem != nullptr) {
            fp.read(static_cast<char *>(mem), (int) len);
            retro_unserialize(mem, len);
            free(mem);
            no_error = true;
        }
        fp.close();
    }
    return no_error;
}