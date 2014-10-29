/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 12/5/13
 * Time: 3:04 PM
 */

function UploadHandler(cntrl, file, pbar, cbTime, cbComplete, cbError) {
    this.cntrl = cntrl;
    this.file = file;
    this.upload_file = new Uploadfile( file, 0 );
    this.pbar = undefined;
    this.$pbar = undefined;
    if (pbar || false) {
        this.pbar = "#" + pbar;
        this.$pbar = $(this.pbar);
    }
    this.cbComplete = cbComplete;
    this.cbTime = cbTime;
    this.cbError = cbError;
    this.upload_start = undefined;
}

UploadHandler.prototype.start = function () {
    var _this = this;
    this.upload_file.endCallback = function(index, status, pending, sent, error) {
        _this.uploadCallback(_this, index, status, pending, sent, error);
    }
    this.upload_file.upload();
};

UploadHandler.prototype.uploadCallback = function (_this, index, status, pending, sent, error) {

    Geof.log("--Upload Status: " + UF_STATUS_NAMES[status]);

    if (error != null && error.length > 0) {
        PanelMgr.showError(error);
    } else {
        var trans;
        if (status === FU_ERROR) {
            trans = new Transaction(Geof.session);
            var delError = GRequest.build('upload', 'delete', null, {'where': {'sendname': _this.upload_file.sendname}});
            trans.addRequest(delError, _this.cbError);
            _this.upload_file = null;
            return;
        }
        else if (status === FU_REGISTERED) {
            if (_this.pbar || false) {
                _this.$pbar.progressbar("option", "max", pending);
                _this.$pbar.progressbar("option", "value", 0);
                _this.$pbar.progressbar({complete: function () {
                    $(_this.pbar + ' > div').css({ 'background': 'green' });
                }});
                _this.upload_start = Date.now();
            }
        }
        else if (status === FU_ACTIVE) {
            var delFU = GRequest.build('upload', 'delete', null, {'where': {'sendname': _this.upload_file.sendname}});
            Transaction.post(delFU, _this.cbComplete);
            _this.upload_file = undefined;
            return;
        }
        else {
            if (_this.pbar || false) {
                _this.$pbar.progressbar("option", "value", sent);
            }
            if (_this.cbTime) {
                _this.cbTime(UploadHandler.getRemainTime(pending, sent), pending, sent);
            }
        }

        var cu = _this.upload_file;
        if (cu != null) {
            if (cu.status === FU_ERROR) {
                PanelMgr.showError("Upload has encountered an error");
            } else if (cu.status == FU_SERVER_ACTIVATE) {
                PanelMgr.showError("Upload complete");
            } else if (cu.status != FU_ACTIVE) {
                cu.upload();
            }
        }
    }
};

UploadHandler.getRemainTime = function (pending, sent) {
    var millis = Date.now() - Geof.cntrl.image.upload_start;
    var send_avg = millis / sent;
    millis = (pending - sent) * send_avg;
    return DateUtil.getMillisAsString(millis);
};

