/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 6/26/13
 * Time: 1:02 PM
 */

var Geof = Geof || {};

Geof.event = {
    
    fileListener : [],
    addFileListener : function(callback) {
        if (callback || false) {
            Geof.event.fileListener.push(callback);
        } else {
            Geof.log("event.addFileListern - bad handler");
        }
    },
    removeFileListener : function(callback) {
        JsUtil.spliceByValue(Geof.event.fileListener,callback);
    },
    fireFileListener : function(obj, data) {
        JsUtil.iterate (Geof.event.fileListener,function(l){l(obj,data)});
    },

    clear:function() {
        Geof.event.fileListener.length = 0;
        delete Geof.event.fileListener;
        Geof.event.fileListener = [];
    }

};