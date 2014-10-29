
var Geof = Geof || {};

Geof.Retrieve = {

    directory: 'core/panel/',
    viewdir:'view/',

    getEntity:function (entity_name, callback, html, file_path) {

        var _this = Geof.Retrieve;

        if (!(Geof.src[entity_name] || false)) {
            Geof.src[entity_name] = {};
        }

        var path = file_path;
        if ( path === undefined ) {
            path = Geof.href() + _this.directory;
        }

        var fqueue = new FuncQueue();
        if (callback) {
            fqueue.push(callback);
        }

        html.forEach(function (ext) {
            var func = function () {
                var cb = function (src) {
                    Geof.src[entity_name][ext] = src;
                    fqueue.pop();
                };
                var file = path + entity_name + "_" + ext + ".html";
                Geof.Retrieve.getUrl(file, cb);
            };
            fqueue.push(func);
        });
        fqueue.start();
    },

    getView:function (entity_name, callback) {
        var _this = Geof.Retrieve;

        if (Geof.src[entity_name] || false) {
            callback();
        } else {
            var cb = function (src) {
                Geof.src[entity_name] = {};
                Geof.src[entity_name]["view"] = src;
                callback();
            };
            var file = Geof.href() + _this.viewdir + entity_name + "_view.html";
            _this.getUrl(file, cb);
        }
    },

    getUrl:function (url, callback) {
        var rnd = "?rnd=r" + DateUtil.getMilliseconds();
        $.get( url + rnd, function (src) {
            callback(src);
        });

    }
};
