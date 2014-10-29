/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 11/26/13
 * Time: 9:17 AM
 */

var Geof = Geof || {};
Geof.cntrl = Geof.cntrl || {};
Geof.cntrl.upload = Geof.cntrl.upload || {ENCODE_PERCENT : 1.4};

Geof.cntrl.tenant = {
    id:'id',
    data:[],
    row:{},
    entity:'tenant',
    prefix:'tnt_',
    fields:['id','loginname','password','firstname','lastname','initials','email','notes','statusid','attempts','lastattempt'],
    defaults:['-1','','','','','','','',0,0,''],
    exclude:['lastattempt'],
    file_path:'panel/',
    required : ['email','email2','password','password2','firstname','lastname','phone'],
    additional : ['company','addr1','addr2','city','state','zipcode'],
    dlg:undefined,

    editConfig: {
        dialogName:'tenant_edit', divName:'editTenant',
        autoOpen: true, minHeight: 340, minWidth:428,
        resizable: false, modal:true
    },

    addNew: function () {
        var _this = Geof.cntrl.tenant;
        Geof.cntrl.showEditDialog(_this, {}, true,_this.initAddNew);
    },

    save:function() {
        var _this = Geof.cntrl.tenant;
        var fields = Geof.cntrl.getFormData(_this.prefix, _this.required.concat(_this.additional));
        fields['loginname'] = fields.email;
        var data = {'fields':fields};
        var cb = function(req) {
            if ( req.error == undefined || req.error.length == 0) {
                PanelMgr.showMessage('Registration Complete','An activation link has been emailed to you.');
            }
            _this.dlg.dialog('close');
        };
        Gicon.setActive('btnTenantSave', true);
        Geof.model.create(data,_this, cb);
    },

    initAddNew:function(dlg) {


        var _this = Geof.cntrl.tenant;
        _this.dlg = dlg;

        var data = {
            email:"jeff.boehmer@gmail.com",
            email2:"jeff.boehmer@gmail.com",
            password:'Change_M3',
            password2:'Change_M3',
            firstname:'Jeff',
            lastname:'Boehmer',
            phone:'970-556-8303',
            company:'Ft. Collins Research, LLC',
            addr1:'1168 Picard Ln.',
            addr2:'',
            city:'Fort Collins',
            state:'CO',
            zipcode:'80526'
        };
        Geof.cntrl.setFormData(_this.prefix, data, undefined);

        var req = _this.required;
        var pre = _this.prefix;

        for (var indx=0;indx < req.length;indx++) {
            var $fld = $("#" + pre + req[indx]);
            $fld.change(_this.validate);
        }
        $('#tnt_email').blur(_this.validate);
        $('#tnt_password').blur(_this.validate);
        $('#tnt_email2').blur(_this.validate);
        $('#tnt_password2').blur(_this.validate);
        Gicon.click("btnTenantSave",_this.save);
    },

    validate:function() {
        var _this = Geof.cntrl.tenant;
        var req = _this.required;
        var pre = _this.prefix;
        var noError = true;

        for (var indx=0;indx < req.length;indx++) {
            var $fld = $("#" + pre + req[indx]);
            var value = $fld.val();
            $fld = $("#r_" + req[indx]);
            $fld.removeClass();
            if ( value.length == 0) {
                $fld.addClass('labelReq');
                noError = false;
            } else {
                $fld.addClass('labelReqOk');
            }
        }

        noError = noError && _this.validateMatch('email');
        noError = noError && _this.validateMatch('password');
        Gicon.setEnabled('btnTenantSave', noError);
    },

    validateMatch:function(id) {
        var _this = Geof.cntrl.tenant;

        var pre = _this.prefix;
        var $error = $("#addTenantError");
        var errmsg = '';
        var $fld1 = $("#" + pre + id);
        var $fld2 = $("#" + pre + id + "2");
        var val1 = $fld1.val();
        var val2 = $fld2.val();

        if (val1.length == 0 && val2.length == 0) {
            return true;
        }
        var $lbl1 = $("#r_" + id);
        var $lbl2 = $("#r_" + id + "2");
        $lbl1.removeClass();
        $lbl2.removeClass();
        if (val1 != val2) {
            errmsg = id + 's do not match';
            $lbl1.addClass('labelReq');
            $lbl2.addClass('labelReq');
        } else {
            $lbl1.addClass('labelReqOk');
            $lbl2.addClass('labelReqOk');
        }
        $error.text(errmsg);
        return errmsg.length == 0;
    }


};

Geof.cntrl.calendar = {
    selectedDay:undefined,
    startDay:undefined,
    selectedColor:'lightblue',
    maxEventColor:'pink',
    normalColor:'white',
    fc:undefined,
    cal_id:undefined,
    selectedDayCallback : undefined,
    selectEventBars:[],
    selectedEvent:undefined,
    selectedEventCallback : undefined,
    hasSelectedEventCB: undefined,
    dates:{},
    // '0':'None', '1':'Running', '2':'Blocked', '3':'Paused', '4':'Shutdown', '5':'Shutoff', '6':'Crashed', '7':'Suspended', '8':'Last'
    colors : ['black','lightgreen','orange','darkgray','yellow','lightgray','pink','blue'],

    initialize:function(cal_id, selectDayCB, selectEventCB, hasSelectedEventCB) {
        var _this = Geof.cntrl.calendar;
        _this.selectedDayCallback = selectDayCB;
        _this.selectedEventCallback = selectEventCB;
        _this.hasSelectedEventCB = hasSelectedEventCB;
        _this.cal_id = cal_id;
        _this.fc = $('#' + cal_id);

        _this.fc.fullCalendar({
            contentHeight:600,
            dayRender: _this.renderDay,
            dayClick: _this.selectDay,
            eventClick: _this.selectEvent
        });
    },

    selectDay:function(date) {
        var _this = Geof.cntrl.calendar;
        if (_this.selectedDay !== undefined) {
            _this.selectedDay.css('background-color','white');
        }
        var $this = $(this);
        if ($this.is( _this.selectedDay)) {
            _this.selectedDay = undefined;
            _this.startDay = undefined;
        } else {
            _this.selectedDay = $this;
            $this.css('background-color', _this.selectedColor);
            _this.startDay = date;
        }
        if (_this.selectedDayCallback !== undefined) {
            _this.selectedDayCallback(date);
        }
    },

    selectEvent:function(calEvent) {
        var _this = Geof.cntrl.calendar;

        if (_this.selectedEventCallback !== undefined) {
            var bgColor = calEvent.backgroundColor;
            var hilite = _this.selectedEventCallback(calEvent);

            var ceTitle = calEvent.title;

            JsUtil.iterate(_this.selectEventBars, function(bar) {
                bar.css('background-color', bar.origColor);
            });

            _this.selectEventBars = [];
            var lastEvent = _this.selectedEvent;
            var lastTitle = lastEvent === undefined ? undefined : lastEvent.title;
            _this.selectedEvent = undefined;

            if (hilite && (ceTitle != lastTitle)) {

                _this.fc.find('.fc-event-title').each(function() {
                    var $this = $(this);

                    if ($this.text() == ceTitle) {
                        _this.selectEventBars.push($this);
                        $this.origColor = bgColor;
                        $this.css('background-color', 'red');

                        $this.parents('.fc-event').each(function () {
                            var $seb = $(this);
                            _this.selectEventBars.push($seb);
                            $seb.origColor = bgColor;
                            $seb.css('background-color', 'red');
                        });

                    }

                });
                _this.selectedEvent = calEvent;
            }
        }

        if (_this.hasSelectedEventCB || false ) {
            _this.hasSelectedEventCB(_this.selectedEvent || false);
        }
    },

    renderDay:function(date, cell) {
        var _this = Geof.cntrl.calendar;
        var count = _this.getDayEventCount(date);
        if (count !== undefined && count >= Geof.max_guests) {
            cell.css("background-color", _this.maxEventColor);
        }
//        else {
//            cell.css("background-color", _this.normalColor);
//        }
    },

    getDayEventCount:function(date) {
      if (date || false) {
          return Geof.cntrl.calendar.dates[date.toDateString()];
      } else {
          return undefined;
      }
    },

    getSelectedDayCount:function() {
        var _this = Geof.cntrl.calendar;
        if (_this.startDay === undefined) {
            return undefined;
        }
        var count = _this.dates[_this.startDay.toDateString()];
        return count === undefined ? 0 : count;
    },

    renderEvent:function(opts) {
        var _this = Geof.cntrl.calendar;
        _this.fc.fullCalendar('renderEvent',{
            title: opts.name,
            start: opts.startdate,
            end: opts.enddate,
            usrid:opts.usrid,
            allDay: true,
            domainid:opts.domainid,
            backgroundColor:_this.colors[opts.status],
            borderColor:'black',
            textColor:'black'
        }, true);

    },

    addDomainDates: function(opts) {
        var _this = Geof.cntrl.calendar;
        var dates = _this.dates;
        if ((opts.startdate == undefined) || (opts.enddate == undefined)) {
            return;
        }
        var startTime = opts.startdate.getTime();
        var endTime = opts.enddate.getTime();

        do {
            var key = (new Date(startTime)).toDateString();
            dates[key] = ( dates[key] == undefined) ? 1 : dates[key] + 1;
            startTime += Geof.oneDay;
        } while ( startTime <= endTime);
        _this.renderEvent(opts);
    },

    render:function() {
        Geof.cntrl.calendar.fc.fullCalendar('render');
    },

    clear:function() {
        var _this = Geof.cntrl.calendar;
        _this.dates = {};
        _this.selectedDay = undefined;
        _this.startDay = undefined;
        _this.fc.fullCalendar('removeEvents');
        _this.fc.fullCalendar('destory');
        _this.fc.fullCalendar('render');
    }

};

Geof.cntrl.domain = {

    id:'id',
    data:[],
    row:{},
    owners:{},
    entity:'domain',
    prefix:'domain_',
    fields:['id','name','xmlpath','ipaddress','ipmac','imageid',
            'buildplanid','description','createdate',
            'status','type','startdate','enddate','runnable','dirname','filename'],
    defaults:[-1,'','','',-1,-1,'',null,0,0],
    domain_state:{
        '0':'None',
        '1':'Running',
        '2':'Blocked',
        '3':'Paused',
        '4':'Shutdown',
        '5':'Shutoff',
        '6':'Crashed',
        '7':'Suspended',
        '8':'Last'
    },
    domain_type:{
        '-1':'None',
        '0':'DB',
        '1':'Host',
        '2':'DB+Host'
    },
    state_info:{
        start:{icon:'icon_geof_play',title:"Start Domain",action:0},
        shutdown:{icon:'icon_geof_stop',title:"Shutdown Domain",action:1},
        pause:{icon:'icon_geof_pause',title:"Pause Domain",action:2}
    },

    exclude:[],
    list_columns: "id,name,status,type,description,dirname",
    order_by:"name,createdate",
    title:'Domains',
    olclass:'olDomain',
    file_path:'panel/',
    domain_name : undefined,
    selectedid:undefined,
    gprogress:undefined,

    list_tmpl : '<li class="ui-widget-content" data-dirname="%dirname" data-name="%name">'
        + '<label class="data flw180">%name</label>'
        + '<label class="data flw60">%usrid</label>'
        + '<label class="data flw80">%status</label>'
        + '<label class="data">%type</label></li>',

    image_tmpl : '<li class="ui-widget-content" data-dirname="%dirname" data-name="%name">'
        + '<label class="data flw160">%path</label>',

    initialize: function(){
        var _this = Geof.cntrl.domain;
        Gicon.click("btnRefreshDomain", _this.populateList);
        Gicon.click("btnDiscardDomain", _this.discard);
        Gicon.click("btnSeverGuest", _this.sever);
        Gicon.click("btnNewDomain", function(){
            _this.setDetail(undefined);
        });

        Gicon.click('btnShowCloneDialog', function () {
            var row = _this.row;
            _this.show_create_guest_dialog(row.name, row.buildplanid,_this.populateList);
        });

        Gicon.click('btnImportGuest', _this.importGuest);

        Gicon.setEnabled("btnNewDomain", true );
        JsUtil.addOptions('domain_status',_this.domain_state);
        JsUtil.addOptions('domain_type',_this.domain_type);
        $("#domain_startdate").datepicker();
        $("#domain_enddate").datepicker();

        $("#domain_buildplanid").blur(_this.validateSave);
        $("#domain_startdate").blur(_this.validateSave);
        $("#domain_enddate").blur(_this.validateSave);
        $("#domain_description").blur(_this.validateSave);
        $("#domain_runnable").change(_this.validateSave);

        Gicon.click('btnDomainSave',_this.save);
        _this.populateList();
    },

    createGuest :function (opts) {

        var _this = Geof.cntrl.domain;
        var cb = function () {
            if (opts.completeCB || false) {
                opts.completeCB();
            }
            var loopTime = Geof.stats.looptime / 1000;
            var ltMin = parseInt(loopTime / 60);
            var ltSecs = parseInt(loopTime - (ltMin  * 60));
            PanelMgr.showMessage("Create Guest", "Completed " + ltMin + ":" + ltSecs);
        };

        var data = {
            fields:{
                is_tenant:(opts.is_tenant||false),
                macipid: opts.macipid || false ? opts.macipid : -999,
                buildplanid: opts.buildplanid || false ? opts.buildplanid : -999,
                name: opts.name || false ? opts.name : '',
                sourceName: opts.sourceName || false ? opts.sourceName :'',
                startdate : opts.startdate || false ? opts.startdate : '',
                enddate: opts.enddate || false ? opts.enddate : '',
                start_guest : (opts.start_guest||false),
                connect_guest: (opts.connect_guest||false),
                overwrite_guest : (opts.overwrite_guest||false),
                shutdown_source : (opts.shutdown_source||false),
                dirName: opts.dirname || false ? opts.dirname : '',
                fileName: opts.filename || false ? opts.filename: ''
            }
        };
        Geof.model.createAs(data, _this, "guest", cb);
    },

    validateSave:function() {
        Gicon.setEnabled("btnDomainSave", Geof.cntrl.domain.row !== undefined);
    },

    populateList:function(domain_name) {
        var _this = Geof.cntrl.domain;
        _this.row = undefined;

        _this.clearDetail();
        Gicon.setActive("btnRefreshDomain", true );
        Gicon.setEnabled("btnDiscardDomain", false );
        Gicon.setEnabled('btnDomainSave',false);

        var $items = $('#olDomains');
        $items.empty();

        var cb = function(req) {
            Gicon.setEnabled("btnRefreshDomain", true );
            _this.data = req.data;

            var lookups = {'status':_this.domain_state,'type':_this.domain_type};
            Templater.processData (_this.data, $items, _this.list_tmpl, lookups);

            $items.selectable({
                stop: function() {
                    var $selected = $( ".ui-selected", this);
                    var enabled = $selected.length > 0;
                    Gicon.setEnabled("btnDiscardDomain", $selected.length == 1 );
                    if (enabled) {
                        if ( $selected[0].dataset.dirname == '%dirname' ) {
                            $selected[0].dataset.dirname = $selected[0].dataset.name;
                        }
                        _this.row = JsUtil.get(_this.data,'dirname', $selected[0].dataset.dirname);
                        _this.setDetail( $selected[0].dataset.dirname );
                    }
                }
            });

            if (domain_name !== undefined) {
                _this.setDetail(domain_name);
                $('#olDomains li[data-name="' + domain_name + '"]').addClass("ui-selected");
            }
        };
        Geof.model.read(null,_this, cb,'generic');
        _this.reloadBuildplans();
    },

    setDetail:function(dirname) {
        var _this = Geof.cntrl.domain;
        _this.clearDetail();

        if (dirname !== undefined) {
            var row = JsUtil.get(_this.data, "dirname", dirname);

            if (row == undefined ) {
                var row = JsUtil.get(_this.data, "name", dirname);
            }

            if ( row !== undefined) {
                if ( row.buildplanid === undefined) {
                    row['buildplanid'] = null;
                }
                _this.row = row;

                if (row.hasFullDetail) {
                    row.startdate = DateUtil.toPickerDate(row.startdate);
                    row.enddate = DateUtil.toPickerDate(row.enddate);
                    var lookups = {'status':_this.domain_state,'type':_this.domain_type};
                    Geof.cntrl.setEditFields( _this, row , lookups);
                    _this.setAssocDetail();

                } else {
                    _this.getDetail(row)
                }
                var type = _this.row.type;
                var isGuest = (type == 1 || type == 2);
                Gicon.setEnabled("btnShowCloneDialog", isGuest);
                Gicon.setEnabled("btnSeverGuest", type == 2);
                Gicon.setEnabled("btnImportGuest", type == 1);
            }
        }
        _this.setStatusBtns();
    },

    setAssocDetail:function() {
        $("#domain_ipmac").text('');
        $("#domain_owner").text('');
        Gicon.setEnabled("macipReachable",false);

        var _this = Geof.cntrl.domain;
        var row = _this.row;
        if (row === undefined) {
            return;
        }

        var trans = new Transaction();

        var usrid = row.usrid;
        if (usrid != undefined && usrid > -1) {
            var owner = _this.owners[usrid];
            if ( owner != undefined) {
                $("#domain_owner").text(owner);
            } else {
                var udata = {columns:'firstname,lastname,loginname',where:{id:usrid}};
                var ur = GRequest.build('usr','read',null,udata);
                trans.addRequest(ur, function(uReq){
                    if ('data' in uReq) {
                        udata = uReq.data[0];
                        if (udata !== undefined) {
                            _this.owners[usrid] = udata.lastname + ', ' + udata.firstname + ' (' + udata.loginname + ')';
                            $("#domain_owner").text(_this.owners[usrid]);
                        }
                    }
                });
            }
        }

        var Domain = row.name;
        if (Domain != undefined && Domain.length > -1) {
            // get the ip and mac address
            var mData = {columns:'ipaddress,macaddress',where:{domain:Domain}};
            var mr = GRequest.build('macip','read',null,mData);
            trans.addRequest(mr, function(mReq){
                mData = mReq.data[0];
                if (mData !== undefined) {
                    $("#domain_ipmac").text(mData['ipaddress'] + ' [' + mData['macaddress'] + ']');
                }
            });

            // check to see if the guest can is reachable
            var pData = {where:{domain:Domain}};
            var pr = GRequest.build('guest','read','ping',pData);
            Transaction.post(pr, function(pReq){
                pData = pReq.data[0];
                if (pData !== undefined) {
                    Gicon.setActive("macipReachable",pData['reachable']);
                }
            });
        }

        if (trans.requests.length > 0) {
            trans.send();
        }
    },

    getDetail:function(domain) {
        var _this = Geof.cntrl.domain;
        var data = {where:{'dirname':domain.dirname}};
        var rDFD = GRequest.build('domain','read','detail',data);
        var do_decode = true;
        Transaction.post(rDFD, function(req) {
            var data = req.data;
            var row = JsUtil.get(_this.data, "dirname", domain.dirname);
            row.hasFullDetail = true;
            Object.keys(data).forEach(function(key) {
                if (key == 'xml_str') {
                    var xml_str = data[key];
                    row['xml_str'] = xml_str;
                    row[key] = JsUtil.xmlToJson(xml_str, do_decode);
                } else {
                    row[key] = data[key];
                }
            });
            _this.setDetail(row.dirname);
            if (row.xml_str || false) {
                _this.setImageFilesXml(row);
            }
        });
    },

    clearDetail:function() {
        Geof.cntrl.clearDialog(Geof.cntrl.domain, undefined);
        $("#olDomImageFiles").empty();
    },

    setImageFilesXml:function(row) {
        if (row.xml_str || false) {
            var devices = row.xml_str.domain.devices;
            var images = [];
            Object.keys(devices).forEach(function(key) {
                if (key == 'disk') {
                    var fullpath = devices[key].source._file;
                    images.push({
                        path:fullpath,
                        id:0,
                        name : fullpath.replace(/^.*[\\\/]/, '')
                    });
                }
            });
            Templater.processData (images, $("#olDomImageFiles"), Geof.cntrl.domain.image_tmpl, null);
        }
    },

    setStatusBtns:function() {
        var _this = Geof.cntrl.domain;
        var btnState = false;

        var row = _this.row;
        var type = row ? (row.type ? row.type : -1) : -1;

        if (type > 0) {
            var $btn = $("#btnDomainStatus");
            var icon = Gicon.getIcon('btnDomainStatus');
            var sinfo = _this.state_info.start;

            btnState = true;
            if (row.status == 1) {
                sinfo = _this.state_info.shutdown;
            } else if (row.status == 5) {
                sinfo = _this.state_info.start;
            }
            $btn.prop('title',sinfo.title);
            if (icon != sinfo.icon) {
                $btn.switchClass(icon, sinfo.icon);
            }

            Gicon.click('btnDomainStatus',function () {
                Gicon.setActive('btnDomainStatus', true);
                _this.setDomainState(sinfo.action);
            });
        }
        Gicon.setEnabled('btnDomainStatus',btnState);
    },

    setDomainState:function( action ) {
        var row = Geof.cntrl.domain.row;
        var name = row.name;
        var data = {fields:{'action':action},where:{'dirname':dirname}};
        var reqState = GRequest.build('domain','update','state',data);
        Transaction.post(reqState, function() {
            Gicon.setEnabled('btnDomainStatus',true);
            row.hasFullDetail = false;
            Geof.cntrl.domain.populateList(name);
        });
    },

    reloadBuildplans:function() {
        var _this = Geof.cntrl.buildplan;
        var buildplaid = (_this.row || false) ? _this.row.buildplanid : -1;
        var cb = function (req) {
            var data = req.data;
            data.unshift({id:-1,name:'None'});
            JsUtil.setOptions('domain_buildplanid', data, 'name', 'id', buildplaid );
        };
        Geof.model.read(null, Geof.cntrl.buildplan, cb);
    },

    save:function () {
        var _this = Geof.cntrl.domain;
        Gicon.setActive('btnDomainSave',true);

        var flds = Geof.cntrl.getDialogData(_this);

        flds.status = JsUtil.reverseLookup(_this.domain_state, flds.status);
        flds.type = JsUtil.reverseLookup(_this.domain_type, flds.type);
        flds.startdate = DateUtil.getSvrDate($("#domain_startdate").val(), '/',null,true);
        flds.enddate = DateUtil.getSvrDate($("#domain_enddate").val(), '/',null,true);
        flds.runnable = $("#domain_runnable").prop("checked");

        if (flds.startdate || false ) {
            flds.startdate += ' 00:00:00';
        }

        if (flds.enddate || false ) {
            flds.enddate += ' 00:00:00';
        }

        var data = {fields:flds};

        var cb = function () {
            Gicon.setEnabled('btnDomainSave',false);
            _this.populateList(flds.name);
        };

        var update = (flds.id || false) ? flds.id >= 0 : false;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    importGuest:function () {
        var _this = Geof.cntrl.domain;
        Gicon.setActive('btnImportGuest',true);
        var row = _this.row;

        if ( row.buildplanid == null ) {
            row.buildplanid = -1;
        }

        row.type = JsUtil.reverseLookup(_this.domain_type, 'DB+Host');
        Geof.model.createAs(
            {'fields':row},
            _this,
            'import',
            function () {
                Gicon.setEnabled('btnImportGuest',true);
                _this.populateList( row.name );
            }
        );
    },

    discard: function() {
        var _this = Geof.cntrl.domain;
        var row = _this.row;

        if (row !== undefined ) {
            _this.discardDomain(row.dirname, row.id, _this.populateList);
        }
    },

    discardDomain: function(dirname, dirid, callback) {
        var cb = function (do_discard) {
            if (do_discard) {
//                var reqIF = GRequest.build('usr_domain', 'delete', null, {'where':{'dirid':dirid}});
//                Transaction.post(reqIF, callback);
//                var reqIF = GRequest.build('macip', 'delete', null, {'where':{'domain':dirname}});
//                Transaction.post(reqIF, callback);
                var reqIF = GRequest.build('domain', 'delete', null, {'where':{'dirname':dirname}});
                Transaction.post(reqIF, callback);
            }
        };
        PanelMgr.showConfirm("Delete Domain", "Delete Selected Domain?", cb);
    },

    sever: function() {
        var _this = Geof.cntrl.domain;
        if (_this.row === undefined ) {
            return;
        }

            var cb = function (do_sever) {
            if (do_sever) {

                _this.row.hasFullDetail = false;
                var id = _this.row.id;

                var cfg = {
                    entity:_this.entity,
                    where:{'id':id},
                    actionAs:'sever',
                    callback:function(){_this.populateList(name);}
                };
                Geof.model.deleteAs(cfg);
            }
        };
        PanelMgr.showConfirm("Sever Domain", "Sever Domain From Database?", cb);
    },

    validateClone : function () {
        var enabled = $("#newGuestName").val().length > 0;
//        var ipCount = $("#olMacip .ui-selected").length;
//        enabled = enabled && (ipCount == 1);
        Gicon.setEnabled("btnCreateGuest", enabled);
    },

    populate_macip : function () {
        var $macip = $("#olMacip");
        $macip.empty();
        var cb = function (req) {
            Templater.processData (req.data, $macip, Geof.cntrl.macip.list_tmpl, null);
            $("#olMacip").find("*").andSelf().attr("unselectable", "on");
        };
        Geof.model.read(null, Geof.cntrl.macip, cb);
    },

    show_create_guest_dialog:function(sourceDomain,buildplanid,completeCB) {
        var $dlg = null;

        var createGuest = function () {
            var _this = Geof.cntrl.domain;

            Gicon.setActive('btnCreateGuest', true);
            var cb = function () {
                if (completeCB || false) {
                    completeCB();
                }
                var loopTime = Geof.stats.looptime / 1000;
                var ltMin = parseInt(loopTime / 60);
                var ltSecs = parseInt(loopTime - (ltMin  * 60));
                Gicon.setEnabled('btnCreateGuest', true);
                _this.gprogress.prependText("Guest created " + ltMin + ":" + ltSecs);
                _this.populate_macip();
            };

            var startdate = DateUtil.getSvrDate($("#guest_startdate").val(), '/',null,true);
            var enddate = DateUtil.getSvrDate($("#guest_enddate").val(), '/',null,true);

            var data = {
                fields:{
                    // macipid is assigned in macipRequest.java
//                    macipid:li.dataset.id,
                    macipid:-1,
                    buildplanid:buildplanid,
                    name:$("#newGuestName").val(),
                    sourceName:sourceDomain,
                    startdate : startdate,
                    enddate :enddate,
                    start_guest : $("#startGuest").is(':checked'),
                    connect_guest: $("#connectGuest").is(':checked'),
                    overwrite_guest : $("#overwriteGuest").is(':checked'),
                    shutdown_source : $("#showdownSource").is(':checked'),
                    dirname: _this.dirName,
                    filename: _this.fileName
                }
            };
            Geof.model.createAs(data, _this, "guest", cb);
        };


        var initializeCB = function (dlg) {
            var _this = Geof.cntrl.domain;

            $dlg = dlg;
            $("#newGuestName").blur(_this.validateClone);
            Gicon.click("btnCreateGuest", createGuest);
            $dlg.show();
            $("#source_domain").text(sourceDomain);

            $("#guest_startdate").datepicker();
            $("#guest_enddate").datepicker();

            _this.gprogress = new GProgress();
            _this.gprogress.setControl('create_guest_progress');

            _this.populate_macip();
        };

        PanelMgr.loadDialogX({
            file: 'create_guest',
            divName: 'create_guest',
            title:'Create Guest',
            directory: 'panel/',
            autoOpen: true,
            minHeight: 370,
            minWidth: 400,
            resizable: false,
            modal: true,
            complete_callback:initializeCB,
            close_callback:null
        });
    }

};

Geof.cntrl.guest = {

    id:'id',
    data:[],
    row:{},
    entity:'guest',
    prefix:'',
    fields:['id','name','xmlpath','ipaddress','imageid',
        'buildplanid','description','createdate',
        'status','type','startdate','enddate'],
    defaults:[-1,'','','',-1,-1,'',null,0,0],

    exclude:[],
    list_columns: "id,name,status,type,description",
    order_by:"name,createdate",
    title:'Guests',
    olclass:'olGuest',
    job_id:undefined,
    gprogress : undefined,

    getGuestInfo:function(cb) {
        Transaction.post(GRequest.build('configuration','read','guest_info',{}), cb);
    },

    discardGuest: function(domainid, callback) {
        var cb = function (do_discard) {
            if (do_discard) {
                var reqIF = GRequest.build('guest', 'delete', null, {'where': {'id': domainid}});
                Transaction.post(reqIF, callback);
            }
        };
        PanelMgr.showConfirm("Delete Demo", "Delete Selected Demo?", cb);
    },

    showCreateDemo:function(startDate, completeCB) {
        var _this = Geof.cntrl.guest;
        var $dlg = null;

        var createDemo = function () {
            Gicon.setActive('btnTenantCreateDemo', true);

            var demoCreatedCB = function () {
                Gicon.setEnabled('btnTenantCreateDemo', false);

                if (completeCB || false) {
                    completeCB();
                }
                var loopTime = Geof.stats.looptime / 1000;
                var ltMin = parseInt(loopTime / 60);
                var ltSecs = parseInt(loopTime - (ltMin  * 60));
                _this.gprogress.prependText('Guest created ' + ltMin + ":" + ltSecs);
            };

            var days = parseInt($('#demoLength').val());
            var millis = startDate.getTime() + (Geof.oneDay * days);
            var endDate = new Date(millis);

            var data = {fields:{
                is_tenant:true,
                macipid: -999,
                buildplanid: -999,
                name:'',
                sourceName: '',
                startdate : DateUtil.formatSvrDate(startDate),
                enddate: DateUtil.formatSvrDate(endDate),
                start_guest : false,
                connect_guest: false,
                overwrite_guest : false,
                shutdown_source : false
            }};

            var r = GRequest.build('guest','create',null, data);
            var trans = Transaction.post(r, null);
            _this.gprogress.startProgress( trans.tid, demoCreatedCB);
        };

        var initializeCB = function (dlg) {
            $dlg = dlg;
            $dlg.show();
            Gicon.click("btnTenantCreateDemo", createDemo);
            $("#demoStartDate").text(startDate.toDateString());

            var options = {};
            for (var indx=1;indx<15;indx++) {
                options[indx] = indx + ' days';
            }
            JsUtil.addOptions('demoLength',options, 14);

            _this.gprogress = new GProgress();
            _this.gprogress.setControl('create_demo_progress');

            Transaction.post(GRequest.build('buildplan','read','demo'),function(req) {
                var data = req.data[0];
                $('#demoBuildplan').text(data ? data.name : '');
            });
        };

        PanelMgr.loadDialogX({
            file: 'create_demo',
            divName: 'create_demo',
            title:'Create Demo',
            directory: 'panel/',
            autoOpen: true,
            minHeight: 150,
            minWidth: 260,
            resizable: false,
            modal: true,
            complete_callback:initializeCB,
            close_callback:null
        });
    }
};

function GProgress() {
    this.pbID = undefined;
    this.taID = undefined;
    this.$pb = undefined;
    this.$ta = undefined;
    this.isComplete = false;
    this.complete_callback = undefined;
    this.tid = undefined;
    this.interval = 5000;
}

GProgress.html = '<div class="inline4"><label class="stdlabel floatLeft">Progress:</label><div id="pb%" class="pbar200 floatLeft mtl4"></div></div>'
    +'<div class="inline4"><label class="stdlabel floatLeft" for="taDemoStatus">Status:</label><br>'
    +'<textarea spellcheck="false" id="ta%" rows="5" cols="44"></textarea></div>';

GProgress.prototype.setControl = function(parent) {
    $("#" + parent).html(
                GProgress.html.replace(new RegExp('pb%', "g"), 'pb' + parent)
                .replace(new RegExp('ta%', "g"), 'ta' + parent)
        );
    this.pbID = 'pb' + parent;
    this.taID = 'ta' + parent;
    this.$pb = $("#" + this.pbID);
    this.$ta = $("#" + this.taID);
    $("#" + this.pbID + " > div").css({ 'background': 'blue' });
};

GProgress.prototype.read = function(_this, tid, msg ) {
    if (msg !== undefined) {
        this.update(this, {message:msg});
    }
    var _this = this;

    var nextRead = function(){ _this.read(_this,tid); };

    var tstateCB = function (rtn) {
        var data = rtn['data'];
        if (data !== undefined ) {
            for (var indx = 0; indx < data.length; indx++) {
                _this.update(_this, data[indx]);
            }
        }
        if ( ! _this.isComplete ) {
            setTimeout( nextRead, _this.interval);
        }
    };
    var rTstate = GRequest.build('tstate','read',null,{where:{transaction_id: tid}})
    Transaction.postNoPause( rTstate, tstateCB);
};

GProgress.prototype.update = function(_this, rec) {
    if (rec !== undefined && _this !== undefined) {
        var total = rec.total_steps || 1;
        var step = rec.step || 0;
        if (rec.error != undefined && rec.error.length > 0) {
            _this.complete(rec.error);
        } else {
            _this.$pb.progressbar({max: total});
            if (step > _this.$pb.progressbar("value")) {
                _this.$pb.progressbar({value: step});
            }

            if (total == step) {
                _this.complete();
            }

            if (rec.message !== undefined) {
                _this.prependText(rec.message);
            }
        }
    }
};

GProgress.prototype.startProgress = function( tid , complete_callback) {
    this.isComplete = false;
    this.complete_callback = complete_callback;
    this.read(this, tid, "Started..." );
};

GProgress.prototype.complete = function(error) {
    var color = 'green';
    if (error != undefined) {
        color = 'red';
        Geof.notifier.addLocalAlert(error);
        this.prependText(error);
    }
    $("#" + this.pbID + " > div").css({ 'background': color });
    this.isComplete = true;
    this.$pb.progressbar({value: 1},{max:1});
    if (this.complete_callback || false) {
        this.complete_callback();
    }
};

GProgress.prototype.setText = function(text) {
    this.$ta.val(text);
};
GProgress.prototype.prependText = function(text) {
    this.$ta.val(text + '\r\n' + this.$ta.val());
};



Geof.cntrl.macip = {

    id: 'id',
    entity: 'macip',
    prefix: 'macip_',
    fields: ['id', 'ipaddress', 'macaddress', 'domain', 'status'],
    defaults: [-1, -1, '', -1, 0],
    exclude: [],
    list_columns: 'id,ipaddress,macaddress,domain,status',
    order_by: "ipaddress",
    title: 'Mac Ip Addresses',
    olclass: 'olMacip',
    file_path: 'panel/',
    list_tmpl: '<li class="ui-widget-content" data-id="%id">'
        + '<label class="data flw110">%ipaddress</label>'
        + '<label class="data flw120">%macaddress</label>'
        + '<label class="data floatRight mr20">%domain</label></li>'
};

Geof.cntrl.image = {

    id:'id',
    entity:'image',
    prefix:'image_',
    fields:['id','name','description','createdate','statusid','lastcloned'],
    defaults:[-1,'','',null,-1,null],
    stati:{'0':'Disabled','1':'Active','2':'Retired'},
    exclude:[],
    list_columns: "id,name,description,createdate,statusid,lastcloned",
    order_by:"name,createdate",
    title:'VM Images',
    olclass:'olImage',
    file_path:'panel/',
    image_file:null,
    imageid : undefined,
    selectedid:undefined,
    upload_file:null,
    upload_start:null,
    list_tmpl : '<li class="ui-widget-content" data-id="%id">'
        + '<label class="data flw130">%name</label>'
        + '<label class="data">%statusid</label>'
        + '<label class="data floatRight mr20">%id</label></li>',

    files_tmpl : '<li class="ui-widget-content" data-id="%id">'
        + '<label class="dataSm flw200">%originalname</label>'
        + '<label class="data floatRight mr8">%filesize</label></li>',

    editConfig: {
        dialogName:'edit_Image', divName:'editImage',
        autoOpen: true, minHeight: 420, minWidth: 440,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.image;
        Gicon.click("btnRefreshImage", _this.populateList);
        Gicon.click("btnEditImage", _this.editSelected);
        Gicon.click("btnDiscardImage", _this.discard);
        Gicon.click("btnNewImage", function(){
            _this.edit({data:[{id:-1}]});}
        );
        Gicon.setEnabled("btnNewImage", true );

        _this.populateList();
    },

    validateNew:function() {
        var is_valid = false;
        Gicon.setEnabled("edit_image_save",is_valid);
    },

    populateList:function() {
        var _this = Geof.cntrl.image;
        Gicon.setEnabled("btnEditImage", false );
        Gicon.setEnabled("btnDiscardImage", false );
        var $items = $('#olImages');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items, _this.list_tmpl, 'statusid',_this.stati);
            $items.selectable({
                stop: function() {
                    var $selected = $( ".ui-selected", this);
                    var enabled = $selected.length > 0;
                    Gicon.setEnabled("btnEditImage", enabled );
                    Gicon.setEnabled("btnDiscardImage", $selected.length == 1 );
                    _this.image_file = undefined;
                    _this.selectedid = undefined;
                    if (enabled) {
                        _this.selectedid = $selected[0].dataset.id;
                    }
                }
            });
            Gicon.setEnabled("btnRefreshImage", true );
        };
        Geof.model.read(null,_this, cb);
    },

    reloadImageFiles:function() {
        var _this = Geof.cntrl.image;
        var id =  _this.imageid;
        var $olIFiles = $("#olImageFiles");
        $olIFiles.empty();
        var cb = function (req) {
            Templater.createSOLTmpl(req.data, $olIFiles, _this.files_tmpl);
            $olIFiles.selectable({
                stop: function () {
                    var enabled = $(".ui-selected", this).length > 0;
                    Gicon.setEnabled("btnDeleteImageFile", enabled);
                }
            });
        };
        var reqIF = GRequest.build('image','read','files',{'where':{'id':id}});
        Transaction.post(reqIF, cb);

    },

    imageSelectionCB : function(evt) {
        var _this = Geof.cntrl.image;
        _this.image_file = null;
        var $new_image = $("#new_image");
        $new_image.text('');
        Gicon.setEnabled("uploadImage", false);

        var files = evt.target.files;
        if (files.length == 1) {
            _this.image_file = files[0];
            $new_image.text(_this.image_file.name);
            $( "#pbImageUpload" ).progressbar({value:0});
            Gicon.setEnabled("uploadImage", true);
        }
    },

    edit: function (req) {
        var _this = Geof.cntrl.image;
        var data = req.data[0];
        Geof.cntrl.showEditDialog(_this, data, false);
        _this.imageid = data.id == -1 ? undefined : data.id;
        $("#editImage").tooltip();
        Gicon.click("selectImage",function() {
            $('#imageFileSelector').click();
        });
        $('#imageFileSelector')[0].addEventListener('change', _this.imageSelectionCB, false);
        $("#pbImageUpload").progressbar();
        var $imgname = $("#image_name");
        $imgname.blur(function() {
            Gicon.setEnabled("edit_image_save", $imgname.val().length > 0);
        });
        Gicon.click("edit_image_save",_this.save);
        Gicon.click('uploadImage', _this.uploadFile);
        Gicon.click('btnDeleteImageFile', _this.confirmDeleteImageFile);
        _this.reloadImageFiles();
    },

    editSelected: function () {
        var _this = Geof.cntrl.image;
        if (_this.selectedid !== undefined) {
            Geof.model.readRecord(_this.selectedid, _this, _this.edit);
        } else {
            _this.edit({data:[{id:-1}]});
        }
    },

    save:function () {
        var _this = Geof.cntrl.image;
        Gicon.setActive('edit_image_save',true);
        var fields = Geof.cntrl.getDialogData(_this);

        var update = (fields.id || false) ? fields.id >= 0 : false;

        var data = {};
        data.fields = fields;
        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('edit_image_save',true);
            Geof.cntrl.setEditFields(_this,data);
        };
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    discard: function() {
        var _this = Geof.cntrl.image;
        if (_this.selectedid === undefined ) {
            return;
        }
        var cb = function (do_discard) {
            if (do_discard) {
                var id = _this.selectedid;
                var reqIF = GRequest.build('image', 'delete', null, {'where': {'id': id}});
                Transaction.post(reqIF, function () {
                    _this.populateList();
                });
            }
        };
        PanelMgr.showConfirm("Delete Image", "Delete Selected Image?", cb);
    },

    confirmDeleteImageFile:function() {
        PanelMgr.showConfirm(
            "Delete Image Files",
            'Delete selected Image files?',
            Geof.cntrl.image.discardImageFile);
    },

    discardImageFile:function(del) {

        if (! del) {
            return;
        }
        var _this = Geof.cntrl.image;

        var $files = $("#olImageFiles .ui-selected");
        var count = $files.length;
        if ( count > 0 ) {
            Gicon.setActive("btnDeleteImageFile", true );
            var trans = new Transaction(Geof.session);

            for ( var indx = 0; indx < count; indx++) {
                var id = $files[indx].dataset.id;
                var where = {'where':{'fileid':id,'imageid': _this.imageid}};
                var reqIF = GRequest.build('image','delete','file',where);
                trans.addRequest(reqIF, null);
            }
            var cb = function () {
                Gicon.setEnabled("btnDeleteImageFile", true);
                Geof.cntrl.image.reloadImageFiles();
            };
            trans.setLastCallback(cb);
            trans.send();
        }
    },

    uploadFile: function() {
        var _this = Geof.cntrl.image;
        var file =  _this.image_file;
        file.links = [
            {'to_table':'image','imageid':_this.imageid }
        ];
        _this.upload_file = new Uploadfile( file );
        _this.upload_file.endCallback = _this.uploadCallback;
        _this.upload_file.upload();
    },

    uploadCallback:function(index, status, pending, sent, error) {
        var $pbar = $('#pbImageUpload');
        var _this = Geof.cntrl.image;
        if ( error != null && error.length > 0) {
            PanelMgr.showError(error);
        } else {

            if (status === FU_ERROR) {
                $pbar.progressbar({complete:function() {
                    $('#pbImageUpload > div').css({ 'background': 'red' });
                }});
                $pbar.progressbar( "option", "value", $pbar.progressbar( "option", "max" ));
                var trans = new Transaction(Geof.session);
                var delFU1 = GRequest.build('upload','delete', null, {'where':{'sendname': _this.upload_file.sendname}});
                trans.addRequest(delFU1, _this.reloadImageFiles);
                _this.upload_file = null;
                return;
            }
            else if ( status === FU_REGISTERED ) {
                $pbar.progressbar( "option", "max", pending );
                $pbar.progressbar( "option", "value", 0);
                $pbar.progressbar({complete:function() {
                    $('#pbImageUpload > div').css({ 'background': 'green' });
                }});
                _this.upload_start = Date.now();
            }
            else if ( status === FU_ACTIVE ){
                var delFU = GRequest.build('upload','delete', null, {'where':{'sendname': _this.upload_file.sendname}});
                Transaction.post(delFU, _this.reloadImageFiles);
                _this.upload_file = null;
                return;
            }
            else {
                $pbar.progressbar( "option", "value", sent);
                $('#remaining_time').text(UploadHandler.getRemainTime(pending, sent));
                $('#dpl_file_sections').text(sent + ' of ' + pending);
            }

            var cu = _this.upload_file;
            if (cu != null) {
                if (cu.status === FU_ERROR) {
                    PanelMgr.showError("Upload has encountered an error");
                } else if (cu.status == FU_SERVER_ACTIVATE){
                    PanelMgr.showError("Upload complete");
                } else if (cu.status != FU_ACTIVE){
                    cu.upload();
                }
            }
        }
    }

};

Geof.cntrl.buildplan = {

    id:'id',
    entity:'buildplan',
    prefix:'buildplan_',
    fields:['id','name','imageid','domainname','baseip','storagelocid','antcmd','imgcfg','description','createdate','status'],
    defaults:[-1,'','',null,-1,null],
    stati:{'0':'Disabled','1':'Active','2':'Retired'},
    exclude:[],
    list_columns: "id,name,status,storagelocid",
    order_by:"name,createdate",
    title:'Buildplans',
    olclass:'olBuildplan',
    file_path:'panel/',
    buildplanid : undefined,
    selectedid:undefined,
    image:undefined,
    images:undefined,
    imageid:undefined,
    installfile:undefined,
    row:null,

    list_tmpl : '<li class="ui-widget-content" data-id="%id">'
        + '<label class="data flw130">%name</label>'
        + '<label class="data">%status</label>'
        + '<label class="data floatRight mr20">%id</label></li>',

    files_tmpl : '<li class="ui-widget-content" data-id="%id">'
        + '<label class="data flw200">%originalname</label>'
        + '<label class="data flw130">%filesize</label>'
        + '<label class="data floatRight mr20">%id</label></li>',

    copy_html : '<div id="dialog-copy" class="hidden" title="Copy Buildplan"><p>'
        + '<div class="inline3"><label class="stdlabel flw80">New Name:</label>'
        + '<input type="text" id="newBuildplanName" size="20"></div>'
        + '</p></div>',

    editConfig: {
        dialogName:'edit_Buildplan', divName:'editBuildplan',
        autoOpen: true, minHeight: 550, minWidth: 460,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.buildplan;
        $( "#imgcfg_section" ).accordion({
            clearStyle: true,
            heightStyle: "content"
        });

        $( "#imgcfg_section" ).accordion({
            clearStyle: true,
            heightStyle: "content"
        });

        Gicon.click('btnBPCreateDomain', _this.createGuest);
        Gicon.click("btnRefreshBuildplan", _this.populateList);
        Gicon.click("btnRefreshStorageLoc", _this.populateStorageLocs);
        Gicon.click("btnCopyBuildplan", _this.showCopy);
        Gicon.click("btnDiscardBuildplan", _this.discard);

        Gicon.click("btnNewBuildplan", _this.create);
        Gicon.setEnabled("btnNewBuildplan", true );
        Gicon.click("discardInstallFile", _this.confirmDeleteInstallFile);
        $("#pbInstallUpload").progressbar();
        Gicon.click("edit_buildplan_save",_this.save);
        Gicon.click("btnChangeImage", _this.showImages);
        $('#buildplan_packages').blur( function() {
            _this.parseTextarea('buildplan_packages');
        });
        $('#buildplan_wgetfiles').blur( function() {
            _this.parseTextarea('buildplan_wgetfiles');
        });
        Gicon.click("selectInstallFile",function() {
            $('#installFileSelector').click();
        });
        $('#installFileSelector')[0].addEventListener('change', _this.selectInstallFileCB, false);
        Gicon.click("uploadInstallFile",_this.uploadInstallFile);
        $('#buildplan_cmds').blur( function() {
            _this.parseTextarea('buildplan_cmds');
        });

        _this.populateList();
    },

    create:function() {
        Geof.cntrl.buildplan.edit({data:[{id:-1,imgcfg:{pkgs:'',checks:'',rcps:[],cmds:''}}]});
    },

    createGuest: function() {
        var _this = Geof.cntrl.buildplan;
        var row = _this.row;
        Geof.cntrl.domain.show_create_guest_dialog(row.domainname, row.id, null);
    },

    validateNew:function() {
        var is_valid = false;
        Gicon.setEnabled("edit_buildplan_save",is_valid);
    },

    populateList:function() {
        var _this = Geof.cntrl.buildplan;

        Gicon.setEnabled("btnBPCreateDomain", false );
        Gicon.setEnabled("btnCopyBuildplan", false );
        Gicon.setEnabled("btnDiscardBuildplan", false );
        var $items = $('#olBuildplans');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items, _this.list_tmpl, 'statusid',_this.stati);
            $items.selectable({
                stop: function() {
                    var $selected = $( ".ui-selected", this);
                    var enabled = $selected.length == 1;
                    Gicon.setEnabled("btnBPCreateDomain", enabled );
                    Gicon.setEnabled("btnCopyBuildplan", enabled );
                    Gicon.setEnabled("btnCreateBuildplan", enabled );
                    Gicon.setEnabled("btnDiscardBuildplan", enabled );
                    _this.selectedid = undefined;
                    if (enabled) {
                        _this.selectedid = parseInt($selected[0].dataset.id);
                        Geof.model.readRecord(_this.selectedid, _this, _this.edit);
                    }

                }
            });
            Gicon.setEnabled("btnRefreshBuildplan", true );
        };
        Geof.model.read(null,_this, cb);
    },

    populateDomains:function(domainname) {
        domainname = domainname === undefined ? '' : domainname;

        var cb = function(req) {
            var data = req.data;
            data.unshift({'name':'None','id':-1});
            JsUtil.setOptions('buildplan_domainname', data, 'name', 'name', domainname );
        };
        Geof.model.read(null,Geof.cntrl.domain, cb);
    },

    reloadInstallFiles:function() {
        var _this = Geof.cntrl.buildplan;
        var id =  _this.buildplanid;
        var $olIFiles = $("#olBpInstallfiles");
        $olIFiles.empty();
        var cb = function (req) {
            Templater.createSOLTmpl(req.data, $olIFiles, _this.files_tmpl);
            $olIFiles.selectable({
                stop: function () {
                    Gicon.setEnabled("discardInstallFile", $(".ui-selected", this).length > 0);
                }
            });
        };
        var reqIF = GRequest.build('buildplan','read','files',{'where':{'id':id}});
        Transaction.post(reqIF, cb);

    },

    edit: function (req) {
        $("#buildplan_packages").val('');
        $("#buildplan_checks").val('');
        $("#buildplan_wgetfiles").val('');
        $("#buildplan_cmds").val('');

        var _this = Geof.cntrl.buildplan;
        var data = req.data[0];
        _this.row = data;

        Geof.cntrl.setEditFields(_this,data);
        _this.populateDomains(data.domainname);

        // Place Imgcfg data on form
        var imgcfg = data.imgcfg;
        if (imgcfg.length > 0) {
            imgcfg = JSON.parse(base64.decode(imgcfg));
        } else {
            imgcfg = {pkgs:'',checks:'',wgets:'',rcps:[],cmds:''};
        }

        $("#buildplan_packages").val(imgcfg.pkgs.replace(new RegExp(',', "g"), '\n'));
        $("#buildplan_checks").val(imgcfg.checks);
        $("#buildplan_wgetfiles").val(imgcfg.wgets.replace(new RegExp(',', "g"), '\n'));
        $("#buildplan_cmds").val(imgcfg.cmds.replace(new RegExp(',', "g"), '\n'));

        _this.buildplanid = data.id == -1 ? undefined : data.id;
        _this.domainname = data.domainname || '';
        Gicon.setEnabled('btnBPCreateDomain',_this.domainname.length > 0);
        _this.imageid = data.imageid;

        var $name = $("#buildplan_name");
        $name.blur( function() {
            Gicon.setEnabled("edit_buildplan_save", $name.val().length > 0);
        });

        _this.enableDetails(data.id >= 0);
        _this.reloadImages(_this.setImage);
        _this.reloadInstallFiles();
        _this.loadIsDemo();
        _this.populateStorageLocs(data.storagelocid || -1);
    },

    populateStorageLocs:function(bp_storageLoc_id) {
        Gicon.setActive("btnRefreshStorageLoc", true );
        var cb = function(req) {
            var data = req.data;
            data.unshift({id : -1, name:'-Default-'});
            JsUtil.setOptions('buildplan_storagelocid', data, 'name', 'id', bp_storageLoc_id);
            Gicon.setEnabled("btnRefreshStorageLoc", true );
        };
        Geof.model.read(null, Geof.cntrl.storage, cb);
    },

    showImages:function() {
        var cntrl = Geof.cntrl.image;
        var _this = Geof.cntrl.buildplan;
        var cb = function () {
            _this.selectedImageId = undefined;
            var $ol = $("#olBpImages");
            $ol.empty();
            Templater.createSOLTmpl(_this.images, $ol, cntrl.list_tmpl, 'statusid', cntrl.stati);
            $ol.selectable({
                stop: function () {
                    _this.selectedImageId = undefined;
                    var $sltd = $(".ui-selected", this);
                    if ($sltd.length == 1) {
                        $sltd = $sltd[0];
                        Gicon.setEnabled("btnSaveBpImage", true);
                        _this.imageid = parseInt($sltd.dataset.id);
                    }
                }
            });
            Gicon.click('btnCancelBpImages', function () {
                $("#imagelist").switchClass('shown', 'hidden');
            });
            Gicon.click('btnSaveBpImage', function () {
                _this.setImage(true);
            });

            $("#imagelist").switchClass('hidden', 'shown');
            $('#olBpImages li[data-id="' + _this.imageid + '"]').addClass('ui-selected');
        };
        _this.reloadImages( cb );
    },

    loadIsDemo:function() {
        var _this = Geof.cntrl.buildplan;
        var rDemo = GRequest.build('buildplan', 'read', 'demo', {});
        Transaction.post(rDemo, function(req) {
            var data = req.data[0];
            var checked = (data !== undefined && _this.selectedid == data.buildplanid);
            $('#cbDemoBuildplan').prop('checked',checked);
        });
    },

    reloadImages:function(callback) {
        Geof.model.read(null, Geof.cntrl.image, function(req) {
            Geof.cntrl.buildplan.images = req.data;
            callback();
        });
    },

    setImage:function(close) {
        var _this = Geof.cntrl.buildplan;
        if (close) {
            $("#imagelist").switchClass('shown','hidden');
        }
        _this.image = JsUtil.get(_this.images,'id',_this.imageid);
        if (_this.image != undefined) {
            $("#image_name").text(_this.image.name);
        }
    },

    editSelected: function () {
        var _this = Geof.cntrl.buildplan;
        if (_this.selectedid !== undefined) {
            Geof.model.readRecord(parseInt(_this.selectedid), _this, _this.edit);
        } else {
            _this.edit({data:[{id:-1}]});
        }
    },

    enableDetails:function(enabled) {
        Gicon.setEnabled('btnChangeImage',enabled);
        Gicon.setEnabled('selectInstallFile',enabled);
        Gicon.setEnabled('edit_buildplan_save',enabled);
    },

    save:function () {
        var _this = Geof.cntrl.buildplan;
        Gicon.setActive('edit_buildplan_save',true);
        var fields = Geof.cntrl.getDialogData(_this);
        fields['imageid'] = _this.image === undefined ? -1 : _this.image.id;
        if ( fields['storagelocid'] == undefined ) {
            fields['storagelocid'] = -1;
        }

        var imgcfg = {pkgs:'',checks:[],wgets:'',rcps:[],cmds:''};
        imgcfg.pkgs = _this.formatTextarea('buildplan_packages');
        imgcfg.checks = $('#buildplan_checks').val();
        imgcfg.wgets = _this.formatTextarea('buildplan_wgetfiles');
        imgcfg.cmds = _this.formatTextarea('buildplan_cmds');

        fields['imgcfg'] = base64.encode(JSON.stringify(imgcfg));

        fields['isDemo'] = $('#cbDemoBuildplan').prop('checked').toString();
        var data = {};
        data.fields = fields;
        var cb = function (req) {
            if (req['pkey']) {
                fields.id = req['pkey']['id'];
            }
            _this.populateList();
            _this.enableDetails(true);
            Geof.cntrl.setEditFields(_this, fields);
        };

        var update = (fields.id || false) ? fields.id >= 0 : false;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    showCopy :function() {
        var _this = Geof.cntrl.buildplan;
        $('#mainBody').append( _this.copy_html );
        var $copy = $( "#dialog-copy" );
        var $newName = $("#newBuildplanName");
        $newName.blur(function() {
            var state = $newName.val().length > 0 ? 'enable' : 'disable';
            $(".ui-dialog-buttonpane button:contains('Copy')").button(state);
        });

        $copy.dialog({
            modal: true,
            resizable: false,
            height:190,
            width:330,
            buttons: {
                "Copy": function() {
                    $( this ).dialog( "close" );
                    $copy.remove();
                    _this.copy($newName.val());
                },
                Cancel: function() {
                    $( this ).dialog( "close" );
                    $copy.remove();
                }
            },
            open: function() {
                $(".ui-dialog-buttonpane button:contains('Copy')").button('disable');
            }
        });

    },

    copy: function(newName) {
        var _this = Geof.cntrl.buildplan;
        if (_this.selectedid === undefined ) {
            return;
        }
        var cb = function (do_discard) {
            if (do_discard) {
                var data = {'fields':{'name':newName},'where':{'id':_this.selectedid}};
                var reqIF = GRequest.build('buildplan', 'create', 'copy', data);
                Transaction.post(reqIF, _this.populateList);
            }
        };
        PanelMgr.showConfirm("Copy Buildplan", "Copy Selected Buildplan?", cb);
    },

    discard: function() {
        var _this = Geof.cntrl.buildplan;
        if (_this.selectedid === undefined ) {
            return;
        }
        var cb = function (do_discard) {
            if (do_discard) {
                var reqIF = GRequest.build('buildplan','delete', null,{'where':{'id':_this.selectedid}});
                Transaction.post(reqIF, _this.populateList);
            }
        };
        PanelMgr.showConfirm("Delete Buildplan", "Delete Selected Buildplan?", cb);
    },

    selectInstallFileCB : function(evt) {
        var _this = Geof.cntrl.buildplan;
        var $name = $("#install_file_name");
        var files = evt.target.files;

        if (files.length > 0) {
            _this.installfile = files[0];
            $name.text(_this.installfile.name);
            Gicon.setEnabled("uploadInstallFile", true);
        } else {
            _this.installfile = null;
            $name.text('');
            Gicon.setEnabled("uploadInstallFile", false);
        }
    },

    uploadInstallComplete:function() {
        var $pbar = $("#pbInstallUpload");
        $pbar.progressbar("option", "value", $pbar.progressbar("option", "max"));
        Gicon.setEnabled('uploadInstallFile', false);
        Geof.cntrl.buildplan.reloadInstallFiles();
    },

    uploadInstallError:function(err_msg) {
        $('#pbInstallUpload').progressbar(
            {complete: function () {
                    $('#pbInstallUpload > div').css({ 'background': 'red' });
                }
            }
        );
        Geof.cntrl.buildplan.uploadInstallComplete();
        PanelMgr.showError(err_msg);
    },

    uploadInstallFile: function() {
        var _this = Geof.cntrl.buildplan;

        _this.installfile.links = [
            {'to_table':'buildplan','buildplanid':_this.buildplanid }
        ];
        var pbar = 'pbInstallUpload';
        var cbTime = undefined;
        var cbComplete = _this.uploadInstallComplete;
        var cbError = _this.uploadInstallError;
        _this.upHandler = new UploadHandler(_this, _this.installfile, pbar, cbTime, cbComplete, cbError);
        Gicon.setActive('uploadInstallFile',true);
        _this.upHandler.start();
    },

    confirmDeleteInstallFile:function() {
        PanelMgr.showConfirm(
            "Delete Install Files",
            'Delete selected install files?',
            Geof.cntrl.buildplan.discardInstallFile);
    },

    discardInstallFile:function(del) {

        if (! del) {
            return;
        }
        var _this = Geof.cntrl.buildplan;

        var $files = $("#olBpInstallfiles .ui-selected");
        var count = $files.length;
        if ( count > 0 ) {
            Gicon.setActive("discardInstallFile", true );
            var trans = new Transaction(Geof.session);

            for ( var indx = 0; indx < count; indx++) {
                var id = $files[indx].dataset.id;
                var where = {'where':{'fileid':id,'buildplanid': _this.buildplanid}};
                var reqIF = GRequest.build('buildplan','delete','file',where);
                trans.addRequest(reqIF, null);
            }
            var cb = function () {
                Gicon.setEnabled("discardInstallFile", true);
                Geof.cntrl.buildplan.reloadInstallFiles();
            };
            trans.setLastCallback(cb);
            trans.send();
        }
    },

    parseTextarea:function(element) {
        var $ele = $("#" + element);
        var txt =  $ele.val().replace(new RegExp(',\n', "g"), '\n');
        txt = txt.replace(new RegExp(',', "g"), '\n');
        var lines = txt.split('\n');
        var line;
        txt = '';
        for (var indx=0;indx<lines.length;indx++) {
            line = lines[indx].trim().replace(new RegExp('\'', "g"), '');
            txt += line.replace(new RegExp('^\t'), '') + '\n';
        }
        $ele.val(txt);
    },

    formatTextarea:function(element) {
        var val = $("#" + element).val();
        val = val.replace(new RegExp('\n', "g"), ',');
        if (val.lastIndexOf(",") === val.length - 1) {
            val = val.substring(0, val.length - 1);
        }
        return val;
    }
};

