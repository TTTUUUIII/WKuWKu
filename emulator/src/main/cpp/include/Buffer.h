//
// Created by deliu on 2025/7/22.
//

#ifndef WKUWKU_BUFFER_H
#define WKUWKU_BUFFER_H
#include <iostream>
#include <utility>

struct buffer_t {
    size_t capacity;
    void* data;
    explicit buffer_t(size_t _capacity): capacity(_capacity) {
        data = malloc(capacity);
    }
    virtual ~buffer_t() {
        free(data);
    }
};

typedef std::function<void(const void*, size_t)> overflow_callback_t;

struct overflow_buffer_t : public buffer_t {
    size_t position;
    overflow_callback_t callback;

    void put(const void* _src, size_t size) {
        const auto* src = reinterpret_cast<const uint8_t*>(_src);
        while (size > 0) {
            size_t space = capacity - position;
            size_t copied_len = std::min(space, size);

            memcpy(reinterpret_cast<uint8_t*>(data) + position, src, copied_len);
            position += copied_len;
            src += copied_len;
            size -= copied_len;

            if (position == capacity) {
                callback(data, capacity);  // Pass full buffer
                position = 0;
            }
        }
    }

    void clear() {
        position = 0;
    }

    overflow_buffer_t(int32_t _capacity, overflow_callback_t _callback): buffer_t(_capacity), position(0), callback(std::move(_callback)) {
        data = malloc(capacity);
    }
};

#endif //WKUWKU_BUFFER_H
