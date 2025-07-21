//
// Created by wn123 on 2025-07-17.
//

#include "AudioOutputStream.h"

#include <utility>
#include "Log.h"

#define TAG "AudioOutputStream"

AudioOutputStream::AudioOutputStream(uint16_t _sr): AudioOutputStream(_sr, oboe::PerformanceMode::LowLatency) {}

AudioOutputStream::AudioOutputStream(uint16_t _sr, oboe::PerformanceMode _mode): performance_mode(_mode), sample_rate(_sr) {
    if (sample_rate == 0)
        sample_rate = 48000;
    sharing_mode = oboe::SharingMode::Shared;
}

AudioOutputStream::~AudioOutputStream() {
    request_close();
}

int32_t AudioOutputStream::write(const void *data, int32_t frames, int64_t timeoutNanoseconds) {
    if (!stream) return frames;
    oboe::StreamState state = stream->getState();
    if (state == oboe::StreamState::Started) {
        oboe::ResultWithValue<int32_t> framesWritten = stream->write(data, frames,
                      timeoutNanoseconds);
        if (framesWritten.error() == oboe::Result::OK) {
            return framesWritten.value();
        }
    } else if (state == oboe::StreamState::Disconnected) {
        request_close();
        request_open();
        request_start();
    }
    return frames;
}

void AudioOutputStream::request_open() {
    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setDirection(oboe::Direction::Output)
            ->setUsage(oboe::Usage::Game)
            ->setSharingMode(sharing_mode)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setPerformanceMode(performance_mode)
            ->setSampleRate(sample_rate)
            ->openStream(stream);
    if (result != oboe::Result::OK) {
        stream = nullptr;
        LOGE(TAG, "Failed to open audio stream!");
    }
}

void AudioOutputStream::request_stop() {
    if (!stream) return;
    oboe::StreamState inputState = oboe::StreamState::Stopping;
    oboe::StreamState nextState = oboe::StreamState::Uninitialized;
    stream->requestStop();
    stream->waitForStateChange(inputState, &nextState, 100 * kNanosPerMillisecond);
}

void AudioOutputStream::request_start() {
    if (!stream) return;
    stream->requestStart();
}

void AudioOutputStream::request_close() {
    if (!stream) return;
    oboe::StreamState state = stream->getState();
    if (state != oboe::StreamState::Stopped) {
        request_stop();
    }
    stream->close();
    stream = nullptr;
}

void AudioOutputStream::request_pause() {
    if (!stream) return;
    stream->requestPause();
}

void AudioOutputStream::set_sharing_mode(oboe::SharingMode _mode) {
    sharing_mode = _mode;
}

void AudioOutputStream::set_performance_mode(oboe::PerformanceMode _mode) {
    performance_mode = _mode;
}

AudioOutputStreamErrorCallback::AudioOutputStreamErrorCallback(
        std::shared_ptr<AudioOutputStream> _ostream): ostream(std::move(_ostream)) {
}

bool AudioOutputStreamErrorCallback::onError(oboe::AudioStream *stream, oboe::Result error) {
    return false;
}

AudioOutputStreamErrorCallback::~AudioOutputStreamErrorCallback() {
    ostream = nullptr;
}

