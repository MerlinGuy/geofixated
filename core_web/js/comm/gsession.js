var Geof = Geof || {};
Geof.stats = Geof.stats || {looptime:0,proctime:0};
Geof.session = null;
Geof.permissions = null;
Geof.security = false;

////////////////////////////////////////////////////////////////////////
function GSession(opts) {
    opts = opts || {};

    Geof.session = this;
    this.sessionId = undefined;
    this.permissions = undefined;
    this.security = false;

    this.btnEncrypt = opts.btnEncrypt;

    this.usr = new GUser();
    var auto_login = GLocal.getBoolean("auto_login");
    if (auto_login) {
        this.usr.loginname = GLocal.get('login');
        this.usr.password = GLocal.get('pwd');
    }

    this.urlroot = "";
    this.url = "";
    this.svr_addr = "";
    this.setupUrl();

    this.loginRequest = undefined;
    this.loginCallbacks = [];
    if (opts.login_callback || false) {
        this.loginCallbacks.push(opts.login_callback);
    }

    this.must_use_rsa = opts.must_use_rsa || false;
    this.auto_renew_rsa = this.must_use_rsa || GLocal.getBoolean('rsa_auto_renew');
    this.autoencrypt = false;
    this.sendAesWithLogin = false;
    this.encrypting_callback = undefined;
    this.aes = undefined;
    this.rsa = undefined;
    this.cipher = undefined;
    this.cipher_type = undefined;

    if (opts.rsa_key !== undefined) {
        this.setRsaKey(opts.rsa_key, true);
        this.saveRsaKeyLocal(opts.rsa_key);
    } else {
        var rsa_key = this.getRsaKeyLocal();
        if (rsa_key !== undefined) {
            this.setRsaKey(rsa_key, true);
        }
    }

    if (opts.btnEncrypt||false) {
        var opts2 = {
            id:opts.btnEncrypt,
            offIcon:'icon_geof_notsecure_enable',
            onIcon:'icon_geof_secure_active',
            onState:Gicon.ACTIVE,
            onCallout:this.activateEncryption,
            offCallout:this.deactivateEncryption
        };
        this.btnEncrypt = Gicon.switchDepend(opts2, GLocal.getBoolean('autoencrypt'));

        this.encrypting_callback = function (encrypting, err_msg) {
            this.btnEncrypt.setState(encrypting);
            if ((err_msg || false) && ( Geof.notifier)) {
                Geof.notifier.addLocal(err_msg,3,1);
            }
        };
    }
}

GSession.prototype.login = function(loginName, password) {
    var _this = Geof.session;
    TransMgr.clearTokens();

    if (loginName !== undefined && password !== undefined) {
        _this.usr = new GUser();
        _this.usr.loginname = loginName;
        _this.usr.password = password;
    } else {
        Geof.notifier.addLocalAlert("Missing login credentials");
        return;
    }

    if (_this.rsa == null && _this.auto_renew_rsa) {

        var cbRSA = function (req) {
            if (req.error && req.error.length > 0) {
                Geof.notifier.addLocalAlert(req.error);
            } else {
                _this.login(loginName, password);
            }
        };
        _this.getServerRsaKey(cbRSA, false);
        return;

    } else if ((!Geof.can_clear_text_login) && (_this.rsa == null)) {
        Geof.notifier.addLocalAlert("No clear text login allowed and Rsa Key is not available");
        return;
    }

    _this.rebuildLoginRequest();
    if (GLocal.getBoolean("auto_login")){
        _this.storeAutoLoginLocal(true);
    }

    var trans = new Transaction();
    _this.loginRequest.order = 0;
    trans.addRequest(_this.loginRequest, _this.loginComplete);

    if (this.sendAesWithLogin && _this.aes != null) {
        var aeskey = this.aes.getKeyAsHex();
        var request = GRequest.build('rsaencryption','create', "aeskey", {"aeskey":aeskey});
        request.order = 1;
        var cb = function(req) {
            if (req.data || false ) {
                if (req.data[0].success) {
                    _this.cipher_type = 'aes';
                    _this.cipher = function(text) { return _this.encryptAes(text) };
                    _this.sendEncryptingCB(true);
                }
            }
        };
        trans.addRequest(request, cb);
    }
    trans.sendOverride();
};

GSession.prototype.loginComplete = function(request) {
	try {
        var _this = Geof.session;
        var stats = Geof.stats;
        stats['sendtime'] = (stats.looptime - stats.proctime)/2;

        var data = request.data;
        var logged_in = (data||false) && data.login;

        if (logged_in) {
            var usr = new GUser();
            usr.firstname = data.firstname;
            usr.lastname = data.lastname;
            usr.storageloc = data.storageloc;
            usr.serverconfig = data.serverconfig;
            usr.usrconfig = data.usrconfig;
            usr.usrid = data.usrid;
            usr.password = _this.usr.password;
            usr.loginname = _this.usr.loginname;

            Geof.login.addLogin( _this.usr.loginname,_this.usr.password,true,true);

            _this.usr = usr;
            _this.sessionId = data.sessionid;
            _this.permissions = data.permissions;
            _this.security = (_this.permissions || false);

        } else {
            _this.usr = undefined;
        }
        _this.sendLoginCallbacks(logged_in, request.error);

	} catch (e) {
		alert(e);
	}
};

GSession.prototype.logout = function() {
    var _this = Geof.session;
    Transaction.post( _this.getLogoutRequest(), _this.logoutComplete);
};

GSession.prototype.logoutComplete = function(request) {
    try {
        var error;
        if (request.length > 0) {
            error = request[0].error;
        }
        if (error || false) {
            alert(error)
        } else {
            var _this = Geof.session;
            _this.sessionId = null;
            _this.sendLoginCallbacks(false);
            _this.permissions = undefined;
            _this.security = false;
            _this.aes = null;
            _this.cipher = null;
            var rsa_key = _this.getRsaKeyLocal();
            if (rsa_key !== undefined) {
                _this.setRsaKey(rsa_key, true);
            }
        }

    } catch (e) {
        alert(e);
    }
};

GSession.prototype.rebuildLoginRequest = function() {
	if (this.usr) {
		this.loginRequest = GRequest.build("session","create",null,{});
		var nWhere = {};
		nWhere.loginname = this.usr.loginname;
		nWhere.password = this.usr.password;
		this.loginRequest.data.where = nWhere;
		return this.loginRequest;
	}
	return null;
};

GSession.prototype.getLogoutRequest = function() {
    if (this.usr || false) {
        return GRequest.build("session","delete",null,null);
    }
    return null;
};

GSession.prototype.usrName = function() {
    var usr = Geof.session.usr;
    return usr.lastname + ", " + usr.firstname;
};

GSession.prototype.id = function() {
    return Geof.session.usr.usrid
};

GSession.prototype.JSON = function() {
	return JSON.stringify(this);
};

GSession.prototype.setupUrl = function() {
    var href = Geof.href();
	this.url = href + Geof.webservice;
	var indx = href .indexOf('//') + 3;
	indx = href .indexOf('/', indx) + 1;
	this.urlroot = href .substr(0, indx);
    this.svr_addr = this.urlroot.substring(0, this.urlroot.lastIndexOf(":"));
};

GSession.prototype.canAutoLogin = function() {
    var _this = Geof.session;
    var state = {
        'usrinfo':(_this.usr
            && _this.usr.loginname
            && _this.usr.loginname.length > 0
            && _this.usr.password
            && _this.usr.password.length > 0),
        'has_cipher':(_this.cipher || false),
        'auto_encrypt':GLocal.getBoolean('autoencrypt'),
        'can_login':false
    };
    if (state.usrinfo) {
        state.can_login = (Geof.can_clear_text_login) || (state.has_cipher && state.auto_encrypt);
    }
    return state;
};

GSession.prototype.tryAutologin = function() {
    if (this.canAutoLogin) {
        this.login(this.usr.loginname, this.usr.password);
    } else {
        Geof.session.sendLoginCallbacks(false);
    }
};

GSession.prototype.addLoginCallback = function(callback) {
    this.loginCallbacks.push(callback);
};

GSession.prototype.removeLoginCallback = function(callback) {
    var indx = $.inArray(callback, this.loginCallbacks);
    if (indx != -1) {
        this.loginCallbacks.splice(indx, 1);
    }
};

GSession.prototype.sendLoginCallbacks = function(success, message) {
    for (var indx=0;indx < this.loginCallbacks.length;indx++) {
        try {
            this.loginCallbacks[indx](success, message);
        } catch (e) {
            Geof.notifier.addLocalAlert("GSession.sendLoginCallbacks " + e);
        }
    }
};

GSession.prototype.sendEncryptingCB = function (encrypted, err_msg) {
    var _this = Geof.session;
    _this.setAutoEncrypt(encrypted);
    if (_this.encrypting_callback || false) {
        _this.encrypting_callback(encrypted, err_msg);
    }
};

GSession.prototype.activateEncryption = function() {

    var _this = Geof.session;
    if (_this.aes != null) {
        _this.cipher = function (text) { return _this.encryptAes(text)};
        _this.cipher_type = 'aes';
        _this.sendEncryptingCB(true);
    }

    var rsa_key = _this.getRsaKeyLocal();

    if (rsa_key != null) {
        _this.setRsaKey(rsa_key);
        _this.sendServerAesKey();

    } else if (Geof.session.auto_renew_rsa) {
        var cb = function (success) {
            if (! success) {
                _this.sendEncryptingCB(false,"Failed to get RSA key from server");
            } else {
                _this.sendServerAesKey();
            }
        };
        _this.getServerRsaKey( cb );
    } else {
        _this.sendEncryptingCB(false, "Autorenew of RSA key is deactivated");
    }
};

GSession.prototype.deactivateEncryption = function() {
    var _this = Geof.session;
    _this.cipher = null;
    _this.cipher_type = undefined;
    _this.setAutoEncrypt(false);
    _this.sendEncryptingCB(false);
};

GSession.prototype.setRsaKey = function(RSA, genAES) {
    try {
        var _this = Geof.session;
        _this.rsa_key = {'id':RSA.id,'modulus':RSA.modulus,'exponent':RSA.exponent};
        _this.rsa = new RSAKey();
        _this.rsa.setPublic(RSA.modulus, RSA.exponent);
        _this.cipher = function(text) { return _this.encryptRsa(text)};
        _this.cipher_type = 'rsa';
        _this.saveRsaKeyLocal();
        if (genAES || false) {
            _this.aes = new SjclAes();
            _this.sendAesWithLogin = true;
            _this.sendEncryptingCB(true);
        }
        return true;
    } catch ( e ) {
        alert("setRsaKey: "  + e );
        return false;
    }
};

GSession.prototype.encryptRsa = function (plainText) {
    if (!(plainText || false)) {
        return null;
    }
    if (this.rsa == null) {
        return null;
    }
    if (JsUtil.isObject(plainText)) {
        plainText = JSON.stringify(plainText);
    }

    var encrypted = '';
    var offset = 0;
    var length = plainText.length;
//    Geof.log(plainText);
//    Geof.log("plainText length: " + plainText.length);
    var section;
    var sec_length;
    while (offset < length) {
        sec_length = Math.min(110, length - offset);
        section = plainText.substring(offset, sec_length + offset);
        if (encrypted.length > 0) {
            encrypted += ',';
        }
//        Geof.log("sec_length: " + sec_length);
//        Geof.log(section);
        encrypted += this.rsa.encrypt(section);
        offset += sec_length;
    }
    return encrypted;
};

GSession.prototype.getServerRsaKey = function(callback, genAES) {
    var _this = Geof.session;
    var cb = function (req) {
        var RSA = req.data[0];
        var rtn = false;
        if (RSA || false) {
            if ('id' in RSA && 'modulus' in RSA && 'exponent' in RSA) {
                rtn = _this.setRsaKey({id:RSA.id,modulus:RSA.modulus,exponent:RSA.exponent}, genAES);
            }
        }
        if (callback || false) {
            callback(rtn);
        }
    };

    var obj = {"entity":"rsaencryption","action":"read","actionas":"rsaencryption","data":{}};
    Transaction.post( GRequest.fromJson(obj), cb, undefined, true);
};

GSession.prototype.encryptAes = function (plainText) {
    if (!(plainText || false)) {
        return null;
    }
    if (this.aes == null) {
        return null;
    }
    if (JsUtil.isObject(plainText)) {
        plainText = JSON.stringify(plainText);
    }

    var encrypted = this.aes.encrypt(plainText);
    while ((encrypted.length % 4) > 0) {
        encrypted += '=';
    }

    return encrypted;
};

GSession.prototype.sendServerAesKey = function() {
//    Geof.log('sendServerAesKey');
    var _this = Geof.session;

    if (_this.rsa || false ) {

        var aes = new SjclAes();

        var cb = function(req) {
            try {
                var record = req.data[0];
                var success = record.success;
                if (success) {
                    _this.aes = aes;
                    _this.cipher = function(text) { return _this.encryptAes(text)};
                    _this.cipher_type = 'aes';
                    _this.sendEncryptingCB(true);
                } else {
                    var err = "GSession.sendServerAesKey: failed to send server AES Key";
                    _this.sendEncryptingCB(false,err);
                }
            } catch(e) {
                _this.sendEncryptingCB(false,"GSession.sendServerAesKey: " + e);
            }
        };
        var req = GRequest.build('rsaencryption','create','aeskey',{aeskey:aes.getKeyAsHex()});
        Transaction.post(req, cb);

    } else {
        _this.sendEncryptingCB(false,'RSA key not set');
    }
};

GSession.prototype.saveRsaKeyLocal = function(rsa_key) {
    if (rsa_key === undefined) {
        rsa_key = this.rsa_key;
    }
    GLocal.setJson('rsa_key', rsa_key);
};

GSession.prototype.getRsaKeyLocal = function() {
    var rtn;
    var value = GLocal.get('rsa_key');
    if (value && value.length > 0) {
        try {
            rtn = JSON.parse(value);
        } catch (e) {}
    }
    return rtn;
};

GSession.prototype.saveRsaRenewal = function(auto_renew) {
    if (Geof.session !== undefined) {
        Geof.session.auto_renew_rsa = auto_renew;
    }
    GLocal.setBoolean('rsa_auto_renew', auto_renew);
};

GSession.prototype.clearLocalRsaKey = function() {
    GLocal.set('rsa_key','');
};

GSession.prototype.getClearcode = function(email) {
    if (email || false ) {
        return SjclAes.hashSHA256 (email);
    } else if (Geof.session.usr && Geof.session.usr.email) {
        return SjclAes.hashSHA256 (Geof.session.usr.email);
    } else {
        return null;
    }
};

GSession.savePki = function (req) {
    if ('data' in req) {
        var rsa_key = req.data[0];
        GLocal.setJson('rsa_key', rsa_key);
    }
};

GSession.prototype.can = function (entity, action) {
    if (this.security) {
        if (this.permissions || false) {
            if (entity in this.permissions && action in GRequest.actions) {
                var act = GRequest.actions[action];
                return this.permissions[entity][act] == 1;
            }
        }
        return false;
    } else {
        return true;
    }
};

GSession.prototype.storeAutoLoginLocal = function(autologin) {
    var _this = Geof.session;
    GLocal.setBoolean("auto_login",autologin);
    if (autologin) {
        GLocal.set('auto_login', 'true');
        GLocal.set('login', _this.usr.loginname);
        GLocal.set('pwd', _this.usr.password);
    } else {
        GLocal.set('auto_login', 'false');
        GLocal.set('login', "");
        GLocal.set('pwd', "");
    }
};

GSession.prototype.setAutoEncrypt = function(autoencrypt) {
    Geof.session.autoencrypt = autoencrypt;
    GLocal.setBoolean('autoencrypt', autoencrypt);
};

////////////////////////////////////////////////////////////////////////

function GUser() {
    this.loginname = null;
    this.password = null;
    this.usrid = -1;
    this.sessionid = -1;
    this.permissions = null;
    this.firstname = "";
    this.lastname = "";
    this.storageloc = [];
    this.serverconfig = [];
    this.usrconfig = [];
    this.email = null;
}
