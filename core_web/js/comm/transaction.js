/*
* Created By: Jeff Boehmer
* Company: Ft. Collins Research
* Website: www.ftcollinsresearch.org
*          www.geofixated.org
*/

var TransMgr = (function () {
    return {
        tokens : undefined,
        preCall:[],
        postCall:[],
        loginRequest:null,
        hold:[],
        in_send:false,
        paused:false,
        id:0,
        NO_SESSION:'No active session',
        ui:'<span class="ui-icon icon_geof_transmit iconRight" id="btnTransmit" title="Connected to Server"></span>',

        transmit_func:function(active){},

        setControl:function(container) {
            $('#' + container).html(TransMgr.ui);
            TransMgr.transmit_func = function(isActive) {
                if (isActive) {
                    Gicon.setActive("btnTransmit",true);
                } else {
                    Gicon.setEnabled("btnTransmit",false);
                }
            };
        },

        send:function (t, callback, override_pause) {
            if (t.requests == 0) {
                return;
            }
            if (callback !== undefined) {
                t.callback = callback;
            }

            TransMgr.hold.push(t);

            if ( ! TransMgr.in_send || (override_pause || false)) {
                if ((!TransMgr.paused) || (override_pause || false) ) {
                    TransMgr.in_send = true;
                    TransMgr._send(TransMgr.hold.pop());
                }
            }
        },

        _send:function (t) {
            try {
                var sid = undefined;
                if (t.no_token === undefined) {
                    var token = TransMgr._popToken(t);

                    if (token == undefined ) {
                        sid = t.sid;
                    }
                }

                var reqs = t.requests;
                var session = t.session;
                var sendCopy = {};
                var cipher = null;

                var sendData = {
                    tid: t.tid,
                    sid: sid,
                    token: token
                };

                if (GLocal.getBoolean("debug_writer")) {
                    sendData['debug'] = true;
                }

                var has_payloads = Object.keys(t.payloads).length > 0;
                if (has_payloads) {
                    sendData['payloads'] = t.payloads;
                    sendData['payload_encrypted'] = false;
                }

                if (session !== null) {
                    if (session.cipher != null) {
                        cipher = session.cipher;
                        sendData['cipher_type'] = session.cipher_type;

                        if (has_payloads && GLocal.getBoolean('encrypt_blobs')) {
                            JsUtil.iterate(t.payloads, function(value, key){
                                sendData.payloads[key] = cipher(value);
                            });
                            sendData.payload_encrypted = true;
                        }

                        reqs = cipher(JSON.stringify(reqs));

                        if (session.cipher_type == 'rsa') {
                            sendData['encryptid'] = session.rsa_key.id;
                        } else if (session.cipher_type == 'aes') {
                            sendData['iv'] = session.aes.getIvAsHex();
                        }
                    }
                }

                JsUtil.iterate(sendData, function(value, key){
                    sendCopy[key] = value;
                });
                sendCopy['requests'] = t.requests;
                sendData['requests'] = reqs;

                TransMgr.firePreCall(sendCopy);

                sendData = JSON.stringify(sendData);
                var postTime = Date.now();

                TransMgr.transmit_func(true);

                var receive = function (json, textStatus, jqXHR) {

                    TransMgr.transmit_func(false);

                    Geof.stats['looptime'] = Date.now() - postTime;
                    Geof.stats['proctime'] = json['proctime'];
                    Geof.stats['server_time'] = json['server_time'];

                    if (textStatus == 'success') {
                        if (cipher != null && ("cipher_type" in json)) {
                            try {
                                json.requests = TransMgr.decryptRequests(session, json);
                            } catch (e) {
                                json.requests = "----";
                                json.error = "Error during decryption of requests: " + e;
                            }
                        }

                        TransMgr.firePostCall(json);
                        if (json.error) {
                            TransMgr.handleTransactionError(t, json);

                        } else {
                            var requests = json.requests;

                            TransMgr.firePostCall(json);
                            // call all follow up listeners
                            if (t.callback || false) {
                                t.callback(requests);
                            }

                            for (var indx=0;indx < requests.length;indx++) {
                                var req = requests[indx];
                                req.looptime = Geof.stats['looptime'];
                                if (req.error) {
                                    TransMgr.showRequestError(req);
                                }
                                CallbackList.call(req);
                                CallbackList.alert(req, textStatus, jqXHR);
                            }
                        }

                        TransMgr.processNext();

                    } else {
                        Geof.notifier.addLocalAlert("ERROR IN POST: " + json);
                    }
                };

                $.ajax({
                    type:"POST",
                    url:t.url,
                    data:sendData,
                    success:receive,
                    error:TransMgr.error,
                    dataType:'json'
                });

            } catch (e) {
                Geof.notifier.addLocalAlert("Error occurred in TransMgr.prototype.send : " + e);
            }
        },

        error:function(jqXHR, textStatus, errorThrown) {
            Geof.notifier.addLocalNotice("Error before Transaction receive called " + errorThrown);
        },

        handleTransactionError:function (t, json) {
            Geof.notifier.addLocalAlert(json.error);

            var err_msg = json.error;
            if (json.error.indexOf(TransMgr.NO_SESSION) != -1) {

                if (TransMgr.paused) {
                    err_msg = "Invalid login";
                } else {
                    TransMgr.setPaused(true);
                    var session = Geof.session;
                    if (session.cipher || false && session.rsa || false) {
                        session.cipher = session.rsa;
                        if (session.canAutoLogin()) {
                            session.addLoginCallback(this.restartQueue);
                            session.tryAutologin();
                            return;
                        }
                    }
                }
            } else if (json.error.indexOf('Invalid Encryptid') > -1) {
                if ('rsa' in json) {
                    Geof.session.setRsaKey(json.rsa);
                    PanelMgr.showError("Encryption Error", "Invalid encryption key used - adding server key");
                    return;
                }
            }
            if (session || false ) {
                session.sendLoginCallbacks(false, err_msg);
            } else if (t.callback || false) {
                t.callback( undefined, json.error );
            }
        },

        decryptRequests:function (session, json) {
            var requests = json.requests;

            if (JsUtil.isString(requests)) {
                var decrypted = null;
                var cipher_type = json.cipher_type;
                if (cipher_type === 'aes') {
                    decrypted = session.aes.decryptStringArray(requests, json.iv || null, null);

                    if (decrypted != null) {
                        requests = JSON.parse("[" + decrypted + "]");
                    } else {
                        Geof.notifier.addLocalAlert("Unable to decrypt Requests string");
                    }
                } else {
                    Geof.notifier.addLocalAlert("Unknown Encryption: " + cipher_type);
                }
            }
            return requests;
        },

        restartQueue:function (success) {
            Geof.session.removeLoginCallback(TransMgr.restartQueue);
            if (success) {
                TransMgr.paused = false;
                TransMgr.processNext();
            }
        },

        processNext:function () {
            if (TransMgr.hold.length > 0) {
                TransMgr.in_send = true;
                TransMgr._send(TransMgr.hold.pop());
            } else {
                TransMgr.in_send = false;
            }
        },

        showRequestError:function (req) {
            CallbackList.error(req);
            var err = 'Entity: ' + req.entity + ', action: ' + req.action + ', error: ' + req.error;
            Geof.notifier.addLocalAlert(err);
            PanelMgr.showErrorDialog(req.entity, req.action,  req.error);
        },

        addCallbacks:function(preCallback, postCallback) {
            if (preCallback || false ) {
                this.preCall.push(preCallback);
            }
            if (postCallback || false ) {
                this.postCall.push(postCallback);
            }
        },

        firePreCall:function (json) {
            for (var i = 0; i < this.preCall.length; i++) {
                try {
                    this.preCall[i](json);
                } catch (e) {
                    Geof.notifier.addLocalAlert('Transaction.firePrecall - ' + e);
                }
            }
        },

        firePostCall:function (json) {
            for (var i = 0; i < this.postCall.length; i++) {
                try {
                    this.postCall[i](json);
                } catch (e) {
                    Geof.notifier.addLocalAlert('Transaction.firePostcall - ' + e);
                }
            }
        },

        setPaused:function (isPaused) {
            TransMgr.paused = isPaused;
            if (isPaused) {
                Geof.notifier.stop();
            } else {
                Geof.notifier.start(false);
            }
        },

        clearTokens:function() {
            TransMgr.tokens = [];
        },

        _addTokenRequest:function(transaction) {
            TransMgr.setPaused(true);
            var rTokens = GRequest.build('profile','update','tokens',{});
            rTokens.order = 999;
            var cb = function(req) {
                try {
                    TransMgr.tokens = req.data[0].tokens.split(',');
                    TransMgr.setPaused(false);
                    TransMgr.processNext();
                } catch (e) {
                    Geof.notifier.addLocalAlert("Token request failed: " + e);
                }
            };
            transaction.addRequest(rTokens, cb);
        },

        _popToken: function(transaction) {
            var tokens = TransMgr.tokens;
            var token = undefined;

            if (tokens == undefined || Geof.session.sessionId == undefined) {
                TransMgr._addTokenRequest(transaction);

            } else if (tokens.length > 0) {
                token = tokens.pop();
                if (tokens.length < 2) {
                    TransMgr._addTokenRequest(transaction);
                }

            } else if (Geof.session.sessionId != undefined) {
                Geof.log("Transaction tokens are exhausted");
            }
            return token;
        }
    };
})();

//////////////////////////////////////////////////////////////////////.

function Transaction(session) {
	this.tid = TransMgr.id++;
	this.session = (session || false) ? session : Geof.session ;
	this.url = this.session.url;
	this.requests = [];
    this.payloads = {};
    this.callback = null;
    this.sid = undefined;
}

Transaction.prototype.sendOverride = function (callback) {
    TransMgr.send(this, callback, true);
};

Transaction.prototype.send = function (callback) {
    TransMgr.send(this, callback, false);
};

Transaction.prototype.addRequest = function (request, callback, payload) {
    if ((request || false ) && (request instanceof GRequest)) {
        this.requests.push(request);
        if (callback || false) {
            CallbackList.add(request.requestid, callback);
        }
        if (payload || false) {
            this.payloads[request.requestid] = payload;
        }
    }
    return this;
};

Transaction.prototype.setLastCallback = function (callback) {
    var rlen = this.requests.length;
    if (rlen > 0) {
        CallbackList.add(this.requests[rlen - 1].requestid, callback);
    }
};

Transaction.post = function (request, callback, payload, no_token) {
    var trans = new Transaction(Geof.session);
    trans.addRequest(request, callback, payload);
    if (no_token || false) {
        trans['no_token'] = true;
    }
    TransMgr.send(trans, null, false);
    return trans;
};

Transaction.postNoPause = function (request, callback, payload, no_token) {
    var trans = new Transaction(Geof.session);
    trans.addRequest(request, callback, payload);
    if (no_token || false) {
        trans['no_token'] = true;
    }
    TransMgr.send(trans, null, true);
    return trans;
};

//////////////////////////////////////////////////////////////////////.

var CallbackList = (function () {

    var listeners = [];
    var errorListeners = [];
    var list = [];

    return {
        addListener:function (requestor, signature, callback) {
            if (!(signature in listeners)) {
                listeners[signature] = {};
            }
            var ar = listeners[signature];
            ar[requestor] = callback;
        },

        removeListener:function (requestor, signature) {
            if (signature in listeners) {
                delete listeners[signature][requestor];
            }
        },

        addErrorListener:function (requestor, signature, callback) {
            if (!(signature in errorListeners)) {
                errorListeners[signature] = [];
            }
            var ar = errorListeners[signature];
            ar[requestor] = callback;
        },

        removeErrorListener:function (requestor, signature) {
            if (signature in errorListeners) {
                delete errorListeners[signature][requestor];
            }
        },

        alert:function (request, textStatus, jqXHR) {

            var action = request.actionas ? request.actionas : request.action;
            var signature = request.entity + ":" + action;

            if (signature in listeners) {
                var list = listeners[signature];

                JsUtil.iterate(list, function(callback){
                    if ((callback) && (callback instanceof Function)) {
                        callback(request.data, textStatus, jqXHR);
                    }
                });
            }
        },

        error:function (request) {
            // check for generic listener
            var callback;
            var indx,list;
            if ('*' in errorListeners) {
                list = errorListeners['*'];
                for (indx=0; indx < list.length;indx++) {
                    callback = list[indx];
                    if (callback && (callback instanceof Function)) {
                        callback(request);
                    }
                }
            }

            var action = request.actionas ? request.actionas : request.action;
            var signature = request.entity + ":" + action;

            if (signature in errorListeners) {
                list = errorListeners[signature];

                for (indx=0; indx < list.length;indx++) {
                    callback = list[indx];

                    if ((callback) && (callback instanceof Function)) {
                        if (request.data || false) {
                            callback(request.data);
                        } else {
                            callback(request.error);
                        }
                    }
                }
            }
        },

        add:function (key, callback) {
            list[key] = callback;
        },

        remove:function (key) {
            var rtn = list[key];
            delete list[key];
            return rtn;
        },

        call:function (request) {
            var key = request.requestid;
            var cb = list[key];
            if (cb) {
                delete list[key];
                try {
                    cb(request);
                } catch (e) {
                    Geof.notifier.addLocalAlert(e);
                }
            }
        }
    };

})();

//////////////////////////////////////////////////////////////////////.

function Gajax(session) {
    this.tid = TransMgr.id++;
    this.session = (session || false) ? session : Geof.session ;
    this.url = this.session.url;
    this.callback = null;
}

//var r = GRequest.build('configuration', 'read', null, {});
//var gajax = new Gajax(Geof.session);
//gajax.send(r);

Gajax.prototype.send = function (request) {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("POST", this.url, true);
    xmlhttp.setRequestHeader("Content-Type", "application/json;charset=UTF-8");

    var sendData = {
        tid:this.tid,
        requests:[],
        sid:this.session.sessionId
    };

    sendData.requests.push(request);
    sendData = JSON.stringify(sendData);

    var rtnLen = 0;
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 3) {
            var len = xmlhttp.responseText.length;
            Geof.log("readyState == 3");
            Geof.log(xmlhttp.responseText.substring(rtnLen));
            rtnLen = len
        } else if (xmlhttp.readyState == 4) {
            Geof.log("readyState == 4");
            Geof.log(xmlhttp.responseText.substring(rtnLen));
            Geof.log("Complete");
        }
    };

    xmlhttp.send(sendData);
};



