package com.tinycast.tvcontroller;

import com.tinycast.helper.FunctionCall;

public interface SmartDeviceSearch {
    void start(int timeout, FunctionCall<Void, SmartDevice> cb);
    void stop();
    void release();
}

