package com.tinycast.tvcontroller;

import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;

public interface SmartDevice {
    String getId();
    String getName();
    void playMedia(String url, FunctionCall<Void, FunctionCallResult<Boolean>> cb);
    void disconnect();
    void connect();
    void isConnected(FunctionCall<Void, FunctionCallResult<Boolean>> cb);
    void isReady(FunctionCall<Void, FunctionCallResult<Boolean>> cb);
    boolean isSameDevice(SmartDevice smartDevice);
}
