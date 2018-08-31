//
//  CastDeviceModule.h
//  TinyCast
//
//  Created by Michael Huang on 8/16/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#ifndef CastDeviceModule_h
#define CastDeviceModule_h

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTInvalidating.h>

#import "CastDevice.h"

@class HttpRequest;

@interface CastDeviceModule : RCTEventEmitter <RCTBridgeModule, RCTInvalidating> {
  bool isDiscovering;
  NSArray<id<CastDeviceSearch>> *smartDeviceSearchList;
  NSMutableDictionary<NSString *, id<CastDevice>> *castDeviceMap;
  id<CastDevice> curCastDevice;
  HttpRequest *httpRequest;
}

@end

#endif /* CastDeviceModule_h */
