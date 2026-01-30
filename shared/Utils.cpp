//
// Created by 86187 on 2026/1/29.
//

#include "Utils.h"
#include <chrono>

namespace util {

    timestamp_t system_current_milliseconds() {
        return std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now().time_since_epoch()).count();
    }

    void frame_time_helper_t::reset() {
        std::lock_guard<std::mutex> lock(mtx);
        prev_frame_time = 0;
        smoothed_frame_time = 0;
    }

    int frame_time_helper_t::frame_rate() {
        if (smoothed_frame_time == 0) return 0;
        std::lock_guard<std::mutex> lock(mtx);
        return static_cast<int>(round(MILLISECONDS_PER_SECOND / smoothed_frame_time));
    }

    void frame_time_helper_t::next_frame() {
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

    std::future<future::result_t> future::message_queue_t::send(int what, const std::shared_ptr<buffer_t>& usr) {
        std::lock_guard<std::mutex> lock(mtx);
        std::shared_ptr<std::promise<result_t>> promise = std::make_shared<std::promise<result_t>>();
        queue.push(std::make_shared<message_t>(what, promise, usr));
        return promise->get_future();
    }

    std::future<future::result_t> future::message_queue_t::send(int what) {
        return send(what, nullptr);
    }

    std::shared_ptr<future::message_t> future::message_queue_t::pop() {
        std::lock_guard<std::mutex> lock(mtx);
        if (queue.empty()) return nullptr;
        std::shared_ptr<message_t> msg = queue.front();
        queue.pop();
        return msg;
    }

    void future::message_queue_t::clear() {
        std::lock_guard<std::mutex> lock(mtx);
        while (!queue.empty()) queue.pop();
    }

    bool future::message_queue_t::empty() {
        std::lock_guard<std::mutex> lock(mtx);
        return queue.empty();
    }
}