package com.tinycast.tvcontroller;

import android.content.Context;

import com.tinycast.helper.FunctionCall;
import com.tinycast.tvcontroller.provider.ChromeCastSearch;
import com.tinycast.tvcontroller.provider.SamsungDeviceSearch;


public class SmartTvController {

    private static final String TAG = SmartTvController.class.getSimpleName();

    private final Context context;
    private SmartDeviceSearch[] smartDeviceSearchList;
    private Boolean isDiscovering = false;

    public SmartTvController(Context context) {
        this.context = context;
    }

    public void release() {
        if(smartDeviceSearchList != null) {
            for(SmartDeviceSearch deviceSearch : smartDeviceSearchList) {
                deviceSearch.stop();
                deviceSearch.release();
            }
            smartDeviceSearchList = null;
        }
    }

    public void startDiscovery(final int timeout, FunctionCall<Void, SmartDevice> cb) {
        if(isDiscovering) return;
        isDiscovering = true;

        if(smartDeviceSearchList == null)
            smartDeviceSearchList = getSmartDeviceSearchList();

        for(SmartDeviceSearch deviceSearch : smartDeviceSearchList) {
            deviceSearch.start(timeout, cb);
        }
    }

    public void stopDiscovery() {
        if(!isDiscovering) return;
        isDiscovering = false;

        for(SmartDeviceSearch deviceSearch : smartDeviceSearchList) {
            deviceSearch.stop();
        }
    }

    private SmartDeviceSearch[] getSmartDeviceSearchList() {
        return new SmartDeviceSearch[] {
                new SamsungDeviceSearch(context),
                new ChromeCastSearch(context)
        };
    }
}
