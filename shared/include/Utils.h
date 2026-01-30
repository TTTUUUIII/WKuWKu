//
// Created by 86187 on 2026/1/29.
//

#ifndef WKUWKU_UTILS_H
#define WKUWKU_UTILS_H

#include <mutex>
#include <unordered_map>
#include <any>
#include <future>
#include <utility>
#include "Buffer.h"

#define NO_ERROR                            0
#define ERROR                               1

namespace util {
    typedef long long timestamp_t;

    long long system_current_milliseconds();

    class frame_time_helper_t {
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

    class properties_t {
    private:
        std::unordered_map<int32_t, std::any> props;
    public:
        template<typename T>
        T get_or_else(int32_t key, const T &default_value) {
            if (props.count(key)) {
                return std::any_cast<T>(props[key]);
            }
            return default_value;
        }

        template<typename T>
        void set(int32_t key, T&& v) {
            props[key] = std::forward<T>(v);
        }
    };

    namespace future {
        struct result_t {
            int state;
            std::shared_ptr<buffer_t> data;

            explicit result_t(int _state, std::shared_ptr<buffer_t> _data) : state(_state),
                                                                             data(std::move(_data)) {}

            explicit result_t(int _state) : result_t(_state, nullptr) {}

            static result_t ok() {
                return result_t{NO_ERROR, nullptr};
            }

            static result_t ok(std::shared_ptr<buffer_t> _data) {
                return result_t{NO_ERROR, std::move(_data)};
            }

            static result_t err() {
                return result_t{ERROR, nullptr};
            }
        };

        struct message_t {
            int what;
            std::shared_ptr<std::promise<result_t>> promise;
            std::shared_ptr<buffer_t> usr;

            explicit message_t(int _what, std::shared_ptr<std::promise<result_t>> _promise,
                               std::shared_ptr<buffer_t> _usr) : what(_what),
                                                                 promise(std::move(_promise)),
                                                                 usr(std::move(_usr)) {}
        };

        class message_queue_t {
        private:
            std::mutex mtx;
            std::queue<std::shared_ptr<message_t>> queue;
        public:
            std::future<result_t> send(int, const std::shared_ptr<buffer_t>&);
            std::future<result_t> send(int);
            std::shared_ptr<message_t> pop();
            void clear();
            bool empty();
        };
    }
}

#endif //WKUWKU_UTILS_H
