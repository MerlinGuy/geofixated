/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 4/29/13
 * Time: 12:33 PM
 */

var Geof = Geof || {};
Geof.media = Geof.media || {};

Geof.cntrl.upload = {

    file_tmpl : '<li class="ui-widget-content" data-id="%index">'
        +'<div class="fileLiBlock"><label>%filename</label>'
        +'<label id="gps_%index" class="gpsRight no_gps">gps</label><br>'
        +'<input type="checkbox" id="cbUpl%index" class="floatLeft" data-id="%index" />'
        +'<div class="filePbar" id="pbar%index"></div></div>' +
        '<span class="ui-icon icon_geof_upload iconRight2R" id="upl%index"></span>'
        +'</li>',

    upload_tmpl : '<li class="ui-widget-content" data-id="%id" data-sendname="%filename" >'
        +'<label class="fileupload">File name:</label>%originalname<br>' +
        '<label class="fileupload">Registered By: </label>%registeredby<br>' +
        '<label class="fileupload">Status: </label>%status</li>',

    image_tmpl : '<img class="previewthumb" src="%result" title="%filename"/>',
    video_tmpl : '<video id="uplPreviewVideo" controls class="previewthumb" />',
    option_tmpl:'<option value="%id">%name</option>',
    INACTIVE : 0,
    ACTIVE : 1,

    stall_msg_tmpl : '<div id="stall-message" title="Upload Stall Warning!">'
        +'<p><span class="ui-icon ui-icon-alert" style="float: left; margin: 0 7px 20px 0;">'
        +'</span>Upload seems to have stalled.  Click Yes to cancel ALL uploads?</p></div>',

    ENCODE_PERCENT : 1.4,
    STALL_TIMEOUT : 300 * 1000, // 300 seconds or 5 minutes,
    UPLOAD_SLEEP_TIME : 500,
    REQUERY_SLEEP : 3000,
    
    filelist : {},
    fileIndex : 0,
    uploading_list : [],
    upload_timeout : null,
    upload_running : false,
    upload_callback : null,
    gmap_popup:null,
    cur_upload:null,
    cancelled: false,
    server_requery : null,

    initialize:function() {
        var _this = Geof.cntrl.upload

        $( '#fileUploadBtnBar' ).tooltip();

        $("#newUplProjectName").keyup(function() {
            var projName = $("#newUplProjectName").val();
            var validated = projName.length > 0;
            Gicon.setEnabled("btnUplProjectSave", validated);
        });

        Gicon.click("btnFileDiscard" , function() {
            $('#olFileuploadList .ui-selected').each( function(){
                $(this).remove();
            })
            _this._handleFileSelection();
            _this.setCheckGpsIconEnabled();
        });

        Gicon.click("btnFileAdd",function() {
            $('#fileUploadSelector').click();
        });

        Gicon.click("btnFileGps", _this.checkFileGps);
        Gicon.click("btnFileUpload",_this.startUpload);
        Gicon.click("btnCancelFileUpload",_this.cancelupload_callback);
        Gicon.click("btnUplReloadProjects", _this.resetProjects );
        Gicon.click("btnUplProjectSave", _this.saveProject);
        Gicon.click("btnUploadPending" ,_this.reloadPendingUploads);
        Gicon.click("btnUploadDiscard", _this.deleteUpload);
        Gicon.click('btnUplShowMap',_this.viewFilesOnMap);
        Gicon.click('btnSyncWithTrack',_this.syncPhotosWithTrack);
        Gicon.click('btnAdjustDatetime',_this.showAdjustTime);
        Gicon.click('btnSyncVideoToTrack',_this.showSyncVideoTrack );
        Gicon.click('btnOpenTrackDialog',_this.showTrackDialog);
        Gicon.click('btnFileDeselectAll',_this.unselectFiles);
        Gicon.click('btnFileSelectAll',function() {
            $('#olFileuploadList li').each(function() {
                $(this).addClass('ui-selected');
            });
            Geof.cntrl.upload._handleFileSelection();
        });
        $('#fileUploadSelector')[0].addEventListener('change', _this.fileSelectionCB, false);


        $("#uplProjects").change(function() {
            var checked = $("#uplProjects :selected").length > 0;
            $('#linkProjects').prop("checked",checked);
        });

        $( 'div.buttonBar' ).tooltip();
        $( 'span.ui-icon' ).tooltip();
        $( 'div.selectProject' ).tooltip();

        _this.setupKeywords();
        _this.resetProjects();

    },

    fileSelectionCB : function(evt) {
        var _this = Geof.cntrl.upload;
        var files = evt.target.files;

        for(var i = 0, f; f = files[i]; i++) {
            f.index = _this.fileIndex++;
            _this.filelist[f.index] = f;
            if (f.filetype == Filetypes.PHOTO) {
                f.thumbnail = true;
            }
            f.filetype = Filetypes.getEnumByExtension(f.name);
            f.fileid = f.name;
            GpsUtil.setFileCreatedate(f, null, false);
            var li= Geof.cntrl.upload.file_tmpl.replace(new RegExp('%index',"g"), f.index);
            li = li.replace(new RegExp('%filename',"g"), f.name);
            $("#olFileuploadList").append( li );
            $( "#pbar" + f.index ).progressbar({
                value: 0
            });
        }
        Gicon.setEnabled("btnFileSelectAll", files.length > 0);

        _this.setCheckGpsIconEnabled();

        $('#olFileuploadList input').bind('click',function() {
            var $cb = $(this);
            var view_id = this.dataset.id;
            var $li = $cb.parents("li:first");
            if ($cb.prop('checked')) {
                $li.addClass("ui-selected");
            } else {
                $li.removeClass("ui-selected");
            }

            _this._handleFileSelection(view_id);
        });

        $('#olFileuploadList').selectable({
            stop: function() {
                _this._handleFileSelection();
            }
        });

        var input =  $('#fileUploadSelector')[0];
        input.value = '';
    },

    _handleFileSelection:function(view_id) {
        var _this = Geof.cntrl.upload;
        Geof.cntrl.selectedFiles = [];
        var sFiles = Geof.cntrl.selectedFiles;
        $("#olFileuploadList input").attr('checked',false);
        $('#olFileuploadList li.ui-selected').each(function() {
            var fileid = this.dataset.id;
            sFiles.push(_this.filelist[fileid]);
            $("#cbUpl" + fileid).prop("checked", true);
        });
        if ((view_id === undefined) && (sFiles.length == 1)) {
            view_id = sFiles[0].index;
        }
        _this.setViewFile(view_id);
        _this.selectionComplete();
    },

    selectionComplete:function() {
        var cntrl = Geof.cntrl;
        var sFiles = cntrl.selectedFiles
        var enabled = sFiles.length > 0;
        Gicon.setEnabled("btnFileDiscard",enabled);
        Gicon.setEnabled("btnFileDeselectAll",enabled);
        Gicon.setEnabled("btnFileUpload",enabled);
        Gicon.setEnabled("btnUplShowMap",enabled);

        var hasPhotos = cntrl.selectedByType(Filetypes.PHOTO).length > 0;
        var videos = cntrl.selectedByType(Filetypes.VIDEO);
        var hasTracks = cntrl.selectedByType(Filetypes.TRACK).length > 0;

        Gicon.setEnabled('btnSyncVideoToTrack', (videos.length == 1 && hasTracks));
        Gicon.setEnabled('btnAdjustDatetime',hasPhotos);
        Gicon.setEnabled("btnSyncWithTrack",(hasPhotos && hasTracks));
        Gicon.setEnabled('btnOpenTrackDialog',hasTracks);

    },

    unselectFiles:function() {
        $('#olFileuploadList .ui-selected').removeClass("ui-selected");
        $("#olFileuploadList input").attr('checked',false);
        Geof.cntrl.upload.selectionComplete();
    },

    getTracksAndPhotoNoGps:function(files) {
        var tracks = [];
        var photos = [];
        for (var indx=0; indx < files.length;indx++) {
            var file = files[indx];
            var type = Filetypes.getTypeByExtension(file.name);
            if ((type === "photo") && (!(file.gpsPoint || false))) {
               photos.push(file);
            } else if (type === "track") {
                tracks.push(file);
            }
        }
        return {'tracks':tracks,'photos':photos};
    },

    syncPhotosWithTrack:function() {

        var cntrl = Geof.cntrl;
        var tfiles = cntrl.selectedByType(Filetypes.TRACK);
        var pfiles = cntrl.selectedByType(Filetypes.PHOTO);
        if (pfiles.length > 0 && tfiles.length > 0) {
            Gicon.setActive('btnSyncWithTrack',true);
            var options = {
                'tracks':[],
                'photos':pfiles
            };
            var cb = function(tracks){
                var adjustGMT = true;
                for (var indx in tracks) {
                    options.tracks.push(tracks[indx]);
                }

                var _this = Geof.cntrl.upload;
                options = GpsUtil.matchPhotoToTrack(options, adjustGMT);
                for (var indx in options.photos) {
                    var file = options.photos[indx];
                    _this.setGpsStatus(file, file.gpsPoint);
                }
                Gicon.setEnabled('btnSyncWithTrack',true);
                _this.checkFileGps();
            }
            GpsUtil.readTrackFiles(tfiles, cb);
        }
    },

    showAdjustTime:function() {
        var files = Geof.cntrl.selectedFiles;
        Geof.cntrl.adjusttime.createDialog(files, true, null, null);
    },

    deleteUpload: function() {
        var _this = Geof.cntrl.upload
        Gicon.setActive("btnUploadDiscard", true);
        var order = 0;
        var trans = new Transaction(Geof.session);

        var $selected = $('#olPendinguploadList .ui-selected');
        var length = $selected.length;
        for (var indx = 0; indx < length; indx++) {
            var sendname = $($selected[indx]).data('sendname');
            if ( sendname || false ) {
                var req = GRequest.buildDataWhere('upload','delete', null);
                req.data.where = {"sendname": $($selected[indx]).data('sendname')};
                req.order = order++;
                trans.addRequest(req, null);
            }
        }
        var jsonRead = GRequest.build('upload','read', null, {});
        jsonRead.order = order;
        trans.addRequest(jsonRead, _this.reloadPendingUploads);
        trans.send();
    },

    setCheckGpsIconEnabled: function() {
        Gicon.setEnabled('btnFileGps', $('#olFileuploadList li').length > 0);
    },

    viewFilesOnMap:function( ) {
        var _this = Geof.cntrl.upload;
        var selectedFiles = Geof.cntrl.selectedFiles;
        var len = selectedFiles.length;
        var trackFiles = [];
        var tracks = [];
        var photoFiles = [];
        var videoFiles = [];

        var _this = Geof.cntrl.upload;
        for (var i=0;i<len;i++) {
            var file = selectedFiles[i];
            var type = Filetypes.getTypeByExtension(file.name);
            if (type === 'track') {
                trackFiles.push(file);
            } else if ((type === 'photo') && (file.gpsPoint || false)) {
                photoFiles.push(file);
            } else if ((type === 'video') && (file.gpsTracks || false)) {
                videoFiles.push(file);
            }
        }

        var cbSendToMap = function(gpopup) {
            _this.gmap_popup = gpopup;
            for (var indx in photoFiles) {
                gpopup.addPhotoMarker(photoFiles[indx]);
            }
            for (var indx in tracks) {
                gpopup.addTrackMarker(tracks[indx]);
            }
            for (var indx in videoFiles) {
                var file = videoFiles[indx];
                gpopup.addVideoMarker(file);
            }
            gpopup.GMap.zoomToMarkers();
        }

        GoogleMap.addMapListener(_this.setExtentPoint);
        Geof.event.addFileListener(_this.setViewFile);
        var options = {
            closeCallback: function() {
                Geof.event.removeMapListener(_this.setExtentPoint);
                Geof.event.removeFileListener(_this.setViewFile);
            },
            completeCB:cbSendToMap
        }

        if ( trackFiles.length > 0 ) {
            var cb = function(tracklist) {
                tracks = tracklist;
                Geof.map_popup.showDialog(options);
            }
            GpsUtil.readTrackFiles(trackFiles,cb);
        } else {
            Geof.map_popup.showDialog(options);
        }
    },

    showTrackDialog:function( ) {
        var selected =$('#olFileuploadList li.ui-selected');
        var len = selected.length;
        var trackFiles = [];
        var tracks = [];

        var _this = Geof.cntrl.upload;
        for (var i=0;i<len;i++) {
            var file = _this.filelist[$(selected[i]).data('id')];
            var type = Filetypes.getTypeByExtension(file.name);
            if (type === 'track') {
                trackFiles.push(file);
            }
        }

        if ( trackFiles.length == 0 ) {
            return;
        }

        var cb = function(tracklist) {
            var callback = function(trackProfile){
                trackProfile.setTrack(tracklist[0]);
            }
            var closeCB = null;
            Geof.track_popup.createTrackDialog(false, callback, closeCB);
        }

        GpsUtil.readTrackFiles(trackFiles,cb);

    },

    pointMarkerCallback: function(marker) {
        Gicon.setEnabled('btnSyncVideoToTrack', marker != null);
    },

    showSyncVideoTrack:function() {
        //TODO: change this check to a function in map_vid.js
        if ($("#map_video_dialog").length == 0){
            var vfiles = Geof.cntrl.selectedByType(Filetypes.VIDEO);
            var tfiles = Geof.cntrl.selectedByType(Filetypes.TRACK);
            Geof.mapvid.initialize(vfiles[0], tfiles, null);
        }
    },

    setViewFile: function(id) {
        $('#fu_latitude').text("");
        $('#fu_longitude').text("");
        $('#fu_filename').text("");
        $('#fu_filesize').text("");
        $('#fu_filedate').text("");
        $('#image_preview').html("");

        if (id === undefined) {
            return;
        }
        var _this = Geof.cntrl.upload;
        if (JsUtil.isObject(id)) {
            id = id.fileid;
            var files = [];
            files.push(id);
            _this.selectFiles(files);
        }
        var file = _this.filelist[id];
        if (file === undefined) {
            return;
        }

        var name = file.name;
        var size = file.size / 1000000;
        size = parseFloat(Math.round(size * 10000) / 10000).toFixed(4);

        if (!( file.createdate || false)) {
            file.createdate = DateUtil.getFileDate(file);
        }

        $('#fu_filename').text(name);
        $('#fu_filesize').text(size + ' mb');
        $('#fu_filedate').text(file.createdate);
        if (file.gpsPoint || false) {
            $('#fu_latitude').text(file.gpsPoint.latitude);
            $('#fu_longitude').text(file.gpsPoint.longitude);
        }

        // Only process image files.
        if(file.type.match('image.*')) {
            var reader = new FileReader();

            // Closure to capture the file information.
            reader.onload = (function(theFile) {
                return function(e) {
                    var img = _this.image_tmpl.replace(new RegExp('%filename',"g"), escape(theFile.name));
                    img = img.replace(new RegExp('%result',"g"), e.target.result);
                    $('#image_preview').html(img);
                };
            })(file);

            // Read in the image file as a data URL.
            reader.readAsDataURL(file);
        } else if(file.type.match('video.*')) {
            var URL = window.URL || window.webkitURL;
            var reader = new FileReader();
            // Closure to capture the file information.
            reader.onload = (function(theFile) {
                return function(e) {
                    $('#image_preview').html(_this.video_tmpl);
                    var vp = document.getElementById('uplPreviewVideo');
                    theFile.vp = vp;
                    vp.src = URL.createObjectURL(file);
                };
            })(file);
            // Read in the image file as a data URL.
            reader.readAsDataURL(file);
        }
    },

    reloadPendingUploads: function() {
        var cb = function(req) {
            Gicon.setEnabled("btnUploadPending", true);
            var _this = Geof.cntrl.upload;
            var data = req.data;
            var $list = $('#olPendinguploadList');
            $list.empty();

            JsUtil.iterate(data,function(value,key) {
                var row = Templater.mergeTemplate(value, Geof.cntrl.upload.upload_tmpl);
                $list.append(row);
            });
            $list.selectable({
                stop: function() {
                    _this.setUploadIconsEnabled();
                }
            });
            _this.setUploadIconsEnabled();
        };

        Gicon.setActive("btnUploadPending", true);
        var obj = {"entity":"upload","action":"read","data":{}};
        Transaction.post( GRequest.fromJson(obj), cb);
    },

    setExtentPoint:function (mapObject) {
        var _this = Geof.cntrl.upload;
//        var _dec = 5;
        if (mapObject.extent || false) {

        } else if (mapObject.point || false) {

            var confirmCB = function(copy) {
                if (! copy) {
                    return;
                }
                var point = mapObject.point;
                var gpsPoint = {'latitude': point.lat(),'longitude':point.lng()};
                $("#olFileuploadList li.ui-selected").each(function() {
                    var id = $(this).data('id');
                    var fupload = _this.filelist[id];

                    fupload['gpsPoint'] = null;
                    fupload['gpsTrack'] = null;
                    fupload.geomtype = 1;
                    fupload.gpsPoint = gpsPoint;
                    if (point.utcdate || false) {
                        fupload.gpsPoint.datetime = point.utcdate;
                    }

                    var item = $("#gps_" + id);
                    if (item || false) {
                        $(item).switchClass('no_gps','has_gps');
                    }
                });
            };
            PanelMgr.showConfirm("Confirm GPS Copy", "Copy map point to selected files?", confirmCB);
        }
    },

    selectFiles:function(fileIds) {
        $("#olFileuploadList .ui-selected").removeClass('ui-selected');
        $("#olFileuploadList input").prop('checked',false);
        for (var indx in fileIds) {
            var id =  + fileIds[indx];
            $("#li_file_" + id).addClass('ui-selected');
            $("#cbUpl" + id).prop('checked',true);
        }
    },

// -----------------------------------------------------
//  File upload functions  -----------------------------

    startUpload: function() {
        if (Gicon.isActive("btnFileUpload")) {
            return;
        }
        var _this = Geof.cntrl.upload;
        _this.cancelled = false;
        _this.resetProgressBars();
        _this.cur_upload = null;
        var files = Geof.cntrl.selectedFiles;
        var filecount = files.length;
        if (filecount === 0) {
            return;
        }

        Geof.log("Start: " + DateUtil.currentTime());

        Gicon.setActive("btnFileUpload", true);
        Gicon.setEnabled("btnCancelFileUpload", true);

        Gicon.setEnabled("btnFileDiscard", false);
        Gicon.setEnabled("btnFileGps", false);
        Gicon.setEnabled("btnFileAdd", false);
        Gicon.setEnabled("btnFileSelectAll", false);
        Gicon.setEnabled("btnFileDeselectAll", false);

        _this.uploading_list = [];

        var keywords = [];
        if ($('#linkKeywords').is(':checked') ) {
            keywords = _this.parseKeywords(true);
        }

        var pids = [];
        if ($('#linkProjects').is(':checked')) {
            var list = $('#uplProjects option:selected');
            for ( var indx=0;indx<list.length;indx++) {
                var pid = $(list[indx]).val();
                pids.push( parseInt(pid));

            }
        }

        JsUtil.iterate(files, function(file,key){
            file.keywords = keywords;
            file.projectids = pids;
            var uf = new Uploadfile( file, file.index || file.id );
            uf.endCallback = _this.uploadCallback;
            _this.uploading_list.push(uf);
        });
        _this.uploadPendingFiles();
    },

    uploadPendingFiles: function () {
        var _this = Geof.cntrl.upload;
        if ( _this.cancelled) {
            var err = "Upload cancelled";
            var gcn = Geof.cntrl.notification;
            Geof.notifier.addLocal(err,gcn.levels.Medium,gcn.types.Local);
            return;
        }
        var cu = _this.cur_upload;
        if (cu != null) {

            if (cu.status === FU_ERROR) {
                //Todo: send error to screen somehow

            } else if (cu.status == FU_SERVER_ACTIVATE){
                _this.activate_requery(cu);
                return;
            } else if (cu.status != FU_ACTIVE){
                cu.upload();
                return;
            }
        }

        if (_this.uploading_list.length === 0) {
            _this.cancelupload_callback();
        } else {
            cu = _this.uploading_list.shift();
            _this.cur_upload = cu;
            Gicon.setActive("upl" + cu.index, true);
            cu.upload();
        }
    },

    activate_requery:function( cu ) {
        var _this = Geof.cntrl.upload;
        if (_this.server_requery == null) {
            var stateCB = function(isRunning) {
                if (isRunning) {
                    Gicon.setActive('btnUploadRequery', true);
                } else {
                    Gicon.setEnabled('btnUploadRequery', false);
                }
            };
            _this.server_requery = new Requery(_this.requeryCB, stateCB, _this.REQUERY_SLEEP);
        }
        _this.server_requery.add(cu);
        _this.server_requery.start(true);

    },

    requeryCB:function(req) {
        var _this = Geof.cntrl.upload
        var requery = this;
        var data = req.data;

        var requests = [];
        for ( var item in data) {
            item = data[item];
            if (item.status == FU_ACTIVE || item.status == FU_ERROR) {
                var sendname = item.filename;
                var upload = requery.remove(sendname);
                if (upload === undefined) {
                    continue;
                }
                var index = upload.index;
                if (item.status == FU_ERROR) {
                    var $pbar = $("#pbar" + index);
                    $pbar.prop('title', item.error);
                    $pbar.tooltip();
                }
                Gicon.setEnabled("upl" + index, false);
                _this.uploadCallback(index, item.status, null, upload.total_steps, null);
                var del = GRequest.build('upload','delete', null, {'where':{'sendname':upload.sendname}});
                requests.push(del);
            }
        }
        if (requests.length > 0) {
            var trans = new Transaction(Geof.session);
            for (var indx in requests) {
                trans.addRequest(requests[indx]);
            }
            trans.send();
        }
        if (requery.count() == 0) {
            requery.stop();
            this.server_requery = null;
        }
    },

    uploadCallback:function(index, status, pending, sent, error) {
        var _this = Geof.cntrl.upload;
        if ( error != null && error.length > 0) {
            //TODO: work this into the error handling system.
            alert(error);
        } else {
            if (status === FU_ERROR) {
                _this.errorProgressBar(index)
            } else if ( status === FU_REGISTERED ) {
                _this.initProgressBar(index, pending);
            } else {
                _this.updateProgressBar(index, sent);
            }
        }
        _this.uploadPendingFiles();
    },

    updateProgressBar: function (index, count, callback) {
        $('#pbar' + index).progressbar( "option", "value", count);
        if (callback) {
            setTimeout(callback, 250);
        }
    },

    initProgressBar:function(index, count, callback) {
//        Geof.log("updateProgressBar, " + index + ": " + count);
        var $pbar = $('#pbar' + index);
        $pbar.progressbar( "option", "max", count );
        $pbar.progressbar( "option", "value", 0);
        $pbar.progressbar({complete:function() {
            $('#pbar' + index + ' > div').css({ 'background': 'green' });
        }});
        if (callback) {
            setTimeout(callback, 250);
        }
    },

    errorProgressBar:function(index) {
        var $pbar = $('#pbar' + index);
        $pbar.progressbar({complete:function() {
            $('#pbar' + index + ' > div').css({ 'background': 'red' });
        }});
        $pbar.progressbar( "option", "value", $pbar.progressbar( "option", "max" ));
    },

    resetProgressBars:function() {
        $("div[id^='pbar']").each(function() {
            $(this).progressbar( "option", "min", 0 );
            $(this).progressbar( "option", "max", 1 );
            $(this).progressbar( "option", "value", 0);
            $(this).find('div').css({ 'background': 'lightgrey' });
        });
    },

    cancelupload_callback:function() {
        var _this = Geof.cntrl.upload;
        if (_this.upload_callback != null) {
            clearTimeout(_this.upload_callback);
            _this.upload_callback = null;
        }
        _this.uploading_list.length= 0;
        Gicon.setEnabled("btnFileUpload", true);
        Gicon.setEnabled("btnFileDiscard", true);
        Gicon.setEnabled("btnFileGps", true);
        Gicon.setEnabled("btnFileAdd", true);
        Gicon.setEnabled("btnCancelFileUpload", false);
        Gicon.setEnabled("btnFileSelectAll", true);
        Gicon.setEnabled("btnFileDeselectAll", true);

        _this.upload_running = false;
        _this.cancelled = true;
    },


// -----------------------------------------------------
//  File Exif / GPS functions  -----------------------------

    checkFileGps : function() {
        var _this = Geof.cntrl.upload;
        Gicon.setActive("btnFileGps", true);
        var files = _this.filelist;
        var count = Object.keys(files).length;
        var checked = 0;

        var options = {
            value:0,
            max:count,
            title:'Check files for GPS progress',
            callback:function(){}
        };

        var pbar = PanelMgr.showProgressBar(options);

        var completeCB = function() {
            pbar.cancel();
            Gicon.setEnabled("btnFileGps", true);
        };

        var gpsCB = function(file, gps) {
            checked++;
            if (gps != null) {
                _this.setGpsStatus(file,gps);
            }
            pbar.setValue(checked);
            if (checked == count) {
                completeCB();
            }
        };
        GpsUtil.scanFilesForGps(files,gpsCB, completeCB);
    },

    setGpsStatus:function(file, gps) {
        if (file || false ){
            var has_gps = gps || false;

            if (file.filetype == Filetypes.PHOTO) {
                file.gpsPoint = gps;
            } else if (file.filetype == Filetypes.TRACK) {
                file.gpsTracks = gps;
            } else if (file.filetype == Filetypes.VIDEO) {
                file.gpsTracks = gps;
            }

            var item = $("#gps_" + file.id);
            if ( gps && ( item || false ) ) {
                $(item).switchClass('no_gps','has_gps');
            } else {
                $(item).switchClass('has_gps','no_gps');
            }
        }
    },

    setUploadIconsEnabled: function() {
        var count = $('#olPendinguploadList .ui-selected').length;
        Gicon.setEnabled("btnUploadDiscard", (count > 0));
    },

    resetProjects: function() {
        Gicon.setActive('btnUplReloadProjects',true);
        var $select = $('#uplProjects');
        $select.empty();
        Geof.model.read(null, Geof.cntrl.project, function (req) {
            var data = req.data;
            for (var indx in data) {
                $select.append(Templater.mergeTemplate(data[indx], Geof.cntrl.project.option_tmpl));
            }
            Gicon.setEnabled('btnUplReloadProjects',true);
        });
    },
    
    saveProject : function() {
        Gicon.setActive('btnUplProjectSave',true);
        var req = GRequest.buildDataFields("project","create",null);
        req.data.fields = {"name":$('#newUplProjectName').val()};
    
        var cb = function() {
            Gicon.setEnabled('btnUplProjectSave',true);
            Geof.cntrl.upload.resetProjects();
        };
        Transaction.post( req, cb);
    },

    setupKeywords:function () {
        var $Keywords = $("#uplKeywords");

        $Keywords.blur(function () {
            var $this = $(this);
            var keywords = Geof.cntrl.upload.parseKeywords();
            $this.val('');
            $this.val(PhraseParser.format(keywords));
        });

        $("#btnUplResetKeywords").click(function () {
            $("#uplKeywords").val('');
        });
    },

    parseKeywords:function(encode) {
        var text = $("#uplKeywords").val();
        if (text.length > 0) {
            return PhraseParser.parse(text,  encode || false);
        }
        return [];
    }
};


