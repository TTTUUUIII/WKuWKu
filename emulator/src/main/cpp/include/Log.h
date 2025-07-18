//
// Created by deliu on 2025/4/30.
//

#ifndef WKUWKU_LOG_H
#define WKUWKU_LOG_H
#include <android/log.h>

#ifndef LOG_TAG
#define LOG_TAG "RetroNative"
#endif

#define LOGD(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[" _tag "] " _fmt, ##__VA_ARGS__)
#define LOGI(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[" _tag "] " _fmt, ##__VA_ARGS__)
#define LOGW(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[" _tag "] " _fmt, ##__VA_ARGS__)
#define LOGE(_tag, _fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "[" _tag "] " _fmt, ##__VA_ARGS__)

#endif //WKUWKU_LOG_H
