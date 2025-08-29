//
// Created by wn123 on 2025-07-17.
//

#include "AudioOutputStream.h"

#include <utility>
#include "Log.h"

#define TAG "Audio"

AudioOutputStream::AudioOutputStream() {
    xrun_count = 0;
    sample_rate = 48000;
    channel_count = oboe::ChannelCount::Stereo;
    sharing_mode = oboe::SharingMode::Shared;
    performance_mode = oboe::PerformanceMode::PowerSaving;
    stream_state = oboe::StreamState::Uninitialized;
    stream = nullptr;
    check_underrun = false;
    underrun_count = 0;
}

AudioOutputStream::~AudioOutputStream() {
    request_close();
}

int32_t AudioOutputStream::write(const void *data, int32_t frames, int64_t timeoutNanoseconds) {
    if (!stream) return frames;
    if (stream_state == oboe::StreamState::Started) {
        const oboe::ResultWithValue<int32_t> result = stream->write(data, frames, timeoutNanoseconds);
        if (result.error() == oboe::Result::OK) {
            if (check_underrun) {
                check_xrun_count();
            }
            return result.value();
        } else if (result.error() == oboe::Result::ErrorDisconnected) {
            stream_state = oboe::StreamState::Disconnected;
            LOGW(TAG, "Stream disconnected.");
            goto reopen;
        } else {
            LOGE(TAG, "Write stream error=%d", result.error());
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
    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setPerformanceMode(performance_mode)
            ->setUsage(oboe::Usage::Game)
            ->setChannelCount(channel_count)
            ->setSharingMode(sharing_mode)
            ->setSampleRate(sample_rate)
            ->setFormat(oboe::AudioFormat::I16)
            ->setDirection(oboe::Direction::Output)
            ->setChannelConversionAllowed(true)
            ->setFormatConversionAllowed(true)
            ->openStream(stream);
    if (result != oboe::Result::OK) {
        LOGE(TAG, "Failed to create stream builder! error=%d.", result);
        return;
    }
    int32_t frames_per_burst = stream->getFramesPerBurst();
    stream->setBufferSizeInFrames(std::max(frames_per_burst, 96 * 24));
    stream_state = oboe::StreamState::Open;
    xrun_count = 0;
    underrun_count = 0;
    LOGI(TAG, "Open output stream, frame_per_bust=%d.", frames_per_burst);
}

void AudioOutputStream::request_stop() {
    if (stream_state != oboe::StreamState::Started
    && stream_state != oboe::StreamState::Paused) {
        LOGE(TAG, "Unable stop stream, invalid state=%d", stream_state);
        return;
    }
    if (stream_state == oboe::StreamState::Paused) {
        request_start();
    }
    stream->requestStop();
    oboe::StreamState input_state = oboe::StreamState::Stopping;
    oboe::StreamState next_state = oboe::StreamState::Uninitialized;
    stream->waitForStateChange(input_state, &next_state, 100 * kNanosPerMillisecond);
    stream_state = oboe::StreamState::Stopped;
}

void AudioOutputStream::request_start() {
    if (stream_state != oboe::StreamState::Open
        && stream_state != oboe::StreamState::Paused
           && stream_state != oboe::StreamState::Stopped) {
        LOGE(TAG, "Unable start stream, invalid state=%d", stream_state);
        return;
    }
    stream->requestStart();
    oboe::StreamState input_state = oboe::StreamState::Starting;
    oboe::StreamState next_state = oboe::StreamState::Uninitialized;
    stream->waitForStateChange(input_state, &next_state, 100 * kNanosPerMillisecond);
    stream_state = oboe::StreamState::Started;
}

void AudioOutputStream::request_close() {
    if (stream_state == oboe::StreamState::Uninitialized) return;
    if (stream_state != oboe::StreamState::Disconnected) {
        request_stop();
    }
    stream->close();
    stream_state = oboe::StreamState::Uninitialized;
    stream = nullptr;
}

void AudioOutputStream::request_pause() {
    if (stream_state != oboe::StreamState::Started) {
        LOGE(TAG, "Unable pause stream, invalid state=%d", stream_state);
        return;
    }
    stream->requestPause();
    oboe::StreamState input_state = oboe::StreamState::Pausing;
    oboe::StreamState next_state = oboe::StreamState::Uninitialized;
    stream->waitForStateChange(input_state, &next_state, 100 * kNanosPerMillisecond);
    stream_state = oboe::StreamState::Paused;
}

void AudioOutputStream::set_sharing_mode(oboe::SharingMode _mode) {
    sharing_mode = _mode;
}

void AudioOutputStream::set_performance_mode(oboe::PerformanceMode _mode) {
    performance_mode = _mode;
}

void AudioOutputStream::set_channel_count(oboe::ChannelCount _channel_count) {
    channel_count = _channel_count;
}

void AudioOutputStream::set_sample_rate(uint16_t _sample_rate) {
    if (_sample_rate >= 8000) {
        sample_rate = _sample_rate;
    }
}

void AudioOutputStream::check_xrun_count() {
    const oboe::ResultWithValue<int32_t> &result = stream->getXRunCount();
    if (result.error() == oboe::Result::OK) {
        int32_t count = result.value();
        if (count - xrun_count > 15 && underrun_count < 5) {
            int32_t buffer_size = stream->getBufferSizeInFrames();
            stream->setBufferSizeInFrames(buffer_size * 2);
            xrun_count = count;
            underrun_count++;
            LOGW(TAG, "Underrun happened, try increase the buffer size.");
        }
    } else {
        LOGE(TAG, "Failed to get xrun count, error=%d", result.error());
    }
}

void AudioOutputStream::set_check_underrun(bool _enable) {
    check_underrun = true;
}

