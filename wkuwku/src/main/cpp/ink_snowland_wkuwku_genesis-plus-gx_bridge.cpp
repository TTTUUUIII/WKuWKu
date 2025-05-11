#include <jni.h>
#include <string>
#include "log.h"
#include "common.h"

#define TAG "Genesis Plus GX"

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

    clazz = env->FindClass("ink/snowland/wkuwku/emulator/GenesisPlusGX");
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