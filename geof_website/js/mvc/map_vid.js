/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 10/10/13
 * Time: 4:21 PM
 */
var Geof = Geof || {};
Geof.mapvid = (function() {
    return {
        vfile:null,
        tfiles:[],
        gmap:null,
        dialog:null,
        dialog_id:null,
        sync_obj:null,
        vp:null,

        initialize:function(vfile, tfiles, callback) {
            var _this = Geof.mapvid
            this.dialog_id = "map_video_dialog";
            this.vfile = vfile;
            this.tfiles = tfiles;

            var htmlLoadedCallback = function($dlg) {
                _this.dialog = $("#" + _this.dialog_id);
                Gicon.click("btnCloseVideoMap",function() {
                    $dlg.close();
                });
                Gicon.click("btnDiscardVideoTrack",_this.removeVideoTrack);
                Gicon.click("btnSyncVideoToTrack",_this.syncVideoToTrackPoints);
                Gicon.click("btnZoomToTrack",_this.zoomToVideoTrack);
                _this.dialog.tooltip();

                _this.initializeMap();
                _this.initializeVideo();
                if (callback||false) {
                    callback(_this.dialog);
                }
            }

            var config = {
                directory:'view/',
                file:'map_video', divName:'map_video_dialog',
                autoOpen: true,
                resizable: false, modal:true,
                close_callback:null,
                height:644,
                width:620,
                title:'Sync video with track',
                complete_callback:htmlLoadedCallback
            }
            PanelMgr.loadDialogX(config);
        },

        initializeMap:function() {
            var _this = Geof.mapvid;
            
            var gmap_typeId = google.maps.MapTypeId.ROADMAP;

            var cz = GpsUtil.getCenterZoom();
            var gmap_center = cz.center;
            var gmap_zoom= cz.zoom;

            var parent = document.getElementById('vmap_holder');
            _this.gmap = new GoogleMap(parent, gmap_center, gmap_zoom, gmap_typeId);

            _this.gmap.clearMarkers();

            Gicon.toggle('btnGmapClosestPoint', function(enabled){
                if ( enabled ) {
                    _this.gmap.removeListener();
                    Gicon.setEnabled("btnSyncVideoToTrack", false );
                    _this.sync_obj = null;
                } else {
                    _this.gmap.addMarkerListener(true, _this.closestMarkerHandler);
                }
            });

            var icoUrl = window.location.pathname + "img/symbol/blu-diamond-lv.png";
            _this.videoMarker = new google.maps.Marker({
                icon: new google.maps.MarkerImage(
                    icoUrl,
                    new google.maps.Size(16,16),
                    new google.maps.Point(0, 0),
                    new google.maps.Point(8, 8)
                ),
                zindex: 20
            });

            _this.gmap.addMarker(_this.videoMarker);
            _this.gmap.setPoint_callback(_this.setSyncButtunStatus);

        },

        initializeVideo:function() {
            var _this = Geof.mapvid;
            var cb = function(tracks) {
                _this.addTracks(tracks);
                var vtracks = _this.vfile.gpsTracks;
                if ((!(vtracks ||false)) || (vtracks.length == 0)) {
                    vtracks = GpsUtil.matchVideoToTracks(_this.vfile, tracks);
                }
                if (vtracks.length > 0) {
                    _this.overlayVideoTrack(vtracks[0]);
                } else {
                    _this.gmap.zoomToMarkers();
                }
                _this.vp = document.getElementById('uplPreviewVideo');
                _this.vp.addEventListener("timeupdate", function() {
                    _this.setVideoLocation(_this.vp.currentTime);
                }, false);
//                _this.vp.addEventListener("play", function(evt) {
//                    Geof.log(evt.type);
//                }, false);
//                _this.vp.addEventListener("pause", function(evt) {
//                    Geof.log(evt.type);
//                }, false);

            }
            FileUtil.setMediaPlugin(_this.vfile, 'media_plugin', function() {
                GpsUtil.readTrackFiles(_this.tfiles.slice(0),cb);
            });
        },
        setRemoveTrackVideoStatus:function() {
            Gicon.setEnabled("btnDiscardVideoTrack", Geof.mapvid.vfile.track || false);
            Gicon.setEnabled("btnZoomToTrack", Geof.mapvid.vfile.track || false);
        },
        setSyncButtunStatus:function(gpsObject) {
            Gicon.setEnabled("btnSyncVideoToTrack", (gpsObject|| false) );
            Geof.mapvid.sync_obj = gpsObject;
        },
        removeVideoTrack:function() {
            var _this = Geof.mapvid;
            _this.gmap.removeMarker(_this.vfile.fileid);
            _this.vfile.track = null;
            _this.setRemoveTrackVideoStatus();

        },
        syncVideoToTrackPoints:function() {
            var _this = Geof.mapvid;
            var fileid = _this.sync_obj.parent_point.marker.fileid;
            var file = _this.getTrackById(fileid);
            var track = file.gpsTracks[0];
            var vp = _this.vp;
            var offset = vp.currentTime;
            var duration = vp.duration;
            var index = _this.sync_obj.parent_point.index;
            var subTrack = GpsUtil.getSubTrack(track, index, duration, offset);
            if (subTrack != null) {
                _this.overlayVideoTrack(subTrack);
            } else {
                alert("Could not match file to track by date");
            }
        },
        overlayVideoTrack : function(track) {
            var _this = Geof.mapvid;
            _this.removeVideoTrack();
            var vfile = _this.vfile;
            vfile.track = track;
            if ((vfile || false) && (vfile.track || false)) {
                var points = GpsUtil.getGooglePoints(vfile.track);
                var opts = {
                    file:vfile,
                    fileid:vfile.fileid,
                    color:'red',
                    weight:2
                }
                var lmarker = _this.gmap.addTrack(points, opts);
                google.maps.event.addListener(lmarker, 'click', function(event){
                    Geof.log("GMPlugin.prototype.overlayVideoTrack click event needs to be completed");
                });
                vfile.gpsTracks = [];
                vfile.gpsTracks.push(track);
                vfile.geomtype = 1;
                vfile.gpsPoint = null;
                vfile.offsets = GpsUtil.getTrackOffsets(track);
                vfile.gpoints = GpsUtil.getGooglePoints(track);
                vfile.marker = lmarker;
            }
            _this.setRemoveTrackVideoStatus();
        },
        getTrackById:function(id) {
            var tfiles = Geof.mapvid.tfiles;
            for (var indx in tfiles) {
                var tfile = tfiles[indx];
                if (tfile.fileid == id) {
                    return tfile;
                }
            }
            return null;
        },
        addTracks : function(tracks) {
            var track;
            for (var indx in tracks) {
                track = tracks[indx];
                var fileid = ('fileid' in track) ? track.fileid : track.name;

                var points = GpsUtil.getGooglePoints(track);
                var lmarker = this.gmap.addTrack(points, {'file':null,'fileid':fileid});
                if (track.bounds || false) {
                    lmarker.bounds = track.bounds;
                }
                google.maps.event.addListener(lmarker, 'click', function(event){gmap.selectMarker(lmarker)});
                Gicon.setEnabled('btnGmapClosestPoint',true);
            }
        },
        setVideoLocation : function( time ) {
            var _this = Geof.mapvid;
            var gpoints = _this.vfile.gpoints;
            if ((gpoints || false ) && gpoints.length > 0) {
                var offsets = _this.vfile.offsets;
                var indx = 0;
                while (indx < offsets.length && offsets[indx] <= time ) {
                    indx++;
                }
                if (indx < offsets.length) {
                    indx--;
                    _this.videoMarker.setVisible(true);
                    _this.videoMarker.setPosition(gpoints[indx]);
                }
            }
        },
        zoomToVideoTrack:function() {
            Geof.mapvid.gmap.zoomToId(Geof.mapvid.vfile.fileid);
        },

        closestMarkerHandler:function(marker) {
            Geof.log("closestMarkerHandler")  ;
        }
    }
})();