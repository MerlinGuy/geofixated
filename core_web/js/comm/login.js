/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 8/13/13
 * Time: 8:28 AM
 */
var Geof = Geof || {};
Geof.can_clear_text_login = true;
Geof.logged_in = false;

Geof.login = {

    option_tmpl:'<li class="ui-widget-content"><label class="flw100">%login</label></li>',
    dialog:null,
    button:null,
    cfg: {
        file:'login_dialog',
        directory:'core/panel/',
        divName:'login_dialog',
        dragbar:'buttonBarLogin'
    },
    ui: '<span class="ui-icon icon_geof_session_enable iconRight" id="btnLoginIcon" title="Login"></span>'
        +'<span class="ui-icon icon_geof_notsecure_enable iconRight" id="btnEncryptRequests" title="Encrypt All Requests"></span>',

    setControl : function( opts ) {
        var _this = Geof.login;

        if (opts === undefined) {
            alert("Geof.login.setControl is mission opts parameter");
            return;
        } else if ( opts.container === undefined ) {
            alert("Geof.login needs a container tag");
            return;
        }

        $('#' + opts.container).html(_this.ui);
        opts['btnEncrypt'] = 'btnEncryptRequests';
        Geof.session = new GSession(opts);

        _this.button = $("#btnLoginIcon" );

        Geof.session.addLoginCallback(_this.toggleLogin);

        var cbLogin = function() {
            if (Geof.session.sessionId || false) {
                Geof.session.logout();
            }
            _this.show();
        };
        Gicon.click('btnLoginIcon', cbLogin, true);

        var cfg = _this.cfg;

        cfg.complete_callback = function() {

            _this.dialog = $('#' + cfg.divName);
            $('#btnLogin').click(function() {
                Geof.session.login($('#loginname').val(),$('#password').val());
            });

            Gcontrol.checkbox('cbAutoLogin', null, "auto_login");

            $('#btnCancelLogin').click( _this.hide );

            $('#login_dialog').keydown(function(event){
                if(event.keyCode==13){
                    $('#btnLogin').trigger('click');
                }
            });

            Gicon.click("btnDiscardLogin", function() {
                var l = $('#glogins .ui-selected');
                if ((l || false) && l.length > 0) {
                    Geof.login.removeLogin($(l[0]).text(), location.href);
                    _this.setLogin('','',false);
                }
            });

            Gicon.click('closeLoginDialog',_this.hide);
            if (opts.login_click || false ){
                Gicon.click("btnLoginIcon", opts.login_click, true);
            }

            _this.initializeSession();
        };
        PanelMgr.loadDialogY( cfg );

    },

    toggleLogin:function(isLoggedIn) {
        var _this = Geof.login;
        Gicon.toggleActive('btnLoginIcon', isLoggedIn);
        if (isLoggedIn) {
            _this.button.title = "Logout" ;
            _this.hide();
        } else {
            _this.button.title =  "Login";
            _this.show();
        }
    },

    show:function(error) {
        $("#lblLoginError").text((error || false) ? error : '');
        Geof.login.setLogin(GLocal.get("login",""),GLocal.get("pwd", ""), GLocal.getBoolean("auto_login"));

        $('#login_dialog').keydown(function(event){
            if(event.keyCode==13){
                $('#btnLogin').trigger('click');
            }
        });

        var dialog = Geof.login.dialog;
        dialog.show();
        Geof.center_in_body(dialog);
        Geof.login.loadLogins();

    },

    hide:function() {
        Geof.login.dialog.hide();
    },

    initializeSession: function() {
        var state = Geof.session.canAutoLogin();
        if (state.can_login) {
            Geof.session.tryAutologin();
        } else if ((! Geof.can_clear_text_login) && (!state.has_cipher) ) {
            Geof.cntrl.profile.edit();
        } else {
            Geof.login.show();
        }
    },

    setLogin:function(login,pwd,isAuto) {
        $("#cbAutoLogin").prop('checked', isAuto);
        $("#loginname").val( login );
        $("#password").val(pwd );

    },

    getLogin: function(Login,Href) {
        if (Href === undefined) {
            Href = location.href;
        }

        var filter = {login:Login,href:Href};
        var match = JsUtil.filter(GLocal.getJson('geof_login',[]), filter);
        return match.length == 0 ? undefined : match[0];
    },

    removeLogin: function(Login, Href) {
        var results = JsUtil.filter(
            GLocal.getJson('geof_login'),
            {login:Login,href:Href},
            true
        );
        GLocal.setJson('geof_login', results);
        Geof.login.loadLogins();
    },

    loadLogins:function() {
        Gicon.setEnabled('btnDiscardLogin',false);

        var $ol = $("#glogins");
        $ol.empty();
        var gl = GLocal.getJson('geof_login');
        gl = JsUtil.filter(gl,{login:null},true);
        GLocal.setJson('geof_login', gl||{});

        gl = JsUtil.filter(gl,{href:location.href});
        Templater.createSOLTmpl (gl, $ol ,Geof.login.option_tmpl);

        $ol.selectable({
            stop: function() {
                var l = $('#glogins .ui-selected');
                Gicon.setEnabled('btnDiscardLogin',false);
                if ((l || false) && l.length > 0) {
                    l = Geof.login.getLogin( $(l[0]).text() );
                    Geof.login.setLogin(l.login,l.password, l.is_auto);
                    Gicon.setEnabled('btnDiscardLogin',true);
                }
            }
        });
    },

    addLogin: function(Login,Password,isDefault,isAuto) {
        if (Login == null || Login.length == 0) {
            return;
        }
        var gl = JsUtil.filter(
            GLocal.getJson('geof_login',[]),
            {login:Login,password:Password},
            true
        );
        isDefault = isDefault || isAuto;
        gl.push({login:Login,password:Password,is_default:isDefault,is_auto:isAuto,href:location.href});
        GLocal.setJson('geof_login', gl);
    }


};