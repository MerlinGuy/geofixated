//////////////////////////////////////////////////////////////////////

function GRequest() {
    this.requestid = GRequest.sequence.toString();
    GRequest.sequence++;

    this.entity = "";
    this.action = "";
    this.actionas = "";
    this.order = -1;
    this.data = {};
    this.error = null;
}
GRequest.actions = {create:0,read:1,update:2,delete:3,execute:4};

/////////////////////////////////////
////  static variables    ///////////

GRequest.sequence = 0;

GRequest.prototype.JSON = function () {
    return JSON.stringify(this);
};

GRequest.fromJson = function (json) {
    try {
        var request = new GRequest();
        request.entity = json.entity;
        request.action = json.action;
        request.data = json.data;
        if (json.actionas) {
            request.actionas = json.actionas;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

GRequest.parse = function (text) {
    try {
        var obj = JSON.parse(text);
        var request = new GRequest();
        request.entity = obj.entity;
        request.action = obj.action;
        request.data = obj.data;
        if (obj.actionas) {
            request.actionas = obj.actionas;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

GRequest.wrap = function (json) {
    try {
        var request = new GRequest();
        if ('requestid' in json) {
            request.requestid = json.requestid;
            request.order = json.requestid;
        }
        if ('order' in json) {
            request.order = json.order;
        }
        request.entity = json.entity;
        request.action = json.action;
        request.data = json.data;
        if (json.actionas) {
            request.actionas = json.actionas;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

GRequest.build = function (entity, action, actionAs, data) {
    try {
        var request = new GRequest();
        request.entity = entity;
        request.action = action;
        request.data = data || {};
        if (actionAs || false) {
            request.actionas = actionAs;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

GRequest.buildDataFields = function (entity, action, actionAs) {
    try {
        var request = new GRequest();
        request.entity = entity;
        request.action = action;
        request.data = {};
        request.data['fields'] = {};
        if (actionAs) {
            request.actionas = actionAs;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

GRequest.buildDataWhere = function (entity, action, actionAs) {
    try {
        var request = new GRequest();
        request.entity = entity;
        request.action = action;
        request.data = {};
        request.data['where'] = {};
        if (actionAs) {
            request.actionas = actionAs;
        }
        return request;
    } catch (err) {
        alert(err);
        return undefined;
    }
};

