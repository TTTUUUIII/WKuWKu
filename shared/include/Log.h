//
// Created by deliu on 2025/4/30.
//

#ifndef WKUWKU_LOG_H
#define WKUWKU_LOG_H
#include <android/log.h>

#ifndef LOG_TAG
#define LOG_TAG "EmulatorV2(Native)"
#endif
#ifdef NDEBUG
#define LOGD(_tag, _fmt, ...) ((void)0)
#else
#define LOGD(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[%s] " _fmt, _tag, ##__VA_ARGS__)
#endif
#define LOGI(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[%s] " _fmt, _tag, ##__VA_ARGS__)
#define LOGW(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "[%s] " _fmt, _tag, ##__VA_ARGS__)
#define LOGE(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "[%s] " _fmt, _tag, ##__VA_ARGS__)

#endif //WKUWKU_LOG_H
