/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 4/22/13
 * Time: 1:18 PM
 */


var Geof = Geof || {};

Geof.cntrl.map = {

    name: 'map',
    center: null,
    zoom: null,
    typeId: google.maps.MapTypeId.ROADMAP,
    file_tmpl: '<li class="ui-widget-content" data-id="%id">'
        + '<label class="">%originalname</label>'
        + '<label id="mapGps%id" class="floatRight no_gps mr8">gps</label></li>',

    line_tmpl: '<li class="ui-widget-content" data-id="%lineid">'
        + ' <label>%originalname</label><label class="floatRight">%pointcount</label></li>',

    gmap: null,
    videoMarker: null,
    pointMarker:null,
    container_width: 0,
    files: [],
    $ol:undefined,
    selectFiles:[],

    initialize: function () {
        var _this = Geof.cntrl.map;

        _this.$ol = $('#olGMapFileList');
        _this.$ol.selectable({
            stop: function () {
                _this.selectFiles = $(".ui-selected", this);
                if (_this.selectFiles.length > 0) {
                    if (_this.selectFiles.length == 1) {
                        var id = $(_this.selectFiles[0]).data('id');
                        Geof.event.fireFileListener(id, _this.gmap);
                    }
                    Gicon.setEnabled('btnCopyGps',
                        Gicon.isActive('btnCntrlMapDrawPoint') && (_this.gmap.getPointMarker() || false));
                } else {
                    Gicon.setEnabled('btnCopyGps', false);
                }
            }
        });

        var cz = GpsUtil.getCenterZoom();
        _this.center = cz.center;
        _this.zoom = cz.zoom;

        var $map = $("#mapViewCanvas")[0];
        _this.gmap = new GoogleMap($map, _this.center, _this.zoom, _this.typeId);

        google.maps.event.addListener(_this.gmap.getMap(), 'center_changed', function (event) {
            _this.setBounds();
        });

        Gicon.click("btnSaveBounds", function () {

            var cb = function(do_save) {
                if (do_save) {
                    GpsUtil.setCenterZoom(Geof.cntrl.map.gmap.getZoomCenterStr());
                    Gicon.setEnabled('iconHasDefaultPoint',GpsUtil.getCenterZoom(null, true) != null);
                }
            }
            PanelMgr.showConfirm("Set Default Zoom / Center", "Save bounds as default zoom and center?", cb);
        });

        Gicon.click("btnCopyExtent", function () {
            alert("implementation missing btnCopyExtent");
        });

        Gicon.click("btnCopyGps", _this.copyGpsToFile);

        Gicon.toggle("btnShowPhoto");
        Gicon.toggle("btnSetMapMarker", _this.loadList);

        $("#btnCntrlMapDrawPoint").click(function () {
            _this.togglePlacePoint();
        });

        $("#btnCntrlMapDrawExtent").click(function () {
            _this.toggleDrawExtent();
        });

        $("#iconHasDefaultPoint").click(function() {
            _this.gmap.gotoDefaultZoomCenter();
        });

        Gicon.setEnabled('iconHasDefaultPoint',GpsUtil.getCenterZoom(null, true) != null);

        _this.gmap.setExtentCallback(_this.setExtent);
        $('.buttonBar').tooltip();

        _this.timecntrl = Geof.timecntrl;
        _this.timecntrl.setStage('timecntrl');
        _this.timecntrl.clickCallback = _this.selectListItem;

        _this.loadList();

        var icoUrl = window.location.pathname + "img/symbol/blu-diamond-lv.png";
        _this.videoMarker = new google.maps.Marker({
            icon: new google.maps.MarkerImage(
                icoUrl,
                new google.maps.Size(16, 16),
                new google.maps.Point(0, 0),
                new google.maps.Point(8, 8)
            ),
            zindex: 20
        });

        Geof.cntrl.map.gmap.addMarker(_this.videoMarker);
        Geof.event.addFileListener(_this.selectItem);
    },

    resize: function () {
        var _this = Geof.cntrl.map;
        var parent = $("#mapview_container").parent();
        var remainder = parent.width() - $("#mapList").width() - 16;
        $("#mapViewCanvas").width(remainder);
        if (Geof.cntrl.map.gmap) {
            Geof.cntrl.map.gmap.resize();
        }
        if (_this.timecntrl || false) {
            $("#timecntrl").width(remainder);
            _this.timecntrl.resize();
        }
    },

    loadList: function (results, refreshAction) {
        var _this = Geof.cntrl.map;

        _this.files = JsUtil.isArray(results) ? results : Geof.cntrl.search.results;
        _this.refresh = (refreshAction || false) ? refreshAction : Geof.cntrl.search.execute;

        _this.gmap.clearMarkers();
        var $ol = _this.$ol;
        $ol.empty();

        if (_this.files && _this.files.length > 0) {
            var options = {
                value: 0,
                max: _this.files.length || 0,
                title: 'Loading map data',
                callback: function () {
                }
            };
            var pbar = PanelMgr.showProgressBar(options);
            var curLoaded = 0;
            var gps_list = [];
            for (var indx = 0; indx < _this.files.length; indx++) {
                var file = _this.files[indx];
                if (file.geomtype != -1) {
                    if ('gpsTrack' in file && file.gpsTrack.points.length > 0) {
                        _this.createTrackMarker(file);

                    } else if ('gpsPoint' in file && file.gpsPoint.latitude !== undefined) {
                        _this.createPointMarker(file);

                    } else {
                        gps_list.push(file);
                    }
                }
                $ol.append(Templater.mergeTemplate(file, _this.file_tmpl));
                pbar.setValue(curLoaded++);
            }

            if (gps_list.length > 0) {
                _this.getGpsData(gps_list, pbar, curLoaded);
            } else {
                pbar.cancel();
            }
            _this.timecntrl.setData(_this.files, 'createdate', _this.gmap);
        }
    },

    selectItem: function (obj) {
        var id;
        if (JsUtil.isObject(obj)) {
            id = obj.fileid;
        } else {
            id = obj;
        }

        var _this = Geof.cntrl.map;
        id = JsUtil.isArray(id) ? id[0] : id;
        $('#olGMapFileList .ui-selected').removeClass('ui-selected');
        $('#olGMapFileList li').each(function () {
            if ($(this).data('id') == id) {
                $(this).addClass('ui-selected');
            }
        });

        if (Gicon.isActive("btnShowPhoto")) {
            _this.showPopup(id);
        }
        Geof.cntrl.map.zoomToFileLocation(id);
    },

    showPopup: function (id) {
        if (id === undefined) {
            return;
        }
        var _this = Geof.cntrl.map;

        var file = _this.getFile(id);

        var options = null;
        if ((file.geomtype === 1) && (file.gpsTrack || false)) {
            var offsets = file.gpsTrack.timeoffsets || [];
            var points = file.gpsTrack.points;

            var syncCB = function (time) {
                var indx = 0;
                while (indx < offsets.length && offsets[indx] <= time) {
                    indx++;
                }
                if (indx < offsets.length) {
                    indx--;
                    _this.videoMarker.setVisible(true);
                    _this.videoMarker.setPosition(points[indx]);
                }
            }

            var callback = function (vp) {
                file.video = vp;
                var vcb = function (index) {
                    _this.videoMarker.setPosition(points[index]);
                    vp.currentTime = offsets[index];
                };

                _this.videoMarker.setVisible(true);
                _this.videoMarker.setPosition(points[0]);
                var tMarker = _this.gmap.getMarker(id);
                _this.gmap.addClosestTrackListener(tMarker, vcb);
            }

            var closeCB = function () {
                file.video = null;
                _this.videoMarker.setVisible(false);
                _this.gmap.removeListener();
            }
            options = {modal: false, callback: callback, syncCB: syncCB, closeCB: closeCB};
        }
        Filetypes.showPopupById(id, options);

    },

    zoomToFileLocation: function (id) {
        Geof.cntrl.map.gmap.zoomToId(id, null);
    },

    getFile: function (id) {
        var files = Geof.cntrl.map.files;
        for (var indx in files) {
            if (files[indx].id === id) {
                return files[indx];
            }
        }
        return null;
    },

    copyGpsToFile:function() {
        var _this = Geof.cntrl.map;
        var sFiles = _this.selectFiles;
        //TODO: change this to handle both points and lines
        if (_this.pointMarker || false ) {
            var point = _this.pointMarker.position;
            var gpsPoint = {'latitude': point.lat(),'longitude':point.lng()};

            var cb = function(do_copy) {

                for (var indx=0;do_copy && indx < sFiles.length;indx++) {
                    var id = $(sFiles[indx]).data('id');
                    var file = _this.getFile(id);
                    if (file !== undefined) {
                        file['gpsPoint'] = gpsPoint;
                        file.geomtype = 1;
                        if (point.utcdate || false) {
                            file.gpsPoint.datetime = point.utcdate;
                        } else {
                            file.gpsPoint.datetime = file.datetime;
                        }
                        _this.createPointMarker(file);
                        _this.saveFileGps(file);
                    }
                }
                _this.updateGpsSymbol();
            }

            PanelMgr.showConfirm("Copy GPS Data", "Copy GPS to selected files?", cb);
        }
    },

    getGpsData: function (data, pbar, curLoaded) {
        var _this = Geof.cntrl.map;
        if (data.length == 0) {
            _this.gmap.zoomToMarkers();
            return;
        }
        var trans = new Transaction(Geof.session);

        var cb = function (requests, error) {
            if (error === undefined) {
                JsUtil.iterate(requests, function (request) {

                    var data = request.data[0];
                    var id;
                    if (data === undefined || data.fileid === undefined) {
                        id = request.fileid;
                    } else {
                        id = data.fileid;
                    }
                    var file = _this.getFile(id);
                    if (file !== undefined && data !== undefined) {
                        if (request.entity == 'linepoint') {
                            var tracks = GpsUtil.convertJsonToTracks(data);
                            var track = tracks[0];
                            if (track.points && track.points.length > 0) {
                                track.complete = true;

                                file.gpsTrack = track;
                                file.offsets = GpsUtil.getTrackOffsets(track);
                                file.gpoints = GpsUtil.getGooglePoints(track);
                                _this.createTrackMarker(file);
                            }
                        } else if (request.entity == 'file_point') {
                            file.gpsPoint = {
                                latitude: data.latitude,
                                longitude: data.longitude,
                                utcdate: DateUtil.parseDate(data.utcdate)
                            };
                            _this.createPointMarker(file);

                        }
                    }

                    if (pbar || false) {
                        pbar.setValue(curLoaded++);
                    }
                    _this.updateGpsSymbol();
                });
                _this.gmap.zoomToMarkers();
            } else {
                PanelMgr.showError("Map_view.getGpsData error", error);
            }
            if (pbar || false) {
                pbar.cancel();
            }

        }

        var req;
        for (var indx in data) {
            var row = data[indx];
            if (row.geomtype == 0) {
                if ('gpsPoint' in row && row.gpsPoint.latitude != undefined) {
                    Geof.cntrl.map.createPointMarker(row);
                } else {
                    var json = {"entity": "file_point", "action": "read",
                        "data": {
                            "columns": "fileid",
                            "join": [
                                {"entity": "point", "join": "parent", "columns": "id,latitude,longitude,utcdate"}
                            ], "where": {"fileid": row.id}
                        }
                    };
                    req = GRequest.fromJson(json);
                    trans.addRequest(req, null);
                }

            } else if (row.geomtype == 1) {
                if ('gpsTrack' in row && !row.gpsTrack.complete) {
                    var json = {"entity": "linepoint", "action": "read",
                        "data": {
                            "where": {"fileid": row.id},
                            "columns": "ordernum,latitude,longitude,utcdate,timeoffset,distance",
                            "orderby": "ordernum"
                        }
                    };
                    req = GRequest.fromJson(json);
                    trans.addRequest(req, null);
                }
            }
        }
        trans.send(cb);
    },

    saveFileGps:function(file) {
        var _this = Geof.cntrl.map;
        var order = 0;
        var createdate = file.createdate;
        var point = file.gpsPoint;
        if ((point.datetime != null) && (DateUtil.formatSvrDate(point.datetime))) {
            createdate = DateUtil.formatSvrDate(point.datetime);
        }

        var trans = new Transaction();
        var data = {
            'fields':{
                'longitude':point.longitude,
                'latitude':point.latitude,
                'utcdate':createdate,
                'altitude':point.altitude || false ? point.altitude : 0,
                'azimuth':point.azimuth || false ? point.azimuth : 0
            }
        };
        var reqGps = GRequest.build('point', 'create', null, data);
        reqGps.order = order++;
        trans.addRequest(reqGps, null);
        data = {
            "reference":"fields",
            'fields':{'fileid':file.id, '$pointid':reqGps.requestid + ",id"}
        };
        reqLink = GRequest.build('file_point', 'create', null, data);
        reqLink.order = order;
        trans.addRequest(reqLink,null);
        trans.send();
    },

    createPointMarker: function (file) {
        var _this = Geof.cntrl.map;
        var gmap = _this.gmap;
        var fileid = file.id;
        var p = file.gpsPoint;

        var options = {
            fileid: fileid,
            name: file.originalname,
            gmap: gmap
        };
        var gs = Geof.session;

        var isPhoto = (file.filetype == Filetypes.PHOTO)
        options['isPhoto'] = isPhoto;

        if (isPhoto && Gicon.isActive("btnSetMapMarker")) {
            options['icon'] = gs.url + '?size=1&id=' + fileid + '&sessionid=' + gs.sessionId;
        } else {
            options['icon'] = undefined;
        }

        file.marker = Geof.cntrl.map.gmap.addPoint(p.latitude, p.longitude, options);

    },

    createTrackMarker: function (file) {
        try {
            var _this = Geof.cntrl.map;
            var gmap = _this.gmap;
            file.video = null;

            var lmarker = null;

            var cb = function (event) {
                var loc = {
                    minDistance: 9999999999, //silly high
                    index: -1
                };
                lmarker.getPath().forEach(function (routePoint, index) {
                    var dist = google.maps.geometry.spherical.computeDistanceBetween(event.latLng, routePoint);
                    if (dist < loc.minDistance) {
                        loc.minDistance = dist;
                        loc.index = index;
                    }
                });
                lmarker.file.gps.selectedIndex = loc.index;
                if (lmarker.file.video != null) {
                    var point = lmarker.file.gps[loc.index];
                    lmarker.file.video.currentTime = point.timeoffset;
                }
            };

            var options = {
                'file': file,
                'fileid': file.id,
                'callback': cb,
                gmap: Geof.cntrl.map.gmap
            }
            lmarker = gmap.addTrack(file.gpsTrack.points, options);
            file.marker = lmarker;
        } catch (e) {
            Geof.log(e);
        }

    },

    setBounds: function () {
        var _this = Geof.cntrl.map;
        $('#gmap_bounds').html(
            _this.formatGmapBounds(_this.gmap.getGoogleBounds(), 4)
        );
    },

    setExtent: function (bounds) {
        $('#gmap_extent').html(
            Geof.cntrl.map.formatGmapBounds(bounds, 3)
        );
    },

    setPoint: function (marker) {
        var _this = Geof.cntrl.map;
        _this.pointMarker = marker;
        var latlng = marker.position;
        var _dec = 4;
        $('#gmap_point').text(latlng.lat().toFixed(_dec)
            + ", " +  latlng.lng().toFixed(_dec));
        Gicon.setEnabled('btnCopyGps', Geof.cntrl.map.selectFiles.length > 0);
    },

    formatGmapBounds: function (bounds, decimal) {
        if (!(bounds || false)) {
            return;
        }

        var _dec = 3;
        if (decimal || false) {
            _dec = decimal;
        }
        var ne = bounds.getNorthEast();
        var sw = bounds.getSouthWest();
        return "(" + parseFloat(ne.lat()).toFixed(_dec)
            + ", " + parseFloat(ne.lng()).toFixed(_dec) + ") - ("
            + parseFloat(sw.lat()).toFixed(_dec)
            + ", " + parseFloat(sw.lng()).toFixed(_dec) + ")";
    },

    toggleDrawExtent: function () {
        Geof.cntrl.map.gmap.removeListener();
        if (Gicon.isEnabled("btnCntrlMapDrawExtent")) {
            Gicon.setActive("btnCntrlMapDrawExtent", true);
            Gicon.setEnabled("btnCntrlMapDrawPoint", true);
            Geof.cntrl.map.gmap.addExtentListener();
        } else {
            Gicon.setEnabled("btnCntrlMapDrawExtent", true);
        }
    },

    togglePlacePoint: function () {
        var _this = Geof.cntrl.map;
        if (Gicon.isEnabled("btnCntrlMapDrawPoint")) {
            Gicon.setActive("btnCntrlMapDrawPoint", true);
            Gicon.setEnabled("btnCntrlMapDrawExtent", true);
            _this.gmap.addMarkerListener(false, _this.setPoint);

        } else {
            Gicon.setEnabled("btnCntrlMapDrawPoint", true);
            $('#gmap_point').text('');
            _this.pointMarker = undefined;
            Gicon.setEnabled('btnCopyGps', false);
            _this.gmap.removeMarkerListener(_this.setPoint);
        }
    },

    updateGpsSymbol:function() {
        var hasGps = Geof.cntrl.map.hasGps;
        $('#olGMapFileList li').each(function() {
            var id = $(this).data('id');
            var file = Geof.cntrl.map.getFile(id);
            var item = $("#mapGps" +  id);
            if (hasGps(file)) {
                $(item).switchClass('no_gps','has_gps');
            } else {
                $(item).switchClass('has_gps','no_gps');
            }
        });

    },

    hasGps:function(file) {
        if (file || false) {
            if (file.gpsPoint || false) {
                return GpsUtil.isValidGps(file.gpsPoint, false);
            } else if  (file.gpsTrack || false) {
                return file.gpsTrack.length > 0;
            }
        }
        return false;
    }

};
