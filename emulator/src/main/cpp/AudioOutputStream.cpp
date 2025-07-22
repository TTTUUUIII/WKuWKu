//
// Created by wn123 on 2025-07-17.
//

#include "AudioOutputStream.h"

#include <utility>
#include "Log.h"

#define TAG "AudioOutputStream"

AudioOutputStream::AudioOutputStream() {
    sample_rate = 48000;
    channel_count = 2;
    sharing_mode = AAUDIO_SHARING_MODE_SHARED;
    performance_mode = AAUDIO_PERFORMANCE_MODE_POWER_SAVING;
    stream_state = AAUDIO_STREAM_STATE_UNINITIALIZED;
    stream = nullptr;
}

AudioOutputStream::~AudioOutputStream() {
    request_close();
}

int32_t AudioOutputStream::write(const void *data, int32_t frames, int64_t timeoutNanoseconds) {
    if (!stream) return frames;
    if (stream_state == AAUDIO_STREAM_STATE_STARTED) {
        aaudio_result_t result = AAudioStream_write(stream, data, frames, timeoutNanoseconds);
        if (result >= 0) {
            return result;
        } else if (result == AAUDIO_ERROR_WOULD_BLOCK) {
            stream_state = AAUDIO_STREAM_STATE_DISCONNECTED;
            goto reopen;
        } else {
            LOGE(TAG, "Failed to write to output stream! error=%d.", result);
        }
    }
    return frames;

    reopen:
    request_close();
    request_open();
    request_start();
    return frames;
}

void AudioOutputStream::request_open() {
    AAudioStreamBuilder *builder;
    aaudio_result_t result;
    result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE(TAG, "Failed to create stream builder! error=%d.", result);
        return;
    }
    AAudioStreamBuilder_setDeviceId(builder, AAUDIO_UNSPECIFIED);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setPerformanceMode(builder, performance_mode);
    AAudioStreamBuilder_setSharingMode(builder, sharing_mode);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setChannelCount(builder, 2);
    AAudioStreamBuilder_setSampleRate(builder, sample_rate);
    AAudioStreamBuilder_setBufferCapacityInFrames(builder, 960);
    result = AAudioStreamBuilder_openStream(builder, &stream);
    if (result == AAUDIO_OK) {
        stream_state = AAUDIO_STREAM_STATE_OPEN;
    } else {
        LOGE(TAG, "Failed to open output stream! error=%d.", result);
    }
    AAudioStreamBuilder_delete(builder);
}

void AudioOutputStream::request_stop() {
    if (stream_state == AAUDIO_STREAM_STATE_UNINITIALIZED
        || stream_state == AAUDIO_STREAM_STATE_STOPPED) return;
    AAudioStream_requestStop(stream);
    aaudio_stream_state_t next_state = AAUDIO_STREAM_STATE_UNINITIALIZED;
    AAudioStream_waitForStateChange(stream, AAUDIO_STREAM_STATE_STOPPING, &next_state, 100 * kNanosPerMillisecond);
    stream_state = AAUDIO_STREAM_STATE_STOPPED;
}

void AudioOutputStream::request_start() {
    if (stream_state == AAUDIO_STREAM_STATE_UNINITIALIZED
    || stream_state == AAUDIO_STREAM_STATE_STARTED) return;
    AAudioStream_requestStart(stream);
    aaudio_stream_state_t next_state = AAUDIO_STREAM_STATE_UNINITIALIZED;
    AAudioStream_waitForStateChange(stream, AAUDIO_STREAM_STATE_STARTING, &next_state, 100 * kNanosPerMillisecond);
    stream_state = AAUDIO_STREAM_STATE_STARTED;
}

void AudioOutputStream::request_close() {
    if (stream_state == AAUDIO_STREAM_STATE_UNINITIALIZED) return;
    if (stream_state != AAUDIO_STREAM_STATE_STOPPED) {
        request_stop();
    }
    AAudioStream_close(stream);
    stream_state = AAUDIO_STREAM_STATE_UNINITIALIZED;
    stream = nullptr;
}

void AudioOutputStream::request_pause() {
    if (stream_state == AAUDIO_STREAM_STATE_UNINITIALIZED
        || stream_state == AAUDIO_STREAM_STATE_PAUSED) return;
    AAudioStream_requestPause(stream);
    stream_state = AAUDIO_STREAM_STATE_PAUSED;
}

void AudioOutputStream::set_sharing_mode(aaudio_sharing_mode_t _mode) {
    sharing_mode = _mode;
}

void AudioOutputStream::set_performance_mode(aaudio_performance_mode_t _mode) {
    performance_mode = _mode;
}

void AudioOutputStream::set_channel_count(uint8_t _channel_count) {
    channel_count = _channel_count;
}

void AudioOutputStream::set_sample_rate(uint16_t _sample_rate) {
    if (_sample_rate >= 8000) {
        sample_rate = _sample_rate;
    }
}

