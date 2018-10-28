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
    self->playerStatus = [[NSMutableDictionary alloc] init];
    self->updateTimer = NULL;
    self->requestCount = 0;
  }
  
  return self;
}

- (void)connect {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self->device connect];
    
    self->updateTimer = [NSTimer scheduledTimerWithTimeInterval:1 repeats:YES block:^(NSTimer * _Nonnull timer) {
      
      if(self->requestCount > 0) {
        return;
      }
      
      self->requestCount++;
      [[self->device mediaControl] getPositionWithSuccess:^(NSTimeInterval position) {
        self->requestCount--;
        [self updateStatusWithPosition:position];
      } failure:^(NSError *error) {
        self->requestCount--;
      }];
      
      self->requestCount++;
      [[self->device mediaControl] getPlayStateWithSuccess:^(MediaControlPlayState playState) {
        self->requestCount--;
        [self updateStatusWithPlaystate:playState];
      } failure:^(NSError *error) {
        self->requestCount--;
      }];
      
      self->requestCount++;
      [[self->device mediaControl] getDurationWithSuccess:^(NSTimeInterval duration) {
        self->requestCount--;
        [self updateStatusWithDuration:duration];
      } failure:^(NSError *error) {
        self->requestCount--;
      }];
      
    }];
  });
}

- (void)disconnect {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self->device disconnect];
    if(self->updateTimer != NULL) {
      [self->updateTimer invalidate];
      self->updateTimer = NULL;
    }
    [self->playerStatus removeAllObjects];
    self->requestCount = 0;
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

- (void)play:(void (^)(FunctionCallResult<NSNumber *> *))cb {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[self->device mediaControl] playWithSuccess:^(id responseObject) {
      cb([[FunctionCallResult alloc] initWithResult:@YES error:NULL]);
    } failure:^(NSError *error) {
      cb([[FunctionCallResult alloc] initWithResult:@NO error:error]);
    }];
  });
}

- (void)pause:(void (^)(FunctionCallResult<NSNumber *> *))cb {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[self->device mediaControl] pauseWithSuccess:^(id responseObject) {
      cb([[FunctionCallResult alloc] initWithResult:@YES error:NULL]);
    } failure:^(NSError *error) {
      cb([[FunctionCallResult alloc] initWithResult:@NO error:error]);
    }];
  });
}

- (void)seek:(NSNumber *)position cb:(void (^)(FunctionCallResult<NSNumber *> *))cb
{
  dispatch_async(dispatch_get_main_queue(), ^{
    [[self->device mediaControl] seek:[position integerValue] success:^(id responseObject) {
      cb([[FunctionCallResult alloc] initWithResult:@YES error:NULL]);
    } failure:^(NSError *error) {
      cb([[FunctionCallResult alloc] initWithResult:@NO error:error]);
    }];
  });
}

- (void)getStatus:(void (^)(FunctionCallResult<NSDictionary *> *))cb {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSDictionary * status = [self->playerStatus copy];
    cb([[FunctionCallResult alloc] initWithResult:status error:NULL]);
  });
}

- (void)updateStatusWithPosition:(NSTimeInterval)position {
  dispatch_async(dispatch_get_main_queue(), ^{
    if(self->updateTimer == NULL) {
      return;
    }
    [self->playerStatus setValue:[[NSNumber alloc] initWithDouble:position] forKey:@"position"];
  });
}

- (void)updateStatusWithDuration:(NSTimeInterval)duration {
  dispatch_async(dispatch_get_main_queue(), ^{
    if(self->updateTimer == NULL) {
      return;
    }
    [self->playerStatus setValue:[[NSNumber alloc] initWithDouble:duration] forKey:@"duration"];
  });
}

- (void)updateStatusWithPlaystate:(MediaControlPlayState)playstate {
  dispatch_async(dispatch_get_main_queue(), ^{
    if(self->updateTimer == NULL) {
      return;
    }
    if(playstate == MediaControlPlayStatePlaying) {
      [self->playerStatus setValue:@"playing" forKey:@"state"];
    } else if(playstate == MediaControlPlayStatePaused) {
      [self->playerStatus setValue:@"paused" forKey:@"state"];
    } else {
      [self->playerStatus setValue:@"stop" forKey:@"state"];
    }
  });
}

@end




