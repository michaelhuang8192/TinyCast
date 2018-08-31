//
//  CastDeviceModule.m
//  TinyCast
//
//  Created by Michael Huang on 8/16/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "CastDeviceModule.h"
#import "TinyCast-Swift.h"

@implementation CastDeviceModule

RCT_EXPORT_MODULE(RNSmartTvController);

- init {
  self = [super init];
  
  if(self) {
    isDiscovering = NO;
    smartDeviceSearchList = NULL;
    castDeviceMap = [[NSMutableDictionary alloc] init];
    curCastDevice = NULL;
    httpRequest = [[HttpRequest alloc] init];
  }
  
  return self;
}

- (void)invalidate {
  if(smartDeviceSearchList != NULL) {
    for(int i = 0; i < [smartDeviceSearchList count]; i++) {
      id<CastDeviceSearch> search = [smartDeviceSearchList objectAtIndex:i];
      [search stop];
      [search close];
    }
    smartDeviceSearchList = NULL;
  }
  
  castDeviceMap = NULL;
  if(curCastDevice != NULL) {
    [curCastDevice disconnect];
    curCastDevice = NULL;
  }
  
  [httpRequest close];
  httpRequest = NULL;
}

- (NSArray<id<CastDeviceSearch>> *)getSmartDeviceSearchList {
  return @[
           [[SamsungDeviceSearch alloc] init],
           [[ChromeCastDeviceSearch alloc] init]
  ];
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"OnDiscoveryFoundDevice"];
}

- (NSDictionary *) createDeviceJSObject:(id<CastDevice>)device {
  return @{@"id": [device getId], @"name": [device getName]};
}

RCT_REMAP_METHOD(startDiscovery, startDiscoveryWithTimeout:(NSInteger*)timeout) {
  
  if(isDiscovering) return;
  isDiscovering = YES;
  
  if(smartDeviceSearchList == NULL)
    smartDeviceSearchList = [self getSmartDeviceSearchList];
  
  void (^onFound)(id<CastDevice>) = ^(id<CastDevice> device) {
    castDeviceMap[[device getId]] = device;
    [self sendEventWithName:@"OnDiscoveryFoundDevice" body:[self createDeviceJSObject:device]];
  };
  
  for(int i = 0; i < [smartDeviceSearchList count]; i++) {
    id<CastDeviceSearch> search = [smartDeviceSearchList objectAtIndex:i];
    [search start:5000 onFound:onFound];
  }
  
}

RCT_EXPORT_METHOD(stopDiscovery) {
  if(!isDiscovering) return;
  isDiscovering = NO;
  
  for(int i = 0; i < [smartDeviceSearchList count]; i++) {
    id<CastDeviceSearch> search = [smartDeviceSearchList objectAtIndex:i];
    [search stop];
  }
  
  [castDeviceMap removeAllObjects];
}

RCT_EXPORT_METHOD(selectDevice:(NSString*)deviceId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  NSLog(@">>>selectDevice%@", deviceId);
  if(curCastDevice != NULL) {
    [curCastDevice disconnect];
    curCastDevice = NULL;
  }
  
  if(deviceId != NULL) {
    curCastDevice = castDeviceMap[deviceId];
  }
  
  resolve(curCastDevice == NULL ? NULL : [self createDeviceJSObject:curCastDevice]);
}

RCT_EXPORT_METHOD(playMedia:(NSString*)url resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  if(curCastDevice == NULL) {
    reject(@"No Device", @"No Device", NULL);
  } else {
    [curCastDevice playMedia:url callback:^(FunctionCallResult<NSNumber *> * callResult) {
      if([callResult error]) {
        reject(@"Error", @"Error", [callResult error]);
      } else {
        resolve([callResult result]);
      }
    }];
  }
}

RCT_EXPORT_METHOD(connect) {
  if(curCastDevice != NULL) {
    [curCastDevice connect];
  }
}

RCT_REMAP_METHOD(isReady, isReadyWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  [self isConnectedWithResolver:resolve rejecter:reject];
}

RCT_REMAP_METHOD(isConnected, isConnectedWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  if(curCastDevice == NULL) {
    reject(@"No Device", @"No Device", NULL);
  } else {
    [curCastDevice isConnected:^(FunctionCallResult<NSNumber *> * callResult) {
      if([callResult error]) {
        reject(@"Error", @"Error", [callResult error]);
      } else {
        resolve([callResult result]);
      }
    }];
  }
}

RCT_REMAP_METHOD(getCurrentCastDevice, getCurrentCastDeviceWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  resolve(curCastDevice == NULL ? NULL : [self createDeviceJSObject:curCastDevice]);
}

RCT_REMAP_METHOD(openUrl, openUrl:(NSString *)url body:(NSString *)body options:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  
  void (^cb)(NSData *, NSURLResponse *, NSError *) = ^(NSData * data, NSURLResponse *res, NSError *err) {
    if(err != NULL) {
      reject(@"Http Request Error", [err localizedDescription], NULL);
    } else if(![res isKindOfClass:[NSHTTPURLResponse class]]) {
        reject(@"Http Request Error", @"Unable to Cast", NULL);
    } else {
      NSHTTPURLResponse *httpRes = (NSHTTPURLResponse*) res;
      
      NSString *dataStr = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
      
      NSMutableDictionary *ret = [[NSMutableDictionary alloc] init];
      if(dataStr != NULL) { [ret setValue:dataStr forKey:@"data"];}
      if(httpRes.allHeaderFields != NULL) { [ret setValue:httpRes.allHeaderFields forKey:@"headers"];}
      [ret setValue:@(httpRes.statusCode) forKey:@"statusCode"];
  
      resolve(ret);
    }
  };
  
  if(![httpRequest openUrl:url body:body options:options cb:cb]) {
    reject(@"Invalid Http Request", @"Invalid Http Request", NULL);
  }
}

@end


