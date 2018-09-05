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

private class RequestCallback {
  let callback: (Bool?, Error?) -> Void;
  
  init(_ callback: @escaping (Bool?, Error?) -> Void) {
    self.callback = callback;
  }
  
  func handleCallback(_ status: Bool? , error: Error?) {
    self.callback(status, error);
  }
}

private class RequestCallbackManager : NSObject, GCKRequestDelegate {
  var requests: [NSInteger: RequestCallback] = [:];
  
  func addRequest(_ request: GCKRequest, callback: @escaping (Bool?, Error?) -> Void) {
    request.delegate = self;
    requests[request.requestID] = RequestCallback(callback);
  }
  
  func clear() {
    requests.removeAll();
  }
  
  func requestDidComplete(_ request: GCKRequest) {
    if let requestCallback = requests.removeValue(forKey: request.requestID) {
      request.delegate = nil;
      requestCallback.handleCallback(true, error: nil);
    }
  }
  
  func request(_ request: GCKRequest, didFailWithError error: GCKError) {
    if let requestCallback = requests.removeValue(forKey: request.requestID) {
      request.delegate = nil;
      requestCallback.handleCallback(nil, error: error);
    }
  }
  
  func request(_ request: GCKRequest, didAbortWith abortReason: GCKRequestAbortReason) {
    if let requestCallback = requests.removeValue(forKey: request.requestID) {
      request.delegate = nil;
      requestCallback.handleCallback(nil, error: request.error);
    }
  }
}

private class ChromeCastDevice : NSObject, CastDevice {
  let device : GCKDevice?;
  let requestCallbackManager: RequestCallbackManager;
  
  init(_ device: GCKDevice) {
    self.device = device;
    self.requestCallbackManager = RequestCallbackManager();
  }
  
  func getId() -> String! {
    return self.device?.deviceID;
  }
  
  func getName() -> String! {
    return (self.device?.friendlyName ?? "") + " (GoogleSDK)";
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
        let request = session?.remoteMediaClient?.loadMedia(media!);
        self.handleResult(request, cb: cb);
      }
    }
  }
  
  func disconnect() {
    DispatchQueue.main.async {
      GCKCastContext.sharedInstance().sessionManager.endSessionAndStopCasting(false);
      self.requestCallbackManager.clear();
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
  
  func handleResult(_ request: GCKRequest?, cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    if let request = request {
      self.requestCallbackManager.addRequest(request, callback: { (status, error) in
        if(error != nil) {
          cb(FunctionCallResult<NSNumber>(result: false, error: error));
        } else {
          cb(FunctionCallResult<NSNumber>(result: status, error: nil));
        }
      });
    } else {
      cb(FunctionCallResult<NSNumber>(result: nil, error: NSError(domain: "com.tinappsdev.TinyCast", code: 9999)));
    }
  }
  
  func play(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    DispatchQueue.main.async {
      let session = GCKCastContext.sharedInstance().sessionManager.currentSession;
      let request = session?.remoteMediaClient?.play();
      self.handleResult(request, cb: cb);
    }
  }
  
  func pause(_ cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    DispatchQueue.main.async {
      let session = GCKCastContext.sharedInstance().sessionManager.currentSession;
      let request = session?.remoteMediaClient?.pause();
      self.handleResult(request, cb: cb);
    }
  }
  
  func seek(_ position: NSNumber!, cb: ((FunctionCallResult<NSNumber>?) -> Void)!) {
    DispatchQueue.main.async {
      let option = GCKMediaSeekOptions();
      option.interval = position.doubleValue;
      let session = GCKCastContext.sharedInstance().sessionManager.currentSession;
      let request = session?.remoteMediaClient?.seek(with: option);
      self.handleResult(request, cb: cb);
    }
  }
  
  func getStatus(_ cb: ((FunctionCallResult<NSDictionary>?) -> Void)!) {
    DispatchQueue.main.async {
      if let session = GCKCastContext.sharedInstance().sessionManager.currentSession,
        let status = session.remoteMediaClient?.mediaStatus
      {
        var res: [String : Any] = [:];
        
        if(status.playerState == GCKMediaPlayerState.playing) {
          res["state"] = "playing";
        } else if(status.playerState == GCKMediaPlayerState.paused) {
          res["state"] = "paused";
        } else if(status.playerState == GCKMediaPlayerState.idle) {
          res["state"] = "stop";
        }
        
        if let info = status.mediaInformation {
          res["duration"] = info.streamDuration;
        }
        
        res["position"] = status.streamPosition;
        
        cb(FunctionCallResult<NSDictionary>(result: res, error: nil));
        
      } else {
        cb(FunctionCallResult<NSDictionary>(result: nil, error: nil));
      }
    }
  }
  
}

