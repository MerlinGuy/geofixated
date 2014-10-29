var Geof = Geof || {};
Geof.recorder = {

    item:[],
    local_events:[],
    replyCallback:null,
    prefix:'rcdr',
    dialog:null,
    show_test:false,
    max_items:40,
    selected_id:undefined,
    filterRequests: ['notification.read:new','tstate.read'],
    ui:'<span class="ui-icon icon_geof_recorder_enable iconRight" id="btnViewRecorder" title="View Recorder"></span>',

    cfg:{
        file:'recorder_dialog',
        directory:'core/panel/',
        divName:'recorder_dialog',
        dragbar:'recordButtonBar',
        complete_callback:null
    },

    liTemplate:'<li class="ui-widget-content" id="rcdr%id" data-id="%id">'
        + '<label class="idLeft">%title</label><label class="idRightF7">%id</label><br>'
        + '<label class="idLeft">%stime</label><label id="rcdr%idrtime" class="idRightF7">%rtime</label></li>',

    setControl:function (container) {
        var rcdr =  Geof.recorder;
        rcdr.cfg.complete_callback = rcdr.setupButtonEvents;
        if ( container || false ) {
            $('#' + container).html( rcdr.ui );
            TransMgr.addCallbacks( rcdr.addSent, rcdr.addReply );
            PanelMgr.loadDialogY( rcdr.cfg );

        } else {
            alert("Geof.recorder needs a container tag");
        }
    },

    setupButtonEvents:function () {
        var _this = Geof.recorder;
        _this.dialog = $('#' + _this.cfg.divName);

        Gicon.click("recorderBtnDiscard", _this.discard);
        Gicon.click("recorderBtnDiscardAll", _this.clear);
        Gicon.click("btnRunTest", _this.runTest);

        Gcontrol.checkbox('showNotifys', _this.populateList, "show_notifys");

        Gicon.click("closeRecorderDialog", function () {
            _this.dialog.hide()
        });

        $('#olRecorderItems').selectable({
            stop:function () {
                var $selected = $(".ui-selected", this);
                var selectCount = $selected.length;
                var enabled = selectCount > 0;
                _this.selected_id = undefined;
                Gicon.setEnabled('recorderBtnDiscard', enabled);
                _this.selected_id = undefined;
                if (selectCount == 1) {
                    _this.selected_id = $($selected[0]).data('id');
                    var record = _this.getRecord(_this.selected_id);
                    if (record || false) {
                        if (record.data && 'payloads' in record.data) {
                            record.data['payloads'] = "!!NOT SHOWN DUE TO LENGTH!!";
                        }
                        var sent = JSON.stringify(record.sent);
                        $("#divSendJson").html(JFormat.format(sent));

                        var reply = JSON.stringify(record.reply);
                        $('#divReplyJson').html(JFormat.format(reply));
                    }
                } else {
                    _this.clearDetail();
                }

            }
        });

        Gicon.click('btnViewRecorder', _this.show, true);
    },

    runTest:function() {
        var cb = function(req) {
            Geof.log(JSON.stringify(req));
        };
        var test = '{one:1,,:}}}}';
        var r = GRequest.build('test','read',null,{fields:{return_value: base64.encode(test)}});
        Transaction.post(r,cb);

    },

    clearDetail:function() {
        $("#divSendJson").html('');
        $('#divReplyJson').html('');
        Geof.recorder.selected_id = undefined;
    },

    discard:function() {
        var _this = Geof.recorder;
        var $items = $("#olRecorderItems");
        var $selected = $items.find(".ui-selected");
        $selected.each(function () {
            var li = $(this);
            var rid = li.data('id');
            _this.removeRecord(rid);
            li.remove();
            if (_this.selected_id !== undefined && rid == _this.selected_id) {
                _this.clearDetail();
            }
        });
        _this.setDiscardState($items.find("li").length > 0);
        Gicon.setEnabled('recorderBtnDiscard', false);
    },

    show:function () {
        var dialog = Geof.recorder.dialog;
        Geof.recorder.render();
        dialog.show();
        if (Geof.recorder.show_test) {
            $("#btnRunTest").switchClass("hidden","show")
        }else {
            $("#btnRunTest").switchClass("show","hidden");
        }
        Geof.center_in_body(dialog);
    },

    setDiscardState:function (enabled) {
        Gicon.setEnabled('recorderBtnDiscardAll', enabled);
    },

    render:function() {
        var _this = Geof.recorder;
        var tmpl = _this.liTemplate;
        var $ol = $('#olRecorderItems');
        $ol.empty();
        var isChecked = $("#showNotifys").is(":checked");

        JsUtil.iterate(_this.item, function(rec) {
            if ( isChecked || ! _this.isFiltered(rec.title) ) {
                $ol.prepend(Templater.mergeTemplate(rec,tmpl));
            }
        });
    },

    addSent:function (sent) {
        var _this = Geof.recorder;
        if (typeof sent == 'string') {
            sent = JSON.parse(sent)
        }
        var key = sent.tid;
        var stime = DateUtil.currentTime(true);
        var rtime = '00:00:00';
        var record = {'id':key, 'sent':sent, 'reply':null, 'stime':stime, rtime:rtime, 'data':sent, 'title':'unknown'};
        var req1 = sent.requests[0];
        if (req1 || false) {
            if (req1.actionas && req1.actionas.length > 0) {
                record.title = req1.entity + '.' + req1.action + ':' + req1.actionas;
            } else {
                record.title = req1.entity + '.' + req1.action;
            }
        }
        _this.item.push(record);
        _this.addItemToList(record);
        _this.setDiscardState($("#olRecorderItems").find("li").length > 0);
        while (_this.item.length > _this.max_items) {
            var deleted = _this.item.shift();
        }
    },

    addItemToList:function (record) {
        var _this = Geof.recorder;
        if ( $("#showNotifys").is(":checked") || !_this.isFiltered(record.title)) {
            var li = Templater.mergeTemplate(record, _this.liTemplate);
            $('#olRecorderItems').prepend(li);
        }
    },

    isFiltered:function(request) {
        var fr = Geof.recorder.filterRequests;
        for (var indx=0;indx<fr.length;indx++) {
            if (request == fr[indx]) {
                return true;
            }
        }
        return false;
    },

    populateList:function () {
        var _this = Geof.recorder;
        $('#olRecorderItems').empty();
        JsUtil.iterate( _this.item, function(value){
            _this.addItemToList(value);
        });
        _this.setDiscardState($("#olRecorderItems").find("li").length > 0);
    },

    addReply:function (reply) {
        var _this = Geof.recorder;
        if (typeof reply == 'string') {
            reply = JSON.parse(reply);
        }

        _this.decodeRequest(reply);

        JsUtil.get(_this.item,'tid',reply.tid);
        var record = JsUtil.get(_this.item,'id',reply.tid);
        if (record || false) {
            record.reply = reply;
            record.rtime = DateUtil.currentTime(true);
            if (_this.replyCallback != null) {
                _this.replyCallback(record);
            }
        }
    },

    getRecord:function (key) {
        return JsUtil.get(Geof.recorder.item,'id',key);
    },

    removeRecord:function (key) {
        JsUtil.spliceByField(Geof.recorder.item,'id',key);
        $("#olRecorderItems > li[data-id=" + key + "]").remove();
    },

    size:function () {
        return Geof.recorder.item.length;
    },

    clear:function () {
        var _this = Geof.recorder;
        _this.item = [];
        $("#olRecorderItems").empty();
        _this.setDiscardState(false);
        Gicon.setEnabled('recorderBtnDiscard', false);
        _this.clearDetail();
    },

    decodeRequest:function (data) {
        var req;
        var requests = data.requests;
        var rtn = undefined;
        for (var dIndex=0;dIndex < requests.length;dIndex++) {
            req = requests[dIndex];
            if (req.encode) {
                var obj;

                if ( (req.error || false) && req.error.length > 0) {
                    obj = req.error
                } else {
                    obj= req.data;
                }

                if (typeof obj == "string") {
                    rtn = base64.decode(obj);
                } else if (obj instanceof Array) {
                    for (var indx = 0; indx < obj.length; indx++) {
                        obj[indx] = JFormat.decodeJson( obj[indx]);
                    }
                    rtn = obj;

                } else { // not sure try and decode
                    rtn = base64.decode(obj);
                }
                break;
            }
        }
        return rtn;
    }
};