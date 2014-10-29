/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 4/8/13
 * Time: 4:36 PM
 */

var Geof = Geof || {};

Geof.increment = 0;

Geof.cntrl = Geof.cntrl || {};

Geof.cntrl.option_tmpl = '<option value="%value" %selected>%text</option>';

Geof.cntrl.childSelectCB = null;
Geof.cntrl.selectedFiles = [];
Geof.src = {};

/*
 This function builds a list of option values as html and replaces the 'Select' control's
 html.

 data : Json array of row data
 selectid : id of the select control to fill with data
 fld_value : row column to use as option value
 text_format : a format field to use instead of standard %text i.e. %lastname, %firstname
 text_cols : an array of row columns to use in text_format replacement
 fld_selected : row field to use for setting the 'selected' option
 selected_value :
 */
Geof.cntrl.fill_select = function(data, selectid, fld_value, text_format, text_cols, fld_selected, selected_value) {
    var html = '';
    var opt = '';
    var useSelected = fld_selected && selected_value;
    var tmpl = Geof.cntrl.option_tmpl;
    if (text_format || false ) {
        tmpl = tmpl.replace('%text', text_format);
    }

    JsUtil.iterate(data, function(row){
        opt = tmpl.replace('%value', row[fld_value]);
        var col;
        for (var iCol=0;iCol<text_cols.length;iCol++) {
            col = text_cols[iCol];
            opt = opt.replace('%' + col, row[col]);
        }
        if (useSelected && selected_value == row[fld_selected]) {
            opt = opt.replace('%selected', ' selected="true"');
        } else {
            opt = opt.replace('%selected', '');
        }
        html += opt;
    });

    $("#" + selectid).html(html);
};

Geof.cntrl.selectedByType = function (type) {
    var selected = Geof.cntrl.selectedFiles;
    var rtn = [];
    if (selected && selected.length > 0) {
        for (var indx = 0; indx < selected.length; indx++) {
            var item = selected[indx];
            if (item.filetype == type) {
                rtn.push(item);
            }
        }
    }
    return rtn;
};

Geof.cntrl.showEditDialog = function(cntrl, data, isNew, callback) {
    if (!(data || false)) {
        isNew = true;
        data = {};
    } else if (JsUtil.isArray(data)) {
        data = data[0];
    }
    isNew = (isNew || false);
    var filename = ('filename' in cntrl) ? cntrl.filename : cntrl.entity;

    if (! (filename in Geof.src)) {
        var cb = function() {
            Geof.cntrl.showEditDialog(cntrl,data,isNew,callback);
        };
        var filelist = ('filelist' in cntrl) ? cntrl.filelist : ["list","edit"];
        Geof.Retrieve.getEntity(filename, cb, filelist, cntrl.file_path);
        return;
    }
    var html = Geof.src[filename].edit;
    $("#mainBody").append(html);
    if (isNew) {
        Geof.cntrl.clearDialog(cntrl);
    } else {
        Geof.cntrl.setEditFields(cntrl,data);
    }

    var pos_tag = cntrl.entity + '_dialog_position';
    var pos = GLocal.get(pos_tag);
    if ( pos != undefined) {
        pos = pos.split(",");
        cntrl.editConfig.position = [parseInt(pos[1]),parseInt(pos[0])];
    }
    var $dlg = $("#" + cntrl.editConfig.divName);
    $dlg.dialog(cntrl.editConfig);
    $dlg.on("dialogclose", function() {
        $dlg.remove()
    });

    $dlg.on("dialogdragstop",function(event, ui) {
        GLocal.set(pos_tag, ui.position.top + "," + ui.position.left);
    });

    if (callback || false) {
        callback($dlg);
    }
};

Geof.cntrl.setEditFields = function(cntrl, data, lookups) {
    try {
        Geof.cntrl.clearDialog(cntrl);
        if (!(data || false)) {
            return;
        }
        var fields = cntrl.fields;
        JsUtil.iterate ( fields, function(fld) {
            var $cntrl = $("#" + cntrl.prefix + fld);
            if ($cntrl.length > 0) {
                var value = data[fld];
                var lookup = lookups === undefined ? undefined : lookups[fld];
                if (lookup !== undefined) {
                    value = lookup[value];
                }
                if (undefined !== value) {
                    var el = $cntrl[0];
                    var className = Geof.cntrl.getObjectClass(el);
                    if (className.indexOf("Label") > -1) {
                        $cntrl.text(value);
                    } else if (className.indexOf("Select") > -1) {
                        if (! (value || false)) {
                            value = -1;
                        }
                        $cntrl.val(value.toString());

                    } else if (el !== undefined && className === 'HTMLInputElement') {
                        if (el.type == 'text'){
                            $cntrl.val(value);
                        } else if (el.type == 'radio') {
                            $cntrl.prop('checked',value);
                        } else if (el.type == 'checkbox'){
                            $cntrl.prop('checked',value);
                        }
                    } else {
                        $cntrl.val(value);
                    }
                }
            }

        });
        return null;
    } catch (e) {
        alert(e);
    }
};

Geof.cntrl.clearDialog = function(cntrl,lookups) {
    var fields = cntrl.fields;
    var defaults = cntrl.defaults;
    for (var indx=0;indx<fields.length;indx++) {
        var fld = fields[indx];
        var controlName = cntrl.prefix + fld;
        var $cntrl = $("#" + controlName);
        var value = defaults[indx];

        var lookup = lookups === undefined ? undefined : lookups[fld];
        if (lookup !== undefined) {
            value = lookup[value];
        }

        var el = $cntrl[0];
        var className = Geof.cntrl.getObjectClass(el);
        if (className.indexOf("Label") > -1) {
            $cntrl.text(value);
        } else if (className.indexOf("Select") > -1) {
            $cntrl.val(value);
        } else if (el !== undefined && className === 'HTMLInputElement') {
            if (el.type == 'text'){
                $cntrl.val('');
            } else if (el.type == 'radio') {
                $cntrl.prop('checked',false);
            } else if (el.type == 'checkbox'){
                $cntrl.prop('checked',false);
            }
        } else {
            $cntrl.val(value);
        }
    }
};

Geof.cntrl.getObjectClass = function (obj) {
    if (obj && obj.constructor && obj.constructor.toString) {
        var arr = obj.constructor.toString().match(/function\s*(\w+)/);
        if (arr && arr.length == 2) {
            return arr[1];
        }
    }
    return "";
};

Geof.cntrl.getDialogData = function(cntrl, exclude) {
    var fldList = cntrl.fields;
    var value;
    var fields = {};
    JsUtil.iterate (fldList, function(fld) {
        if ( (exclude && JsUtil.has(fld, exclude)) || JsUtil.has(fld, cntrl.exclude )) {
            return;
        }
        var controlName = cntrl.prefix + fld;
        var $id = $("#" + controlName);
        var className = Geof.cntrl.getObjectClass( $id[0] );
        if (className.indexOf("Label") > -1) {
            value = $id.text();
        } else {
            value = $id.val();
        }
        fields[fld] = value;
    });
    return fields;
};

Geof.cntrl.getNodeName = function(id) {
    try {
        var elt = document.getElementById(id);
        return elt === undefined ? '' : elt.nodeName;
    } catch (e){
        return '';
    }
};

Geof.cntrl.getNodeType = function(id) {
    try {
        var elt = document.getElementById(id);
        return elt === undefined ? '' : elt.type;
    } catch (e){
        return '';
    }
};

Geof.cntrl.getFormData = function(prefix, fields) {
    var value;
    var values = {};
    prefix = (! (prefix || false)) ? '' : prefix;
    JsUtil.iterate (fields, function(fld) {
        var controlName = prefix + fld;
        var $id = $("#" + controlName);
        var className = Geof.cntrl.getObjectClass( $id[0] );
        if (className.indexOf("Label") > -1) {
            value = $id.text();
        } else {
            value = $id.val();
        }
        values[fld] = value;
    });
    return values;
};

Geof.cntrl.setFormData = function(prefix, data, lookups) {
    prefix = (! (prefix || false)) ? '' : prefix;
    JsUtil.iterate(data, function(val, cntrl) {
        var cname = prefix + cntrl;
        var $cntrl= $("#" + cname);

        val = lookups === undefined ? val : lookups[val];

        if (val !== undefined) {
            var nodeName = Geof.cntrl.getNodeName(cname);

            if (nodeName == '') {
                //Try radio and checkbox buttun
                var $rdio = $("input:radio[name=" + cname + "][value='" + val + "']");
                if ($rdio) {
                    $rdio.prop('checked', true);
                }

            } else {
                if (nodeName == "LABEL") {
                    $cntrl.text(val);

                } else if (nodeName == "SELECT") {
                    $cntrl.val(val.toString());

                } else if (nodeName == 'INPUT') {
                   var type = Geof.cntrl.getNodeType(cname);
                   if (type == 'text'||type == 'password') {
                       $cntrl.val(val);
                   } else if (type == 'checkbox') {
                       $cntrl.prop('checked', true);
                   }
                }
            }

        }
    })
};

Geof.cntrl.saveSublink = function(subInfo, btnName) {
    Gicon.setActive(btnName, true);
    var order = 0;
    var trans = new Transaction(Geof.session);
    var entity = subInfo.linkEntity;
    var jReq = GRequest.build(entity, "delete", null, {where:subInfo.where});
    jReq.order = order++;
    trans.addRequest(jReq, null);

    $('#' + subInfo.olName + " .ui-selected").each(function() {
        var data = {fields:{}};
        data.fields[subInfo.parentid] = subInfo.id;
        data.fields[subInfo.childid] = $.data(this,'id');
        jReq = GRequest.build(entity, "create", null, data);
        jReq.order = order++;
        trans.addRequest(jReq, null);
    });
    trans.setLastCallback(function() {
        Gicon.setEnabled(btnName, true);
        Geof.cntrl.parentSelectCB(subInfo.id);
    });
    trans.send();

};

Geof.cntrl.deleteSublink = function (subInfo, btnName) {
    Gicon.setActive(btnName, true);
    var trans = new Transaction(Geof.session);
    var entity = subInfo.linkEntity;
    $('#' + subInfo.olName + " .ui-selected").each(function () {
        var data = {where: {}};
        data.where[subInfo.childid] = $.data(this, 'id');
        trans.addRequest(GRequest.build(entity,"delete",null,data), null);
    });
    trans.setLastCallback(function () {
        Gicon.setEnabled(btnName, true);
        Geof.cntrl.parentSelectCB(subInfo.id);
    });
    trans.send();
};

Geof.cntrl.deleteChild = function (subInfo, btnName) {
    Gicon.setActive(btnName, true);
    var trans = new Transaction(Geof.session);
    $('#' + subInfo.olName + " .ui-selected").each(function () {
        var data = {where: {}};
        data.where[subInfo.cEntity.id] = $.data(this, 'id');
        jReq = GRequest.build(subInfo.child, "delete", null, data);
        trans.addRequest(jReq, null);
    });
    trans.setLastCallback(function () {
        Gicon.setEnabled(btnName, true);
        Geof.cntrl.parentSelectCB(subInfo.id);
    });
    trans.send();
};

Geof.cntrl.selectLI = function (olName, data, field, callback) {
    Geof.cntrl.deselectAll(olName, null);
    for (var indx=0;indx<data.length;indx++) {
        $('#' + olName + ' li[data-id="' + data[indx][field] + '"]').addClass('ui-selected');
    }
    if (callback || false) {
        callback();
    }
};

Geof.cntrl.selectAll = function (olName, callback) {
    $('#' + olName + " li").addClass('ui-selected');
    if (callback || false) {
        callback();
    }
};

Geof.cntrl.deselectAll = function (olName, callback) {
    $('#' + olName + " li").removeClass('ui-selected');
    if (callback || false) {
        callback();
    }
};

Geof.cntrl.enableSublinks = function (enabled) {
    if (enabled) {
        $("div .sub_link").switchClass('disabled', 'enabled');
    } else {
        $("div .sub_link").switchClass('enabled', 'disabled');
    }
},

Geof.cntrl.parentSelected = function (selected) {
    var enable = false;
    if (selected || false)  {
        var id = undefined;
        if (JsUtil.isArray(selected)) {
            if (selected.length == 1) {
                id = selected[0];
            }
        } else {
            id = selected;
        }
        enable = id || false;
        if (Geof.cntrl.parentSelectCB || false) {
            Geof.cntrl.parentSelectCB(id);
        }
    }
    Geof.cntrl.enableSublinks(enable);
};

Geof.cntrl.setChildSelectCB = function (olName, childEntity, deselectName, deleteName) {
    Geof.cntrl.childSelectCB = function () {
        var selected = $('#' + olName + ' .ui-selected');
        var ids = Geof.model.selectedIds(selected, childEntity);
        Gicon.setEnabled(deselectName, (ids.length > 0));
        Gicon.setEnabled(deleteName, (ids.length > 0));
    };
    return Geof.cntrl.childSelectCB;
};

Geof.cntrl.getLink = function (entityName, linkName) {
    var entity = Geof.cntrl[entityName];
    if (entity !== undefined) {
        var links = entity.link;
        if (links != undefined) {
            var len = links.length;
            for (var indx = 0; indx < len; indx++) {
                if (links[indx].name === linkName) {
                    return links[indx];
                }
            }
        }
    }
    return undefined;
};

Geof.cntrl.audit = {
    id:'id',
    entity:'requestaudit',
    filename:'audit',
    prefix:'adt_',
    fields:['requestname','action','actionas','usrid','sessionid','rundate'],
    defaults:[],
    exclude:[],
    list_columns: "",
    order_by:"rundate desc",
    title:'Request Audit Log',
    list_tmpl:'<li class="ui-widget-content"><label class="flw120">%requestname</label><label class="flw40">%actionname</label>' +
        '<label class="flw60">&nbsp;%actionas</label><label class="flw50">%usrid</label><label class="flw240">%sessionid</label>' +
        '<label class="flw120">%rundate</label></li>',
    link: {},
    editConfig: { },

    initialize: function() {
        var _this = Geof.cntrl.audit;
        Gicon.click("btnRefreshAudit", _this.populateList);
        Gicon.click("btnEditAudit", _this.save);
        $(" .subDlgWrapper").tooltip();
        $("#adt_level").change( function() {
            Gicon.setEnabled("btnEditAudit",true);
        });

        var $after = $("#adt_after");
        $after.val(DateUtil.todayPickerDate(true));
        $after.datetimepicker();
        $("#adt_before").datetimepicker();
        $("#adt_truncate_before").datetimepicker();
        $("#adt_truncate_before").change( function() {
            Gicon.setEnabled("btnDiscardAudit",$("#adt_truncate_before").val().length);
        });
        Gicon.click("btnDiscardAudit", _this.delete);
        _this.populateList();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshAudit", true );
        var _this = Geof.cntrl.audit;

        var $items = $('#olAudit');
        $items.empty();
        var cb = function (req) {
            Templater.createSOLTmpl(req.data, $items, _this.list_tmpl);
            $items.selectable();
            Gicon.setEnabled("btnRefreshAudit", true);
        };
        var where = [];
        var after = $("#adt_after").val();
        if (after.length > 0) {
            after = DateUtil.getSvrDate(after,"/",":");
            where.push({"column":"rundate","operator":">","value":after});
        }
        var before = $("#adt_before").val();
        if (before.length > 0) {
            before = DateUtil.getSvrDate(before,"/",":");
            where.push({"column":"rundate","operator":"<","value":before});
        }
        var usrid = $("#adt_usrid").val();
        if (usrid.length > 0) {
            where.push({"column":"usrid","operator":"=","value":usrid});
        }
        var sessionid = $("#adt_sessionid").val();
        if (sessionid.length > 0) {
            where.push({"column":"sessionid","operator":"=","value":sessionid});
        };
        Geof.model.read(where,_this, cb, null);
    },

    delete: function (){
        var after = $("#adt_truncate_before").val();
        if (after.length == 0) {
            return;
        }
        var cb = function(doDelete) {
            if (doDelete) {
                var _this = Geof.cntrl.audit;
                Gicon.setActive('btnDiscardAudit',true);
                after = DateUtil.getSvrDate(after,"/",":");
                var where = [{"column":"rundate","operator":"<","value":after}];

                Geof.model.delete(where, _this, function() {
                    Gicon.setEnabled('btnDiscardAudit',true);
                    _this.populateList();
                });
            }
        };
        PanelMgr.showDeleteConfirm("Truncate Request Audit Logs", "Truncate Request Audit Logs?", cb);

    }

};

Geof.cntrl.authcode = {

    id:'id',
    entity:'authcode',
    prefix:'authcode_',
    fields:['id','guid','usrid','startdate','enddate','maxuses','lastused'],
    defaults:[-1,'',-1,null,null,-1,null],
    exclude:['lastused'],
    list_columns: "id,guid,usrid,startdate,enddate",
    order_by:"usrid,startdate,enddate",
    title:'Authorization Codes',
    olclass:'olAuthcode',
    list_tmpl : '<li class="ui-widget-content" id="%%id" data-id="%id">'
        + '<label>Code:</label><label class="data">%guid</label><br>'
        + '<label>User:</label><label class="data">%usrid</label><br>'
        + '<label>Start Date:</label><label class="data">%startdate</label><br>'
        + '<label>End Date:</label><label class="data">%enddate</label></li>',

    editConfig: {
        dialogName:'edit_authcode', divName:'editAuthcode',
        autoOpen: true, minHeight: 290, minWidth: 500,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.authcode;
        Gicon.click("btnRefreshAuthcode", _this.populateList);
        Gicon.click("btnEditAuthcode", _this.editSelected);
        Gicon.click("btnDiscardAuthcode", _this.delete);
        Gicon.setEnabled("btnNewAuthcode", true );
        Gicon.click("btnNewAuthcode", _this.edit);
        _this.populateList();
    },

    validateNew:function() {
        var is_valid = false;
        if (JsUtil.hasValue("authcode_guid") && JsUtil.hasValue("authcode_usrid")) {
            var max_uses = $("#authcode_maxuses").val();
            var start = DateUtil.parseDate($("#authcode_startdate").val());
            var end = DateUtil.parseDate($("#authcode_enddate").val());
            if ( (start || false) && (end || false)) {
                if (DateUtil.isBefore(start,end)) {
                    is_valid = true;
                }
            }
            if (! is_valid && JsUtil.toInt(max_uses) > 0) {
                is_valid = true;
            }
        }
        Gicon.setEnabled("edit_authcode_save",is_valid);
    },

    populateList:function() {
        var _this = Geof.cntrl.authcode;
        Gicon.setEnabled("btnEditAuthcode", false );
        Gicon.setEnabled("btnDiscardAuthcode", false );
        var $items = $('#olAuthcodes');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var enabled = $( ".ui-selected", this).length > 0;
                    Gicon.setEnabled("btnEditAuthcode", enabled );
                    Gicon.setEnabled("btnDiscardAuthcode", enabled );
                }
            });
            Gicon.setEnabled("btnRefreshAuthcode", true );
        };
        Geof.model.read(null,_this, cb);
    },

    edit: function (req) {
        var _this = Geof.cntrl.authcode;

        var data = (req || false) ? req.data : [{}];

        if ('startdate' in data[0]) {
            data[0].startdate = DateUtil.parseToPickerDate(data[0].startdate);
        }
        if ('enddate' in data[0]) {
            data[0].enddate = DateUtil.parseToPickerDate(data[0].enddate);
        }

        Geof.cntrl.showEditDialog(_this, data, false, function() {
            _this.setUsrList(data);
            $("#editAuthcode").tooltip();
            Gicon.click("downloadNewAuthcode", _this.requestGuid);
            Gicon.click("edit_authcode_save", _this.save);
            $( "#authcode_startdate" ).datetimepicker();
            $( "#authcode_enddate" ).datetimepicker();
            $(".auth_validate").each(function() {
                $(this).change(Geof.cntrl.authcode.validateNew);
            })
        });

    },

    editSelected: function () {
        var _this = Geof.cntrl.authcode;
        var list = $("#olAuthcodes .ui-selected");
        var $item = $(list[0]);
        Geof.model.readSelected($item, _this, _this.edit);
    },

    requestGuid:function () {
        var cb = function(req) {
            var guid = req.data[0]['guid'];
            $('#authcode_guid').val(guid);
            Gicon.setEnabled('downloadNewAuthcode',true);
            Geof.cntrl.authcode.validate();
        };
        Gicon.setActive('downloadNewAuthcode',true);
        var obj = {"entity":"authcode","action":"create","actionas":"create_guid","data":{}};
        Transaction.post( GRequest.fromJson(obj), cb);
    },

    save:function () {
        var _this = Geof.cntrl.authcode;
        Gicon.setActive('edit_authcode_save',true);
        var flds = Geof.cntrl.getDialogData(_this);

        var update = (flds.id || false) ? flds.id >= 0 : false;

        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('edit_authcode_save',true);
        };
        var data = {};
        flds['startdate'] = DateUtil.getSvrDate(flds['startdate'],"/",":");
        flds['enddate'] = DateUtil.getSvrDate(flds['enddate'],"/",":");
        var maxuses = flds.maxuses;
        if (maxuses == undefined || maxuses.length == 0) {
            flds.maxuses = -1;
        }
        data.fields = flds;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    setUsrList:function(data) {
        var usrid = -1;
        if ((data || false) && (data.length == 1)) {
            usrid = data[0].usrid;
        }

        var cb = function(req) {
            Geof.cntrl.fill_select(req.data,'authcode_usrid','id','%lastname, %firstname',['lastname','firstname'],'id',usrid);
        };
        var obj = {'entity':'usr','action':'read','data':{'columns':'firstname,lastname,id','orderby':'lastname, firstname'}};
        Transaction.post(GRequest.fromJson(obj), cb);
    },

    delete: function(){
        var _this = Geof.cntrl.authcode;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected authorization codes?");
    }
};

Geof.cntrl.configuration = {
    id:'id',
    entity:'configuration',
    filename:'configuration',
    prefix:'cnfg_',
    fields:[],
    defaults:[],
    exclude:[],
    list_columns: "",
    order_by:"",
    title:'Configuration',
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="flw130">%name</label>'
        + '<label class="flw200">%value</label></li>',
    link: {},
    editConfig: { },

    initialize: function() {
        var _this = Geof.cntrl.configuration;
        _this.populateList();
        Gicon.click("btnRefreshConfiguration", _this.populateList);
        Gicon.click("btnImportConfiguration", _this.reload);
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshConfiguration", true );
        var _this = Geof.cntrl.configuration;

        var $items = $('#olConfiguration');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({});
            Gicon.setEnabled("btnRefreshConfiguration", true );
        };
        Geof.model.read(null,_this, cb);
    },

    reload :function() {
        Gicon.setActive("btnImportConfiguration", true );
        var _this = Geof.cntrl.configuration;
        var cb = function() {
            Gicon.setEnabled("btnImportConfiguration", true );
        };
        Geof.model.readAs(null,'reload',_this, cb);
    }
};

Geof.cntrl.dbpool = {
    id:'id',
    entity:'dbpool',
    prefix:'dbp_',
    fields:['id','lifetime','statusname','status','connected','sessionid','querytime','connstr'],
    defaults:[-1,'','','','','','',''],
    exclude:[],
    list_columns: "id,lifetime,statusname,sessionid",
    order_by:"connected",
    title:'Database Pool Connections',
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="flw80">%lifetime</label><label class="flw70">%statusname</label>' +
        '<label class="flw130"> %connected</label><label class="flw220">%sessionid</label><label class="idRight">%id</label></li>',
    editConfig: {
        dialogName:'edit_dbpool', divName:'editDbpool',
        autoOpen: true, minHeight: 250, minWidth: 500,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.dbpool;
        Gicon.click("btnRefreshDbpool", _this.populateList);
        Gicon.click("btnEditDbpool", _this.editSelected);
        Gicon.click("btnDiscardDbpool", _this.delete);
        _this.populateList();
    },

    populateList:function() {
        var _this = Geof.cntrl.dbpool;
        Gicon.setEnabled("btnEditDbpool", false );
        Gicon.setEnabled("btnDiscardDbpool", false );
        var $items = $('#olDbpools');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var count = $( ".ui-selected", this).length;
                    Gicon.setEnabled("btnEditDbpool", count == 1 );
                    Gicon.setEnabled("btnDiscardDbpool", count > 0 );
                }
            });
            Gicon.setEnabled("btnRefreshDbpool", true );
        };
        Geof.model.read(null,_this, cb);
    },

    edit: function (req) {
        var _this = Geof.cntrl.dbpool;
        var data = (req || false) ? req.data : {};
        Geof.cntrl.showEditDialog(_this, data, false);
        $("#editDbpool").tooltip();
        Gicon.click("edit_dbpool_discard", _this.delete);
    },

    editSelected: function () {
        var _this = Geof.cntrl.dbpool;
        var list = $("#olDbpools .ui-selected");
        if (list.length != 1) {
            Geof.log("fix the editSelected error where the selected count <> 1");
        }
        var $item = $(list[0]);
        Geof.model.readSelected($item, _this, _this.edit);
    },

    save:function () {},
    delete: function(){
        var _this = Geof.cntrl.dbpool;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected db connection?");
    }
};

Geof.cntrl.encryption = {
    id:'id',
    entity:'rsaencryption',
    filename:'encryption',
    prefix:'enc_',
    fields:['id' ,'modulus','exponent','pexponent','p','q','dp','dq','qinv','createdate'],
    defaults:[],
    exclude:[],
    list_columns: "id,createdate",
    edit_columns: "id ,modulus,exponent,pexponent,p,q,dp,dq,qinv,createdate",
    order_by:"createdate desc",
    title:'RSA Encryption Keys',
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="flw60">%id</label><label class="flw160">%createdate</label></li>',
    editConfig: {
        dialogName:'edit_encryption', divName:'editEncryption',
        autoOpen: true, minHeight: 550, minWidth: 600,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.encryption;
        Gicon.click("btnNewEncryption", _this.save);
        Gicon.click("btnRefreshEncryption", _this.populateList);
        Gicon.click("btnViewEncryption", _this.editSelected);
        Gicon.click("btnDiscardEncryption", _this.delete);
        _this.populateList();
    },

    populateList:function() {
        var _this = Geof.cntrl.encryption;
        Gicon.setEnabled("btnViewEncryption", false );
        Gicon.setEnabled("btnDiscardEncryption", false );
        var $items = $('#olRsaencryptions');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var count = $( ".ui-selected", this).length;
                    Gicon.setEnabled("btnViewEncryption", count == 1 );
                    Gicon.setEnabled("btnDiscardEncryption", count > 0 );
                }
            });
            Gicon.setEnabled("btnRefreshEncryption", true );
        };
        Geof.model.read(null,_this, cb);
    },

    edit: function (req) {
        var _this = Geof.cntrl.encryption;
        var data = (req || false) ? req.data : {};
        Geof.cntrl.showEditDialog(_this, data, false);
        $("#editEncryption").tooltip();
        Gicon.click("edit_encryption_discard", _this.delete);
    },

    editSelected: function () {
        var _this = Geof.cntrl.encryption;
        var list = $("#olEncryption .ui-selected");
        if (list.length != 1) {
            Geof.log("fix the editSelected error where the selected count <> 1");
        }
        var $item = $(list[0]);
        Geof.model.readSelected($item, _this, _this.edit);
    },

    save:function () {
        var _this = Geof.cntrl.encryption;
        Gicon.setActive('btnNewEncryption',true);

        var cb = function (req,textStatus,jqXHR) {
            _this.populateList();
            Gicon.setEnabled('btnNewEncryption',true);
        };
        Geof.model.create({}, _this, cb);

    },

    delete: function(){
        var _this = Geof.cntrl.encryption;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected encryption keys?");
    }
};

Geof.cntrl.entity = {
    id:'id',
    entity:'entity',
    prefix:'ent_',
    fields: ["name",'id','loadtime','status','entitytype','indatabase'],
    defaults: ['',-1,0,0,0,1],
    exclude:null,
    list_columns: "id,name,indatabase",
    order_by:"name",
    title:'Entity',
    link: [
        {
            name:'entityfield',
            type:'child',
            entity:'entityfield',
            icon:'img/symbol/Database.png',
            buttons:[
                {action:'save',callback:function(info,name) {Geof.cntrl.entity.saveEntityField(info,name)}}
            ],
            readCallback:function(data) { Geof.cntrl.entityfield.setLiCheckboxes(data); }
        }
    ],
    editConfig: {
        dialogName:'entity_list', divName:'entity_list',
        autoOpen: true, minHeight: 280, minWidth: 540,
        resizable: false, modal:true
    },
    icon:'keys',

    stati: ["missing",'okay','changed','dropped','not_table','error'],
    list_tmpl: '<li class="ui-widget-content" data-id="%id" data-name="%name">%name'
        +'<label class="idRight w14">%id</label><label class="idRight status60 font10">%status</label></li>',

    initialize: function() {
        var _this = Geof.cntrl.entity;
        _this.populateList();
        Gicon.click("btnFixEntity", _this.fix);
        Gicon.click("btnRefreshEntity", _this.populateList);
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        var _this = Geof.cntrl.entity;
        Gicon.setActive('btnRefreshEntity',true);
        var $pbar = PanelMgr.showProgressBar({title:'Loading Entities',indeterminate:true});
        var $items = $('#olEntitys');
        $items.empty();
        var cb = function(req) {
            Templater.createEntityList (req.data, $items, _this.list_tmpl, _this.stati);
            $items.selectable({
                stop: function() {
                    var selected = $('#olEntitys .ui-selected');
                    var selected_ids = Geof.model.selectedIds(selected, _this);
                    Geof.cntrl.parentSelected(selected_ids);
                }
            });
            $pbar.cancel();
            Gicon.setEnabled('btnRefreshEntity',true);
        };
        Geof.model.read(null,_this, cb);
    },

    addNew: function () {
        var _this = Geof.cntrl.entity;
        Geof.cntrl.showEditDialog(_this, {}, false);
    },

    editSelected: function () { },

    fix:function() {
        Gicon.setActive('btnFixEntity',true);
        var $pbar = PanelMgr.showProgressBar({title:'Fixing Entities',indeterminate:true});
        var _this = Geof.cntrl.entity;
        var trans = new Transaction(Geof.session);
        var jReq = GRequest.build(_this.entity, 'update','fix',{});
        var cb = function(skip, error){
            Gicon.setEnabled('btnFixEntity',true);
            $pbar.cancel();
            if (error || false ) {
                PanelMgr.showError('Entity Fix Error',error);
            } else {
                _this.populateList();
            }
        }
        trans.addRequest(jReq);
        trans.send(cb);
    },

    saveEntityField:function(info, name) {
        Gicon.setActive(name,true);
        var _this = Geof.cntrl.entity;

        var trans = new Transaction(Geof.session);
        $('#sub_ol_entityfield li').each(function() {
            var _this = $(this);
            var id =  _this.data('id');
            var fld = _this.data('fld');

            var flds = {};
            var prefix = "#" + id + '_' + fld;
            flds['isspatial'] = $(prefix + '_isspatial').is(':checked');
            flds['isdefault'] = $(prefix + '_isdefault').is(':checked');
            flds['istemporal'] = $(prefix + '_istemporal').is(':checked');
            var data = {fields:flds,where:{entityid:id,fieldname:fld}};
            trans.addRequest(GRequest.build('entityfield','update',null,data), null);
        });
        trans.setLastCallback(function() {
            Gicon.setEnabled(name,true);
            Geof.cntrl.parentSelected(info.id);
        });
        trans.send();
    },

    save: function () { },

    delete: function (){}
};

Geof.cntrl.entityfield = {
    id:'id',
    entity:'entityfield',
    prefix:'ef_',
    fields: ["fieldname",'entityid','isdefault','ispkey','datatype','isrequired','isauto','isspatial','istemporal'],
    defaults: ['',-1,false,false,0,false,false,false,false],
    exclude:null,
    list_columns: "entityid.fieldname,isdefault,ispkey,datatype,isrequired,isauto,isspatial,istemporal",
    order_by:"fieldname",
    title:'<label class="hdrLabelLeft2 flw40">Field</label>'
        + '<label class="hdrLblRSm w50tc">Temporal</label><label class="hdrLblRSm w50tc">Spatial</label>'
        + '<label class="hdrLblRSm w50tc">Auto</label><label class="hdrLblRSm w52tc">Required</label>'
        + '<label class="hdrLblRSm w50tc">Datatype</label><label class="hdrLblRSm w48tc">Pkey</label>'
        + '<label class="hdrLblRSm w48tc">Default</label>',

    editConfig: {},
    icon:'Database',
    list_tmpl: '<li class="ui-widget-content" data-id="%entityid" data-fld="%fieldname">%fieldname'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_istemporal">'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_isspatial">'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_isauto" disabled="disabled" >'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_isrequired" disabled="disabled" >'
        + '<input class="permCol2" readonly id="%entityid_%fieldname_datatype"/>'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_ispkey" disabled="disabled" >'
        + '<input type="checkbox" class="permCol" id="%entityid_%fieldname_isdefault"></li>',

    initialize: function() {
        Geof.cntrl.entityfield.populateList();
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        var _this = Geof.cntrl.entityfield;
        var $items = $('#olEntityfields');
        $items.empty();
        var cb = function(req) {
            $items.selectable();
        };
        Geof.model.read(null,_this, cb);
    },

    setLiCheckboxes:function(  data ) {
        var opts = "isdefault,ispkey,datatype,isrequired,isauto,isspatial,istemporal".split(',');
        for (var indx=0;indx<data.length;indx++) {
            var row = data[indx];
            for (var i=0;i<opts.length;i++) {
                var fld = opts[i];
                var selector = "#" + row.entityid + "_" + row.fieldname + "_" + fld;
                var cntl = $(selector);
                if (fld == 'datatype') {
                    cntl.val(Datatype.getType(row[fld]));
                } else {
                    cntl.prop('checked', row[fld]);
                }
            }
        }
    },

    addNew: function () {},

    editSelected: function () { },

    save: function () { },

    delete: function (){}
};

Geof.cntrl.file = {
    id: 'id',
    entity: 'file',
    prefix: 'note_',
    fields: ['id', 'filename', 'fileext', 'filesize', 'originalname', 'status', 'checksumval', 'createdate', 'notes', 'storagelocid', 'viewid', 'filetype', 'geomtype', 'duration'],
    defaults: [-1, '', '', 0, '', -1, 0, null, '', -1, -1, -1, -1, -1],
    exclude: [],
    list_columns: "id,originalname,status",
    full_list_columns: 'id,filename,fileext,filesize,originalname,status,checksumval,createdate,notes,storagelocid,viewid,filetype,geomtype,duration',
    order_by: "id",
    title: 'Files',
    list_tmpl: '<li class="ui-widget-content" data-id="%id"><label class="floatLeft font9">%originalname</label></li>',

    editConfig: {
        dialogName: 'edit_file', divName: 'editFile',
        autoOpen: true, minHeight: 350, minWidth: 360,
        resizable: false, modal: false
    },

    getFile: function (fileid, callback) {
        var cb = function (req) {
            var data = req.data;
            if (callback || false) {
                if (data !== undefined && data.length == 1) {
                    callback(data[0]);
                } else {
                    callback(null);
                }
            }
        };
        Geof.model.readOptions({
            entity: 'file',
            where: {id: fileid},
            columns: Geof.cntrl.file.full_list_columns,
            callback: cb
        });
    }
};

Geof.cntrl.logger = {
    id:'id',
    entity:'logger',
    filename:'logger',
    prefix:'lgr_',
    fields:['level','filepath','content'],
    defaults:[],
    exclude:[],
    list_columns: "",
    order_by:"",
    title:'Logger',
    list_tmpl:'',
    link: {},
    editConfig: { },

    initialize: function() {
        var _this = Geof.cntrl.logger;
        Gicon.click("btnRefreshLogger", _this.populateList);
        Gicon.click("btnEditLogger", _this.save);
        Gicon.click("btnDiscardLogger", _this.delete);
        $(" .subDlgWrapper").tooltip();
        $("#lgr_level").on("change", function () {
            Gicon.setEnabled("btnEditLogger", true);
        });

        var logger_view = GLocal.get("logger_view","init.log");

        $("#lgr_file").on("change", function () {
            var value = $(this).val();
            GLocal.set("logger_view", value);
            Gicon.setEnabled("btnDiscardLogger", value == 'geof.log');
            _this.populateList();
        });
        $('#lgr_file option[value="' + logger_view + '"]').prop('selected', true);
        _this.populateList();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshLogger", true );
        var _this = Geof.cntrl.logger;

        var filename = $('#lgr_file').val();
        var where = {'filename':filename};
        var cb = function(req) {
            var data = req.data;
            if ((data || false) && (data.length == 1)) {
                data = data[0];
                var text = '';
                for (var indx=0;indx<data.content.length;indx++) {
                    text = text + base64.decode( data.content[indx] ) + "\n";
                }
                $('#lgr_content').val(text);
                $('#lgr_level').val(data.level);
            }

            Gicon.setEnabled("btnRefreshLogger", true );
        };
        Geof.model.readAs(where,"settings",_this, cb);
    },

    save: function () {
        var _this = Geof.cntrl.logger;
        Gicon.setActive('btnEditLogger',true);

        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('btnEditLogger',true);
        };
        var lvl = $("#lgr_level").val()
        Geof.model.update({level:lvl,fields:{id:-1}}, _this, cb);
    },

    delete: function (){
        var cb = function () {
            Gicon.setActive('btnDiscardLogger', true);
            var obj = {"entity": "logger", "action": "delete", "data": {}};
            Transaction.post(GRequest.fromJson(obj), function () {
                Gicon.setEnabled('btnDiscardLogger', true);
                Geof.cntrl.logger.populateList();
            });

        };
        PanelMgr.showConfirm("Truncate Server Logs", "Truncate Logs?", cb);
    }
};

Geof.cntrl.permission = {
    id:'id',
    entity:'permission',
    prefix:'perm_',
    fields: ['id','name'],
    defaults: ['',''],
    exclude:null,
    list_columns:"id,name",
    list_block:"sub_list_block_permission.html",
    order_by:"name",
    title:'<label class="hdrLabelLeft flw145">Permission</label>'
        + '<label class="hdrLabelLeft w50tc">Create</label><label class="hdrLabelLeft w48tc">Read</label>'
        + '<label class="hdrLabelLeft w48tc">Update</label><label class="hdrLabelLeft w48tc">Delete</label>'
        + '<label class="hdrLabelLeft w48tc">Execute</label>'
        + '<div id="cbPermissionAll"><label class="hdrLabelLeft flw145"></label>'
        + '<input type="checkbox" class="permCol45" id="cbPermissionCreate" title="Select all Create">'
        + '<input type="checkbox" class="permCol45" id="cbPermissionRead" title="Select all Read"><input type="checkbox" class="permCol45" id="cbPermissionUpdate" title="Select all Update">'
        + '<input type="checkbox" class="permCol45" id="cbPermissionDelete" title="Select all Delete"><input type="checkbox" class="permCol45" id="cbPermissionExecute" title="Select all Execute"></div>',
    editConfig: {},
    icon:'keys',
    list_tmpl: '<li class="ui-widget-content" data-id="%entityid"><div class="floatLeft">'
        + '<label class="stdLabelLeftSm flw110 panel_ligrn">%name</label>'
        + '<div class="floatLeft panel_ligrn"><input type="checkbox" class="permColBar" id="%entityid_crude" title="Select entire row"></div>'
        + '<input type="checkbox" class="permCol45" id="%entityid_createable"><input type="checkbox" class="permCol45" id="%entityid_readable">'
        + '<input type="checkbox" class="permCol45" id="%entityid_updateable"><input type="checkbox" class="permCol45" id="%entityid_deleteable">'
        + '<input type="checkbox" class="permCol45" id="%entityid_executable">'
        +  '</div></li>',

    setLiCheckboxes:function(  data ) {

        var opts = ["createable","readable","updateable","deleteable","executable"];
        for (var indx=0;indx<data.length;indx++) {
            var id = data[indx].entityid;
            function getRowClickFunction(id) {
                var rowcb = '#' + id + '_crude';
                var cb = $(rowcb).click(function() {
                    Geof.cntrl.permission.selectRowStart(id, $(this).prop('checked'));
                })
                return cb;
            }
            getRowClickFunction(id);
            for (var i in opts) {
                var $cb = $("#" + id + "_" + opts[i]);
                $cb.prop('checked', data[indx][opts[i]]);
                $cb.click(Geof.cntrl.permission.setDeselectStatus);
            }
        }
        $('#cbPermissionCreate').click(function(){
                Geof.cntrl.permission.selectRow('createable', $(this).prop('checked'));
        });
        $('#cbPermissionRead').click(function(){
                Geof.cntrl.permission.selectRow('readable', $(this).prop('checked'));
        });
        $('#cbPermissionUpdate').click(function(){
                Geof.cntrl.permission.selectRow('updateable', $(this).prop('checked'));
        });
        $('#cbPermissionDelete').click(function(){
                Geof.cntrl.permission.selectRow('deleteable', $(this).prop('checked'));
        });
        $('#cbPermissionExecute').click(function(){
                Geof.cntrl.permission.selectRow('executable', $(this).prop('checked'));
        });
//        $('#NINE_crude').click(function(){
//            alert('clicked');
//            Geof.cntrl.permission.selectRowStart('9', $(this).prop('checked'))
//        });
    },
    selectAllSub:function() {
        $('#sub_ol_permission :checkbox').each(function() {
                $(this).prop('checked', true);
            }
        );
        Gicon.setEnabled('btnSub_permissionDeselectAll', true);
    },
    deselectAllSub:function() {
        $('#sub_ol_permission :checkbox').each(function() {
                $(this).prop('checked', false);
            }
        );
        $('#cbPermissionAll :checkbox').each(function() {
                $(this).prop('checked', false);
            }
        );
        Gicon.setEnabled('btnSub_permissionDeselectAll', false);
    },
    setDeselectStatus:function() {
        var checked = $('#sub_ol_permission :checkbox:checked');
        Gicon.setEnabled('btnSub_permissionDeselectAll', checked.length>0);
    },
    selectRow:function(name, state) {
        var ele = "#sub_ol_permission :checkbox[id$="+name+"]";
        $(ele).each(function() {
                $(this).prop('checked', state);
            }
        );
        Geof.cntrl.permission.setDeselectStatus();
    },
    deselectRow:function(name) {
        var ele = "#sub_ol_permission :checkbox[id$="+name+"]";
        $(ele).each(function() {
                $(this).prop('checked', false);
            }
        );
        Geof.cntrl.permission.setDeselectStatus();
    },
    selectRowStart:function(name, state) {
        var ele = "#sub_ol_permission :checkbox[id^="+name+"]";
        $(ele).each(function() {
                $(this).prop('checked', state);
            }
        );
//        Geof.cntrl.permission.setDeselectStatus();
    }
};

Geof.cntrl.notification = {
    id:'id',
    entity:'notification',
    prefix:'note_',
    fields:['id','message','level','usrid','notificationid','createdate','type'],
    defaults:[-1,'',0, -1,null,null],
    exclude:[],
    list_columns: "id,message,level,type",
    types:{'System':0,'Annotation':1,'Local':2},
    levels:{'Low':0,'Medium':1,'High':2,'Alert':3},
    order_by:"id",
    title:'Notifications',
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="floatLeft font9">%message</label><label class="idRight font9">%id</label></li>',

    full_list_tmpl : '<li class="ui-widget-content" id="linotify%id" data-id="%id">'
        + '<label class="idLeft">%createdate</label><label class="idLeft ml14">From: %lastname, %firstname</label>'
        + '<label class="idRightF9">[%id]</label>'
        + '<label class="idRightF9 mr14">%lvl</label>%typeblock<label class="idRightF9 mr4">Type:</label><br>'
        + '<input type="checkbox" data-id="%id" class="cbNotifyPop"/>'
        + '<textarea class="notifyPopup notifyNormal" spellcheck="false" id="taNofity%id" disabled>%message</textarea>'
        +'</li>',

    type_blocks:[
        '<label class="idRightF9 mr14" data-id="%id">System Generated</label>',
        '<label class="nota_anno idRightF9 mr14" data-id="%id">Annotation</label>',
        '<label class="idRightF9 mr14" data-id="%id">Local</label>'
    ],

    editConfig: {
        dialogName:'edit_notification', divName:'editNotification',
        autoOpen: true, minHeight: 350, minWidth: 360,
        resizable: false, modal:false
    },

    getTypeName:function(value) {
        var types = Geof.cntrl.notification.types;
        for (var indx=0;indx<types.length;indx++) {
            if (types[indx] == value) {
                return indx;
            }
        }
        return undefined;
    }
};

Geof.cntrl.profile = {
    id:'id',
    entity:'profile',
    prefix:'profile_',
    fields:['id','loginname','password','firstname','lastname','initials','email','notes','lastattempt','settings'],
    defaults:['-1','','','','','','','',''],
    exclude:['lastattempt'],
    list_columns: "id,firstname,lastname",
    order_by:"lastname, firstname",
    title:'Profile',
    link: [],
    filename:'profile',
    filelist:['edit'],
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="flw130">%name</label>'
        + '<label class="flw290">%value</label></li>',

    ui: '<div id="divEditProfile" class="floatRight">'
        + '<label class="stdlabelSm floatLeft mt12" id="usr_name"></label>'
        + '<span class="ui-icon icon_geof_profile_enable iconRight" id="btnEditProfile" title="Edit Profile"></span>'
        + '</div>',

    cfg:{
        file:'profile_edit',
        directory:'core/panel/',
        divName:'editProfile',
        dragbar:'profileDrayBar',
        complete_callback:null
    },

    setControl:function (container) {
        var profile =  Geof.cntrl.profile;
        profile.cfg.complete_callback = profile.setupButtonEvents;
        if ( container || false ) {
            $('#' + container).html( profile.ui );
            PanelMgr.loadDialogY( profile.cfg );

        } else {
            alert("Geof.cntrl.profiler needs a container tag");
        }
    },

    setupButtonEvents:function () {
        var _this = Geof.cntrl.profile;
        $( "#profile_tabs" ).tabs();

        _this.dialog = $('#' + _this.cfg.divName);
        Gicon.click("closeProfileDialog", function () {
            _this.dialog.hide()
        });
        Gicon.click('btnEditProfile', _this.show, true);
        Gicon.click("btnRequestRSA", _this.get_rsa_data);
        Gicon.click("btnDeleteRSA", _this.clear_local_rsa);
        Gicon.click("btnRefreshRSA", _this.get_local_rsa);
        Gicon.click("btnImportRsa", _this.import_rsa);
        Gicon.click("btnProfileSave", _this.save);
        Gicon.click("edit_profile_updatepwd", _this.change_pwd);
        Gicon.click("btnUploadSettings", _this.save_settings);
        Gicon.click("btnDownloadSettings", _this.load_settings);
        Gicon.click("btnSaveSettings", _this.update_setting);
        Gicon.click("btnDiscardSettings", _this.discard_setting);
        Gicon.click("btnReloadSettings", _this.populateSettingsList);

        Gcontrol.checkbox("cbEncryptBlobs", undefined, "encrypt_blobs");
        Gcontrol.checkbox("cbDebugWriter", undefined, "debug_writer");
        Gcontrol.checkbox("cbTestTransId", undefined, "test_trans_id");

        $("#profile_password").blur(_this.checkPasswords);
        $("#profile_password2").blur(_this.checkPasswords);
        $("#sltSettingNames").click(function() {
            $("#taSettingValue").text($(this).val());
            Gicon.setEnabled('btnSaveSettings',true);
            Gicon.setEnabled('btnDiscardSettings',true);
        });

        $("#import_rsa_key").blur(function () {
            var enabled = false;
            try {
                JSON.parse($(this).val());
                enabled = true;
            } catch (e) {
            }
            Gicon.setEnabled('btnImportRsa', enabled);
        });

        Gcontrol.checkbox('auto_rsa_renewal', _this.save_rsa_renewal, 'auto_rsa_renewal');

    },

    populateSettingsList:function() {
        Gicon.setActive('btnReloadSettings',true);
        $('#sltSettingNames').empty();
        JsUtil.addOptions('sltSettingNames', GLocal.load(),null,true);
        var opts = document.getElementById('sltSettingNames').options;
        if (opts.length > 0) {
            $("#taSettingValue").text(opts[0].value);
        }
        Gicon.setEnabled('btnReloadSettings',true);
    },

    update_setting:function() {
        var name = $('#sltSettingNames').find(":selected").text();
        if (name !== undefined) {
            var cb = function (sav) {
                if (sav) {
                    var value = $("#taSettingValue").text();
                    GLocal.set(name, value);
                    Geof.cntrl.profile.populateSettingsList();
                }
            };
            PanelMgr.showConfirm("Update Setting","Update setting: " + name + "?", cb)
        }
    },

    discard_setting:function(){
        var name = $('#sltSettingNames').find(":selected").text();
        if (name !== undefined) {
            var cb = function (del) {
                if (del) {
                    GLocal.remove(name);
                    Geof.cntrl.profile.populateSettingsList();
                }
            };
            PanelMgr.showConfirm("Delete Setting","Delete setting: " + name + "?", cb)
        }
    },

    save_settings:function() {
        var strSettings = base64.encode(JSON.stringify(GLocal.load()));

        if (strSettings || false) {
            Gicon.setActive("btnUploadSettings",true);
            var cb = function () {
                Gicon.setEnabled("btnUploadSettings", true);
            };
            var r = GRequest.build('profile','update','settings',{fields:{settings:strSettings}});
            Transaction.post(r,cb);
        }
    },

    load_settings:function() {
        Gicon.setActive("btnDownloadSettings",true);
        var cb = function (req) {
            Gicon.setEnabled("btnDownloadSettings", true);
            var data = req.data[0];
            var settings = '';
            if ('settings' in data) {
                settings = base64.decode(data.settings);
                settings = JSON.parse(settings);
                GLocal.save(settings);
            }
        };
        var r = GRequest.build('profile','read',null,{where:{id:Geof.session.usr.usrid}});
        Transaction.post(r,cb);
    },

    show:function () {
        var _this = Geof.cntrl.profile;
        var dialog = _this.dialog;

        if (Geof.logged_in) {
            var cb = function(req) {
                $( "#profile_tabs" ).tabs({ active: 0 });
                var data = req.data[0];
                Geof.session.usr.email =  data.email;
                Geof.cntrl.setEditFields(_this, data);
            };
            Geof.model.readRecord(Geof.session.usr.usrid, _this, cb);
            Gicon.click("btnEmailRSA", _this.email_rsa);
        } else {
            $( "#profile_tabs" ).tabs({active:2,disabled:[0,1]});
            Gicon.setEnabled("btnProfileSave", false);
            Gicon.setEnabled("edit_profile_updatepwd", false);
            Gicon.setEnabled("btnRequestRSA",false);
            Gicon.click("btnEmailRSA", _this.show_import_dialog);
        }

        _this.populateSettingsList();
        _this.get_local_rsa();
        dialog.show();
        Geof.center_in_body(dialog);
    },


    save: function () {
        var _this = Geof.cntrl.profile;
        if ( !(Geof.session || false) || !(Geof.session.usr ||false)) {
            PanelMgr.showError("User is not logged in");
            return;
        }

        Gicon.setActive('btnProfileSave',true);
        var fields = Geof.cntrl.getDialogData(_this,['password']);
        var cb = function () {
            Gicon.setEnabled('btnProfileSave',true);
        };
        var data = {};
        Geof.session.usr.email = fields.email;
        data.fields = fields;
        fields.clearcode = Geof.session.getClearcode();
        data.where = {id:Geof.session.usr.usrid};
        Geof.model.update(data, _this, cb);
    },

    change_pwd: function (){
        var _this = Geof.cntrl.profile;
        $("#profile_error").text('');
        var pwd1 = $("#profile_password").val();
        Gicon.setActive('edit_profile_updatepwd',true);
        var cb = function (req) {
            Gicon.setEnabled('edit_profile_updatepwd', true);
            if (req.error || false) {
                $("#profile_password_error").text(req.error);
            }
        };
        var clearcode = Geof.session.getClearcode();
        var data = {
            fields: {password: pwd1, 'clearcode': clearcode},
            where: {id: Geof.session.usr.usrid}
        };
        Geof.model.updateAs("password",data,_this,cb);

    },

    checkPasswords:function() {
        Gicon.setEnabled('edit_profile_updatepwd',false);
        $('#usr_password_error').text('');
        var pwd1 = $("#profile_password").val();
        var pwd2 = $("#profile_password").val();
        if (pwd1.length > 0 && pwd2.length > 0) {
            if (pwd1.length < 8) {
                $('#profile_password_error').text('Password length less than 8 characters');
            } else if (pwd1 != pwd2) {
                $('#profile_password_error').text('Passwords do not match');
            } else {
                Gicon.setEnabled('edit_profile_updatepwd',true);
            }
        }
    },

    get_local_rsa: function() {
        Gicon.setActive('btnRefreshRSA',true);
        $("#rsa_id").text("");
        $("#rsa_modulus").val("");
        $("#rsa_exponent").val("");
        var rsa = Geof.session.getRsaKeyLocal();
        if (rsa != null) {
            $("#rsa_id").text(rsa.id);
            $("#rsa_modulus").val(rsa.modulus);
            $("#rsa_exponent").val(rsa.exponent);
        }
        Gicon.setEnabled('btnRefreshRSA',true);
    },

    email_rsa: function() {
        Gicon.setActive('btnEmailRSA',true);
        var cb = function () {
            Gicon.setEnabled('btnEmailRSA', true);
        };
        Transaction.post(GRequest.build('rsaencryption','read','rsaemail',{}), cb);
    },

    get_rsa_data: function() {
        var _this = Geof.cntrl.profile;
        Gicon.setActive('btnRequestRSA',true);
        var cb = function (req) {
            Gicon.setEnabled('btnRequestRSA', true);
            if ('data' in req) {
                Geof.session.saveRsaKeyLocal( req.data[0]);
                _this.get_local_rsa();
            }
        };
        Transaction.post(GRequest.build('rsaencryption','read','rsaencryption',{}), cb);
//        var auto_renew = Geof.session.getRsaRenewal();
//        $("#auto_rsa_renewal").prop('checked', auto_renew);
    },

    save_rsa_renewal:function() {
        var auto_renew = $("#auto_rsa_renewal").is(':checked');
        Geof.session.saveRsaRenewal(auto_renew);
    },

    clear_local_rsa: function() {
        var cb = function () {
            Geof.session.clearLocalRsaKey();
            $("#rsa_id").text("");
            $("#rsa_modulus").val("");
            $("#rsa_exponent").val("");
            Geof.session.clearLocalRsaKey();
            Geof.cntrl.profile.get_local_rsa();
        };
        PanelMgr.showDeleteConfirm(
            "Delete RSA Key",
            "Confirm deletion of RSA Key", cb);
    },

    import_rsa:function() {
        Gicon.setActive('btnImportRsa',true);
        value = JSON.parse($("#import_rsa_key").val());
        Geof.session.saveRsaKeyLocal(value);
        Geof.cntrl.profile.get_local_rsa();
        Gicon.setEnabled('btnImportRsa',true);
    },

    show_import_dialog:function() {
        var $dlg = null;
        var checkfields = function () {
            var enabled = $("#rsaemail_email").val().length > 0;
            Gicon.setEnabled("btnEmailRSA2", enabled);
        };

        var rtnCB = function (result) {
            Gicon.setEnabled('btnEmailRSA2', true);
            try {
                var result = JSON.parse(result);
                if ('state' in result) {
                    $dlg.dialog("close");
                } else if ('error' in result) {
                    alert(result.error);
                }
            } catch (e) {
                alert(e);
            }
        };

        var sendFunc = function () {
            Gicon.setActive('btnEmailRSA2', true);
            var ws = Geof.webservice;
            var url = window.location.origin + '/' + ws + '/' + ws + '?clearcode=';
            url += Geof.session.getClearcode($("#rsaemail_email").val());
            $.get(url)
                .success(rtnCB)
                .error(function (jqXHR, textStatus, errorThrown) {
                    Gicon.setEnabled('btnEmailRSA2', true);
                    alert(errorThrown);
                });
        };

        var completeCB = function(dlg){
            $("#rsaemail_email").blur(checkfields);
            Gicon.click("btnEmailRSA2", sendFunc);
            $dlg = dlg;
            $dlg.show();
        };
        var config = {
            file: 'rsa_email', divName: 'rsa_email',
            autoOpen: true, minHeight: 200, minWidth: 400,
            resizable: false, modal: true,
            complete_callback:completeCB
        };
        PanelMgr.loadDialogX(config);
    }

};

Geof.cntrl.project = {
    name: 'Project',
    id:'id',
    entity:'project',
    prefix:'proj_',
    selected: null,
    fields: ["id",'name','status','description'],
    defaults: [-1,''],
    exclude:null,
    list_columns: "id,name",
    order_by:"name",
    editConfig: {
        dialogName:'project_edit', divName:'editProject',
        autoOpen: true, minHeight: 220, minWidth: 350,
        resizable: false, modal:true
    },
    title:'Projects',
    list_tmpl: '<li class="ui-widget-content" id="%uuid_%id" data-id="%id">%name</li>',
    option_tmpl: '<option value="%id">%name</option>',
    header_tmpl: '<label class="hdrLabelLeft">Project Name</label><label class="hdrLabelRight"> id</label>',

    link: [
        {
            name:'file',
            type:'link',
            entity:'file_project',
            icon:'img/symbol/file_64.png',
            buttons:[{action:'delete', callback:Geof.cntrl.deleteSublink}],
            read:{
                "entity":"file_project",
                "data":{
//                    "columns":"ugroupid,entityid,createable,readable,updateable,deleteable,executable",
                    "join":[{"entity":"file","join":"parent","columns":"id,originalname"}]
                }
            }
        }
    ],

    initialize: function(){
        var _this = Geof.cntrl.project;
        _this.populateList();
        Gicon.click("btnRefreshProject", _this.populateList);
        Gicon.click("btnNewProject", _this.addNew);
        Gicon.click("btnEditProject", _this.editSelected);
        Gicon.click("btnDiscardProject", _this.delete);
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshProject", true );
        Gicon.setEnabled("btnEditProject", false );
        Gicon.setEnabled("btnDiscardProject", false );
        var _this = Geof.cntrl.project;
        var $items = $('#olProjects');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var selected = $('#olProjects .ui-selected');
                    var selected_ids = Geof.model.selectedIds(selected, _this);
                    var enabled = selected_ids.length > 0;
                    Gicon.setEnabled("btnEditProject", enabled );
                    Gicon.setEnabled("btnDiscardProject", enabled );
                    Geof.cntrl.parentSelected(selected_ids);
                }
            });
            Gicon.setEnabled("btnRefreshProject", true );
        };
        Geof.model.read(null,_this, cb);
    },

    addNew: function () {
        var _this = Geof.cntrl.project;
        Geof.cntrl.showEditDialog(_this, {}, true);
        Gicon.click("edit_proj_save",_this.save);
    },

    editSelected: function () {
        var _this = Geof.cntrl.project;
        var list = $("#olProjects .ui-selected");
        var $item = $(list[0]);
        var cb = function(req) {
            Geof.cntrl.showEditDialog(_this, req.data, false);
            Gicon.click("edit_proj_save", Geof.cntrl.project.save);
        };
        Geof.model.readSelected($item, _this, cb);
    },

    save: function () {
        var _this = Geof.cntrl.project;
        Gicon.setActive('edit_proj_save',true);
        var fields = Geof.cntrl.getDialogData(_this);
        var update = (fields.id || false) ? fields.id >= 0 : false;

        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('edit_proj_save',true);
        };
        var data = {};
        data.fields = fields;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    delete: function (){
        var _this = Geof.cntrl.project;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected Projects?");
    }

};

Geof.cntrl.session = {
    id:'sessionid',
    entity:'session',
    prefix:'session_',
    fields:['sessionid','lastaccessed','created','userid','active','login','la_seconds'],
    defaults:[-1,'','','','','',''],
    exclude:[],
    list_columns: "sessionid,login,created,active,lastaccessed",
    order_by:"lastaccessed",
    title:'Active Sessions',
    list_tmpl:'<li class="ui-widget-content" data-sessionid="%sessionid"><label class="flw160">%login</label><label class="flw100">%created</label>' +
        '<label class="flw60"> %active</label><label class="flw95">%lastaccessed</label><label class="idRight">%sessionid</label></li>',
    editConfig: {
        dialogName:'edit_session', divName:'editSession',
        autoOpen: true, minHeight: 250, minWidth: 660,
        resizable: false, modal:true
    },

    initialize: function(){
        var _this = Geof.cntrl.session;
        Gicon.click("btnRefreshSession", _this.populateList);
        Gicon.click("btnEditSession", _this.editSelected);
        Gicon.click("btnDiscardSession", _this.delete);
        _this.populateList();
    },

    populateList:function() {
        var _this = Geof.cntrl.session;
        Gicon.setEnabled("btnEditSession", false );
        Gicon.setEnabled("btnDiscardSession", false );
        var $items = $('#olSessions');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var count = $( ".ui-selected", this).length;
                    Gicon.setEnabled("btnEditSession", count == 1 );
                    Gicon.setEnabled("btnDiscardSession", count > 0 );
                }
            });
            Gicon.setEnabled("btnRefreshSession", true );
        };
        Geof.model.read(null,_this, cb);
    },

    edit: function (req) {
        var _this = Geof.cntrl.session;
        var data = (req || false) ? req.data : {};
        Geof.cntrl.showEditDialog(_this, data, false);
        $("#editSession").tooltip();
        Gicon.click("edit_session_discard", _this.delete);
    },

    editSelected: function () {
        var _this = Geof.cntrl.session;
        var list = $("#olSessions .ui-selected");
        var $item = $(list[0]);
        Geof.model.readSelected($item, _this, _this.edit);
    },

    save:function () {},
    delete: function(){
        var _this = Geof.cntrl.session;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected sessions(s)?");
    }
};

Geof.cntrl.storage = {
    id:'id',
    entity:'storageloc',
    filename:'storage',
    prefix:'strg_',
    fields:['id','quota','filecount','canstream','description','systemdir','name','active','statusid','usedspace'],
    defaults:['-1',0,0,false,'','','','',false,0,0],
    exclude:["filecount",""],
    list_columns: "id,name,usedspace,quota",
    order_by:"name,filecount",
    title:'Storage',
    list_tmpl:'<li class="ui-widget-content" data-id="%id"><label class="flw100">%name</label>'
        + '<label class="w80tr">%usedspace</label><label class="w80tr">%quota</label><label class="w120tr">%id</label></li>',
    link: {},
    storagelocs:{},
    editConfig: {
        dialogName:'storage_edit', divName:'editStorage',
        autoOpen: true, minHeight: 380, minWidth: 460,
        resizable: false, modal:true
    },

    initialize: function() {
        var _this = Geof.cntrl.storage;
        _this.populateList();
        Gicon.click("btnRefreshStorage", _this.populateList);
        Gicon.click("btnNewStorage", _this.addNew);
        Gicon.click("btnEditStorage", _this.editSelected);
        Gicon.click("btnDiscardStorageloc", _this.delete);
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshStorage", true );
        Gicon.setEnabled("btnEditStorage", false );
        Gicon.setEnabled("btnDiscardStorageloc", false );
        var _this = Geof.cntrl.storage;
        var $items = $('#olStoragelocs');
        $items.empty();
        var cb = function(req) {
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var enabled = $( ".ui-selected", this).length > 0;
                    Gicon.setEnabled("btnEditStorage", enabled );
                    Gicon.setEnabled("btnDiscardStorageloc", enabled );
                }
            });
            Gicon.setEnabled("btnRefreshStorage", true );
        };
        Geof.model.read(null,_this, cb);
    },

    addNew: function () {
        Geof.cntrl.storage.showEditDialog();
    },

    editSelected: function () {
        var _this = Geof.cntrl.storage;
        var list = $("#olStoragelocs .ui-selected");
        if (list.length != 1) {
            Geof.log("fix the editSelected error where the selected count <> 1");
        }
        var $item = $(list[0]);
        Geof.model.readSelected($item, _this, Geof.cntrl.storage.showEditDialog);
    },

    showEditDialog:function(req) {
        var isNew = ! (req || false);
        var data = isNew ? {}: req.data ;
        var _this = Geof.cntrl.storage;
        Geof.cntrl.showEditDialog(_this, data, isNew);
        $("#strg_systemdir").prop('disabled', !isNew);
        Gicon.click("edit_strg_save",_this.save);
        Gicon.click("validateSystemDir",_this.validateSystemDir);
        $(" .ui-icon").tooltip();
    },

    save: function () {
        var _this = Geof.cntrl.storage;
        Gicon.setActive('edit_strg_save',true);
        var fields = Geof.cntrl.getDialogData(_this);
        var update = (fields.id || false) ? fields.id >= 0 : false;

        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('edit_strg_save',true);
        };
        var data = {};
        data.fields = fields;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    delete: function (){
        var _this = Geof.cntrl.storage;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected storage locations?");
    },

    validateSystemDir:function () {
        var cb = function(req) {
            var data = req.data[0];
            var error = '';
            if ('error' in data) {
                error = data.error;
            }
            var $systemdir = $("#strg_systemdir");
            if (error.length > 0) {
                $systemdir.switchClass("font12Green","font12Red");
                $systemdir.switchClass("font12","font12Red");
                PanelMgr.showErrorDialog("StorageLoc","Validate System Directory",  error );
            } else {
                $systemdir.switchClass("font12Red","font12Green");
                $systemdir.switchClass("font12","font12Green");
            }
            Gicon.setEnabled('validateSystemDir',true);
        };
        Gicon.setActive('validateSystemDir',true);
        var senddata = {
            "validate":{
                "directory":true,
                "name":false
            },
            "fields":{
                "systemdir": $("#strg_systemdir").val()
            }
        };
        var obj = {"entity":"storageloc","action":"read","actionas":"validate","data":senddata};
        Transaction.post(GRequest.fromJson(obj), cb);
    },

//    storageName: function(storageid, cb) {
//        var _this = Geof.cntrl.storage;
//        if (cb || false) {
//            var rtn = function (sloc) {
//                cb((sloc || false) ? sloc.name : "Unknown");
//            };
//            _this.getStoragelocs(rtn, storageid);
//        } else {
//            var sloc = _this.getStoragelocs[storageid];
//            return (sloc || false) ? sloc.name : "Unknown";
//        }
//    },

    storageDir: function(storageid, cb) {
        var dir = null;
        if (cb || false) {
            var cb = function (sloc) {
                if (sloc !== undefined) {
                    dir = sloc.systemdir.substring(sloc.systemdir.lastIndexOf("/") + 1);
                }
                cb(dir);
            };
            Geof.cntrl.storage.getStoragelocs(cb, storageid);
        }else {
            var sloc = Geof.cntrl.storage.storagelocs[storageid];
            if (sloc !== undefined) {
                dir = sloc.systemdir.substring(sloc.systemdir.lastIndexOf("/") + 1);
            }
        }
        return dir;
    },

    getStoragelocs:function(cb, storageid) {
        var _this = Geof.cntrl.storage;

        if (storageid in _this.storagelocs) {
            cb(_this.storagelocs[storageid]);
        } else {

            var gReq = GRequest.fromJson({"entity":"storageloc","action":"read","data":{}});
            Transaction.post( gReq, function(req) {
                var slocs = {};
                JsUtil.iterate(req.data, function(storage){
                    slocs[storage.id] = storage;
                });
               _this.storagelocs = slocs;
                cb(slocs[storageid]);
            });
        }
    }

};

Geof.cntrl.usr = {
    id:'id',
    entity:'usr',
    prefix:'usr_',
    fields:['id','loginname','password','firstname','lastname','initials','email','notes','statusid','attempts','lastattempt'],
    defaults:['-1','','','','','','','',0,0,''],
    exclude:['lastattempt'],
    list_columns: "id,firstname,lastname,loginname",
    order_by:"lastname, firstname",
    title:'User',
    list_tmpl:'<li class="ui-widget-content" data-id="%id">%lastname, %firstname<label class="idRight">%id</label></li>',
    logins:[],
    link: [
        {
            name:'ugroup',
            type:'link',
            entity:'usr_ugroup',
            icon:'img/symbol/Groups-64.png',
            buttons:[{action:'save',callback:Geof.cntrl.saveSublink}]
        },
        {
            name:'authcode',
            type:'child',
            icon:'img/symbol/Certificate.png',
            buttons:[{action:'delete', callback:Geof.cntrl.deleteChild}]
        }
    ],
    editConfig: {
        dialogName:'user_edit', divName:'editUsr',
        autoOpen: true, minHeight: 340, minWidth:380,
        resizable: false, modal:true
    },

    initialize: function() {
        var _this = Geof.cntrl.usr;
        Gicon.click("btnRefreshUsr", _this.populateList);
        Gicon.click("btnNewUsr", _this.addNew);
        Gicon.click("btnEditUsr", _this.editSelected);
        Gicon.click("btnDiscardUsr", _this.delete);
        Gicon.click("btnSendRsa", _this.confirmSendRsa);
        Gicon.click("btnSendRsa", _this.confirmSendRsa);

        $(" .subDlgWrapper").tooltip();
        _this.populateList();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshUsr", true );
        Gicon.setEnabled("btnEditUsr", false );
        Gicon.setEnabled("btnDiscardUsr", false );
        Gicon.setEnabled("btnSendRsa", false );
        var _this = Geof.cntrl.usr;
        var $items = $('#olUsrs');
        $items.empty();
        var cb = function(req) {
            _this.logins = [];
            JsUtil.iterate(req.data, function(usr){
                _this.logins.push(usr.loginname);
            });
            Templater.createSOLTmpl (req.data, $items,  _this.list_tmpl);
            $items.selectable({
                stop: function() {
                    var selected = $('#olUsrs .ui-selected');
                    _this.selected_ids = Geof.model.selectedIds(selected, _this);
                    var enabled = _this.selected_ids.length > 0;
                    Gicon.setEnabled("btnEditUsr", enabled );
                    Gicon.setEnabled("btnDiscardUsr", enabled );
                    Gicon.setEnabled("btnSendRsa", enabled );
                    Geof.cntrl.parentSelected(_this.selected_ids);
                }
            });
            Gicon.setEnabled("btnRefreshUsr", true );
        };
        Geof.model.read(null,_this, cb);
    },

    addNew: function () {
        var _this = Geof.cntrl.usr;
        Geof.cntrl.showEditDialog(_this, {}, true);
        Gicon.click("edit_usr_save",_this.save);
    },

    editSelected: function () {
        var _this = Geof.cntrl.usr;
        var list = $("#olUsrs .ui-selected");
        var $item = $(list[0]);
        var cb = function(req) {
            Geof.cntrl.showEditDialog(_this, req.data, false, function($dlg) {
                Gicon.click("edit_usr_save", Geof.cntrl.usr.save);
                Gicon.click("edit_usr_updatepwd", Geof.cntrl.usr.change_pwd);
                $("#usr_password").blur(_this.checkPasswordUpdate);
                $("#usr_password2").blur(_this.checkPasswordUpdate);
            });
        };
        Geof.model.readSelected($item, _this, cb);
    },

    save: function () {
        var $usrSaveError = $('#usr_save_error');
        $usrSaveError.text('');

        var _this = Geof.cntrl.usr;
        var exclude = ['attempts','lastattempt'];

        var flds = Geof.cntrl.getDialogData( _this, exclude );
        var isNew = (flds.id || -1) < 0;
        var error = _this.validate(flds, isNew);

        if (error !== undefined && error.length > 0) {
            $usrSaveError.text( error);
            return;
        }

        Gicon.setActive('edit_usr_save',true);

        var cb = function (req) {
            Gicon.setEnabled('edit_usr_save',true);
            if (req.error !== undefined) {
                PanelMgr.showError("Save User Error", req.error);
            } else {
                _this.populateList();
            }
        };

        var data = {fields:flds};
        if (isNew) {
            Geof.model.create(data, _this, cb);
        } else {
            Geof.model.update(data, _this, cb);
        }
    },

    validate:function(fields, isNew) {
        var _this = Geof.cntrl.usr;
        if ( isNew && JsUtil.has(fields.loginname, _this.logins)) {
            return "Login is currently in use.";
        }

        var err_pwd = _this.checkPasswords();
        if (err_pwd == '' && isNew) {
            return "Passwords are empty";
        } else if (err_pwd != undefined) {
            return err_pwd;
        }
        if (! jcv_checkEmail(fields.email)) {
            return "Invalid email address.";
        }
    },

    delete: function (){
        var _this = Geof.cntrl.usr;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected users?");
    },

    confirmSendRsa:function() {
        PanelMgr.showConfirm(
            "Confirm Rsa Email",
            "Email Rsa key to selected users?",
            function(send) {
                if (send) {
                    Geof.cntrl.usr.sendRsa();
                }
            }
        );
    },

    sendRsa:function() {
        var _this = Geof.cntrl.usr;
        Gicon.setActive("btnSendRsa", true);
        var cb = function (req) {
            Gicon.setEnabled("btnSendRsa", true);
        };
        var ids = _this.selected_ids;
        var trans = new Transaction(Geof.session);
        var order = 0;
        for (var indx=0;indx<ids.length;indx++) {
            var data = {where:{usrid:ids[indx]}};
            var jReq = GRequest.build('rsaencryption','execute','rsaemail',data);
            jReq.order = order++;
            trans.addRequest(jReq, null);
        }
        trans.setLastCallback(cb);
        trans.send()
    },

    change_pwd: function (){
        $("#usr_error").text('');
        Gicon.setActive('edit_usr_updatepwd',true);
        var cb = function (req) {
            Gicon.setEnabled('edit_usr_updatepwd', true);
            if (req.error || false) {
                $("#usr_error").text(req.error);
            }
        };
        var data = {
            fields: {password: $("#usr_password").val()},
            where: {id: $("#usr_id").text()}
        };
        var obj = {"entity":"usr","action":"update","actionas":"password","data":data};
        Transaction.post( GRequest.fromJson(obj), cb);
    },

    checkPasswordUpdate:function() {
        Gicon.setEnabled('edit_usr_updatepwd',false);
        $('#usr_password_error').text('');
        var error = Geof.cntrl.usr.checkPasswords();
        if (error !== undefined) {
            $('#usr_password_error').text(error);
        } else if (error != '') {
            Gicon.setEnabled('edit_usr_updatepwd',true);
        }
    },

    checkPasswords:function() {

        var pwd1 = $("#usr_password").val();
        var pwd2 = $("#usr_password2").val();
        if (pwd1.length > 0 && pwd2.length > 0) {
             if (pwd1.length < 8) {
                return 'Password length less than 8 characters';
            } else if (pwd1 != pwd2) {
                return 'Passwords do not match';
            }
        } else {
            return '';
        }
    }

};

Geof.cntrl.ugroup = {
    id:'id',
    entity:'ugroup',
    prefix:'grp_',
    fields: ['id','name','description'],
    defaults: ['-1','',''],
    exclude:[],
    list_columns: "id,name,description",
    order_by:"name",
    title:'User Groups',
    link: [
        {
            name:'usr',
            type:'link',
            entity:'usr_ugroup',
            icon:'img/symbol/usr-64.png',
            buttons:[{action:'save',callback:Geof.cntrl.saveSublink}]
        },
        {
            name:'permission',
            type:'link',
            entity:'ugroup_entity',
            icon:'img/symbol/keys.png',
            buttons:[
                {action:'save',callback:function(info,name) {Geof.cntrl.ugroup.savePermission(info,name)}},
                {action:'delete',callback:function(info,name) {Geof.cntrl.ugroup.deletePermission(info,name)}},
                {action:'add',callback:function(info,name) {Geof.cntrl.ugroup.showEntityDialog()}}
            ],
            read:{
                "entity":"ugroup_entity",
                "data":{
                    "columns":"ugroupid,entityid,createable,readable,updateable,deleteable,executable",
                    "join":[{"entity":"entity","join":"parent","columns":"name"}]
                }
            },
            readCallback:function(data) {
                Geof.cntrl.permission.setLiCheckboxes(data);
            }
        }
    ],

    editConfig: {
        dialogName:'edit_group', divName:'editGroup',
        autoOpen: true, minHeight: 280, minWidth: 380,
        resizable: false, modal:true
    },
    icon:'usergroup',
    list_tmpl: '<li class="ui-widget-content" data-id="%id">%name<label class="idRight">%id</label></li>',

    initialize: function() {
        var _this = Geof.cntrl.ugroup;
        _this.populateList();
        Gicon.click("btnRefreshGrp", _this.populateList);
        Gicon.click("btnNewGrp", _this.addNew);
        Gicon.click("btnEditGrp", _this.editSelected);
        Gicon.click("btnDiscardGrp", _this.delete);
        $(" .subDlgWrapper").tooltip();
    },

    populateList:function() {
        Gicon.setActive("btnRefreshGrp", true );
        Gicon.setEnabled("btnEditGrp", false );
        Gicon.setEnabled("btnDiscardGrp", false );
        var _this = Geof.cntrl.ugroup;
        var $items = $('#olUgroups');
        $items.empty();
        var cb = function (req) {
            Templater.createSOLTmpl(req.data, $items, _this.list_tmpl);
            $items.selectable({
                stop: function () {
                    var selected = $('#olUgroups .ui-selected');
                    var selected_ids = Geof.model.selectedIds(selected, _this);
                    var enabled = selected_ids.length > 0;
                    Gicon.setEnabled("btnEditGrp", enabled);
                    Gicon.setEnabled("btnDiscardGrp", enabled);
                    Geof.cntrl.parentSelected(selected_ids);
                }
            });
            Gicon.setEnabled("btnRefreshGrp", true);
        };
        Geof.model.read(null,_this, cb);
    },

    addNew: function () {
        var _this = Geof.cntrl.ugroup;
        Geof.cntrl.showEditDialog(_this, {}, false);
        Gicon.click("edit_grp_save",_this.save);
    },

    editSelected: function () {
        var _this = Geof.cntrl.ugroup;
        var list = $("#olUgroups .ui-selected");
        var $item = $(list[0]);
        var cb = function(req) {
            Geof.cntrl.showEditDialog(_this, req.data, false);
            Gicon.click("edit_grp_save", Geof.cntrl.ugroup.save);
        };
        Geof.model.readSelected($item, _this, cb);
    },

    save: function () {
        var _this = Geof.cntrl.ugroup;
        Gicon.setActive('edit_group_save',true);
        var fields = Geof.cntrl.getDialogData(_this);
        var update = (fields.id || false) ? fields.id >= 0 : false;

        var cb = function () {
            _this.populateList();
            Gicon.setEnabled('edit_group_save',true);
        };
        var data = {};
        data.fields = fields;
        if (update) {
            Geof.model.update(data, _this, cb);
        } else {
            Geof.model.create(data, _this, cb);
        }
    },

    delete: function (){
        var _this = Geof.cntrl.ugroup;
        Geof.model.deleteListConfirm( _this, _this.populateList, "Delete selected user groups?");
    },

    showEntityDialog:function() {
        var _this = Geof.cntrl.ugroup;
        var complete_callback = function (dlgEntity) {
            _this.dlgEntity = dlgEntity;
            _this.dlgEntity.dialog('open');

            var $items = $('#olEntitys');
            $items.empty();
            var cb = function (req) {
                Templater.createSOLTmpl(req.data, $items, Geof.cntrl.entity.list_tmpl);
                $items.selectable({});
            };
            Geof.model.read(null, Geof.cntrl.entity, cb);
        };
        var close_callback = function () {
            var tmpl = Geof.cntrl.permission.list_tmpl;
            $('#olEntitys .ui-selected').each(function () {
                var $this = $(this);
                var id = $this.data('id');
                if ($('#sub_ol_permission li[data-id="' + id + '"]').length == 0) {
                    var li = tmpl.replace(new RegExp('%entityid', "g"), id);
                    li = li.replace('%name', $this.data('name'));
                    $('#sub_ol_permission').append(li);
                }
            });
        };
        var options = {
            control: Geof.cntrl.entity,
            config: {
                title: 'Set User Group Permissions',
                dialogName: 'entity_list', divName: 'entity_list',
                autoOpen: true, minHeight: 280, width: 342,
                resizable: false, modal: true
            },
            complete_callback: complete_callback,
            close_callback: close_callback
        };
        PanelMgr.loadDialog(options);
    },

    savePermission:function(info, name) {
        Gicon.setActive(name,true);
        var ugroupid = info.id;
        var trans = new Transaction(Geof.session);
        var order = 0;
        var jReq = GRequest.build('ugroup_entity','delete',null,{where:{'ugroupid':ugroupid}});
        jReq.order = order++;
        trans.addRequest(jReq, null);
        $('#sub_ol_permission li').each(function() {
            var fields = {'ugroupid':ugroupid,entityid:-1,createable:false,readable:false,updateable:false,deleteable:false,executable:false};
            var _this = $(this);
            fields.entityid = _this.data('id');
            var cbs = _this.find("input[type='checkbox']");
            cbs.each(function() {
                var val = this.id.split('_');
                fields[val[1]] = $(this).is(':checked');
            });
            jReq = GRequest.build('ugroup_entity','create',null,{'fields':fields});
            jReq.order = order++;
            trans.addRequest(jReq, null);
        });
        trans.setLastCallback(function() {
            Gicon.setEnabled(name,true);
            Geof.cntrl.parentSelected(info.id);
        });
        trans.send();
        $('#cbPermissionAll :checkbox').each(function() {
                $(this).prop('checked', false);
            }
        );
    },

    deletePermission:function(info,name) {
        Gicon.setActive(name,true);

        var trans = new Transaction(Geof.session);
        $('#sub_ol_permission .ui-selected').each(function() {
            var entityid = $(this).data('id');
            var jReq = GRequest.build('ugroup_entity','delete',null,{where:{'ugroupid':info.id,'entityid':entityid}});
            trans.addRequest(jReq, null);
        });
        trans.setLastCallback(function() {
            Gicon.setEnabled(name,true);
            Geof.cntrl.parentSelected(info.id);
        });
        trans.send();

    }
};

