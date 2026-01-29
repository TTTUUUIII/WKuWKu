//
// Created by deliu on 2025/7/22.
//

#ifndef WKUWKU_BUFFER_H
#define WKUWKU_BUFFER_H
#include <iostream>
#include <utility>
#include <queue>
#include <vector>
#include <mutex>

struct buffer_t {
    size_t capacity;
    void* data;
    explicit buffer_t(size_t _capacity) : capacity(_capacity) {
        data = malloc(capacity);
    }

    buffer_t(buffer_t&& other) noexcept : capacity(other.capacity), data(other.data) {
        other.data = nullptr;
    }
    virtual ~buffer_t() {
        if (data) free(data);
    }

    buffer_t(const buffer_t&) = delete;
    buffer_t& operator=(const buffer_t&) = delete;
};

class framebuffer_t {
private:
    std::mutex mtx;
    std::vector<buffer_t> buffers;
    std::queue<int> free_idxs;
    std::queue<int> full_idxs;
    int cur_read_idx = -1;
public:
    explicit framebuffer_t(int size_in_bytes, int count) {
        for (int i = 0; i < count; ++i) {
            buffers.emplace_back(size_in_bytes);
            free_idxs.push(i);
        }
    }

    int acquire_write_idx() {
        std::lock_guard<std::mutex> lock(mtx);
        int write_idx = -1;
        if(!free_idxs.empty()) {
            write_idx = free_idxs.front();
            free_idxs.pop();
        } else {
            write_idx = full_idxs.front();
            full_idxs.pop();
        }
        return write_idx;
    }

    void submit_buffer(int idx) {
        if(idx == -1) return;
        std::lock_guard<std::mutex> lock(mtx);
        full_idxs.push(idx);
    }

    void* data_ptr(int idx) {
        if(idx == -1) {
            return nullptr;
        }
        return buffers[idx].data;
    }

    std::unique_ptr<buffer_t> copy_pixels() {
        std::lock_guard<std::mutex> lock(mtx);
        if(cur_read_idx != -1) {
            buffer_t& fb = buffers[cur_read_idx];
            std::unique_ptr<buffer_t> data_ptr = std::make_unique<buffer_t>(fb.capacity);
            memcpy(data_ptr->data, fb.data, fb.capacity);
            return std::move(data_ptr);
        }
        return nullptr;
    }

    int acquire_read_idx() {
        std::lock_guard<std::mutex> lock(mtx);
        if(!full_idxs.empty()) {
            if(cur_read_idx != -1) {
                free_idxs.push(cur_read_idx);
            }
            cur_read_idx = full_idxs.front();
            full_idxs.pop();
        }
        return cur_read_idx;
    }

    virtual ~framebuffer_t() {
        std::lock_guard<std::mutex> lock(mtx);
        while(!full_idxs.empty()) full_idxs.pop();
        while(!free_idxs.empty()) free_idxs.pop();
        cur_read_idx = -1;
        buffers.clear();
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

    overflow_buffer_t(int32_t _capacity, overflow_callback_t _callback): buffer_t(_capacity), position(0), callback(std::move(_callback)) {}
};

#endif //WKUWKU_BUFFER_H
