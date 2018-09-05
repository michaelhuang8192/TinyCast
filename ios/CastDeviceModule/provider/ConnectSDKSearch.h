//
//  ConnectSDKSearch.h
//  TinyCast
//
//  Created by Michael Huang on 9/1/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#ifndef ConnectSDKSearch_h
#define ConnectSDKSearch_h

#import <Foundation/Foundation.h>
#import "CastDevice.h"
#import <ConnectSDK/ConnectSDK.h>

@interface ConnectSDKSearch : NSObject <CastDeviceSearch, DiscoveryManagerDelegate> {
  DiscoveryManager *discoveryManager;
  void (^onFound)(id<CastDevice>);
}
@end

@interface ConnectSDKDevice : NSObject<CastDevice> {
  ConnectableDevice *device;
}
- initWithDevice:(ConnectableDevice *)device;

@end

#endif /* ConnectSDKSearch_h */
