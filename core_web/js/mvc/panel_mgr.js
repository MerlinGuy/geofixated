/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 1/31/13
 * Time: 8:10 AM
 */

var PanelMgr = (function () {

    return {

        $body:undefined,
        htmlLoc: undefined,
        cntlLoc: "html/cntl/",
        main_overlay: null,
        main_overlay_page: 'mainOverlay',
        dlg_wrapper: null,
        dlg_wrapper_page: 'dialogWrapper',
        cached_pages: [],
        confirm_html: '<div id="dialog-confirm" title="__title__"><p>__msg__</p></div>',
        progress_html: '<div id="dialog-progress" title="__title__"><div id="progress-bar" class="pbar"></div></div>',

        error_html: '<div id="dialog-error" title="An Error Occurred">'
            + '<div class="display-line"><label class="stdlabelLrg">Entity:</label><label class="readOnly">%entity</label></div>'
            + '<div class="display-line"><label class="stdlabelLrg">Action:</label><label class="readOnly">%action</label></div>'
            + '<div class="display-line"><label class="stdlabelLrg">Msg:</label><label class="readOnly">%msg</label></div></div>',

        open_error_html: '<div id="dialog-error" title="%title">'
            + '<div class="display-line"><label class="stdlabelLrg">Error:</label><label class="readOnly">%msg</label></div></div>',

        initialize: function (opts) {
            if (PanelMgr.$body === undefined) {
                PanelMgr.$body = $(document.body);
            }

            if (PanelMgr.viewDiv === undefined && ( opts !== undefined )) {
                PanelMgr.viewDiv = $(opts.view);
                PanelMgr.htmlLoc = Geof.href() + 'core/panel/';
                PanelMgr.jsLoc = Geof.href() + 'panel/';
            }

            if (PanelMgr.main_overlay == null) {
                var cb = function (html) {
                    PanelMgr.main_overlay = $(html);
                    PanelMgr.main_overlay.appendTo(document.body);
                    PanelMgr.initialize(opts);
                };
                PanelMgr.getUrl(PanelMgr.main_overlay_page, cb);
            }
            else if (PanelMgr.dlg_wrapper == null) {
                var cb2 = function (html) {
                    PanelMgr.dlg_wrapper = html;
                    PanelMgr.initialize(opts);
                };
                PanelMgr.getUrl(PanelMgr.dlg_wrapper_page, cb2);
            }
        },

        loadDialog: function (options) {
            var _this = this;

            var control = options.control;
            var complete_callback = options.complete_callback;
            var close_callback = options.close_callback;
            var config = options.config || false ? options.config : control.editConfig;
            var dialogName = config.dialogName;

            var cb = function ($dialog) {
                PanelMgr.$body.append($dialog);
                var $dlg = $("#" + config.divName);
                $dlg.dialog(config);
                var closeF = function () {
                    if (close_callback) {
                        close_callback();
                    }
                    $(this).remove();
                    $dlg.remove();
                };
                $dlg.dialog({close: closeF});
                complete_callback($dlg);
            };

            if (dialogName in _this.cached_pages) {
                cb(_this.cached_pages[dialogName]);

            } else {
                this.getUrl(dialogName, function (html) {
                    var $dialog = $(html);
                    _this.cached_pages[dialogName] = $dialog;
                    cb($dialog);
                });
            }
        },

        loadDialogX: function (cfg) {
            var _this = this;

            var cb = function (html) {

                PanelMgr.$body.append($(html));
                var $dlg = $("#" + cfg.divName);
                $dlg.dialog(cfg);

                var closeF = function () {
                    if (cfg.close_callback) {
                        cfg.close_callback();
                    }
                    $dlg.remove();
                };

                $dlg.dialog({close: closeF});
                if (cfg.complete_callback) {
                    cfg.complete_callback($dlg);
                }
            };

            var dir = _this.htmlLoc;
            if ('directory' in cfg) {
                dir = cfg.directory;
            }
            Geof.Retrieve.getUrl(dir + cfg.file + '.html', cb);
        },

        loadDialogY: function (cfg) {
            var _this = this;

            var cb = function (html) {

                PanelMgr.$body.append($(html));
                if ('dragbar' in cfg) {
                    var dragbar = "#" + cfg.dragbar;

                    $('#' + cfg.divName).draggable({handle: dragbar });
                    $(dragbar).mousedown(function () {
                        $(this).css('cursor', 'move');
                    });
                    $(dragbar).mouseup(function () {
                        $(this).css('cursor', 'default');
                    });
                }
                if (cfg.complete_callback || false) {
                    cfg.complete_callback();
                }
            };

            var dir = _this.htmlLoc;
            if ('directory' in cfg) {
                dir = cfg.directory;
            }
            Geof.Retrieve.getUrl(dir + cfg.file + '.html', cb);
        },

        loadListPage: function (entity_name, path, overlay, callback) {
            if (Geof.src[entity_name] || false) {
                $('#' + overlay).empty();

                var ent = Geof.cntrl[entity_name];
                var html = Geof.src[entity_name]['list'];

                var wrapper = PanelMgr.dlg_wrapper.replace(/_\$\$_/g, ent.prefix);
                wrapper = wrapper.replace(/%entity_name/g, ent.title);
                wrapper = wrapper.replace(/%content/g, html);
                $('#' + overlay).append(wrapper);

                $("#" + ent.prefix + 'closeDialog').on("click", function () {
                    Geof.menuctrl.gotoMenu();
                });
                var $subPanel = $('#' + ent.prefix + 'sub_icon_holder');
                var links = ent.link || [];

                var html2 = '<div>';
                for (var indx=0;indx<links.length;indx++) {
                    var elink = links[indx];
                    var ename = elink.name;
                    if (!ename in Geof.cntrl) {
                        continue;
                    }
                    var sublink = entity_name + ',' + ename;
                    html2 += '<div class="sub_link disabled" id="sublink_div_' + ename + '"><span class="pointer">'
                        + '<img src="' + elink.icon + '" class="sub_link" data-sublink="' + sublink + '"/>'
                        + '</span></div>';
                }
                html2 += '</div>';
                $subPanel.append(html2);

                $("span .sub_link").click(PanelMgr.loadSubList);
                if (callback || false) {
                    callback(entity_name);
                }
            } else {
                var cbLoadListPage = function(){
                    PanelMgr.loadListPage(entity_name, path, overlay, callback);
                };
                Geof.Retrieve.getEntity(entity_name, cbLoadListPage, ["list","edit"], path);
            }
        },

        loadSubList: function () {

            var sublink = $(this).data('sublink').split(",");

            var info = {};
            info.parent = sublink[0];
            info.child = sublink[1];
            var child = info.child;

            if (Geof.cntrl.lastSubBtn !== undefined) {
                Geof.cntrl.lastSubBtn.switchClass('active', 'enabled');
            }
            Geof.cntrl.lastSubBtn = $("#sublink_div_" + child);

            var parent = info.parent;
            var pEntity = Geof.cntrl[parent];
            var cEntity = Geof.cntrl[child];

            var pSelected = $("#ol" + JsUtil.capitalize(pEntity.entity) + "s .ui-selected");
            pSelected = Geof.model.selectedIds(pSelected, pEntity);

            info.cEntity = cEntity;
            info.pEntity = pEntity;
            info.olName = 'sub_ol_' + child;

            info.parentid = parent + pEntity.id;
            info.childid = child + cEntity.id;
            info.linkEntity = parent + '_' + child;
            info.id = pSelected || false ? pSelected[0] : -1;

            $('#' + info.olName).empty();

            if (info.id === -1) {
                return;
            }

            info.data = {};
            info.where = {};
            var parentid = info.parentid;
            info.where[parentid] = info.id;

            var deselectName = 'btnSub_' + child + 'DeselectAll';
            var selectName = 'btnSub_' + child + 'SelectAll';
            var deleteName = 'btnSub_' + child + 'Delete';
            Geof.cntrl.lastSubBtn.switchClass('enabled', 'active');

            var $subpanel = $("#" + pEntity.prefix + "sub_panel_holder");
            $subpanel.empty();

            var selectCallback = Geof.cntrl.setChildSelectCB(info.olName, cEntity, deselectName, deleteName);

            // Determine the read and the read callback
            Geof.cntrl.parentSelectCB = null;

            var link;
            var linktype = '';
            var buttons = {};
            var link2 = Geof.cntrl.getLink(parent, child);

            if ('entity' in link2) {
                info.linkEntity = link2.entity;
            }

            if (link2 !== undefined) {
                linktype = link2.type;
                buttons = link2.buttons || [];
                info.read = link2.read;
                info.readCallback = link2.readCallback;

                if (linktype == 'child') {
                    info.data['where'] = info.where;

                } else if (linktype == 'link') {
                    if (info.readCallback === undefined) {
                        info.linkRead = function (id) {

                            var ldata = {columns: child + cEntity.id, where: {}};
                            ldata.where[info.parentid] = id;
                            var jReq = GRequest.build(info.linkEntity, 'read', null, ldata);

                            Transaction.post(jReq, function (req) {
                                Geof.cntrl.selectLI(info.olName, req.data, child + 'id', null);
                                selectCallback();
                            });
                        }
                    }
                }
            }

            var populateCB = function (req, id) {
                var $items = $('#' + info.olName);
                if (cEntity.olclass !== undefined) {
                    $items.addClass(cEntity.olclass);
                } else {
                    $items.addClass('olSub_pnl_listBlock');
                }
                $items.empty();
                Gicon.setEnabled(selectName, req.data.length > 0);
                Templater.createSOLTmpl(req.data, $items, cEntity.list_tmpl);
                $items.selectable({
                    stop: selectCallback
                });
                selectCallback();

                if (info.linkRead != null) {
                    info.linkRead(id);
                } else if (info.readCallback || false) {
                    info.readCallback(req.data);
                }
            };

            // callback query to fill the sub list
            Geof.cntrl.parentSelectCB = function (id) {
                if (id === undefined || id == -1) {
                    return;
                }
                var req2;
                if (info.read || false) {
                    var ldata = {};
                    ldata.columns = info.read.data.columns;
                    ldata.join = info.read.data.join;
                    ldata.where = {};
                    ldata.where[info.parentid] = id || -1;
                    req2 = GRequest.build(info.read.entity, "read", null, ldata);

                } else {
                    info.where[parentid] = id || -1;
                    req2 = GRequest.build(child, "read", null, info.data);
                }
                Transaction.post(req2, function (req) {
                    populateCB(req, id);
                });
            };

            // Get the sublist html and append it to the sub panel
            var cb = function (html) {

                html = html.replace(new RegExp('%entity', "g"), cEntity.entity);
                html = html.replace(new RegExp('%title', "g"), cEntity.title);
                $subpanel.append(html);

                $("#sub_buttonBar_" + child + " .iconLeft").each(function () {
                    $(this).hide();
                });

                var func = function (btn, name) {
                    return function(){btn.callback(info, name)};
                };

                for (var indx=0; indx < buttons.length;indx++) {
                    var btn = buttons[indx];
                    var name = 'btnSub_' + child + JsUtil.capitalize(btn.action);
                    $('#' + name).show();
                    Gicon.click(name, func(btn, name));
                }

                if (info.id || false) {
                    if (cEntity.selectAllSub || false) {
                        Gicon.click(selectName, cEntity.selectAllSub);
                    }
                    else {
                        Gicon.click(selectName, function () {
                            Geof.cntrl.selectAll(info.olName, selectCallback)
                        });
                    }
                    if (cEntity.deselectAllSub || false) {
                        Gicon.click(deselectName, cEntity.deselectAllSub, true);
                    }
                    else {
                        Gicon.click(deselectName, function () {
                            Geof.cntrl.deselectAll(info.olName, selectCallback)
                        });
                    }
                    Geof.cntrl.parentSelectCB(info.id || -1);
                }
            };
            if (cEntity.list_block || false ) {
                Geof.Retrieve.getUrl(PanelMgr.htmlLoc + cEntity.list_block, cb);
            }
            else {
                Geof.Retrieve.getUrl(PanelMgr.htmlLoc + 'sub_list_block.html', cb);
            }

        },

        getUrl: function (pageName, complete_callback) {
            Geof.Retrieve.getUrl(this.htmlLoc + pageName + '.html', complete_callback);
        },

        showDeleteConfirm: function (title, message, callback) {

            var div = this.confirm_html.replace(new RegExp('__title__', "g"), title);
            div = $(div.replace(new RegExp('__msg__', "g"), message));

            PanelMgr.$body.append(div);

            var delCB = function () {
                $(this).dialog("close");
                $("#dialog-confirm").remove();
                callback(true);
            };

            var cancelCB = function () {
                $(this).dialog("close");
                $("#dialog-confirm").remove();
                callback(false);
            };

            $("#dialog-confirm").dialog({
                resizable: false,
                height: 220,
                width: 440,
                modal: true,
                buttons: {
                    "Delete": delCB,
                    Cancel: cancelCB
                }
            }).css("font-size", "14px");
        },

        showConfirm: function (title, message, callback) {

            var div = this.confirm_html.replace(new RegExp('__title__', "g"), title);
            div = $(div.replace(new RegExp('__msg__', "g"), message));

            PanelMgr.$body.append(div);

            var delCB = function () {
                $(this).dialog("close");
                $("#dialog-confirm").remove();
                callback(true);
            };

            var cancelCB = function () {
                $(this).dialog("close");
                $("#dialog-confirm").remove();
                callback(false);
            };

            $("#dialog-confirm").dialog({
                resizable: false,
                height: 220,
                width: 440,
                modal: true,
                buttons: {
                    Okay: delCB,
                    Cancel: cancelCB
                }
            }).css("font-size", "14px");
        },

        showMessage: function (title, message) {

            var div = this.confirm_html.replace(new RegExp('__title__', "g"), title);
            div = $(div.replace(new RegExp('__msg__', "g"), message));

            PanelMgr.$body.append(div);

            var delCB = function () {
                $(this).dialog("close");
                $("#dialog-confirm").remove();
            };

            $("#dialog-confirm").dialog({
                resizable: false,
                height: 220,
                width: 440,
                modal: true,
                buttons: {
                    Okay: delCB
                }
            }).css("font-size", "14px");
        },

        showProgressBar: function (options) {

            var title = (options.title || "");
            var div = this.progress_html.replace(new RegExp('__title__', "g"), title);

            PanelMgr.$body.append(div);
            var $dlg = $("#dialog-progress");
            var $bar = $("#progress-bar");

            var cancelFunction = function () {
                $dlg.remove();
                if (options.callback || false) {
                    options.callback(false);
                }
            };

            var btnCode = {};

            if (options['showCancel'] || false) {
                btnCode.Cancel = cancelFunction;
            }

            $dlg.dialog({
                resizable: false,
                height: 100,
                width: 440,
                modal: true,
                buttons: btnCode
            }).css("font-size", "14px");

            if (options.indeterminate || false) {
                $bar.progressbar({value:false});

            } else {
                $bar.progressbar({
                    value: options.value || 0,
                    max: options.max || 100
                });
            }

            return {
                cancel: cancelFunction,
                setValue: function (newValue) {
                    $bar.progressbar("option", "value", newValue);
                }
            };
        },

        showErrorDialog: function (entity, action, error) {
            var div = this.error_html.replace(new RegExp('%entity', "g"), entity);
            div = div.replace(new RegExp('%action', "g"), action);
            div = div.replace(new RegExp('%msg', "g"), error);

            PanelMgr.$body.append(div);
            var cancelCB = function () {
                $(this).dialog("close");
                $("#dialog-error").remove();
            };

            $("#dialog-error").dialog({
                resizable: false, modal: true,
                height: 280, width: 480,
                buttons: {Cancel: cancelCB}
            }).css("font-size", "14px");
        },

        showError: function (title, msg) {
            var div = this.open_error_html.replace(new RegExp('%title', "g"), title);
            div = div.replace(new RegExp('%msg', "g"), msg);

            PanelMgr.$body.append(div);
            var cancelCB = function () {
                $(this).dialog("close");
                $("#dialog-error").remove();
            };

            $("#dialog-error").dialog({
                resizable: false, modal: true,
                height: 280, width: 480,
                buttons: {Cancel: cancelCB}
            }).css("font-size", "14px");
        }
    };
})();