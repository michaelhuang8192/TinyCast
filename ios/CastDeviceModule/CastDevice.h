//
//  CastDevice.h
//  TinyCast
//
//  Created by Michael Huang on 8/18/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#ifndef CastDevice_h
#define CastDevice_h

#import <Foundation/Foundation.h>

@interface FunctionCallResult<ObjectType> : NSObject

- initWithResult:(id)result error:(NSError*)error;

@property ObjectType result;
@property NSError * error;

@end


@protocol CastDevice

- (NSString *)getId;
- (NSString *)getName;

- (void)playMedia:(NSString *)url callback:(void (^)(FunctionCallResult<NSNumber *> *))cb;
- (void)disconnect;
- (void)connect;
- (void)isConnected:(void (^)(FunctionCallResult<NSNumber *> *))cb;
- (void)isReady:(void (^)(FunctionCallResult<NSNumber *> *))cb;
- (BOOL)isSameDevice:(id<CastDevice>)device;

@end


@protocol CastDeviceSearch

- (void) start:(long) timeout onFound:(void (^)(id<CastDevice>))onFound;
- (void) stop;
- (void) close;

@end


#endif /* CastDevice_h */
