var Geof = Geof || {};
Geof.cntrl = Geof.cntrl || {};
Geof.src = Geof.src || {};

Geof.menuctrl = {
    stage:undefined,
    container:undefined,
    divPanel:undefined,
    divMenu:undefined,
    divView:undefined,
    dftStrWidth:2,
    callbacks:[],
    history:[],
    history_loc:null,
    add_history:true,
    home_menu:{type:'menu',name:'main'},
    cur_menu: undefined,
    last_menu:null,
    initialized:false,
    active_quick:undefined,
    quicklist:[],

    quicktmpl : '<span class="pointer floatLeft quick_icon" id="quickicon_%menuname"></span>',

    ui: '<span class="ui-icon icon_geof_backward iconLeft" id="btnNavBack" title="Navigate Back"></span>'
    +'<span class="ui-icon icon_geof_forward iconLeft" id="btnNavForward" title="Navigate Forward"></span>'
    +'<span class="ui-icon icon_geof_home iconLeft" id="btnNavHome" title="Home"></span>'
    +'<span class="ui-icon icon_geof_important iconLeft" id="btnSetFavorite" title="Set Favorite"></span>'
    +'<span class="ui-icon icon_geof_go_favorite iconLeft" id="btnGoFavorite" title="Go to Favorite"></span>'
    +'<hr class="vertical_bar floatLeft"><div class="w220 floatLeft" id="div_quick_icon">'
    +'<span class="ui-icon icon_geof_cancel iconRight" id="btnRemoveQuickLink" title="Remove Quick Link"></span>'
    +'<span class="ui-icon icon_geof_new iconRight" id="btnAddQuickLink" title="Add Quick Link"></span>'
    +'</div><hr class="vertical_bar floatLeft">',

    displayUI: '<div id="divPanel" class="panel"></div>'
                + '<div id="divMenu" class="menu"></div>'
                + '<div id="divView" class="view"></div>',

    buttons : ["btnNavBack","btnNavForward","btnNavHome","btnGoFavorite","btnSetFavorite","btnRemoveQuickLink","btnAddQuickLink"],

    $quick_div:undefined,

    config: {
        use_grid:true,
        x_offset: 300,
        x_size: 200,
        y_offset: 200,
        y_size: 150
    },

    setControl:function(iconContainer, displayContainer) {
        var _this =  Geof.menuctrl;
        if ( iconContainer || false ) {
            $('#' + iconContainer).html( _this.ui );
            // Setup Navigation events
            Gicon.click("btnNavBack", function() {
                _this.gotoMenu(-1);
            });

            Gicon.click("btnNavForward", function() {
                _this.gotoMenu(1);
            });

            Gicon.click("btnNavHome", function() {
                _this.change({type:'menu',name:'main'});
            });

            Gicon.click("btnSetFavorite", function() {
                _this.setHomeMenu(_this.cur_menu);
                _this.setMenuIcon(_this.cur_menu);
            }, Gicon.DISABLED);

            Gicon.click("btnGoFavorite", function() {
                _this.change(_this.home_menu);
            });

            Gicon.click('btnAddQuickLink',_this.saveQuickIcon);
            Gicon.click('btnRemoveQuickLink',_this.removeQuickIcon);

            Geof.menuctrl.addCallback(_this.setMenuIcon);
            Geof.menuctrl.$quick_div = $("#div_quick_icon");

        } else {
            alert("Geof.menucntrl.setControl needs a iconContainer tag");
        }

        if (displayContainer || false ) {
            _this.container = $("#" + displayContainer);
            _this.container.html( _this.displayUI );
            _this.divPanel = $("#divPanel");
            _this.divMenu = $("#divMenu");
            _this.divView = $("#divView");
            $(window).resize(this.resize);
            this.resize();
        } else {
            alert("Geof.menucntrl.setControl needs a displayContainer tag");
        }

    },

    setStage: function(div) {
        var _this = Geof.menuctrl;
        if (! _this.initialized) {
            _this.initialized = true;
            Geof.menuctrl.getQuickList();

            JsUtil.iterate( Geof.menus, function(menu, key) {
                for (var indx=0; indx<menu.length;indx++) {
                    menu[indx].parent = key;
                }
            });

            _this.stage = new Kinetic.Stage({container: div});

            var ls_home_menu = GLocal.get('home_menu') ;
            if (ls_home_menu != null && ls_home_menu != 'null') {
                try {
                    _this.home_menu = $.parseJSON(ls_home_menu);
                } catch(e) { }
            }
            _this.change( _this.home_menu );
        }
    },

    resize:function() {
        var _this = Geof.menuctrl;
        var stage = _this.stage;
        if (stage !== undefined) {
            var parent = $('#divMenu');
            stage.setWidth(parent.width() -4);
            stage.setHeight(parent.height() -4);
        }
        var wH = window.innerHeight;
        var wW = window.innerWidth;
        _this.container.height(wH - 42);
        _this.container.width(wW - 10);
    },

    setMenuIcon: function (menu) {
        var _this = Geof.menuctrl;
        Gicon.setEnabled("btnNavBack", _this.hasHistory());
        Gicon.setEnabled("btnNavForward", _this.inHistory());
        var isFavorite = ((menu.type == _this.home_menu.type) && (menu.name == _this.home_menu.name));
        Gicon.toggleActive('btnSetFavorite', isFavorite);
    },

    enableButtons:function(enabled) {
        var btns = Geof.menuctrl.buttons;
        for (var indx=0; indx<btns.length; indx++ ) {
            Gicon.setEnabled(btns[indx],enabled);
        }
    },

    change:function(evt) {
        var _this = Geof.menuctrl;

        if (!(evt || false)) {
            evt = {type:'menu',name:'main'};
        }

        if (evt.type == 'menu') {
            _this.setMenu(evt.name);
        } else if (evt.type == 'panel') {
           _this.setPanel(evt.name,evt.path);
        } else if (evt.type == 'view') {
           _this.setView(evt);
        }  else if (evt.type == 'map') {
            _this.setDisplay(evt);
        } else {
            return;
        }
        if ( _this.add_history) {
            _this.addHistory(evt);
        }
        _this.cur_menu = evt;
        _this.hiliteActiveQuick();
        _this.fireCallbacks();
    },

    hasHistory:function() {
        return Geof.menuctrl.history.length > 1;
    },

    inHistory:function() {
        var mctrl = Geof.menuctrl;
        var hloc = mctrl.history_loc;
        return (hloc > -1 && hloc < (mctrl.history.length -1));
    },

    addHistory:function(evt) {
        var _this = Geof.menuctrl;
        _this.history.push(evt);
        _this.history_loc = _this.history.length -1;
    },

    setMenu: function(menu_name) {
        var _this = Geof.menuctrl;
        try {
            _this.stage.removeChildren();
            _this.stage.clear();
//            _this.showMenu();

            if (menu_name == 'main') {
                _this.history.length = 0;
            }

            var layer = new Kinetic.Layer();
            var connectorLayer = new Kinetic.Layer();
            _this.stage.add(connectorLayer);
            _this.stage.add(layer);

            JsUtil.iterate( Geof.menus[menu_name], function(value) {
                new Geof.UiIcon(value, _this.config, layer, undefined);
            });
            _this.last_menu = menu_name;
            _this.showMenu();

        } catch(e){
            _this.setMenu('main');
        }
    },

    hideAll:function() {
        var _this = Geof.menuctrl;
        _this.divMenu.hide();
        _this.divPanel.hide();
        _this.divView.hide();
    },

    gotoMenu:function(indx) {
        var _this = Geof.menuctrl;
        _this.add_history = false;
        var newIndex = -1;
        if (indx !== undefined) {
            newIndex = _this.history_loc + indx;
        }

        if (newIndex > -1 && newIndex <= _this.history.length) {
            _this.history_loc = newIndex;
            var evt = _this.history[_this.history_loc];
        } else if (Geof.menuctrl.cur_menu !== undefined){
            evt = {name:_this.cur_menu.parent,type:'menu'};
        } else {
            evt = {type:'menu',name:'main'};
        }
        this.change(evt);
        _this.add_history = true;
    },

    setCurrentMenu:function(menu) {
        Geof.menuctrl.cur_menu = menu;
    },

    getMenu:function(menuName) {
        var menu, submenu;
        var menus = Geof.menus;
        var rtn = undefined;
        Object.keys(menus).forEach(function(key) {
            menu = menus[key];
            for (var indx=0; (indx<menu.length) && (rtn === undefined); indx++) {
                submenu = menu[indx];
                if (submenu.name == menuName) {
                    rtn = submenu;
                    break;
                }
            }
        });
        return rtn;
    },

    setHomeMenu:function(menu) {
        GLocal.set('home_menu',JSON.stringify(menu));
        Geof.menuctrl.home_menu = menu;
    },

    setDisplay: function(menu) {
        var _this = Geof.menuctrl;
        var baseX = 300;
        var incX = 150;
        var baseY = 0;
        var incY = 100;
        try {
            _this.stage.removeChildren();
            _this.stage.clear();
            var menu_name = menu.name;
            _this.showMenu();

            var layer = new Kinetic.Layer();
            var connectorLayer = new Kinetic.Layer();
            _this.stage.add(connectorLayer);
            _this.stage.add(layer);

            menu = Geof.menus[menu_name];
            var icons = menu.icons;
            var icon;
            var iconMap = {};
            Object.keys(icons).forEach(function(key) {
                icon = icons[key];
                icon.isMap = true;
                icon.x = baseX + (icon.col * incX);
                icon.y = baseY + (icon.row * incY);
                iconMap[icon.name] = new Geof.UiIcon(icon, layer, connectorLayer);
            });
            connectorLayer.draw();
            layer.draw();
            if (menu.initialize || false) {
                menu.initialize(iconMap);
            }
        } catch(e){
            _this.setMenu('main');
        }
    },

    setPanel:function(entity_name, path) {
        PanelMgr.loadListPage(entity_name, path, 'divPanel', Geof.menuctrl.showPanel);
    },

    showMenu:function() {
        var _this = Geof.menuctrl;
        _this.divPanel.hide();
        _this.divView.hide();
        _this.divMenu.show();
        if (_this.last_menu == null){
            _this.last_menu = {type:'menu',name:'main'};
            _this.change(_this.last_menu);
        }
        _this.resize();
    },

    showPanel:function(entity_name) {
        var _this = Geof.menuctrl;
        _this.divMenu.hide();
        _this.divView.hide();
        _this.divPanel.show();
        _this.divPanel.tooltip();
        var ent = Geof.cntrl[entity_name];
        ent.initialize();

        var pnl = $("#" + ent.prefix + 'dlgWrapper');
        if (pnl.height() <= 200) {
            var checker;
            var func = function() {
                Geof.log("showPanel checking size");
                if (pnl.height() > 200) {
                    window.clearInterval(checker);
                    pnl.switchClass('hidden', 'shown');
                    _this.resize();
                }
            };
            checker = setInterval(func, 50);
        }
    },

    showView:function(entity_name) {
        var _this = Geof.menuctrl;
        _this.divView.tooltip();
        Geof.cntrl[entity_name].initialize();
        _this.divMenu.hide();
        _this.divPanel.hide();
        _this.divView.show();
        _this.resize();
    },

    setView:function(view) {
        var viewfile = view.viewfile || view.name;
        var _this = Geof.menuctrl;

        var src = Geof.src[viewfile];
        if ( src || false) {
//            Geof.log("gmenu.setView " + JSON.stringify(view));
            _this.divView.html(src.view);
            $("#btnclose" + viewfile + "view").on("click", function () {
                Geof.menuctrl.showMenu();
            });
            _this.showView(viewfile);
            if (view.name != viewfile) {
                Geof.cntrl[viewfile].setView(view);
            }
            _this.resize();
        } else {
            Geof.Retrieve.getView(viewfile, function(){_this.setView(view)});
        }
    },

    saveQuickIcon:function() {
        var _this = Geof.menuctrl;
        var cur_menu = _this.cur_menu;
        if (cur_menu !== undefined) {
            var qlist = _this.quicklist;
            for (var indx=0;indx<qlist.length; indx++) {
                if(qlist[indx].name === cur_menu.name) {
                    return;
                }
            }
            var icon = {name:cur_menu.name,img:cur_menu.imageSrc};
            qlist.push(icon);
            _this.quicklist = qlist;
            _this._addQuickIcon(icon,true);
            GLocal.setJson("quicklist",qlist);
            Gicon.setEnabled('btnRemoveQuickLink', true);
            Gicon.setEnabled('btnAddQuickLink', false);
        }

    },

    removeQuickIcon:function() {
        var _this = Geof.menuctrl;
        var cur_menu = _this.cur_menu;
        if (cur_menu !== undefined) {
            var qlist = _this.quicklist;
            var newlist = [];
            for (var indx=0;indx<qlist.length; indx++) {
                if(qlist[indx].name !== cur_menu.name) {
                    newlist.push(qlist[indx]);
                }else {
                    $("#quickicon_" + cur_menu.name).remove();
                }
            }
            _this.quicklist = newlist;
            GLocal.setJson("quicklist",newlist);
            Gicon.setEnabled('btnRemoveQuickLink', false);
            Gicon.setEnabled('btnAddQuickLink', true);
        }
    },

    getQuickList:function() {
        var _this = Geof.menuctrl;
        var qlist = GLocal.getJson("quicklist", []);
        for (var indx=0;indx<qlist.length; indx++) {
            _this._addQuickIcon(qlist[indx]);
        }
        _this.quicklist = qlist;
    },

    _addQuickIcon:function(icon, isActive) {
        var _this = Geof.menuctrl;
        if (_this.$quick_div !== undefined){
            var tmpl = _this.quicktmpl;
            _this.$quick_div.append(
                Templater.mergeTemplate({menuname:icon.name},tmpl)
            );
            var $icon =$("#quickicon_" + icon.name);
            var dir = isActive || false ? Geof.img_dir : Geof.img_gray_dir;
            $icon.css("background-image","url(" + dir + icon.img + ")");
            $icon.click(function() {
                _this.callQuickIcon($icon, icon.name);
            });

        }
    },

    callQuickIcon:function($icon, menuname) {
        var menu = Geof.menuctrl.getMenu(menuname);
        Geof.menuctrl.change(menu);
    },

    hiliteActiveQuick:function() {
        var _this = Geof.menuctrl;
        var cur_menu = _this.cur_menu;
        if (cur_menu !== undefined) {
            if (_this.active_quick !==  undefined) {
                var url = _this.active_quick.css('background-image');
                url = url.replace(new RegExp('/symbol/', "g"), "/symbolgray/");
                _this.active_quick.css('background-image',url);
                _this.active_quick = undefined;
            }
            var id = "quickicon_" + cur_menu.name;
            var found = false;
            var spans = $("#div_quick_icon span");
            for ( var indx=0; indx<spans.length && (! found); indx++ ) {
                var $this = $(spans[indx]);
                if ($this.attr('id') == id) {
                    _this.active_quick = $this;
                    var url2 = _this.active_quick.css('background-image');
                    url2 = url2.replace(new RegExp('/symbolgray/', "g"), "/symbol/");
                    _this.active_quick.css('background-image',url2);
                    Gicon.setEnabled('btnRemoveQuickLink', true);
                    found = true;
                }
            }

            if (found) {
                Gicon.setEnabled('btnRemoveQuickLink', true);
                Gicon.setEnabled('btnAddQuickLink', false);
            } else {
                Gicon.setEnabled('btnRemoveQuickLink', false);
                Gicon.setEnabled('btnAddQuickLink', true);
            }
        }
    },

    addCallback:function(callback) {
        this.callbacks.push(callback);
    },

    removeCallback:function(callback) {
        JsUtil.splice(this.callbacks, callback);
    },

    fireCallbacks:function() {
        var callbacks = this.callbacks;
        var menu = this.cur_menu;
        Object.keys(callbacks).forEach(function(key) {
            callbacks[key](menu);
        });
    }
};

