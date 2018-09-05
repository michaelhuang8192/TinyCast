//
//  ConnectSDKSearch.m
//  TinyCast
//
//  Created by Michael Huang on 8/30/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import "ConnectSDKSearch.h"


@implementation ConnectSDKSearch

- (void)close {
  [self stop];
}

- (void)start:(long)timeout onFound:(void (^)(id<CastDevice>))onFound {
  
  dispatch_async(dispatch_get_main_queue(), ^{
    if(discoveryManager != NULL) { return; }
    
    discoveryManager = [DiscoveryManager sharedManager];
    self->onFound = onFound;
    discoveryManager.delegate = self;
    [discoveryManager startDiscovery];
    
  });
  
}

- (void)stop {
  dispatch_async(dispatch_get_main_queue(), ^{
    if(discoveryManager == NULL) { return; }
    
    [discoveryManager stopDiscovery];
    discoveryManager.delegate = NULL;
    self->onFound = NULL;
    discoveryManager = NULL;
  });
}

- (void)discoveryManager:(DiscoveryManager *)manager didFindDevice:(ConnectableDevice *)device {
  [self addDevice:device];
}

- (void)discoveryManager:(DiscoveryManager *)manager didUpdateDevice:(ConnectableDevice *)device {
  [self addDevice:device];
}

- (void)addDevice:(ConnectableDevice *)device {
  void (^onFound_)(id<CastDevice>) = self->onFound;
  if(onFound_ != NULL && [device hasCapability:@"MediaPlayer.Play.Video"]) {
    onFound_([[ConnectSDKDevice alloc] initWithDevice:device]);
  }
  
}

- (void)discoveryManager:(DiscoveryManager *)manager didLoseDevice:(ConnectableDevice *)device {
  
}

- (void)discoveryManager:(DiscoveryManager *)manager didFailWithError:(NSError *)error {
  
}

@end


@implementation ConnectSDKDevice

- initWithDevice:(ConnectableDevice *)device {
  self = [super init];
  if(self != NULL) {
    self->device = device;
  }
  
  return self;
}

- (void)connect {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self->device connect];
  });
}

- (void)disconnect {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self->device disconnect];
  });
}

- (NSString *)getId {
  return [self->device id];
}

- (NSString *)getName {
  return [NSString stringWithFormat:@"%@ (%@)", [self->device friendlyName], @"ConnectSDK"];
}

- (void)isConnected:(void (^)(FunctionCallResult<NSNumber *> *))cb {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSNumber *res = [[NSNumber alloc] initWithBool:[self->device connected]];
    cb([[FunctionCallResult alloc] initWithResult:res error:NULL]);
  });
}

- (void)isReady:(void (^)(FunctionCallResult<NSNumber *> *))cb {
  [self isConnected:cb];
}

- (BOOL)isSameDevice:(id<CastDevice>)device {
  return NO;
}

- (void)playMedia:(NSString *)url callback:(void (^)(FunctionCallResult<NSNumber *> *))cb {
  
  dispatch_async(dispatch_get_main_queue(), ^{
    NSURL *mediaURL = [NSURL URLWithString:url];
    if(mediaURL == NULL) {
      cb([[FunctionCallResult alloc] initWithResult:@NO error:NULL]);
      return;
    }
    MediaInfo *mediaInfo = [[MediaInfo alloc] initWithURL:mediaURL mimeType:@"video/mp4"];

    [[self->device mediaPlayer] playMediaWithMediaInfo:mediaInfo shouldLoop:NO success:^(MediaLaunchObject *mediaLaunchObject) {
      cb([[FunctionCallResult alloc] initWithResult:@YES error:NULL]);
    } failure:^(NSError *error) {
      cb([[FunctionCallResult alloc] initWithResult:@NO error:error]);
    }];
  });
}

@end




