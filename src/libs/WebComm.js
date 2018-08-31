var Util = require("./Util");
var _ = require('lodash');
var MSG_TYPE = "WEB_COMM_CALL::";


function WebComm(sendMsg, handlers) {
	var callbacks = {};
	var callbackSeq = 0;

	function encode(str) {
		return Util.base64EncodeSafe(JSON.stringify(str));
	}

	function decode(str) {
		return JSON.parse(Util.base64DecodeSafe(str));
	}

	function serializeMsgObject(msgObject) {
		return MSG_TYPE + encode(msgObject);
	}

	function serializeAndSendMsg(msgObject) {
		sendMsg(serializeMsgObject(msgObject));
	}

	function isValidMsg(serializedMsgObject) {
		return serializedMsgObject && serializedMsgObject.substr(0, MSG_TYPE.length) === MSG_TYPE;
	}

	function deserializeMsgObject(serializedMsgObject) {
		try {
			if(!isValidMsg(serializedMsgObject)) return null;
			return decode(serializedMsgObject.substr(MSG_TYPE.length));
		} catch(err) {
			console.log(">>deserializeMsgObject -> decode error", err);
		}
		return null;
	}

	function makeCall(func, args, timeoutMs) {
		var id = callbackSeq++;
		var cb = {
			result: Util.defer()
		};

		timeoutMs = Math.max(timeoutMs || 30000, 0);
		cb.timeoutInst = setTimeout(() => {
			delete callbacks[id];
			cb.result.reject(new Error("Timeout"));
		}, timeoutMs);

		callbacks[id] = cb;

		var msgObject = {
			id: id,
			func: func,
			args: args
		};

		//console.log(">>call", msgObject);
		serializeAndSendMsg({type: "CALL", data: msgObject});

		return cb.result.promise;
	}

	function dispatchMsg(serializedMsgObject) {
		if(!serializedMsgObject) return false;

		var msgObject = deserializeMsgObject(serializedMsgObject);
		if(!msgObject) return false;

		if(msgObject.type === "CALL") {
			return handleCall(msgObject.data);
		} else if(msgObject.type === "RESULT") {
			return handleResult(msgObject.data);
		}

		return false;
	}

	function handleResult(msgObject) {
		var cb = callbacks[msgObject.id];
		if(!cb) return false;

		delete callbacks[msgObject.id];
		clearTimeout(cb.timeoutInst);

		if(msgObject.err)
          cb.result.reject(new Error(msgObject.err));
        else
          cb.result.resolve(msgObject.data);

      	return true;
	}

	function handleCall(msgObject) {
		var res = {id: msgObject.id};

		var func = handlers[msgObject.func];
		if(!func) {
			res.err = "No Such Method";
			serializeAndSendMsg(res);
			return;
		}

		func.apply(undefined, msgObject.args || [])
		.then(function(data) {
			res.data = data;
		})
		.catch(function(err) {
			res.err = err && err.message || "Unknown Error";
		})
		.then(function() {
			serializeAndSendMsg({type: "RESULT", data: res});
		});
	}

	return {
		dispatchMsg: dispatchMsg,
		makeCall: makeCall,
		isValidMsg: isValidMsg
	};
}

module.exports = WebComm;

