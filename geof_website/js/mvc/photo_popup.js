
var Geof = Geof || {};
Geof.photo_popup = null;

function PhotoPopup(parent, default_size) {
    this.parent = parent;
    this.file = null;
    this.modal = true;
    this.rotateCallback = undefined;

    this.resetParameters();

    if (default_size || false) {
        GLocal.set('default_photo_size', default_size);
    } else {
        default_size = GLocal.get('default_photo_size') || 3;
        if (default_size.length == 0) {
            default_size = 3;
        }
    }
    this.photo_size = default_size
}

PhotoPopup.prototype.resetParameters = function() {
    this._ROTATE = 0;
    this.ROTATE_CLASS = 'rotate0';
    this._IMG_WIDTH = 0;
    this._IMG_HEIGHT = 0;
    this._IMG_TOP = 0;
    this._IMG_LEFT = 0;
    this._VIEW_ID = 0;
};

PhotoPopup.prototype.rotateImage = function (angle) {
    var $img = $('#photoDialogImg');
    if (this._IMG_WIDTH === 0) {
        this._IMG_WIDTH = $img.width();
        this._IMG_HEIGHT = $img.height();
        this._IMG_TOP = (this._IMG_WIDTH - this._IMG_HEIGHT) / 2;
        this._IMG_LEFT = (this._IMG_HEIGHT - this._IMG_WIDTH) / 2;

    }
    this._ROTATE = this._ROTATE + angle;
    if (Math.abs(this._ROTATE) >= 360 ){
        this._ROTATE = 0;
    } else if (this._ROTATE < 0) {
        this._ROTATE = 360 + this._ROTATE;
    }
    this._VIEW_ID = parseInt(this._ROTATE / 90) % 4;

    var width=this._IMG_WIDTH, height=this._IMG_HEIGHT;
    var top=0, left=0;
    if (this._VIEW_ID % 2 == 1) {
        width = this._IMG_HEIGHT;
        height = this._IMG_WIDTH;
        top = this._IMG_TOP;
        left= this._IMG_LEFT;
    }
    var $holder = $('#imgHolder');
    $holder.height(height);
    $holder.width(width);
    $img.css({top:top+'px',left:left+'px'});

    var newClass = "rotate" + this._ROTATE;
    $img.switchClass(this.ROTATE_CLASS, newClass);
    this.ROTATE_CLASS = newClass;
    if (this.rotateCallback || false) {
        this.rotateCallback(this._VIEW_ID, width, height);
    }
};

PhotoPopup.prototype.closeDialog = function () {

    if (document.getElementById('photoDialog')) {
        var $pDlg = $('#photoDialog');
        try {
            $pDlg.dialog("close");
        } catch (e){}

        $pDlg.remove();
        JsUtil.removeChild('photoDialog');
        $('#btnPhotoRotateRight').unbind('click');
        $('#btnPhotoRotateLeft').unbind('click');
        Geof.cntrl.annotation.delay_initialization=false;
        Geof.cntrl.annotation.got_parent_resize=false;
    }
};

PhotoPopup.prototype.showDialog = function (options) {

    var _this = this;
    _this.closeDialog();
    this.modal = options.modal || true;
    this.file = options.file || null;
    this.photo_size = options.size || null;
    if (this.photo_size == null) {
        this.photo_size = GLocal.get('default_photo_size') || 3;
    }

    this.resetParameters();

    var fileid = _this.file.id;

    var cb = function(html) {

        $('#' + _this.parent).append(html);
        var $dlg = $('#photoDialog');

        var $iv_photosize = $("#iv_photosize");
        $iv_photosize.val(_this.photo_size);
        var size = "?size=" + _this.photo_size;

        var $img = $('#photoDialogImg');
        var url =  Geof.session.url + size + '&id=' + fileid + '&sessionid=' + Geof.session.sessionId;
        $img.attr('src', url);
        $img.load(function() {
            Geof.cntrl.annotation.resize(true);
        });

        var pos = GLocal.get('default_photo_position') || "20,20";
        pos = pos.split(",");
        var top = parseInt(pos[0]);
        var left = parseInt(pos[1]);

        $dlg.dialog({ autoOpen: false, position:[left, top],
            close: function() {
                _this.closeDialog();
            },

            dragStop:function(event, ui) {
                var top = ui.position.top;
                var left = ui.position.left;
                GLocal.set('default_photo_position', top + "," + left);
            },
            modal:false, resizable:true, draggable:true, width:'auto', height:'auto', title:_this.file.originalname
        });

        var aopts = {
            divElement : $("#annotationDiv"),
            ol : 'olAnnotations',
            underlay : 'photoDialogImg',
            file : _this.file,
            type : Filetypes.PHOTO,
            isvisible:false,
            parent:_this,
            selected_ids:[]
        };

        JsUtil.merge(options.annotation, aopts, true);

        Geof.cntrl.annotation.setOptions(aopts);
        _this.rotateCallback = Geof.cntrl.annotation.canvasResized;

        Gicon.click('btnPhotoRotateRight',function() {
            _this.rotateImage(90);
        });

        Gicon.click('btnPhotoRotateLeft',function() {
            _this.rotateImage(-90);
        });

        $iv_photosize.on('change', function() {
            var photo_size = $(this).val();
            GLocal.set('default_photo_size', photo_size);
            $dlg.dialog('close');
            Geof.photo_popup.showDialog({
                file:_this.file,
                modal:_this.modal,
                size:photo_size
            });
        });

        $(".buttonBar").tooltip();
        $dlg.dialog( "open" );
    };
    Geof.Retrieve.getUrl("view/photo_dialog.html", cb);
};