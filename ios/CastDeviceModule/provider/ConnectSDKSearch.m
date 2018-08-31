//
//  ConnectSDKSearch.m
//  TinyCast
//
//  Created by Michael Huang on 8/30/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "CastDevice.h"
#import <ConnectSDK/ConnectSDK.h>


@interface ConnectSDKSearch : NSObject <CastDeviceSearch, DevicePickerDelegate, ConnectableDeviceDelegate> {
  DiscoveryManager *discoveryManager;
  
}
@end

@implementation ConnectSDKSearch

- (void)close {
  
}

- (void)start:(long)timeout onFound:(void (^)(id<CastDevice>))onFound {
  
  dispatch_async(dispatch_get_main_queue(), ^{
    if(discoveryManager != NULL) { return; }
    
    discoveryManager = [DiscoveryManager sharedManager];
    [discoveryManager startDiscovery];
    
  });
  
}

- (void)stop {
  dispatch_async(dispatch_get_main_queue(), ^{
    if(discoveryManager == NULL) { return; }
    
    [discoveryManager stopDiscovery];
    discoveryManager = NULL;
  });
}

- (void)devicePicker:(DevicePicker *)picker didSelectDevice:(ConnectableDevice *)device {
  
}

- (void)connectableDeviceDisconnected:(ConnectableDevice *)device withError:(NSError *)error {
  
}

- (void)connectableDeviceReady:(ConnectableDevice *)device {
  
}

@end

