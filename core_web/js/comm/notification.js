
var Geof = Geof || {};

Geof.notifier = {
    interval : 60,
    index : 0,
    callbacks : [],
    id : null,
    isRunning : false,
    local_events : [],
    last_local : -1,
    dialog:null,
    ui: '<span class="floatLeft w40m0 textRight"><label class="notify_default" id="lblNotificationCount">0</label></span>'
        + '<div id="divNotify" class="floatLeft">'
        + '<span class="ui-icon icon_geof_notify" id="iconNotifications" title="Notifications"></span>'
        + '<div id="divNotifyLed" class="runningBar borderBottomGrey"></div></div>',

    button:'iconNotifications',

    cfg : {
        file:'notification_dialog',
        directory:'core/panel/',
        divName:'popupNotifications',
        autoOpen: true, minHeight: 500, minWidth:500,
        resizable: false, modal:true
    },

    setControl : function(container, refresh_interval, startnow) {
        var _this = Geof.notifier;

        if ( container || false ) {
            $('#' + container).html(_this.ui);
        } else {
            alert("Geof.notifier needs a container tag");
        }

        if (refresh_interval || false) {
            _this.interval = refresh_interval;
        }

        Gicon.click(_this.button,_this.show,true);

        var cfg = _this.cfg;

        cfg.close_callback = function() {
            $("#" + cfg.divName).hide();
        };

        cfg.complete_callback = function() {
            _this.dialog =  $("#" + cfg.divName);
            Gicon.click(_this.button,_this.showNotifications, Gicon.DISABLED);
            Gicon.click('btnNotifyDeselect',_this.funcDeselectAll);
            Gicon.click('btnNotifySelect', _this.funcSelectAll);
            Gicon.click('btnNotifyMarkRead', _this.markRead);
            Gicon.click('btnNotifyRefresh', _this.funcRefresh);
            Gicon.click('btnNotifyClose', cfg.close_callback);

            var opts = {
                id:'btnNotifyRun',
                offIcon:'icon_geof_play_enable',
                onIcon:'icon_geof_pause_enable',
                onState:Gicon.ENABLE,
                onCallout:Geof.notifier.start,
                offCallout:Geof.notifier.stop
            };

            Gicon.switchDepend(opts, true);

            _this.dialog.tooltip();
            if (startnow || false ) {
                _this.start();
            }
        };

        PanelMgr.loadDialogY( cfg );
    },

    start : function(read_now) {
        var _this = Geof.notifier;
        if (_this.isRunning) {
            _this.stop();
        }
        _this.id = setInterval(_this.read,_this.interval * 1000);
        _this.isRunning = true;
        $('#divNotifyLed').switchClass('borderBottomGrey','borderBottomGreen');
        if (read_now || false) {
            _this.read();
        }
    },

    stop : function() {
        var _this = Geof.notifier;
        if (_this.id != null) {
            clearInterval(_this.id);
            _this.id = null;
            $('#divNotifyLed').switchClass('borderBottomGreen','borderBottomGrey');
        }
        _this.isRunning = false;
    },

    show:function() {
        Geof.notifier.dialog.show();
    },

    hide:function() {
        Geof.notifier.dialog.hide();
    },

    addLocalAlert : function( msg) {
        var n = Geof.cntrl.notification;
        Geof.notifier.addLocal(msg, n.levels.Alert ,n.types.Local);
    },

    addLocalNotice : function( msg) {
        var n = Geof.cntrl.notification;
        Geof.notifier.addLocal(msg,n.levels.High,n.types.Local);
    },

    addLocal : function( msg, level, type) {
        var _this = Geof.notifier;
        var notif = {id:_this.last_local--};
        notif.createdate = new Date();
        notif.message = msg;
        notif.level = level;
        notif.type = type;
        if (Geof.session.usr || false) {
            notif.firstname = Geof.session.usr.firstname;
            notif.lastname = Geof.session.usr.lastname;
        } else {
            notif.firstname = 'current';
            notif.lastname = 'user';
        }
        _this.local_events.push(notif);
        _this.setIconStatus();
    },

    readShow : function() {
        Geof.notifier.read(true);
    },

    read : function(showDetails) {
        if (! Geof.logged_in ) {
            return;
        }
        var _this = Geof.notifier;
        var curState = Gicon.getState(_this.button);
        Gicon.setActive(_this.button,true);
        var cb = function(req) {
            Gicon.setValueIcon(curState, _this.button, true);
            _this.queryData = (req.data === undefined) ? [] : req.data;
            _this.setIconStatus();
            if (showDetails || false) {
                _this.showNotifications();
            }
        };
        var reqNew = GRequest.build("notification","read",'new',{});
        Transaction.post(reqNew,cb);
    },

    setIconStatus : function() {
        var _this = Geof.notifier;
        var data = _this.sort(_this.queryData,_this.local_events);
        var group = _this.group(data);
        $('#lblNotificationCount').text(group.count);

        var state = Gicon.DISABLED;
        if (group.levels[3] > 0) {
            state = Gicon.ALERT;
        } else if (group.count > 0) {
            state = Gicon.ENABLE;
        }
        Gicon.setValueIcon(state, _this.button, true);
    },

    showNotifications : function( ) {
        var _this = Geof.notifier;
        var li;
        var $ol = $('#olNotifyItems');
        $ol.empty();

        var data = _this.sort(_this.queryData,_this.local_events);
        Gicon.setEnabled('btnNotifySelect',data.length > 0);

        var row;
        var cntrl = Geof.cntrl.notification;
        for (var indx=0;indx < data.length;indx++) {
            row = data[indx];
            li = cntrl.full_list_tmpl.replace(new RegExp('%typeblock',"g"), cntrl.type_blocks[row.type]);
            li = Templater.mergeTemplate(row, li);
            if (row.level == Geof.cntrl.notification.levels.Alert) {
                li = li.replace(new RegExp('notifyNormal',"g"),'notifyAlert');
            }
            li = li.replace(new RegExp('%lvl',"g"), JsUtil.getName(cntrl.levels,row.level));
            $ol.prepend(li);
        }

        $(".nota_anno").click(function() {
            _this.showAnnotation($(this).data('id'))
        });

        var $icon = $("#" + _this.button);
        var offset = $icon.position();
        var ddtop = offset.top + $icon.height() - 6;
        $(".cbNotifyPop").change(_this.handleSelection);

        var $popup = $("#popupNotifications" );
        $popup.css({top: ddtop, left: offset.left -568, position:'absolute'});
        $popup.draggable({handle:'#notificationButtonBar'});
        var $notifBar = $('#notificationButtonBar');
        $notifBar.mousedown(function() {
            $(this).css('cursor', 'move');
        });
        $notifBar.mouseup(function() {
            $(this).css('cursor', 'default');
        });
        _this.handleSelection();
        $popup.show();

        Gicon.setEnabled('btnNotifyRefresh', true);
    },

    showAnnotation : function(id) {
        //get Annotation for notification
        var annotationid = null;

        var cbAnno = function(file) {
            if (file == null) {
                return;
            }
            var options = {
                'file':file,
                'annotation':{
                    isvisible:true,
                    selected_ids:[annotationid]
                }
            };
//            $("#popupNotifications").hide();
            Filetypes.showPopup2(options);
        };

        var cb = function(req) {
            var data = req.data[0];
            if (data !== undefined) {
                annotationid = data['annotationid'];
                Geof.cntrl.annotation.getFile(annotationid, cbAnno);
            }
        };

        var options = {
            entity:'notification_annotation',
            where:{notificationid:id},
            columns:'annotationid',
            callback:cb
        };
        Geof.model.readOptions(options);

    },

    sort : function(data1, data2) {
        if (data1 === undefined) {
            data1 = [];
        }
        if (data2 === undefined) {
            data2 = [];
        }
        var merged = data1.concat(data2);
        merged.sort(Geof.notifier.sortFunc);
        return merged;
    },

    group : function(data) {
        var group = {'count':0,'levels':[0,0,0,0]};
        if (data || false) {
            group.count = data.length;
            for (var indx=0;indx < data.length;indx++) {
                var lvl = data[indx].level;
                group.levels[lvl] = group.levels[lvl] + 1;
            }
        }
        return group;
    },

    fireCallbacks : function(req) {
        for (var indx=0;indx < this.callbacks.length;indx++) {
            this.callbacks[indx](req);
        }
    },

    addCallback : function(callback) {
        this.callbacks.push(callback);
    },

    removeCallback : function(callback) {
        var len = this.callbacks.length;
        for (var indx=0; indx < len; indx++) {
            if (this.callbacks[indx] == callback) {
                this.callbacks.splice(indx,1);
            }
        }
    },

    handleSelection : function() {
        var hasSelected = $('.cbNotifyPop').filter(":checked").length > 0;
        Gicon.setEnabled('btnNotifyDeselect',hasSelected);
        Gicon.setEnabled('btnNotifyMarkRead',hasSelected);
    },

    funcDeselectAll : function() {
        $('.cbNotifyPop').prop('checked', false);
        Geof.notifier.handleSelection();
    },

    funcSelectAll : function() {
        $('.cbNotifyPop').prop('checked', true);
        Geof.notifier.handleSelection();
    },

    markRead : function() {
        var _this = Geof.notifier;
        var trans = new Transaction(Geof.session);
        var data;
        var hasSendable = false;

        $('.cbNotifyPop').filter(":checked").each(function() {
            var id = $(this).data('id');
            if ( id > 0) {
                data = {'where':{'notificationid':id}};
                trans.addRequest(GRequest.build('notification','update','read',data), null);
                hasSendable = true;
            } else {
                var e = _this.local_events;
                for (var indx=0;indx < e.length;indx++) {
                    if (e[indx].id == id) {
                        _this.local_events.splice(indx,1);
                    }
                }
            }
        });
        if (hasSendable) {
            trans.setLastCallback(Geof.notifier.readShow);
            trans.send();
        } else {
            Geof.notifier.readShow();
        }
        _this.showNotifications();
    },

    funcRefresh : function() {
        var _this = Geof.notifier;
        Gicon.setActive('btnNotifyRefresh', true);
        _this.read(true);
    },

    sortFunc : function(a,b) {
        try {

            if (JsUtil.isString(a.createdate)) {
                a.createdate = DateUtil.parseDate(a.createdate);
            }
            if (JsUtil.isString(b.createdate)) {
                b.createdate = DateUtil.parseDate(b.createdate);
            }
            return a.createdate.getTime() - b.createdate.getTime();
        } catch (e) {
            return 0;
        }
    }
};
