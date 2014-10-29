var Geof = Geof || {};
Geof.map_popup = null;

GMapPopup.filesloaded = false;
GMapPopup.html = null;
GMapPopup.curDialog = null;

function GMapPopup( parent ) {
    this.parent = parent;
    this.GMap = null;
    this.files = null;
    this.toggleButtons = [
        "btnGmapDrawExtent",
        "btnGmapDrawPoint",
        "btnGmapClosestPoint",
        "btnGmapCropTrack",
        "btnGmapDrawExtent"];

}

GMapPopup.prototype.showDialog = function (options) {

    var _this = this;
    var modal = (options.modal ||false) ? true : options.modal;

    var cb = function(html) {
        GMapPopup.html = html;
        $('#mainBody').append(html);
        var $dlg = $('#gmapDialog');
        GMapPopup.curDialog = $dlg;

        _this.initialize_gmap_dialog(_this);
        var pos = GLocal.get('default_map_position', "20,20");
        pos = pos.split(",");
        var top = parseInt(pos[0]);
        var left = parseInt(pos[1]);

        var dlg_options = { autoOpen: false, position:[left, top],
            close: function() {
                $('#gmapDialog').remove();
                GMapPopup.curDialog = null;
                if (options.closeCallback) {
                    options.closeCallback();
                }

            },
            resizeStop: function () {
                var mapHolder = $('#mapHolder');
                var bar = $('#gmapviewerBar');
                var width = bar.width();
                var height = this.clientHeight - (bar[0].clientHeight  + 20);
                mapHolder.width(width).height(height);
                google.maps.event.trigger(_this.GMap.map, 'resize');
            },
            dragStop:function(event, ui) {
                var top = ui.position.top;
                var left = ui.position.left;
                GLocal.set('default_map_position', top + "," + left);
            },
            modal:modal, resizable:true, draggable:true, width:'auto', height:'auto'
        };

        $dlg.dialog(dlg_options);
        $dlg.dialog( "open" );

        Gicon.click('btnGmapDrawExtent',function() {
            _this.toggleDrawExtent(_this.GMap)
        });

        Gicon.click('btnGmapDrawPoint',function() {
            _this.togglePlacePoint(_this.GMap)
        });

        Gicon.click('btnGmapClosestPoint',function() {
            _this.toggleSelectClosestPoint(_this.GMap)
        });

        Gicon.click('btnGmapCropTrack',function() {
            _this.toggleCropTrack(_this.GMap)
        });

        $(".buttonBar").tooltip();
        if (options.completeCB || false) {
            options.completeCB(_this);
        }

        if (GpsUtil.isValidBounds(options.bounds)){
            _this.GMap.fitBounds(options.bounds);
        } else if (options.center && options.zoom) {
            _this.GMap.setZoomCenter(options.center, options.zoom);
        }
    };

    if ( GMapPopup.curDialog != null) {
        GMapPopup.curDialog.dialog("close");
    }
    if (GMapPopup.html == null) {
        Geof.Retrieve.getUrl("view/gmap_dialog.html", cb);
    } else {
        cb(GMapPopup.html);
    }
};

GMapPopup.prototype.toogleButton = function(btnName) {
    var activate = Gicon.isEnabled(btnName);
    for (var indx in this.toggleButtons) {
        var name = this.toggleButtons[indx];
        if (activate && (btnName === name)) {
            Gicon.setActive(name, true);
        } else {
            if ((name === 'btnGmapCropTrack') && (this.GMap.selectedMarkerType() != 1)) {
                Gicon.setEnabled(name, false);
            }   else {
                Gicon.setEnabled(name, true);
            }
        }
    }
    return activate;
};

GMapPopup.prototype.setPointInfo = function(marker) {
    var p = marker.point;
    if (p !== undefined) {
        var text = p.lat().toFixed(5) + ", " + p.lng().toFixed(5);
        if (p.utcdate || false) {
            text += " - " + p.utcdate;
        }
        $("#gmap_pointinfo").text(text);
    }
};

GMapPopup.prototype.toggleDrawExtent = function(gmap) {
    if ( this.toogleButton("btnGmapDrawExtent") ) {
        this.GMap.addExtentListener();
    } else {
        this.GMap.removeListener();
    }
};

GMapPopup.prototype.togglePlacePoint = function(gmap) {
    if ( this.toogleButton("btnGmapDrawPoint") ) {
        this.GMap.addMarkerListener(false);
        this.GMap.addMapListener(this.setPointInfo);
    } else {
        this.GMap.removeListener();
        this.GMap.removeMapListener(this.setPointInfo);
    }
};

GMapPopup.prototype.toggleSelectClosestPoint = function(gmap) {
    if ( this.toogleButton("btnGmapClosestPoint") ) {
        this.GMap.addMarkerListener(true);
        this.GMap.addMapListener(this.setPointInfo);
    } else {
        this.GMap.removeListener();
        this.GMap.removeMapListener(this.setPointInfo);
    }
};

GMapPopup.prototype.toggleCropTrack= function(gmap) {
    if ( this.toogleButton("btnGmapCropTrack") ) {
        this.GMap.addCropTrackListener();
    } else {
        this.GMap.removeListener();
    }
};

GMapPopup.prototype.setExtentPointCallback = function(callback) {
    if (this.GMap || false ) {
        this.GMap.setExtentCallback(callback);
    }
};

GMapPopup.prototype.initialize_gmap_dialog = function(_this) {
    var gmap_center;
    var gmap_zoom;
    var gmap_typeId = google.maps.MapTypeId.ROADMAP;

    var cz = GpsUtil.getCenterZoom();
    gmap_center = cz.center;
    gmap_zoom= cz.zoom;

    var $map = $("#mapHolder")[0];
    _this.GMap = new GoogleMap($map, gmap_center, gmap_zoom, gmap_typeId);

    _this.GMap_Popup = new GMapPopup('mainBody');
    _this.GMap.clearMarkers();
    for (var indx in this.files) {
        _this.findMapData(this.files[indx].id);
    }
};

GMapPopup.prototype.findMapData = function(fileid) {

    var _this = this;
    var cb = function(req) {
        var data = req.data;
        var record;
        for (var indx in data) {
            record = data[indx];
            if (record.pointid > 0) {
                _this.addPointMarker(record.id);
            } else if (record.lineid > 0) {
                _this.loadTrackPoints(record.lineid);
            }
        }
    };

    var obj =  {
        "entity":"file",
        "action":"read",
        "data":{
            "columns":"id,originalname,filetype",
            "join":[
                {
                    "entity":"file_point",
                    "join":"outer",
                    "columns":"pointid"
                },
                {
                    "entity":"file_line",
                    "join":"outer",
                    "columns":"lineid"
                }
            ],
            "where":{
                "status":1,
                "id":fileid
            }
        }
    };
    Transaction.post( GRequest.fromJson(obj), cb);
};

GMapPopup.prototype.loadTrackPoints = function(lineid) {

    var _this = this;
    var cb = function(req) {
        var data = req.data;
        var linepoints = [];
        var bounds = new google.maps.LatLngBounds();
        var pWidth = _this.GMap.getDrawRange(2);
        if ("compressed" in data) {
            if (data.linepoints.length === 0) {
                return;
            }
            var lps = data.linepoints.split(',');
            var p;
            var lastp = null;
            for (var lp in lps) {
                p = lps[lp].split(' ');
                var point = new google.maps.LatLng(p[0],p[1]);
                if (lastp != null) {
                    var dist = GpsUtil.getDistance(lastp, point);
                    if (dist > pWidth) {
                        bounds.extend(point);
                        linepoints.push(point);
                        lastp = point;
                    }
                } else {
                    bounds.extend(point);
                    linepoints.push(point);
                    lastp = point;
                }
            }
        } else {
            for (var indx in data) {
                var lp = data[indx];
                p = new google.maps.LatLng(lp.latitude, lp.longitude)
                bounds.extend(p);
                linepoints.push(p);
            }
        }

        var gmap = _this.GMap;
        var lmarker = gmap.addTrack(linepoints, {
                'file':null,
                'fileid':lineid,
                'callback':cb
            }
        );
        _this.GMap.getMap().setCenter( bounds.getCenter());
    };

    var obj =  {
        "entity":"linepoint",
        "action":"read",
        "actionas":"compressed",
        "data":{
            "where":{"lineid":lineid},
            "orderby":[{"column":"ordernum","order":"asc"}]
        }
    };
    Transaction.post(GRequest.fromJson(obj), cb);
};

GMapPopup.prototype.addTrackMarker = function(track) {

    var _this = this;
    var fileid = ('fileid' in track) ? track.fileid : -1;
    var points = [];
    var p;
    for (var indx in track.points) {
        var lp = track.points[indx];
        p = new google.maps.LatLng(lp.latitude, lp.longitude);
        p.utcdate = track.times[indx];
        points.push(p);
    }
    var gmap = _this.GMap;
    var lmarker = gmap.addTrack(points, {'file':null,'fileid':fileid});
    if (track.bounds || false) {
        lmarker.bounds = track.bounds;
    }
    google.maps.event.addListener(lmarker, 'click', function(event){gmap.selectMarker(lmarker)});
    Gicon.setEnabled('btnGmapClosestPoint',true);
};

GMapPopup.prototype.addVideoMarker = function(file) {

    var _this = this;
    var fileid = ('fileid' in file) ? file.fileid : -1;
    var points = [];
//    var bounds = new google.maps.LatLngBounds();
    var p, lp;
    var track = file.gpsTracks[0];
    var tpoints = track.points;
    for (var indx in tpoints) {
        var lp = tpoints[indx];
        p = new google.maps.LatLng(lp.latitude, lp.longitude);
        points.push(p);
    }
    var gmap = _this.GMap;
    var lmarker = gmap.addTrack(points, {'file':file,'fileid':fileid});
    if (file.bounds || false) {
        lmarker.bounds = file.bounds;
    }
    var vp = file.vp;
    if (vp || false) {
        var markers = [];
        markers.push(lmarker);
        var times = track.times;
        var clickCB = function(event){
            if ( event.latLng ) {
                var cpoint = gmap.getClosestTrackPoint(markers, event.latLng);
                gmap.setPointMarker(cpoint.latlng);
                file.vp.currentTime = (times[cpoint.index].getTime() - times[0].getTime()) / 1000;;
            }
        };
        google.maps.event.addListener(lmarker, 'click', clickCB);

        if (! (track.offsets || false)) {
            track.offsets = GpsUtil.getTrackOffsets(track);
        }
        var timeupdateCB = function() {
            if (! vp.paused) {
                var curTime = vp.currentTime;
                var offsets = track.offsets
                for (var indx in offsets) {
                    if (offsets[indx]> curTime) {
                        indx = parseInt(indx) - 1;
                        var point = track.points[indx];
                        gmap.setPointMarker(new google.maps.LatLng(point.latitude, point.longitude));
                        break;
                    }
                }
            }
        }

        vp.addEventListener("timeupdate",timeupdateCB, false);
    }
};

GMapPopup.prototype.addPhotoMarker = function(file) {
    var _this = this;
    var gmap = _this.GMap;
    var options = {
        fileid:file.id,
        name: file.name
    };
    var gps = file.gpsPoint;
    var marker = gmap.addPoint(gps.latitude, gps.longitude,options);
    google.maps.event.addListener(marker, 'click', function(event){gmap.selectMarker(marker.fileid)});
    gmap.setCenter(marker.getPosition());

};

GMapPopup.prototype.addPointMarker = function(fileid) {
    var _this = this;
    var gmap = _this.GMap;
    var cb = function(req) {
        var data = req.data;
        if (data.length === 0) {
            return;
        }
        var record = data[0];
        var options = {
            fileid:fileid,
            name: record.originalname
        };
        var marker = gmap.addPoint(record.latitude, record.longitude,options);
        google.maps.event.addListener(marker, 'click', function(event){gmap.selectMarker(marker)});
        gmap.setCenter(marker.getPosition());
    };

    var obj =  {
        "entity":"file",
        "action":"read",
        "data":{
            "columns":"id,originalname,filename,filesize,status",
            "join":[
                {
                    "entity":"point",
                    "join":"outer",
                    "columns":"id as pointid,latitude,longitude,utcdate"
                }
            ],
            "where":{
                "id":fileid
            }
        }
    };
    Transaction.post(GRequest.fromJson(obj), cb);
};
