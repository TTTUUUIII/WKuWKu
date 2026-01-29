//
// Created by 86187 on 2026/1/29.
//

#include "Util.h"
#include <chrono>

namespace util {

    timestamp_t util::system_current_milliseconds() {
        return std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now().time_since_epoch()).count();
    }

    void util::FrameTimeHelper::reset() {
        std::lock_guard<std::mutex> lock(mtx);
        prev_frame_time = 0;
        smoothed_frame_time = 0;
    }

    int util::FrameTimeHelper::frame_rate() {
        if (smoothed_frame_time == 0) return 0;
        std::lock_guard<std::mutex> lock(mtx);
        return static_cast<int>(round(MILLISECONDS_PER_SECOND / smoothed_frame_time));
    }

    void util::FrameTimeHelper::next_frame() {
        std::lock_guard<std::mutex> lock(mtx);
        timestamp_t now = system_current_milliseconds();
        if (prev_frame_time == 0) {
            prev_frame_time = now;
            smoothed_frame_time = 0;
            return;
        }
        auto frame_time = static_cast<float>(now - prev_frame_time);
        prev_frame_time = now;
        if (smoothed_frame_time == 0) {
            smoothed_frame_time = frame_time;
            return;
        }
        smoothed_frame_time += SMOOTHING_FACTOR * (frame_time - smoothed_frame_time);
    }
}