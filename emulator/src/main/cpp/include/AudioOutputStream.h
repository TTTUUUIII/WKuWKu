//
// Created by wn123 on 2025-07-17.
//

#ifndef WKUWKU_AUDIOOUTPUTSTREAM_H
#define WKUWKU_AUDIOOUTPUTSTREAM_H

#include <functional>
#include <memory>
#include <oboe/Oboe.h>

#define kNanosPerMillisecond    (1000000L)

class AudioOutputStream: public std::enable_shared_from_this<AudioOutputStream>{
private:
    std::shared_ptr<oboe::AudioStream> stream;
    uint16_t sample_rate;
    oboe::PerformanceMode performance_mode;
    oboe::SharingMode  sharing_mode;
    oboe::StreamState stream_state;
    oboe::ChannelCount channel_count;
public:
    explicit AudioOutputStream();
    ~AudioOutputStream();
    void set_sharing_mode(oboe::SharingMode);
    void set_performance_mode(oboe::PerformanceMode);
    void set_channel_count(oboe::ChannelCount);
    void set_sample_rate(uint16_t _sample_rate);
    void request_open();
    void request_start();
    void request_pause();
    int32_t write(const void* /* buffer */, int32_t /* numFrames */, int64_t /* timeoutNanoseconds */ );
    void request_stop();
    void request_close();
};

#endif //WKUWKU_AUDIOOUTPUTSTREAM_H
