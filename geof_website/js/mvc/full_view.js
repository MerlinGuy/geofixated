/**
 * User: jeff boehmer
 * Date: 5/31/13
 * Time: 9:09 AM
 */

var Geof = Geof || {};

Geof.cntrl.full = {
    sltSearchs:'sltFullSearches',
    selectedSearch:'#sltFullSearches option:selected',
    btnRefresh:'btnFullRefreshSearch',
    btnClearMedia: 'btnClearFullSearchMedia',
    btnRunSearch:'btnRunFullSearch',
    lblMediaList:'fullMediaList',
    lblResultsView:'fullResultsCount',
    divResultsView: 'fullResultsView',

    initialize:function () {
        var _this = Geof.cntrl.full;
        _this.$sltSearch = $("#" + _this.sltSearchs);
        _this.$lblMediaList = $('#' + _this.lblMediaList);
        _this.$lblResultsView = $('#' + _this.lblResultsView);
        _this.$divResultsView = $('#' + _this.divResultsView);
        _this.$fullMediaTypes = $('#fullMediaTypes');
        _this.$btnClearMedia =  $('#btnClearFullSearchMedia');

        Gicon.click(_this.btnRefresh,_this.populateSelect);
        Gicon.click(_this.btnRunSearch, _this.execute);
        _this.$btnClearMedia.click(_this.clearMediaTypes);

        _this.$sltSearch.click(function() {
            Gicon.setEnabled(_this.btnRunSearch, $(_this.selectedSearch).val() != -1);
        });

        var mtypes = GLocal.get('search_media_types','');
        _this.$lblMediaList.text(mtypes);

        mtypes = mtypes.split(",");
        for (var indx=0;indx<mtypes.length;indx++) {
            JsUtil.iterate( Filetypes.media_names, function(value,key) {
                if (value == mtypes[indx]) {
                    $("input:checkbox[name=fullMediaTypesCB][value='" + key + "']").prop('checked', true );
                }
            });
        }

        _this.$lblMediaList.click(_this.selectMedia);
        _this.populateSelect();
    },

    selectMedia:function() {
        var _this = Geof.cntrl.full;
        var $list = _this.$lblMediaList;
        var offset = $list.position();
        var ddtop = offset.top + $list.height() + 9;
        var dropdown = _this.$fullMediaTypes;
        dropdown.css({top:ddtop, left:offset.left, position:'absolute'});
        dropdown.show();

        $(document).on('mouseup', function (e) {
            if (dropdown.has(e.target).length === 0) {
                $(document).unbind('mouseup');
                dropdown.hide();
                var list = _this.getMediaTypes(false);
                var text = [];
                for (var indx=0;indx<list.length;indx++) {
                    text.push(Filetypes.media_names[parseInt(list[indx])]);
                }
                text = text.join();
                $list.text(text);
                GLocal.set('search_media_types',text);
            }
        });
    },

    getMediaTypes:function(join) {
        var list = $("input[name=fullMediaTypesCB]:checked").map(
            function () {
                return this.value;
            }).get();

        if (join || false) {
            list = list.join();
        }
        return list
    },

    clearMediaTypes:function () {
        Geof.cntrl.full.$lblMediaList.text('');
        $("input[name=fullMediaTypesCB]").each(function(){$(this).prop('checked', false);});
    },

    setView: function(evt) {
        var _this = Geof.cntrl.full;
        Geof.cntrl.search.results = [];
        var viewName = evt.name;
        _this.view = Geof.cntrl[viewName];
        var cb = function() {
            var container = Geof.cntrl.full.$divResultsView;
            container.empty();
            container.append( Geof.src[viewName].view );
            _this.view.initialize();
            _this.view.resize(container);
            if (evt.onload || false) {
                evt.onload();
            }
        };
        Geof.Retrieve.getView(viewName, cb);
    },

    executeCB:function(req) {
        var _this = Geof.cntrl.full;
        var results = req.data;
        Geof.cntrl.search.results = results;

        JsUtil.iterate(results, function(row){
            if (row.geomtype == 0) {
                row.gpsPoint = {
                    latitude:row.latitude,
                    longitude:row.longitude,
                    utcdate:row.utcdate
                };

            } else if (row.geomtype == 1) {
                row.gpsTrack = {
                    points:[],
                    bounds:{
                        minlat:row.minlat,
                        minlon:row.minlon,
                        maxlat:row.maxlat,
                        maxlon:row.maxlon
                    },
                    times:[],
                    start:row.startdate,
                    end:row.enddate,
                    complete:false
                };
            }
        });

        _this.view.loadList(results, _this.execute);
        _this.$lblResultsView.text("Files - " + results.length);
        Gicon.setEnabled(_this.btnRunSearch, true);
        if (_this.refreshCallback || false) {
            _this.refreshCallback(results);
            _this.refreshCallback = null;
        }
    },

    execute: function(callback) {
        var _this = Geof.cntrl.full;
        _this.refreshCallback = callback;
        Gicon.setActive(_this.btnRunSearch,true);
        _this.$lblResultsView.text("");

        var searchid = parseInt($(_this.selectedSearch).val());
        var data = {
            "where":{"id":searchid},
            filetypes: _this.getMediaTypes(true)
        };
        Transaction.post(GRequest.build("search","execute",null,data),_this.executeCB);
    },

    populateSelect:function() {
        var _this = Geof.cntrl.full;
        Gicon.setEnabled(_this.btnRunSearch, false);
        Gicon.setActive(_this.btnRefresh, true);

        var _search = _this.$sltSearch;
        _search.empty();
        var tmpl = Geof.cntrl.search.option_tmpl;
        _search.append(Templater.mergeTemplate( {name:'none',id:-1}, tmpl));

        var cb = function (req) {
            JsUtil.iterate(req.data, function (value) {
                _search.append(Templater.mergeTemplate(value, tmpl));
            });
            Gicon.setEnabled(_this.btnRefresh, true);
        };
        Geof.model.read(null, Geof.cntrl.search, cb);
    }

};

