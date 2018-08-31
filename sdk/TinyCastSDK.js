var $ = require("jquery");
var WebComm = require("../src/libs/WebComm");
var Util = require("../src/libs/Util");

var isReady = Util.defer();
var handlers = {
	isReady: function() {
		return isReady.promise;
	}
};
var webComm = WebComm(function(msg) {
	window.postMessage(msg, "*");
}, handlers);

function onReady() {
	return webComm.makeCall("ready", null, 1000)
	.catch(function() {
		return onReady();
	});
}

function setReady(lku) {
	$.extend(handlers, lku);
	isReady.resolve();

	$(function() {
		onReady();
	});
}

document.addEventListener("message", function(event) {
	if(event && webComm.isValidMsg(event.data))
		webComm.dispatchMsg(event.data);
});

exports.openUrl = function(url, body, options) {
	return webComm.makeCall("openUrl", [url, body, options]);
};

exports.setReady = setReady;
exports.defer = Util.defer;


