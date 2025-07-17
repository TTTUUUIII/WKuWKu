//
// Created by wn123 on 2025-07-17.
//

#ifndef WKUWKU_AUDIOOUTPUTSTREAM_H
#define WKUWKU_AUDIOOUTPUTSTREAM_H

#include <oboe/Oboe.h>

#define kNanosPerMillisecond    (1000000L)

class AudioOutputStream: public std::enable_shared_from_this<AudioOutputStream>{
private:
    std::shared_ptr<oboe::AudioStream> stream;
    uint16_t sample_rate;
public:
    explicit AudioOutputStream(uint16_t _sr);
    ~AudioOutputStream();
    void request_open();
    void request_start();
    void request_pause();
    void write(const void* /* buffer */, int32_t /* numFrames */, int64_t /* timeoutNanoseconds */ );
    void request_stop();
    void request_close();
};

class AudioOutputStreamErrorCallback: public oboe::AudioStreamErrorCallback {
private:
    std::shared_ptr<AudioOutputStream> ostream;
public:
    explicit AudioOutputStreamErrorCallback(std::shared_ptr<AudioOutputStream> _ostream);
    ~AudioOutputStreamErrorCallback() override;
    bool onError(oboe::AudioStream *, oboe::Result) override;
};

#endif //WKUWKU_AUDIOOUTPUTSTREAM_H
