#include <jni.h>
#include <libretro/libretro.h>
#include <string>
#include "log.h"
#include "common.h"

#define TAG "Fceumm"

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

em_context_t ctx = {nullptr};
jobject variable_object;
jobject variable_entry_object;

//static void set_variable_value(JNIEnv *env, jobject value) {
//    env->SetObjectField(variable_object, ctx.variable_value_field, value);
//}
//
//static void set_variable_value(JNIEnv *env, jint value) {
//    jclass clazz = env->FindClass("java/lang/Integer");
//    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
//    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
//    set_variable_value(env, val);
//}
//
//static void set_variable_value(JNIEnv *env, const char *value) {
//    set_variable_value(env, env->NewStringUTF(value));
//}
//
//static jobject get_variable_value(JNIEnv *env) {
//    return env->GetObjectField(variable_object, ctx.variable_value_field);
//}
//
//static void set_variable_entry(JNIEnv *env, const char *key, jobject value) {
//    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
//                        env->NewStringUTF(key));
//    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
//}
//
//static void set_variable_entry(JNIEnv *env, const char *key, jint value) {
//    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
//                        env->NewStringUTF(key));
//    jclass clazz = env->FindClass("java/lang/Integer");
//    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
//    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
//    set_variable_entry(env, key, val);
//}
//
//static jobject get_variable_entry_value(JNIEnv *env) {
//    return env->GetObjectField(variable_entry_object, ctx.variable_entry_value_field);
//}
//
//static void log_print_callback(enum retro_log_level level, const char *fmt, ...) {
//    char buffer[512];  // 缓冲区存储格式化日志
//    va_list args;
//    va_start(args, fmt);
//    vsnprintf(buffer, sizeof(buffer), fmt, args);
//    va_end(args);
//    switch (level) {
//        case RETRO_LOG_ERROR:
//            LOGE(TAG, "%s", buffer);
//            break;
//        case RETRO_LOG_INFO:
//            LOGI(TAG, "%s", buffer);
//            break;
//        case RETRO_LOG_WARN:
//            LOGW(TAG, "%s", buffer);
//            break;
//        default:
//            LOGD(TAG, "%s", buffer);
//    }
//}
//
//static bool environment_callback(unsigned cmd, void *data) {
//    JNIEnv *env;
//    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
//        LOGE(TAG, "ERROR: unable attach env thread!");
//        return false;
//    }
//
//    switch (cmd) {
//        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
//            *(bool *) data = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
//                                                    nullptr);
//            break;
//        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
////            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MEMORY_MAPS");
//            break;
//        case RETRO_ENVIRONMENT_SET_VARIABLE:
//            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
//            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
//        case RETRO_ENVIRONMENT_GET_VARIABLE: {
//            struct retro_variable *variable;
//            variable = (struct retro_variable *) data;
//            set_variable_entry(env, variable->key, nullptr);
//            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
//                                   variable_entry_object);
//            auto value = (jstring) get_variable_entry_value(env);
//            variable->value = env->GetStringUTFChars(value, JNI_FALSE);
//        }
//            break;
//        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
//            struct retro_log_callback *log_cb;
//            log_cb = (struct retro_log_callback *) data;
//            log_cb->log = log_print_callback;
//            break;
//        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
//            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
//        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
//            set_variable_value(env, *((jint *) data));
//            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
//                                          variable_object);
//        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
//            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
//            auto path = (jstring) get_variable_value(env);
//            *((const char **) data) = env->GetStringUTFChars(path, JNI_FALSE);
//        }
//            break;
//        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
//            jint index = 0;
//            struct retro_input_descriptor *desc;
//            desc = (struct retro_input_descriptor *) data;
//            jobject array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
//            while (desc->description != nullptr) {
//                jobject it = env->NewObject(
//                        ctx.input_descriptor_clazz,
//                        ctx.input_descriptor_constructor,
//                        desc->port,
//                        desc->device,
//                        desc->index,
//                        desc->id,
//                        env->NewStringUTF(desc->description));
//                env->CallVoidMethod(array_list, ctx.array_list_add_method, index, it);
//                desc++;
//                index++;
//            }
//            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, array_list);
//        }
//            break;
//        case RETRO_ENVIRONMENT_GET_LANGUAGE: {
//            set_variable_value(env, RETRO_LANGUAGE_DUMMY);
//            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
//            auto language = (jobject) get_variable_value(env);
//            jclass integer_clazz = env->FindClass("java/lang/Integer");
//            jmethodID int_value_method = env->GetMethodID(integer_clazz, "intValue", "()I");
//            *(unsigned *) data = (unsigned int) env->CallIntMethod(language, int_value_method);
//        }
//            break;
//        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
//        case RETRO_ENVIRONMENT_SET_HW_RENDER | RETRO_ENVIRONMENT_EXPERIMENTAL:
//        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
//        case RETRO_ENVIRONMENT_SET_HW_RENDER:
//        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
//        case RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK:
//        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
//        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
//        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
//        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
//            break;
//        default:
////            LOGW(TAG, "WARN: cmd %d ignored!", cmd);
//            return false;
//    }
//    return true;
//}

static const JNINativeMethod methods[] = {
        {"nativePowerOn", "()V", (void *) em_power_on},
        {"nativePowerOff", "()V", (void *) em_power_off},
        {"nativeReset", "()V", (void*) em_reset},
        {"nativeLoad", "(Ljava/lang/String;)Z", (void*) em_load_game},
        {"nativeRun", "()V", (void*) em_run},
        {"nativeSaveState", "(Ljava/lang/String;)Z", (void*) em_save_state},
        {"nativeLoadState", "(Ljava/lang/String;)Z", (void*) em_load_state},
        {"nativeSaveMemoryRam", "(Ljava/lang/String;)Z", (void*) em_save_memory_ram},
        {"nativeLoadMemoryRam", "(Ljava/lang/String;)Z", (void*) em_load_memory_ram},
        {"nativeGetSystemInfo", "()Link/snowland/wkuwku/common/EmSystemInfo;", (void*) em_get_system_info},
        {"nativeGetSystemAvInfo", "()Link/snowland/wkuwku/common/EmSystemAvInfo;", (void*) em_get_system_av_info},
};

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: jni load failed!");
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

    clazz = env->FindClass("ink/snowland/wkuwku/emulator/Fceumm");
    ctx.video_refresh_method = env->GetMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetMethodID(clazz, "onInputPoll", "()V");
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
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    ctx.jvm = nullptr;
}