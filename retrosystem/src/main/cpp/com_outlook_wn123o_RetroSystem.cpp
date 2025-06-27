#include <jni.h>
#include <unordered_map>
#include "GLWindow.h"
#include "RetroCore.h"
#include "com_outlook_wn123o_RetroSystem.h"

static std::unordered_map<std::string, std::string> all_cores;
static std::unordered_map<std::string, RetroCore*> all_loaded_cores;
static std::shared_ptr<RetroCore> current_core;

static EGLDisplay current_dyp;
static EGLSurface current_sf;
static std::unique_ptr<GLWindow> current_window;
static GLRenderer renderer = {
        on_create,
        on_draw,
        on_destroy
};

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_attachSurface(JNIEnv *env, jclass clazz,
                                                              jobject surface) {
    current_window = std::make_unique<GLWindow>(ANativeWindow_fromSurface(env, surface));
    current_window->set_renderer(&renderer);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_adjustSurface(JNIEnv *env, jclass clazz, jint vw,
                                                              jint vh) {
    if (current_window) {
        current_window->adjust_viewport(vw, vh);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_detachSurface(JNIEnv *env, jclass clazz) {
    if (current_window) {
        current_window->release();
    }
}

static void on_create(EGLDisplay dyp, EGLSurface sr) {
    current_dyp = dyp;
    current_sf = sr;
}

static void on_draw() {
    glClearColor(1.0f, 0.f, 0.f , 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    eglSwapBuffers(current_dyp, current_sf);
}

static void on_destroy() {

}

extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_add(JNIEnv *env, jclass clazz, jstring alias,
                                                    jstring path) {
    const char *core_alias = env->GetStringUTFChars(alias, JNI_FALSE);
    const char *core_path = env->GetStringUTFChars(path, JNI_FALSE);
    all_cores[core_alias] = core_path;
    env->ReleaseStringUTFChars(alias, core_alias);
    env->ReleaseStringUTFChars(path, core_path);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_use(JNIEnv *env, jclass clazz, jstring alias) {
    const char *core_alias = env->GetStringUTFChars(alias, JNI_FALSE);
    bool no_error = true;
    if (all_loaded_cores.count(core_alias) > 0) {
        current_core = std::shared_ptr<RetroCore>(all_loaded_cores[core_alias]);
    } else if (all_cores.count(core_alias) > 0) {

    } else {
        no_error = false;
    }
    env->ReleaseStringUTFChars(alias, core_alias);
    return no_error;
}