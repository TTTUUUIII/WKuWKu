//
// Created by 86187 on 2026/1/29.
//

#ifndef WKUWKU_UTIL_H
#define WKUWKU_UTIL_H

#include <mutex>

namespace util {
    typedef long long timestamp_t;

    long long system_current_milliseconds();

    class FrameTimeHelper {
    private:
        const float SMOOTHING_FACTOR = .03f;
        const float MILLISECONDS_PER_SECOND = 1000.f;
        std::mutex mtx;
        timestamp_t prev_frame_time;
        float smoothed_frame_time;
    public:
        void reset();
        void next_frame();
        int frame_rate();
    };
}

#endif //WKUWKU_UTIL_H
