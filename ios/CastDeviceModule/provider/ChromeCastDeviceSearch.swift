//
//  ChromeCastDeviceSearch.swift
//  TinyCast
//
//  Created by Michael Huang on 8/19/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

import Foundation
import GoogleCast

class ChromeCastDeviceSearch : NSObject {
  static var hasSetup = false;
  var onFound:((CastDevice?) -> Void)? = nil;
  
}

extension ChromeCastDeviceSearch : GCKLoggerDelegate, GCKDiscoveryManagerListener {
  
  static func setup() {
    DispatchQueue.main.async {
      if(ChromeCastDeviceSearch.hasSetup) {
        return;
      }
      ChromeCastDeviceSearch.hasSetup = true;
      let options = GCKCastOptions(receiverApplicationID:"CC1AD845");
      GCKCastContext.setSharedInstanceWith(options);
    }
  }
  
  func registerDeviceListener(_ onFound: ((CastDevice?) -> Void)!) {
    DispatchQueue.main.async {
      if(self.onFound != nil) { return; }
      self.onFound = onFound;
      GCKCastContext.sharedInstance().discoveryManager.add(self);
    }
  }
  
  func unregisterDeviceListener() {
    DispatchQueue.main.async {
      GCKCastContext.sharedInstance().discoveryManager.remove(self);
      self.onFound = nil;
    }
  }
  
  func notifyWithDiscoveredDevices() {
    DispatchQueue.main.async {
      let discoveryManager = GCKCastContext.sharedInstance().discoveryManager;
      for i in 0..<discoveryManager.deviceCount {
        self.onFound?(ChromeCastDevice(discoveryManager.device(at: i)));
      }
    }
  }
  
  func didUpdate(_ device: GCKDevice, at index: UInt) {
    self.onFound?(ChromeCastDevice(device));
  }
  
  func didUpdate(_ device: GCKDevice, at index: UInt, andMoveTo newIndex: UInt) {
    self.onFound?(ChromeCastDevice(device));
  }
  
  func didInsert(_ device: GCKDevice, at index: UInt) {
    self.onFound?(ChromeCastDevice(device));
  }
  
}

extension ChromeCastDeviceSearch : CastDeviceSearch {
  func start(_ timeout: Int, onFound: ((CastDevice?) -> Void)!) {
    if(!ChromeCastDeviceSearch.hasSetup) {
      ChromeCastDeviceSearch.setup();
    }
    
    registerDeviceListener(onFound);
    notifyWithDiscoveredDevices();
  }
  
  func stop() {
    unregisterDeviceListener();
  }
  
  func close() {
    stop();
    
  }
  
  
}


class ChromeCastDevice : NSObject, CastDevice {
  let device : GCKDevice?;
  var onPlayMediaCallback: ((FunctionCallResult<NSNumber>?) -> Void)?;
  
  init(_ device: GCKDevice) {
    self.device = device;
  }
  
  func getId() -> String! {
    return self.device?.deviceID;
  }
  
  func getName() -> String! {
    return self.device?.friendlyName;
  }
  
  func playMedia(_ url: String!, callback cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    DispatchQueue.main.async {
      let builder = GCKMediaInformationBuilder(contentID: url);
      builder.streamType = GCKMediaStreamType.buffered;
      builder.contentType = "video/mp4";
      let media:GCKMediaInformation? = builder.build();
      let session = GCKCastContext.sharedInstance().sessionManager.currentSession;
      
      if(media == nil || session == nil) {
        cb(FunctionCallResult<NSNumber>(result: nil, error: NSError(domain: "com.tinappsdev.TinyCast", code: 9999)));
      } else {
        self.onPlayMediaCallback?(FunctionCallResult<NSNumber>(result: false, error: nil));
        self.onPlayMediaCallback = cb;
        let request = session?.remoteMediaClient?.loadMedia(media!);
        request?.delegate = self;
      }
    }
  }
  
  func disconnect() {
    DispatchQueue.main.async {
      GCKCastContext.sharedInstance().sessionManager.endSessionAndStopCasting(false)
    }
  }
  
  func connect() {
    DispatchQueue.main.async {
      GCKCastContext.sharedInstance().sessionManager.startSession(with: self.device!);
    }
  }
  
  func isConnected(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    DispatchQueue.main.async {
      let session = GCKCastContext.sharedInstance().sessionManager.currentSession;
      cb(FunctionCallResult<NSNumber>(result: session != nil && session?.connectionState == GCKConnectionState.connected, error:nil));
    }
  }
  
  func isReady(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    isConnected(cb);
  }
  
  func isSameDevice(_ device: CastDevice!) -> Bool {
    return false;
  }
  
}

extension ChromeCastDevice: GCKRequestDelegate {
  
  func requestDidComplete(_ request: GCKRequest) {
    let cb = self.onPlayMediaCallback;
    self.onPlayMediaCallback = nil;
    cb?(FunctionCallResult<NSNumber>(result:true, error:nil));
  }
  
  func request(_ request: GCKRequest, didFailWithError error: GCKError) {
    let cb = self.onPlayMediaCallback;
    self.onPlayMediaCallback = nil;
    cb?(FunctionCallResult<NSNumber>(result:nil, error:error));
  }
  
  func request(_ request: GCKRequest, didAbortWith abortReason: GCKRequestAbortReason) {
    let cb = self.onPlayMediaCallback;
    self.onPlayMediaCallback = nil;
    cb?(FunctionCallResult<NSNumber>(result:false, error:nil));
  }
  
}




