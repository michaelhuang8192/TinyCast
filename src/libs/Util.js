
var Promise = require("bluebird");
var utf8 = require("utf8");

var base64Encode, base64Decode;
if(typeof atob === 'undefined' || typeof btoa === 'undefined') {
	var Base64 = require("Base64");
	base64Encode = Base64.btoa;
	base64Decode = Base64.atob;
} else {
	base64Encode = btoa;
	base64Decode = atob;
}

exports.defer = function defer() {
	var resolve, reject;
	var promise = new Promise(function() {
		resolve = arguments[0];
		reject = arguments[1];
	});
	return {
		resolve: resolve,
		reject: reject,
		promise: promise
	};
};

exports.cacheOne = function(func) {
	var current = undefined;

	return function() {
		if(current && current.arg0 === arguments[0])
			return current.res;

		var res = func.apply(undefined, arguments);
		current = {arg0: arguments[0], res: res};
		return res;
	};
};

exports.cacheMore = function(func) {
	var cache = {};

	return function() {
		var current = cache[arguments[0]];
		if(current && current.arg0 === arguments[1])
			return current.res;

		var res = func.apply(undefined, arguments);
		cache[arguments[0]] = {arg0: arguments[1], res: res};
		return res;
	};
};

exports.waitUtilTimeout = function(condFunc, timeout, delayMs) {
	var curTs = new Date().getTime();
	
	function tryCond(resolve, reject) {
		condFunc.call()
		.then(function(res) {
			if(res)
				resolve();
			else if(new Date().getTime() - curTs < timeout)
				setTimeout(function() {
					tryCond(resolve, reject);
				}, delayMs == undefined ? 200 : delayMs);
			else
				reject(new Error("timeout"));
		})
		.catch(function(err) {
			reject(err);
		});
	}

	return new Promise(function(resolve, reject) {
		tryCond(resolve, reject);
	});
};


exports.base64EncodeSafe = function(s) {
	return base64Encode(utf8.encode(s))
	.replace(/[+=/]/gi, function(x) {
		if(x === '+')
			return '.';
		else if(x === '=')
			return '_';
		else if(x === '/')
			return '-';
		else 
			return x;
	});
};

exports.base64DecodeSafe = function(s) {
	s = s.replace(/[._-]/gi, function(x) {
		if(x === '.')
			return '+';
		else if(x === '_')
			return '=';
		else if(x === '-')
			return '/';
		else 
			return x;
	});

	return utf8.decode(base64Decode(s));
};



