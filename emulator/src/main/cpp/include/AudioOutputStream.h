//
// Created by wn123 on 2025-07-17.
//

#ifndef WKUWKU_AUDIOOUTPUTSTREAM_H
#define WKUWKU_AUDIOOUTPUTSTREAM_H

#include <functional>
#include <memory>
#include <aaudio/AAudio.h>

#define kNanosPerMillisecond    (1000000L)

class AudioOutputStream: public std::enable_shared_from_this<AudioOutputStream>{
private:
    AAudioStream *stream;
    uint16_t sample_rate;
    aaudio_performance_mode_t performance_mode;
    aaudio_sharing_mode_t  sharing_mode;
    aaudio_stream_state_t stream_state;
    uint8_t channel_count;
public:
    explicit AudioOutputStream();
    ~AudioOutputStream();
    void set_sharing_mode(aaudio_sharing_mode_t);
    void set_performance_mode(aaudio_performance_mode_t);
    void set_channel_count(uint8_t _channel_count);
    void set_sample_rate(uint16_t _sample_rate);
    void request_open();
    void request_start();
    void request_pause();
    int32_t write(const void* /* buffer */, int32_t /* numFrames */, int64_t /* timeoutNanoseconds */ );
    void request_stop();
    void request_close();
};

#endif //WKUWKU_AUDIOOUTPUTSTREAM_H
