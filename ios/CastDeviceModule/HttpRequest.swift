//
//  HttpRequest.swift
//  TinyCast
//
//  Created by Michael Huang on 8/25/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

import Foundation

class HttpRequest : NSObject, URLSessionDelegate, URLSessionTaskDelegate {
  var session : URLSession? = nil;
  
  override init() {
    super.init();
    session = URLSession(configuration: .default, delegate: self, delegateQueue: nil);
  }
  
  func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
    completionHandler(nil);
  }
  
  func close() {
    session?.invalidateAndCancel()
    session = nil;
  }
  
  func openUrl(_ urlString: String, body: String?, options: [String: NSObject]?, cb: @escaping (Data?, URLResponse?, Error?) -> Swift.Void) -> Bool {
    let url = URL(string: urlString);
    if(url == nil) { return false };
    
    var request = URLRequest(url: url!);
    if(body == nil) {
      request.httpMethod = "GET";
    } else {
      request.httpMethod = "POST";
      request.httpBody = body?.data(using: .utf8);
      
      if let contentType = options?["contentType"] as? String {
        request.addValue(contentType, forHTTPHeaderField: "Content-Type");
      }
    }
    
    var task: URLSessionDataTask? = nil;
    if(options?["disableRedirection"] as? Bool ?? false) {
      task = session?.dataTask(with: request, completionHandler: cb);
    } else {
      task = URLSession.shared.dataTask(with: request, completionHandler: cb);
    }
    
    if(task != nil) {
      task?.resume();
    }
    return task != nil;
  }
  
}

