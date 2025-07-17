//
// Created by wn123 on 2025-07-17.
//

#include "AudioOutputStream.h"

#include <utility>
#include "Log.h"

#define TAG "AudioOutputStream"

AudioOutputStream::AudioOutputStream(uint16_t _sr): sample_rate(_sr) {
    if (!sample_rate) {
        sample_rate = 48000;
    }
}

AudioOutputStream::~AudioOutputStream() {
    request_close();
}

void AudioOutputStream::write(const void *data, int32_t frames, int64_t timeoutNanoseconds) {
    if (!stream) return;
    oboe::StreamState state = stream->getState();
    if (state == oboe::StreamState::Started) {
        stream->write(data, frames,
                      timeoutNanoseconds);
    } else if (state == oboe::StreamState::Disconnected) {
        request_close();
        request_open();
        request_start();
    }
}

void AudioOutputStream::request_open() {
    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setDirection(oboe::Direction::Output)
            ->setUsage(oboe::Usage::Game)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSampleRate(sample_rate)
            ->setFormatConversionAllowed(true)
            ->setChannelConversionAllowed(true)
            ->setErrorCallback(std::make_shared<AudioOutputStreamErrorCallback>(shared_from_this()))
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

AudioOutputStreamErrorCallback::AudioOutputStreamErrorCallback(
        std::shared_ptr<AudioOutputStream> _ostream): ostream(std::move(_ostream)) {
}

bool AudioOutputStreamErrorCallback::onError(oboe::AudioStream *stream, oboe::Result error) {
    return false;
}

AudioOutputStreamErrorCallback::~AudioOutputStreamErrorCallback() {
    ostream = nullptr;
}

