var GpsUtil = (function () {

    return {
        POINT: 0,
        TRACK: 1,
        LINE: 1,
        POLYGON: 2,
        MAX_LAT: 90.0,
        MIN_LAT: -90.0,
        MAX_LNG: 180.0,
        MIN_LNG: -180.0,
        DFT_LAT: 40.552287,
        DFT_LNG: -105.076675,

        CENTER_ZOOM_TAG: 'map_center_zoom',

        getCenterZoom: function (dftValue, nullNotFound) {
            var cz = GLocal.get(GpsUtil.CENTER_ZOOM_TAG, dftValue);
            if (cz === undefined || cz === '' || cz === null) {
                if (nullNotFound || false ) {
                    return null;
                } else {
                    return {
                        center: new google.maps.LatLng(GpsUtil.DFT_LAT, GpsUtil.DFT_LNG),
                        zoom: 12
                    };
                }
            } else {
                var val = cz.split(',');
                return {
                    center: new google.maps.LatLng(parseFloat(val[0]), parseFloat(val[1])),
                    zoom: parseInt(val[2])
                };
            }
        },

        setCenterZoom: function (center_zoom) {
            GLocal.set(GpsUtil.CENTER_ZOOM_TAG, center_zoom);
        },

        convertLatitude: function (GPSLatitude, GPSLatitudeRef) {
            var sign = ((GPSLatitudeRef) && (GPSLatitudeRef.toUpperCase() == "S")) ? -1 : 1;

            if ((GPSLatitude) && (GPSLatitude.length == 3)) {
                return (sign * (GPSLatitude[0]
                    + (GPSLatitude[1] / 60)
                    + (GPSLatitude[2] / 3600))).toFixed(6);
            } else {
                return 0;
            }

        },

        convertLongitude: function (GPSLongitude, GPSLongitudeRef) {
            var sign = ((GPSLongitudeRef) && (GPSLongitudeRef.toUpperCase() == "W")) ? -1 : 1;

            if ((GPSLongitude) && (GPSLongitude.length == 3)) {
                return (sign * (GPSLongitude[0]
                    + (GPSLongitude[1] / 60)
                    + (GPSLongitude[2] / 3600))).toFixed(6);
            } else {
                return 0;
            }
        },

        convertGpsTime: function (GPSTimeStamp) {
            if ((GPSTimeStamp) && (GPSTimeStamp.length == 3)) {
                return JFormat.lpad(GPSTimeStamp[0], '0', 2)
                    + ":" + JFormat.lpad(GPSTimeStamp[1], '0', 2)
                    + ":" + JFormat.lpad(GPSTimeStamp[2], '0', 2);
            } else {
                return "00:00:00";
            }
        },

        convertAltitude: function (GPSAltitude, GPSAltitudeRef) {
            return GPSAltitude * ((GPSAltitudeRef > 0) ? GPSAltitudeRef : 1);
        },

        convertAzimuth: function () {
            return 0;
        },

        scanFilesForGps: function (files, gpsCallback, completeCallback) {
            var _this = GpsUtil;
            var point_files = [];
            var track_files = [];
            var video_files = [];
            var tracks = [];
            for (var indx=0;indx < files.length;indx++) {
                var file = files[indx];
                file.id = indx;
                if (file.filetype == Filetypes.PHOTO) {
                    if (GpsUtil.isValidGps(file.gpsPoint)) {
                        gpsCallback(file, file.gpsPoint);
                    } else {
                        point_files.push(file);
                    }
                } else if (file.filetype == Filetypes.TRACK) {
                    if (file.gpsTracks || false) {
                        gpsCallback(file, file.gpsTracks);
                    } else {
                        track_files.push(file);
                    }
                } else if (file.filetype == Filetypes.VIDEO) {
                    if (file.gpsTrack === undefined && file.gpsPoint === undefined) {
                        video_files.push(file);
                    }
                }
            }

            var nextCB = function () {
                if (point_files.length > 0) {
                    var f = point_files.pop();
                    var scanCB = function (binaryFile) {
                        _this.scanEXIF(binaryFile, f, gpsCallback, nextCB);
                    };
                    var bfReader = new BinaryFile();
                    bfReader.readFile(f, f.id, scanCB);

                }
                else if (track_files.length > 0) {
                    var f = track_files.pop();
                    var cb = function () {
                        gpsCallback(f, f.gpsTracks);
                        if (video_files.length > 0) {
                            for (var indx=0;indx<f.gpsTracks.length;indx++) {
                                tracks.push(f.gpsTracks[indx]);
                            }
                        }
                        nextCB();
                    };
                    GpsUtil.readTrackFile(f, cb);
                }
                else if (video_files.length > 0) {
                    var f = video_files.pop();
                    var date = DateUtil.dateFromFilename(f.name);
                    if (date != null) {
                        f.createdate = date;
                    }
                    if (f.createdate !== undefined) {
                        var cb = function () {
                            for (var indx in tracks) {
                                var track = GpsUtil.getVideoSubTrack(tracks[indx], f);
                                if (track != null) {
                                    f.gpsTracks = [track];
                                    gpsCallback(f, f.gpsTracks);
                                    break;
                                }
                            }
                            nextCB();
                        };
                        if (f.duration === undefined) {
                            var cbDuration = function (duration) {
                                f.duration = duration;
                                cb();
                            };
                            FileUtil.getVideoDuration(f, cbDuration);
                        } else {
                            cb();
                        }
                    }
                }
                else if (completeCallback || false) {
                    completeCallback();
                }
            };
            nextCB();
        },

        scanEXIF: function (binaryFile, file, gpsCallback, nextCB) {
            if (binaryFile || false) {
                var tags = EXIF.findEXIFinJPEG(binaryFile);
                var gps = GpsUtil.hasGpsTags(tags) ? GpsUtil.getGpsDecimal(tags) : null;
                gpsCallback(file, gps);
            }
            if (nextCB) {
                nextCB();
            }
        },

        hasGpsTags: function (tags) {
            try {
                return  (tags.GPSLatitude) && (tags.GPSLatitude.length == 3)
                    && (tags.GPSLongitude) && (tags.GPSLongitude.length == 3)
                    && (tags.GPSTimeStamp) && (tags.GPSTimeStamp.length == 3)
                    && (tags.GPSDateStamp) && (tags.GPSDateStamp.length > 0);
            } catch (e) {
                return false;
            }
        },

        getGpsDecimal: function (tags) {
            var gps = {};
            if (GpsUtil.hasGpsTags(tags)) {
                var date = tags.GPSDateStamp.replace(new RegExp(':', "g"), '-');
                gps.latitude = GpsUtil.convertLatitude(tags.GPSLatitude, tags.GPSLatitudeRef);
                gps.longitude = GpsUtil.convertLongitude(tags.GPSLongitude, tags.GPSLongitudeRef);
                gps.datetime = date + ' ' + GpsUtil.convertGpsTime(tags.GPSTimeStamp);
                gps.altitude = GpsUtil.convertAltitude(tags.GPSAltitude, tags.GPSAltitudeRef);
            }
            return gps;
        },

        convertJsonToTracks: function (data) {
            var tracks = [];
            var track = {
                'start': null,
                'end': null,
                'distance': '',
                'points': [],
                'times': [],
                'timeoffsets': [],
                'bounds': new google.maps.LatLngBounds(),
                complete: true
            };

            var row, point;
            for (var indx in data) {
                row = data[indx];
                point = new google.maps.LatLng(row.latitude, row.longitude);
                track.points.push(point);
                track.bounds.extend(point);
                track.times.push(DateUtil.parseDate(row.utcdate));
                track.timeoffsets.push('timeoffset' in row ? row.timeoffset : 0.0);
            }
            track.distance = google.maps.geometry.spherical.computeLength(track.points);
            tracks.push(track);
            return tracks;
        },

        readTrackFiles: function (trackFiles, callback) {
            var tracks = [];
            if (trackFiles.length > 0) {
                var readFile = null;
                var tFile = null;

                var collectTracks = function (rtnTracks) {
                    for (var rIndx in rtnTracks) {
                        var track = rtnTracks[rIndx];
                        track.fileid = tFile.fileid || false ? tFile.fileid : tFile.name;
                        track.file = tFile;
                        tFile.gpsTracks.push(track);
                        if ('error' in track) {
                            Geof.notifier.addLocalAlert("GPSUTIL.readTrackFiles " + track.error);
                        } else {
                            tracks.push(track);
                        }
                    }
                    tFile.startdate = DateUtil.getMinDate(tFile.startdate, GpsUtil.getTrackFileStart(tFile));
                    tFile.enddate = DateUtil.getMaxDate(tFile.enddate, GpsUtil.getTrackFileEnd(tFile));
                    tFile.gmt_diff = DateUtil.getDateDiff(tFile.startdate, tFile.createdate);

                    if (trackFiles.length > 0) {
                        readFile();
                    } else if (tracks.length > 0) {
                        callback(tracks);
                    } else {
                        Geof.notifier.addLocalAlert("GPSUTIL.readTrackFiles No tracks found");
                    }
                };

                readFile = function () {
                    tFile = trackFiles.pop();
                    tFile.gpsTracks = [];
                    var reader = Filetypes.getReader(tFile.name);
                    if (reader != null) {
                        reader = new this[reader]();
                        reader.readFile(tFile, collectTracks);
                    }
                };
                readFile();
            } else {
                callback(tracks);
            }
        },

        readTrackFile: function (trackFile, callback) {
            var tracks = [];
            if (trackFile || false) {
                var readFile = null;
//        Geof.log('file count: ' + trackFiles.length);

                var collectTracks = function (rtnTracks) {
                    for (var rIndx=0;rIndx<rtnTracks.length;rIndx++) {
                        var track = rtnTracks[rIndx];
                        track.fileid = trackFile.id;
                        trackFile.gpsTracks.push(track);
                        if ('error' in track) {
                            Geof.notifier.addLocalAlert("GPSUTIL.readTrackFile " + track.error);
                        } else {
                            tracks.push(track);
                        }
                    }
                    trackFile.createdate = GpsUtil.getTrackFileStart(trackFile);
                    callback(tracks);
                };

                trackFile.gpsTracks = [];
                var reader = Filetypes.getReader(trackFile.name);
                if (reader != null) {
                    var fn = (window || this);
                    reader = new fn[reader]();
                    reader.readFile(trackFile, collectTracks);
                } else {
                    callback([]);
                }
            } else {
                callback([]);
            }

        },

        isValidGps: function (gps, checkdate, datefield) {
            if (gps || false) {
                if (GpsUtil.isValidLatLng(gps.latitude, gps.longitude)) {
                    if (checkdate || false) {
                        datefield = (datefield || false) ? datefield : 'datetime';
                        return DateUtil.isValidDate(gps[datefield]);
                    } else {
                        return true;
                    }
                }
            }
            return false;

        },

        isValidLatLng: function (lat, lng) {
            if (!(lat || false)) {
                return false;
            }
            if (lat < -90 || lat > 90) {
                return false
            }
            if (!(lng || false)) {
                return false;
            }
            if (lng < -180 || lng > 180) {
                return false
            }
            return true;
        },

        isValidBounds: function (bounds) {
            if (bounds || false) {
                var ne = bounds.getNorthEast();
                var sw = bounds.getSouthWest();
                if ((ne || false) && (sw || false)) {
                    return (
                        GpsUtil.isValidLatLng(ne.lat(), ne.lng())
                            && GpsUtil.isValidLatLng(sw.lat(), sw.lng())
                        );
                }
            }
            return false;
        },

        setFileCreatedate: function (file, createdate, adjustGMT) {
            if (createdate || false) {
                file.createdate = createdate;
                return;
            }
            createdate = (file.createdate || false) ? file.createdate : DateUtil.getFileDate(file);
            if (!(createdate || false)) {
                return;
            }
            if (Object.prototype.toString.call(createdate) !== '[object Date]') {
                createdate = DateUtil.parseDate(createdate);
            }
            if (adjustGMT) {
                createdate = new Date(createdate.valueOf() + createdate.getTimezoneOffset() * 60000);
            }
            file.createdate = createdate;
        },

        matchPhotoToTrack: function (options, adjustGMT) {
            var photos = options.photos;
            var tracks = options.tracks;
            for (var pIndx in photos) {
                var photo = photos[pIndx];
                GpsUtil.setFileCreatedate(photo, null, adjustGMT);
                if (!(photo.createdate || false)) {
                    continue;
                }
                var createMillis = photo.createdate.getTime();
                var matched = false;
                for (var tIndx in tracks) {
                    var points = tracks[tIndx].points;
                    var times = tracks[tIndx].times;
                    var first = times[0].getTime();
                    var last = times[times.length - 1].getTime();
                    if (createMillis < first || createMillis > last) {
                        continue;
                    }
                    var createdate
                    for (var indx=0;indx<times.length;indx++) {
                        createdate = times[indx];
                        if (createMillis <= createdate.getTime()) {
                            var nextTime = times[indx];
                            var gps = {};
                            if (indx > 0) {
                                var prevTime = times[indx - 1];
                                var fraction = (createMillis - prevTime) / (nextTime - prevTime);
                                var nextPoint = points[indx];
                                nextPoint = new google.maps.LatLng(nextPoint.latitude, nextPoint.longitude);
                                var prevPoint = points[indx - 1];
                                prevPoint = new google.maps.LatLng(prevPoint.latitude, prevPoint.longitude);
                                p = google.maps.geometry.spherical.interpolate(prevPoint, nextPoint, fraction);
                                gps.latitude = p.lat();
                                gps.longitude = p.lng();

                            } else {
                                gps.latitude = points[indx].latitude;
                                gps.longitude = points[indx].longitude;
                            }
                            gps.datetime = createdate;
                            photo.createdate = createdate;
                            photo.gpsPoint = gps;
                            matched = true;
                            break;
                        }

                    };
                    if (matched) {
                        break;
                    }
                }
            }
            return options;
        },

        getBoundsFromTrack: function (track) {
            var b = track.bounds;
            var bounds;
            if ((b || false) && (b.minlat || false) && b.minlat != null) {
                var ne = new google.maps.LatLng(b.maxlat, b.maxlng);
                var sw = new google.maps.LatLng(b.minlat, b.minlng);
                bounds = new google.maps.LatLngBounds(sw, ne);

            } else {
                var points = track.points;
                bounds = new google.maps.LatLngBounds();
                for (var i = 0; i < points.length; i++) {
                    var p = points[i];
                    bounds.extend(new google.maps.LatLng(p.latitude, p.longitude));
                }
            }
            return bounds;
        },

        getGoogleBounds: function (gbounds) {
            var bounds = null;
            if (gbounds || false) {
                var ne = new google.maps.LatLng(gbounds.maxlat, gbounds.maxlng);
                var sw = new google.maps.LatLng(gbounds.minlat, gbounds.minlng);
                bounds = new google.maps.LatLngBounds(sw, ne);
            }
            return bounds;
        },

        getTrackCreatedate: function (file) {
            var createmillis = null;
            if ((file.gpsTracks || false) && (file.gpsTracks.length > 0)) {
                var tracks = file.gpsTracks;
                var times;
                var millis;
                for (var indx in tracks) {
                    times = tracks[indx].times;
                    if ((times || false) && (times.length > 0)) {
                        millis = times[0].getTime();
                        if (createmillis == null || millis < createmillis) {
                            createmillis = millis;
                        }
                    }
                }
            }
            return createmillis != null ? new Date(createmillis) : createmillis;
        },

        getTrackFileStart: function (file) {
            var start = null;
            var gpsTracks = file.gpsTracks;
            if ((gpsTracks || false) && (gpsTracks.length > 0)) {
                var times;
                var millis;
                for (var indx in gpsTracks) {
                    times = gpsTracks[indx].times;
                    if ((times || false) && (times.length > 0)) {
                        millis = times[0].getTime();
                        start = (start == null) ? millis : Math.min(millis, start);
                    }
                }
            }
            return start != null ? new Date(start) : null;
        },

        getTrackFileEnd: function (file) {
            var end = null;
            var gpsTracks = file.gpsTracks;
            if ((gpsTracks || false) && (gpsTracks.length > 0)) {
                var times;
                var millis;
                for (var indx in gpsTracks) {
                    times = gpsTracks[indx].times;
                    var last = (times || false) ? times.length - 1 : null;
                    if (last != null) {
                        millis = times[last].getTime();
                        end = (end == null) ? millis : Math.max(end, millis);
                    }
                }
            }
            return end != null ? new Date(end) : null;
        },

        getSubTrack: function (track, index, duration, offset) {
            offset = offset || 0;

            var times = track.times;
            var points = track.points;
            var start = index;

            var millis = times[index].getTime() - (offset * 1000);

            while ((times[start].getTime() > millis) && (start > -1)) {
                start--;
            }

            if (start == -1) {
                Geof.notifier.addLocalAlert("GPSUTIL.getSubTrack - Time is off at the front of the track");
                return null;
            }

            millis = times[start].getTime() + (duration * 1000);
            var end = start;
            var len = times.length;
            while ((times[end].getTime() < millis) && (end < len)) {
                end++;
            }

            if (end == len) {
                Geof.notifier.addLocalAlert("GPSUTIL.getSubTrack - Time is off at the end of the track");
                return null;
            }
            var subTrack = {points: [], times: []};
            var p, t;
            while (start <= end) {
                p = points[start];
                t = times[start];
                p.utcdate = t;
                subTrack.points.push(p);
                subTrack.times.push(t);
                start++;
            }
            return subTrack;
        },

        matchVideoToTracks: function (vFile, tFiles) {
            var tracks = [];
            for (var indx in tFiles) {
                var tFile = tFiles[indx];
                var gmt_diff = null;
                if ((tFile.file || false) && (tFile.file.gmt_diff || false)) {
                    gmt_diff = tFile.file.gmt_diff;
                }
                var track = GpsUtil.getVideoSubTrack(tFiles[indx], vFile, gmt_diff);
                if (track != null) {
                    tracks.push(track);
                }
            }
            return tracks;
        },

        getVideoSubTrack: function (track, videofile, gmt_diff) {

            if (videofile.createdate === undefined || videofile.duration === undefined) {
                return null;
            }
            var createdate = DateUtil.parseDate(videofile.createdate, null, null);
            createdate = DateUtil.convertToGMT(createdate);
            var vStart = createdate.getTime();
            var vEnd = vStart + (videofile.duration * 1000);

            var times = track.times;
            var tStart = times[0].getTime();
            var tEnd = times[times.length - 1].getTime();

            if (vStart < tStart || vStart > tEnd) {
                return null;
            }

            var afterStart = false;
            var startTime = null;
            var startIndx = null;
            var endTime = null;
            var endIndx = null;
            var fulltrack = false;
            var millis;
            for (var indx in times) {
                millis = times[indx].getTime();
                if (afterStart) {
                    if (vEnd <= millis) {
                        endTime = millis;
                        endIndx = parseInt(indx);
                        fulltrack = true;
                        break;
                    }
                } else {
                    if (vStart <= millis) {
                        startTime = millis;
                        startIndx = parseInt(indx);
                        afterStart = true;
                    }
                }
            }

            if (!fulltrack) {
                return null;
            }

            var points = track.points;

            var subTrack = {points: [], times: []};
            var prev = times[startIndx - 1];
            var fraction = (vStart - prev) / (startTime - prev);
            var p, t;
            p = GpsUtil.interpolate(points[startIndx - 1], points[startIndx], fraction);
            p.utcdate = vStart;
            subTrack.points.push(p);
            subTrack.times.push(new Date(vStart));

            //Push the interim points into the array
            for (var indx = startIndx + 1; indx < endIndx; indx++) {
                p = points[indx];
                t = times[indx];
                p.utcdate = t;
                subTrack.points.push(p);
                subTrack.times.push(t);
            }

            prev = times[endIndx - 1];
            var fraction = (vEnd - prev) / (endTime - prev);
            p = GpsUtil.interpolate(points[endIndx - 1], points[endIndx], fraction);
            p.utcdate = vEnd;
            subTrack.points.push(p);
            subTrack.times.push(new Date(vEnd));
            return subTrack;
        },

        interpolate: function (p1, p2, fraction) {
            var latlon1 = new google.maps.LatLng(p1.latitude, p1.longitude);
            var latlon2 = new google.maps.LatLng(p2.latitude, p2.longitude);
            var ll = google.maps.geometry.spherical.interpolate(latlon1, latlon2, fraction);
            return {latitude: ll.lat(), longitude: ll.lng()}
        },

        getGooglePoints: function (track, start, end) {
            start = start || 0;
            var tpoints = track.points;
            var times = track.times;
            end = end || tpoints.length;

            var points = [];
            var p;
            for (var indx = start; indx < end; indx++) {
                p = tpoints[indx];
                points.push(new google.maps.LatLng(p.latitude, p.longitude));
            }
            return points;
        },

        setTrackOffsets: function (track) {
            var offsets = [];
            var times = track.times;
            var start = times[0].getTime();
            for (var indx in times) {
                offsets.push((times[indx].getTime() - start) / 1000);
            }
            return offsets;
        },

        getTrackOffsets: function (track) {
            var offsets = [];
            var times = track.times;
            var start = times[0].getTime();
            for (var indx in times) {
                offsets.push((times[indx].getTime() - start) / 1000);
            }
            return offsets;
        },

        trackDistances: function (track) {
            var distances = [];
            var min = Number.POSITIVE_INFINITY;
            var max = 0;
            distances.push(0);
            var points = track.points;
            var len = points.length;
            var p1, dist;
            var p2 = points[0];
            p2 = new google.maps.LatLng(p2.latitude, p2.longitude);
            for (var indx = 1; indx < len; indx++) {
                p1 = points[indx];
                p1 = new google.maps.LatLng(p1.latitude, p1.longitude);
                dist = google.maps.geometry.spherical.computeDistanceBetween(p1, p2);
                if (dist > max) {
                    max = dist;
                }
                if (dist < min) {
                    min = dist;
                }
                distances.push(dist);
                p2 = p1;
            }
            return {'items': distances, 'max': max, 'min': min};
        },

        trackSpeeds: function (track, max_speed) {
            var speeds = [];
            var min = Number.POSITIVE_INFINITY;
            var max = 0;
            speeds.push(0);
            var points = track.points;
            var len = points.length;
            var p1, dist, speed;
            var p2 = points[0];
            var t = track.times;

            p2 = new google.maps.LatLng(p2.latitude, p2.longitude);
            for (var indx = 1; indx < len; indx++) {
                p1 = points[indx];
                p1 = new google.maps.LatLng(p1.latitude, p1.longitude);
                dist = google.maps.geometry.spherical.computeDistanceBetween(p1, p2);
                var secs = (t[indx].getTime() - t[indx - 1].getTime()) / 1000;
                speed = dist / secs;
                if (speed > max_speed) {
                    speed = max_speed;
                }
                if (speed > max) {
                    max = speed;
                }
                if (speed < min) {
                    min = speed;
                }
                speeds.push(speed);

                p2 = p1;
            }
            return {'items': speeds, 'max': max, 'min': min};
        }, trackTimes: function (track, max_time) {
            var times = [];
            times.push(0);
            var min = Number.POSITIVE_INFINITY;
            var max = 0;
            var t = track.times;
            var len = t.length;
            var t2 = t[0];
            var t1;
            var secs;

            for (var indx = 1; indx < len; indx++) {
                t1 = t[indx];
                secs = (t1.getTime() - t2.getTime()) / 1000;
                if (secs > max_time) {
                    secs = max_time;
                }
                if (secs > max) {
                    max = secs;
                }
                if (secs < min) {
                    min = secs;
                }
                times.push(secs);
                t2 = t1;
            }
            return {'items': times, 'max': max, 'min': min};
        },

        getBoundedTrack: function (track, bounds) {
            var subtrack = {points: [], times: []};
            var ne = bounds.getNorthEast();
            var sw = bounds.getSouthWest();
            var maxLat = ne.lat();
            var maxLng = ne.lng();
            var minLat = sw.lat();
            var minLng = sw.lng();
            var p;
            var points = track.points;
            for (var indx=0;indx<points.length;indx++) {
                p = points[indx];
                if ((p.latitude >= minLat) && (p.latitude <= maxLat)
                    && (p.longitude >= minLng) && (p.longitude <= maxLng)) {
                    subtrack.points.push(p);
                    subtrack.times.push(track.times[indx]);
                }
            }
            return subtrack;
        },

        getDistance: function (latlng1, latlng2) {
            var point1 = this.map.getProjection().fromLatLngToPoint(latlng1);
            var point2 = this.map.getProjection().fromLatLngToPoint(latlng2);
            var dX = point1.x - point2.x;
            var dY = point1.y - point2.y;
            return Math.sqrt((dX * dX) + (dY * dY)) * 10000;
        },

        isValidLatLngStr: function (strLatLng) {
            if (strLatLng != null && strLatLng.length > 0) {
                var coordinate = strLatLng.split(',');
                if (coordinate.length == 2) {
                    var lat = parseFloat(coordinate[0]);
                    var lng = parseFloat(coordinate[1]);
                    return ((lat !== NaN) && (lng !== NaN) && (lat >= -90.0) && (lat <= 90.0) && (lng >= -180.0) && (lng <= 180.0));
                }
            }
            return false;
        }
    }

})();


