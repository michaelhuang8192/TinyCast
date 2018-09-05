//
//  SamsungDeviceSearch.swift
//  TinyCast
//
//  Created by Michael Huang on 8/18/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

import Foundation

class SamsungDeviceSearch : NSObject, CastDeviceSearch, ServiceSearchDelegate {

  var onFound:((CastDevice?) -> Void)? = nil;
  var serviceSearch : ServiceSearch? = nil;
  var queue : DispatchQueue? = DispatchQueue(label: "SamsungDeviceSearchQueue");
  
  override init() {
    super.init();
  }
  
  func onServiceFound(_ service: Service) {
    onFound?(SamsungDevice(service));
  }
  
  func onServiceLost(_ service: Service) {
    
  }

  func start(_ timeout: Int, onFound: ((CastDevice?) -> Void)!) {
    queue?.async {
      if(self.serviceSearch != nil) {
        return;
      }
      
      self.onFound = onFound;
      self.serviceSearch = Service.search();
      self.serviceSearch?.delegate = self;
      self.serviceSearch?.start();
    }
  }

  func stop() {
    queue?.async {
      if(self.serviceSearch == nil) {
        return;
      }
      
      self.serviceSearch?.stop();
      self.serviceSearch?.delegate = nil;
      self.serviceSearch = nil;
      self.onFound = nil;
    }
  }
  
  func close() {
    stop();
    queue = nil;
  }
  
}

class SamsungDevice : NSObject, CastDevice, ConnectionDelegate {
  let service: Service;
  var queue: DispatchQueue? = nil;
  var player: VideoPlayer? = nil;
  var isConnected: Bool = false;
  
  init(_ service: Service) {
    self.service = service;
    super.init();
  }
  
  func getId() -> String! {
    return service.id;
  }
  
  func getName() -> String! {
    return service.name + " (SamsungSDK)"
  }
  
  func disconnect() {
    if(queue != nil) {
      queue?.async {
        if(self.player != nil) {
          self.player!.disconnect(true, completionHandler: nil);
          self.player!.connectionDelegate = nil;
          self.player = nil;
        }
      }
      queue = nil;
    }
  }
  
  func connect() {
    if(queue == nil) {
      queue = DispatchQueue(label: "SamsungDevice");
    }
    
    queue?.async {
      self.player?.connectionDelegate = nil;
      self.player = self.service.createVideoPlayer("DMP");
      self.isConnected = true;
      self.player?.connectionDelegate = self;
    }
  }
  
  func playMedia(_ url: String!, callback cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    if(queue == nil) {
      cb(FunctionCallResult<NSNumber>(result: nil, error: NSError(domain: "com.tinappsdev.TinyCast", code: 9999)));
      return;
    }
    
    queue?.async {
      if(self.player != nil) {
        self.player!.playContent(URL(string: url)!) {
          (error) -> Void in
          cb(FunctionCallResult<NSNumber>(result: NSNumber(value:error != nil), error: error));
        };
      } else {
        cb(FunctionCallResult<NSNumber>(result: nil, error: NSError(domain: "com.tinappsdev.TinyCast", code: 9999)));
      }
    }
  }
  
  func isConnected(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    if(queue == nil) {
      cb(FunctionCallResult<NSNumber>(result: NSNumber(value:false), error: nil));
      return;
    }
    
    queue?.async {
      let isConnected = self.player != nil && self.isConnected;
      cb(FunctionCallResult<NSNumber>(result: NSNumber(value:isConnected), error: nil));
    }
    
  }
  
  func isReady(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    isConnected(cb);
  }
  
  func isSameDevice(_ device: CastDevice!) -> Bool {
    return false;
  }
  
  func onConnect(_ error: NSError?) {
    queue?.async {
      self.isConnected = error == nil ? true : false;
    }
  }
  
  func onDisconnect(_ error: NSError?) {
    queue?.async {
      self.isConnected = false;
    }
  }
  
}

