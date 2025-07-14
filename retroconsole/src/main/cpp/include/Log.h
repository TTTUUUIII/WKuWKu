//
// Created by deliu on 2025/4/30.
//

#ifndef WKUWKU_LOG_H
#define WKUWKU_LOG_H
#include <android/log.h>

#define LOGD(tag, ...) __android_log_print(ANDROID_LOG_DEBUG, tag, __VA_ARGS__)
#define LOGI(tag, ...) __android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__)
#define LOGW(tag, ...) __android_log_print(ANDROID_LOG_WARN, tag, __VA_ARGS__)
#define LOGE(tag, ...) __android_log_print(ANDROID_LOG_ERROR, tag, __VA_ARGS__)

#endif //WKUWKU_LOG_H
