//
//  CastDevice.m
//  TinyCast
//
//  Created by Michael Huang on 8/19/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "CastDevice.h"


@implementation FunctionCallResult

- initWithResult:(id)result error:(NSError*)error {
  self = [super init];
  
  if(self) {
    self.result = result;
    self.error = error;
  }
  
  return self;
}

@end

