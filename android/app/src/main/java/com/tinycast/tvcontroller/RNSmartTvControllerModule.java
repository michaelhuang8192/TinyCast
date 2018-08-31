
package com.tinycast.tvcontroller;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;

import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.internal.zzs.TAG;

public class RNSmartTvControllerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    private SmartTvController smartTvController;
    private Map<String, SmartDevice> smartDevices = new HashMap<>();
    private SmartDevice curSmartDevice;
    private HttpRequest httpRequest;


    public RNSmartTvControllerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.smartTvController = new SmartTvController(reactContext);
        this.httpRequest = new HttpRequest();
    }

    @Override
    public String getName() {
        return "RNSmartTvController";
    }


    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();

        if(curSmartDevice != null) {
            curSmartDevice.disconnect();
            curSmartDevice = null;
        }

        if(smartTvController != null) {
            smartTvController.release();
            smartTvController = null;
        }

        if(httpRequest != null) {
            httpRequest.close();
            httpRequest = null;
        }

    }

    private WritableMap createSmartDeviceMap(SmartDevice device) {
        if(device == null) return null;

        WritableMap params = Arguments.createMap();
        params.putString("name", device.getName());
        params.putString("id", device.getId());

        return params;
    }

    private void onFoundSmartDevice(final SmartDevice device) {
        Integer objectId = null;
        smartDevices.put(device.getId(), device);

        Log.i(TAG, ">>>ADDED: " + device.getId());
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(
                        "OnDiscoveryFoundDevice",
                        createSmartDeviceMap(device)
                );
    }

    @ReactMethod
    public void selectDevice(String id, final Promise promise) {
        try {
            SmartDevice device = null;
            if(curSmartDevice != null) {
                curSmartDevice.disconnect();
                curSmartDevice = null;
            }
            Log.i(TAG, ">>>selectDevice" + id);
            if (id != null) {
                device = curSmartDevice = smartDevices.get(id);
            }

            promise.resolve(createSmartDeviceMap(device));
        } catch(Exception e) {
            promise.reject("ERROR", e);
        }
    }

    @ReactMethod
    public void startDiscovery(Integer timeout) {
        this.smartTvController.startDiscovery(timeout, new FunctionCall<Void, SmartDevice>() {
            @Override
            public Void call(SmartDevice device) {
                onFoundSmartDevice(device);
                return null;
            }
        });
    }

    @ReactMethod
    public void stopDiscovery() {
        this.smartTvController.stopDiscovery();
        smartDevices.clear();
    }

    @ReactMethod
    public void playMedia(String url, final Promise promise) {
        try {
            SmartDevice device = curSmartDevice;
            if (device != null) {
                device.playMedia(url, new FunctionCall<Void, FunctionCallResult<Boolean>>() {
                    @Override
                    public Void call(FunctionCallResult<Boolean> arg) {
                        if(arg.isError())
                            promise.reject(arg.getError());
                        else
                            promise.resolve(arg.getResult());
                        return null;
                    }
                });
            } else {
                promise.reject("No Device", new Exception("No Device"));
            }
        } catch(Exception e) {
            promise.reject("Error", e);
        }
    }

    @ReactMethod
    public void connect() {
        SmartDevice device = curSmartDevice;
        if (device != null) {
            device.connect();
        }
    }

    @ReactMethod
    public void isReady(final Promise promise) {
        isConnected(promise);
    }

    @ReactMethod
    public void isConnected(final Promise promise) {
        SmartDevice device = curSmartDevice;
        if (device != null) {
            device.isConnected(new FunctionCall<Void, FunctionCallResult<Boolean>>() {
                @Override
                public Void call(FunctionCallResult<Boolean> arg) {
                    if(arg.isError())
                        promise.reject(arg.getError());
                    else
                        promise.resolve(arg.getResult());
                    return null;
                }
            });
        } else {
            promise.reject("No Device", new Exception("No Device"));
        }
    }

    @ReactMethod
    public void getCurrentCastDevice(final Promise promise) {
        promise.resolve(createSmartDeviceMap(curSmartDevice));
    }

    private WritableMap createJsMapFromHttpResponse(HttpRequest.HttpRepsonse res) {
        WritableMap params = Arguments.createMap();
        if(res == null) return params;

        params.putInt("statusCode", res.getStatusCode());
        if(res.getData() != null) params.putString("data", res.getData());
        if(res.getHeaders() != null) {
            WritableMap headers = Arguments.createMap();
            for(Map.Entry<String, String> kv : res.getHeaders().entrySet()) {
                headers.putString(kv.getKey(), kv.getValue());
            }
            params.putMap("headers", headers);
        }

        return params;
    }

    @ReactMethod
    public void openUrl(String url, String body, ReadableMap options, final Promise promise) {
        if(httpRequest != null) {
            httpRequest.openUrl(url, body, options != null ? options.toHashMap() : null, new FunctionCall<Void, FunctionCallResult<HttpRequest.HttpRepsonse>>() {
                @Override
                public Void call(FunctionCallResult<HttpRequest.HttpRepsonse> arg) {
                    if(arg.isError())
                        promise.reject("Error Response", arg.getError());
                    else {
                        promise.resolve(createJsMapFromHttpResponse(arg.getResult()));
                    }
                    return null;
                }
            });
        } else {
            promise.reject("No HttpRequest Object", new Exception("No HttpRequest Object"));
        }
    }
}