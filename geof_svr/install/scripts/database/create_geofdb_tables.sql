\connect geofdb;

--DROP TABLE usr;
-- Tables holds user login, status, and other information
CREATE TABLE usr
(
  id serial NOT NULL,
  loginname character varying(20) NOT NULL,
  clearcode character varying(255) NOT NULL,
  salt character varying(255) NOT NULL,
  digest character varying(255) NOT NULL,
  firstname character varying(20),
  lastname character varying(20),
  initials character varying(5),
  email character varying(50) NOT NULL,
  notes character varying(4000),
  settings text,
  statusid smallint,
  attempts smallint,
  lastattempt timestamp without time zone,
  CONSTRAINT usr_pkey PRIMARY KEY (id),
  CONSTRAINT usr_unq_loginname UNIQUE (loginname)
)
WITH (OIDS=FALSE);
ALTER TABLE usr OWNER TO geof;

INSERT INTO usr (loginname,salt,digest,clearcode,firstname,lastname,email,statusid,attempts)
VALUES ('admin',
'0dfed15271c43619b69014f88d8d3ab3937431fc',
'19a98c8fb4d52a9b7e764549ef3395b993ff18210fb6b8c3790fd71160e5a995a4476c966b5bfad13ae5208176b382218aa2e983359160d674ea5e72f62ed659',
'',
'firstname_admin','lastname_admin','<your admin email here>',1,0);


-- table for holding users default and configuration data
CREATE TABLE usrconfig
(
	usrid integer NOT NULL,
	name character varying(80) NOT NULL,
	value character varying(512),
	description character varying(512),
	CONSTRAINT usrconfig_pkey PRIMARY KEY (usrid, name)
)
WITH (OIDS=FALSE);
ALTER TABLE usrconfig OWNER TO geof;


--DROP TABLE serverconfig;
-- table for holding application level configuration data
CREATE TABLE serverconfig
(
  name character varying(80) NOT NULL,
  value character varying(512),
  description character varying(512),
  CONSTRAINT serverconfig_pkey PRIMARY KEY (name)
)
WITH (OIDS=FALSE);
ALTER TABLE serverconfig OWNER TO geof;

INSERT INTO serverconfig VALUES ('defaultextent', '40.48235,-105.17941 40.61279,-104.97959', 'The default extent any map tool should open up with when no extent is defined.  Format: UpperLeft LowerRight');
INSERT INTO serverconfig VALUES ('searchmedia', 'photo,video,track', 'System required list of all searchable media types.');
INSERT INTO serverconfig VALUES ('debuglevel', 'verbose', 'Logger file debug level');
INSERT INTO serverconfig VALUES ('maxdbconnections', '40', 'Maximum number of allowed database connections');
INSERT INTO serverconfig VALUES ('dbautoconnect', '0', 'Number of pre-connected database connections');
INSERT INTO serverconfig VALUES ('sessiontimeout', '1200000', 'Number of milliseconds before an idle session is removed by the Session manager');
INSERT INTO serverconfig VALUES ('photosizes', '80,280,800', 'The default sizes for generated thumbnails');
INSERT INTO serverconfig VALUES ('pointsrid', '4326', 'The PostGIS SRID value for Latitude/Longitude');
INSERT INTO serverconfig VALUES ('maxqueryrows', '801', 'The maximum number of rows any query will return.');
INSERT INTO serverconfig VALUES ('rooturl', '/var/lib/tomcat6/webapps/geof', 'Location of webservice on Tomcatserver');
INSERT INTO serverconfig VALUES ('maxfilesectionsize', '2000000', 'Maximum size of a file section sent to the server.');
INSERT INTO serverconfig VALUES ('title', 'Geofixated', 'Application title');

-----------------------------------------------------
---------  System Metadata Tables  ------------------

-- Table: entityignore (must be populated before entity function executions!
-- DROP TABLE entityignore;
CREATE TABLE entityignore (
	tablename character varying(256),
	primary key (tablename)
)
WITH (OIDS=FALSE);
ALTER TABLE entityignore OWNER TO geof;

INSERT INTO entityignore (tablename) VALUES ('geometry_columns');
INSERT INTO entityignore (tablename) VALUES ('spatial_ref_sys');
INSERT INTO entityignore (tablename) VALUES ('jdbc_datatype');


-- Table: entity
-- DROP TABLE entity;
CREATE TABLE entity (
	id			serial,
	name		varchar(80),
	description character varying(256),
	loadtime	smallint DEFAULT 0,
	status 		int DEFAULT 0,
	entitytype 	smallint DEFAULT 0,
	indatabase 	int DEFAULT 1,
	PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE entity OWNER TO geof;

SELECT insert_entity('annotation');
SELECT insert_entity('authcode');
SELECT insert_entity('configuration');
SELECT insert_entity('dbaction');
SELECT insert_entity('dbpool');
SELECT insert_entity('encrypt');
SELECT insert_entity('entity');
SELECT insert_entity('entitychildren');
SELECT insert_entity('entityfield');
SELECT insert_entity('entityignore');
SELECT insert_entity('entitylink');
SELECT insert_entity('file');
SELECT insert_entity('file_keyword');
SELECT insert_entity('file_line');
SELECT insert_entity('file_point');
SELECT insert_entity('file_project');
SELECT insert_entity('keyword');
SELECT insert_entity('line');
SELECT insert_entity('linepoint');
SELECT insert_entity('link');
SELECT insert_entity('logger');
SELECT insert_entity('notification');
SELECT insert_entity('notification_annotation');
SELECT insert_entity('permission');
SELECT insert_entity('point');
SELECT insert_entity('profile');
SELECT insert_entity('project');
SELECT insert_entity('pulleyupload');
SELECT insert_entity('request');
SELECT insert_entity('requestaudit');
SELECT insert_entity('rsaencryption');
SELECT insert_entity('search');
SELECT insert_entity('search_keyword');
SELECT insert_entity('search_project');
SELECT insert_entity('serverconfig');
SELECT insert_entity('session');
SELECT insert_entity('storageloc');
SELECT insert_entity('table');
SELECT insert_entity('taskmgr');
SELECT insert_entity('test');
SELECT insert_entity('ugroup');
SELECT insert_entity('ugroup_entity');
SELECT insert_entity('upload');
SELECT insert_entity('usr');
SELECT insert_entity('usrconfig');
SELECT insert_entity('usr_notification');
SELECT insert_entity('usr_ugroup');
SELECT insert_entity('version');


-- Table: entityfield
-- DROP TABLE entityfield;
CREATE TABLE entityfield
(
  entityid integer,
  fieldname character varying(80),
  isdefault boolean DEFAULT false,
  ispkey boolean DEFAULT false,
  datatype integer DEFAULT 0,
  isrequired boolean DEFAULT false,
  isauto boolean DEFAULT false,
  isspatial boolean DEFAULT false,
  istemporal boolean DEFAULT false,
  primary key (entityid, fieldname)
)
WITH (OIDS=FALSE);
ALTER TABLE entityfield OWNER TO geof;

-- Inserts for table entities
SELECT f_insert_entityf('annotation');
SELECT f_insert_entityf('authcode');
SELECT f_insert_entityf('dbaction');
SELECT f_insert_entityf('entity');
SELECT f_insert_entityf('entitychildren');
SELECT f_insert_entityf('entityfield');
SELECT f_insert_entityf('entityignore');
SELECT f_insert_entityf('entitylink');
SELECT f_insert_entityf('file');
SELECT f_insert_entityf('file_keyword');
SELECT f_insert_entityf('file_line');
SELECT f_insert_entityf('file_point');
SELECT f_insert_entityf('file_project');
SELECT f_insert_entityf('keyword');
SELECT f_insert_entityf('line');
SELECT f_insert_entityf('linepoint');
SELECT f_insert_entityf('logger');
SELECT f_insert_entityf('notification');
SELECT f_insert_entityf('notification_annotation');
SELECT f_insert_entityf('point');
SELECT f_insert_entityf('project');
SELECT f_insert_entityf('pulleyupload');
SELECT f_insert_entityf('requestaudit');
SELECT f_insert_entityf('rsaencryption');
SELECT f_insert_entityf('search');
SELECT f_insert_entityf('search_keyword');
SELECT f_insert_entityf('search_project');
SELECT f_insert_entityf('serverconfig');
SELECT f_insert_entityf('storageloc');
SELECT f_insert_entityf('ugroup');
SELECT f_insert_entityf('ugroup_entity');
SELECT f_insert_entityf('usr');
SELECT f_insert_entityf('usrconfig');
SELECT f_insert_entityf('usr_notification');
SELECT f_insert_entityf('usr_ugroup');
-- Inserts for non-table entities
select * from f_insert_entity_special('configuration',0,1,1,0);
select * from f_insert_entity_special('dbpool',0,1,1,0);
select * from f_insert_entity_special('encrypt',0,1,1,0);
select * from f_insert_entity_special('link',0,1,1,0);
select * from f_insert_entity_special('logger',0,1,1,0);
select * from f_insert_entity_special('permission',0,1,1,0);
select * from f_insert_entity_special('profile',0,1,1,0);
select * from f_insert_entity_special('request',0,1,1,0);
select * from f_insert_entity_special('session',0,1,1,0);
select * from f_insert_entity_special('table',0,1,1,0);
select * from f_insert_entity_special('taskmgr',0,1,1,0);
select * from f_insert_entity_special('test',0,1,1,0);
select * from f_insert_entity_special('upload',0,1,1,0);
select * from f_insert_entity_special('version',0,1,1,0);

-----------------------------------------------------
------------  File Storage Tables  ------------------
-- Table: storageloc
-- DROP TABLE storageloc;
CREATE TABLE storageloc
(
  id 			serial NOT NULL,
  name 			character varying(80),
  systemdir 	character varying(512),
  active 		boolean default false,
  description 	character varying(512) default ''::character varying,
  quota 		integer default 0,
  usedspace		numeric(10,3) default 0.0,
  filecount 	integer default 0,
  canstream 	boolean default false,
  CONSTRAINT 	storageloc_pkey PRIMARY KEY (id),
  CONSTRAINT 	storageloc_uniq_name UNIQUE (name)
)
WITH (OIDS=FALSE);
ALTER TABLE storageloc OWNER TO geof;


-- Table: authcode
-- DROP authcode;

CREATE TABLE authcode
(
	id 		    serial	NOT NULL,
	guid		character varying(40) NOT NULL,
	usrid 		integer NOT NULL,
	startdate	TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
	enddate		TIMESTAMP WITH TIME ZONE DEFAULT NOW() + '1 day'::interval,
	createdate	TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
	createdby	integer,
	maxuses		integer DEFAULT -1,
	usedcount	integer	DEFAULT 0,
	lastused	TIMESTAMP WITH TIME ZONE,
	CONSTRAINT authcode_pkey PRIMARY KEY (id),
	CONSTRAINT idx_authcode_guid UNIQUE (guid)
);

ALTER TABLE authcode
  OWNER TO geof;


-- Table: pulleyupload

-- DROP TABLE pulleyupload;

CREATE TABLE pulleyupload
(
  id 			serial NOT NULL,
  guid 			character varying(40) NOT NULL,
  usrid 		integer NOT NULL,
  filetype 		integer NOT NULL,
  uploaddate 	timestamp with time zone DEFAULT now(),
  originalname 	character varying(80),
  filename 		character varying(512),
  notes 		character varying(512),
  success 		boolean,
  CONSTRAINT 	pulleyupload_pkey PRIMARY KEY (id ),
  CONSTRAINT 	idx_pulleyupload_guid UNIQUE (guid )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE pulleyupload
  OWNER TO geof;


-------------------------------------------
-------------------------------------------
-- Table: search
-- DROP TABLE search;

CREATE TABLE search
(
  id 			serial NOT NULL,
  name 			character varying(80),
  minlat 		numeric(13,8),
  minlon 		numeric(13,8),
  maxlat 		numeric(13,8),
  maxlon 		numeric(13,8),
  mindatetime 	timestamp without time zone,
  maxdatetime 	timestamp without time zone,
  distance 		numeric(12,3),
  unitofmeasure integer,
  status 		integer NOT NULL,
  description 	character varying(512),
  spatialtype 	smallint,
  temporaltype 	smallint,
  usespatial 	boolean,
  usetemporal 	boolean,
  usekeywords 	boolean,
  keywordtype 	smallint,
  useprojects 	boolean,
  useworkgroups boolean,
  registerdate 	timestamp without time zone DEFAULT now(),
  registeredby 	integer NOT NULL,
  originalname 	character varying(80),
  minregdate 	timestamp without time zone,
  maxregdate 	timestamp without time zone,
  CONSTRAINT 	search_pkey PRIMARY KEY (id ),
  CONSTRAINT 	search_unq_registeredby UNIQUE (name , registeredby )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE search
  OWNER TO geof;

-------------------------------------------
-------------------------------------------
-- Table: keyword
-- DROP TABLE keyword;
CREATE TABLE keyword
(
  id 			serial NOT NULL,
  keyword 		character varying(512),
  status 		integer default 1,
  createdate 	timestamp without time zone default CURRENT_TIMESTAMP,
  createdby 	integer NOT NULL,
  description 	character varying(512),
  CONSTRAINT 	keyword_pkey PRIMARY KEY (id),
  CONSTRAINT 	keyword_uniq UNIQUE (keyword)
)
WITH (OIDS=FALSE);
ALTER TABLE keyword OWNER TO geof;

-- Table: search_keyword
-- DROP TABLE search_keyword;
CREATE TABLE search_keyword 
(
	searchid	integer,
	keywordid	integer,
	CONSTRAINT 	search_keyword_pkey PRIMARY KEY (searchid, keywordid)
)
WITH (OIDS=FALSE);
ALTER TABLE search_keyword OWNER TO geof;

-------------------------------------------
-------------------------------------------
-- Table: project
-- DROP TABLE project;
CREATE TABLE project
(
  id 			serial NOT NULL,
  name 			character varying(128),
  status 		integer default 1,
  createdate 	timestamp without time zone default CURRENT_TIMESTAMP,
  createdby 	integer NOT NULL,
  description 	character varying(512),
  CONSTRAINT 	project_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE project OWNER TO geof;


-- Table: search_project
-- DROP TABLE search_project;
CREATE TABLE search_project 
(
	searchid	integer,
	projectid	integer,
	CONSTRAINT 	search_project_pkey PRIMARY KEY (searchid, projectid)
)
WITH (OIDS=FALSE);
ALTER TABLE search_project OWNER TO geof;



----------------------------------------------------------
-- DROP TABLE dbaction
CREATE TABLE dbaction
(
  id serial NOT NULL,
  name character varying(128),
  description character varying(512),
  CONSTRAINT dbaction_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE dbaction OWNER TO geof;

INSERT INTO dbaction (id, name, description) VALUES (1, 'create', 'Adding a new record/file to the database/directory');
INSERT INTO dbaction (id, name, description) VALUES (2, 'read', 'Return information about a record/file in the database/directory');
INSERT INTO dbaction (id, name, description) VALUES (3, 'update', 'Changing the field value of an existing record/file in the database/directory');
INSERT INTO dbaction (id, name, description) VALUES (4, 'delete', 'Removing an existing record/file from the database/directory');
INSERT INTO dbaction (id, name, description) VALUES (5, 'execute', 'Request to perform an action which processes data or runs an external script');


---------------------------------------
--DROP TABLE requestaudit

CREATE TABLE requestaudit
(
	requestname 	character varying(128) NOT NULL,
	actionname  	character varying(64) NOT NULL,
	actionas  		character varying(64) NOT NULL,
	usrid 			integer NOT NULL,
	sessionid 		character varying(128) NOT NULL,
	rundate 		timestamp DEFAULT current_timestamp NOT NULL,
	completedate 	timestamp,
	result 			smallint
)
WITH (OIDS=FALSE);
ALTER TABLE requestaudit OWNER TO geof;

CREATE index requestaudit_rundate_indx on requestaudit (rundate);

------------ Spatial Tables  ---------------------

-- Table: point
-- DROP TABLE point;
CREATE TABLE point
(
  id 			serial NOT NULL,
  longitude 	numeric(13,8),
  latitude 		numeric(13,8),
  utcdate 		timestamp without time zone,
  altitude 		numeric(10,3),
  azimuth 		numeric(10,3),
  registeredby 	integer default -1,
  CONSTRAINT 	point_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE point OWNER TO geof;

SELECT AddGeometryColumn('point', 'geom', 4326, 'POINT', 2);

CREATE INDEX point_spatial ON point USING GIST ( geom ); 

------------------------------------------------
-- Table: line
-- DROP TABLE line;
CREATE TABLE line
(
  id 			serial NOT NULL,
  pointcount 	integer default 0,
  minlat 		numeric(13,8) default 0.0,
  minlon 		numeric(13,8) default 0.0,
  maxlat 		numeric(13,8) default 0.0,
  maxlon 		numeric(13,8) default 0.0,
  startdate 	timestamp without time zone,
  enddate 		timestamp without time zone,
  registeredby 	integer default -1,
  description 	character varying(512),
  CONSTRAINT 	line_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE line OWNER TO geof;

SELECT AddGeometryColumn('line', 'geom', 4326, 'LINESTRING', 2);

CREATE INDEX line_spatial ON line USING GIST ( geom ); 

------------------------------------------------
-- Table: linepoint
-- DROP TABLE linepoint;
CREATE TABLE linepoint
(
  lineid 		integer NOT NULL,
  ordernum 		integer NOT NULL,
  longitude 	numeric(13,8),
  latitude 		numeric(13,8),
  utcdate 		timestamp without time zone,
  timeoffset 	real default 0.0,
  altitude 		numeric(10,3),
  distance 		numeric(13,3),
  azimuth 		numeric(10,3),
  geom 			geometry,
  CONSTRAINT 	linepoint_pkey PRIMARY KEY (lineid, ordernum),
  CONSTRAINT 	enforce_dims_geom CHECK (ndims(geom) = 2),
  CONSTRAINT 	enforce_geotype_geom CHECK (geometrytype(geom) = 'POINT'::text OR geom IS NULL),
  CONSTRAINT 	enforce_srid_geom CHECK (srid(geom) = 4326)
)
WITH (OIDS=FALSE);
ALTER TABLE linepoint OWNER TO geof;


---------------------------------------------------------
-- Table: file_keyword
-- DROP TABLE file_keyword;
CREATE TABLE file_keyword 
(
	fileid		integer NOT NULL,
	keywordid 	integer NOT NULL,
	confidence 	numeric(5,4) default 1.0,
	CONSTRAINT 	file_keyword_pkey PRIMARY KEY (fileid, keywordid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_keyword OWNER TO geof;

---------------------------------------------------------
-- Table: file_project
-- DROP TABLE file_project;

CREATE TABLE file_project 
(
	fileid		integer,
	projectid	integer,
	CONSTRAINT file_project_pkey PRIMARY KEY (fileid, projectid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_project OWNER TO geof;

---------------------------------------------------------
-- Table: file_point
-- DROP TABLE file_point;

CREATE TABLE file_point 
(
	fileid		integer,
	pointid		integer,
	CONSTRAINT 	file_point_pkey PRIMARY KEY (fileid, pointid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_point OWNER TO geof;

---------------------------------------------------------
-- Table: file_line
-- DROP TABLE file_line;

CREATE TABLE file_line
(
	fileid		integer,
	lineid		integer,
	CONSTRAINT 	file_line_pkey PRIMARY KEY (fileid, lineid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_line OWNER TO geof;

---------------------------------------------------------
-- Table: rsaencryption
-- DROP TABLE rsaencryption;

CREATE TABLE rsaencryption
(
	id 			serial NOT NULL,
	modulus 	text,
	exponent 	character varying(64),
	pexponent 	text,
	p 			text,
	q 			text,
	dp 			text,
	dq 			text,
	qinv 		text,
	createdate 	timestamp without time zone DEFAULT now(),
	CONSTRAINT 	rsaencryption_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE rsaencryption OWNER TO geof;

---------------------------------------------------------
-- Table: notification
-- DROP TABLE notification;

CREATE TABLE notification
(
	id 				serial NOT NULL,
	message 		text,
	level 			smallint default 0,
	usrid 			integer,
	notificationid 	integer,
	type 			smallint default 0,
	createdate 		timestamp without time zone DEFAULT now(),
	CONSTRAINT 		notification_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE notification OWNER TO geof;

---------------------------------------------------------
-- Table: usr_notification
-- DROP TABLE usr_notification;

CREATE TABLE usr_notification
(
	usrid			integer,
	notificationid	integer,
	readdate 		timestamp without time zone,
	CONSTRAINT 		usr_notification_pkey PRIMARY KEY (usrid, notificationid)
)
WITH (OIDS=FALSE);
ALTER TABLE usr_notification OWNER TO geof;

---------------------------------------------------------
-- Table: annotation
-- DROP TABLE annotation;
CREATE TABLE annotation
(
	id 				serial NOT NULL,
	fileid 			integer NOT NULL,
	title 			character varying(256) NOT NULL,
	description 	character varying(4000),
	registerdate 	timestamp without time zone DEFAULT now(),
	registeredby 	integer default -1,
    longitude 		numeric(13,8),
    latitude 		numeric(13,8),
    startoffset 	numeric(13,8),
    endoffset 		numeric(13,8),
    ratiox 			numeric(13,8),
    ratioy 			numeric(13,8),
	rotation 		smallint default 0,
    type 			smallint default 0,
	CONSTRAINT 		annotation_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE annotation OWNER TO geof;

---------------------------------------------------------
-- Table: notification_annotation
-- DROP TABLE notification_annotation;

CREATE TABLE notification_annotation 
(
	notificationid		integer,
	annotationid		integer,
	CONSTRAINT 			notification_annotation_pkey PRIMARY KEY (notificationid, annotationid)
)
WITH (OIDS=FALSE);
ALTER TABLE notification_annotation OWNER TO geof;


---------------------------------------------------------
-- Table: ugroup
-- DROP TABLE ugroup;
CREATE TABLE ugroup
(
	id 			serial NOT NULL,
	name 		character varying(128),
	description character varying(512),
	CONSTRAINT 	ugroup_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE ugroup OWNER TO geof;


INSERT INTO ugroup (name, description) VALUES ('BaseUser', 'Role which allows the user to login into the system.');
INSERT INTO ugroup (name, description) VALUES ('Sys Admin', 'Role which allows the user to login into the system.');
INSERT INTO ugroup (name, description) VALUES ('Administrator', 'test');

---------------------------------------------------------
-- Table: ugroup_entity
-- DROP TABLE ugroup_entity;
CREATE TABLE ugroup_entity
(
	ugroupid	integer,
	entityid	integer,
	createable	boolean,
	readable	boolean,
	updateable	boolean,
	deleteable	boolean,
	executable	boolean,
	CONSTRAINT ugroup_entity_pkey PRIMARY KEY (ugroupid, entityid)
)
WITH (OIDS=FALSE);
ALTER TABLE ugroup_entity OWNER TO geof;

SELECT f_insert_ugroup_entity('Administrator','authcode',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','configuration',false,true,true,false,false);
SELECT f_insert_ugroup_entity('Administrator','dbaction',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','dbpool',false,true,false,true,false);
SELECT f_insert_ugroup_entity('Administrator','encrypt',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','entity',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','entitychildren',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','entityfield',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','entityignore',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','entitylink',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','file',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','file_keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','file_line',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','file_point',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','file_project',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','line',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','linepoint',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','link',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','logger',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','notification',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','permission',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','point',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','project',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','pulleyupload',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','request',false,true,false,false,false);
SELECT f_insert_ugroup_entity('Administrator','requestaudit',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','rsaencryption',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','search',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Administrator','search_keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','search_project',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','serverconfig',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','session',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','storageloc',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Administrator','table',true,false,false,false,false);
SELECT f_insert_ugroup_entity('Administrator','taskmgr',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Administrator','test',false,true,false,false,false);
SELECT f_insert_ugroup_entity('Administrator','ugroup',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','ugroup_entity',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','upload',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','usr',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','usrconfig',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','usr_ugroup',true,true,true,true,false);
SELECT f_insert_ugroup_entity('Administrator','version',false,true,false,false,false);
SELECT f_insert_ugroup_entity('BaseUser','annotation',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','authcode',false,true,false,false,false);
SELECT f_insert_ugroup_entity('BaseUser','file',true,true,true,false,false);
SELECT f_insert_ugroup_entity('BaseUser','file_keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','file_line',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','file_point',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','file_project',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','line',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','linepoint',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','notification',true,true,true,false,false);
SELECT f_insert_ugroup_entity('BaseUser','notification_annotation',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','point',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','profile',false,true,true,false,false);
SELECT f_insert_ugroup_entity('BaseUser','project',true,true,false,false,false);
SELECT f_insert_ugroup_entity('BaseUser','search',true,true,true,true,true);
SELECT f_insert_ugroup_entity('BaseUser','search_keyword',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','search_project',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','session',true,true,false,true,false);
SELECT f_insert_ugroup_entity('BaseUser','upload',true,true,true,true,false);
SELECT f_insert_ugroup_entity('BaseUser','usr',false,true,true,false,false);
SELECT f_insert_ugroup_entity('BaseUser','usr_notification',true,true,true,false,false);
SELECT f_insert_ugroup_entity('Sys Admin','dbpool',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Sys Admin','pulleyupload',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Sys Admin','rsaencryption',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Sys Admin','storageloc',true,true,true,true,true);
SELECT f_insert_ugroup_entity('Sys Admin','taskmgr',true,true,true,true,true);

---------------------------------------------------------
-- Table: usr_ugroup
-- DROP TABLE usr_ugroup;
CREATE TABLE usr_ugroup
(
	usrid		integer,
	ugroupid	integer,
	CONSTRAINT 	usr_ugroup_pkey PRIMARY KEY (usrid, ugroupid)
)
WITH (OIDS=FALSE);
ALTER TABLE usr_ugroup OWNER TO geof;


INSERT INTO usr_ugroup (usrid, ugroupid) VALUES (1, 1);
INSERT INTO usr_ugroup (usrid, ugroupid) VALUES (1, 2);
INSERT INTO usr_ugroup (usrid, ugroupid) VALUES (1, 3);

---------------------------------------------------------
----  Media tables   ------------------------------------
-- Table: file
-- DROP TABLE file;
CREATE TABLE file
(
	id 				serial NOT NULL,
	filename 		character varying(128) NOT NULL,
	fileext 		character varying(25),
	filesize 		integer NOT NULL,
	originalname 	character varying(500),
	filetype 		integer NOT NULL,
	status 			integer NOT NULL,
	checksumval 	character varying(50),
	createdate 		timestamp without time zone,
	duration 		integer default 0,
	registerdate 	timestamp without time zone DEFAULT now(),
	registeredby 	integer default -1,
	notes 			character varying(4000),
	storagelocid 	integer,
	viewid 			integer default -1,
	geomtype 		smallint default 0,
	CONSTRAINT 		file_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE file OWNER TO geof;

---------------------------------------------------------
----  JDBC lookup table  --------------------------------
-- Table: jdbc_datatype
-- DROP TABLE jdbc_datatype;
CREATE TABLE jdbc_datatype (
	id serial,
	pname character varying(512) NOT NULL,
	jname character varying(512) NOT NULL,
	jenum integer NOT NULL,
	CONSTRAINT java_datatype_key PRIMARY KEY (id)
	)
WITH (
  OIDS=FALSE
);
ALTER TABLE jdbc_datatype
  OWNER TO geof;

INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('array', 'array', 2003);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('bigint', 'bigint',  -5);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('bitea', 'binary',  -2);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('boolean', 'bit',  -7);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('timestamp with time zone', 'timestamp',  93);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('decimal','decimal',  3);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('integer', 'integer',  4);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('numeric', 'numeric',  2);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('USER-DEFINED', 'other',  1111);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('real', 'real',  7);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('smallint', 'smallint',  5);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('time', 'time', 92);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('timestamp without time zone', 'timestamp',  93);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('text', 'varchar',  12);
INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('character varying', 'varchar',  12);
--The following JDBC data types are currently not used (4/10/14)
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('bytea', 'blob',  2004);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('boolean', 'boolean',  16);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('text', 'char',  1);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('bytea', 'clob',  2005);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('DATALINK',  70);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('DISTINCT',  2001);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('DOUBLE',  8);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('real', 'float',  6);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('JAVA_OBJECT',  2000);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('LONGNVARCHAR',  -16);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('LONGVARBINARY',  -4);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('LONGVARCHAR',  -1);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('NCHAR',  -15);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('NCLOB',  2011);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('NULL',  0);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('NVARCHAR',  -9);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('REF',  2006);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('ROWID',  -8);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('SQLXML',  2009);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('STRUCT',  2002);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('smallint', 'smallint',  -6);
--INSERT INTO jdbc_datatype (pname, jname, jenum) VALUES('VARBINARY',  -3);


---------------------------------------------------------
---------------------------------------------------------
-- Reset sequence start counts to max + 1 so there is no overlap
SELECT setval('authcode_id_seq', (SELECT MAX(id+1) FROM authcode));
SELECT setval('dbaction_id_seq', (SELECT MAX(id+1) FROM dbaction));
SELECT setval('entity_id_seq', (SELECT MAX(id+1) FROM entity));
SELECT setval('file_id_seq', (SELECT MAX(id+1) FROM file));
SELECT setval('keyword_id_seq', (SELECT MAX(id+1) FROM keyword));
SELECT setval('line_id_seq', (SELECT MAX(id+1) FROM line));
SELECT setval('point_id_seq', (SELECT MAX(id+1) FROM point));
SELECT setval('project_id_seq', (SELECT MAX(id+1) FROM project));
SELECT setval('pulleyupload_id_seq', (SELECT MAX(id+1) FROM pulleyupload));
SELECT setval('ugroup_id_seq', (SELECT MAX(id+1) FROM ugroup));
SELECT setval('search_id_seq', (SELECT MAX(id+1) FROM search));
SELECT setval('storageloc_id_seq', (SELECT MAX(id+1) FROM storageloc));
SELECT setval('ugroup_id_seq', (SELECT MAX(id+1) FROM usr));
SELECT setval('usr_id_seq', (SELECT MAX(id+1) FROM usr));


--- Create Functions
---------------------------------------------------------
---------------------------------------------------------
--------------
-- Auto validgps and geom update trigger for point	
CREATE OR REPLACE FUNCTION func_point_validgps() RETURNS trigger AS $func_point_validgps$
    BEGIN
        -- Check if latitiude/longitude/utcdate forms a valid gps point
        IF (NEW.latitude < -90) OR (NEW.latitude > 90) OR (NEW.longitude < -180) 
					OR (NEW.longitude > 180) OR (NEW.utcdate <= TIMESTAMP '0001-01-01 00:00:00') THEN
			NEW.geom = NULL;
		ELSE 
			NEW.geom = geomfromtext('POINT('||NEW.longitude||' '||NEW.latitude||')', 4326);
        END IF;

        RETURN NEW;
    END;

$func_point_validgps$ LANGUAGE plpgsql;

--------------
CREATE TRIGGER tgr_point_validgps BEFORE INSERT OR UPDATE ON point
    FOR EACH ROW EXECUTE PROCEDURE func_point_validgps();

--------------
-- Auto validgps and geom update trigger for linepoint	
CREATE OR REPLACE FUNCTION func_linepoint_validgps() RETURNS trigger AS $func_linepoint_validgps$
    BEGIN
        -- Check if latitiude/longitude/utcdate forms a valid gps point
        IF (NEW.latitude < -90) OR (NEW.latitude > 90) 
			OR (NEW.longitude < -180) OR (NEW.longitude > 180) 
			OR (NEW.utcdate <= TIMESTAMP '0001-01-01 00:00:00') THEN
		NEW.geom = NULL;
	ELSE 
		NEW.geom = geomfromtext('POINT('||NEW.longitude||' '||NEW.latitude||')', 4326);
        END IF;

        RETURN NEW;
    END;
$func_linepoint_validgps$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_linepoint_validgps BEFORE INSERT OR UPDATE ON linepoint
    FOR EACH ROW EXECUTE PROCEDURE func_linepoint_validgps();
	
--------------
-- Automatically set the file.geomtype whenever a file is linked to file_point
CREATE OR REPLACE FUNCTION func_filepoint_setgeomtype() RETURNS trigger AS $func_filepoint_setgeomtype$
    BEGIN
	UPDATE file SET geomtype = 0 WHERE id = NEW.fileid;
        RETURN NEW;
    END;
$func_filepoint_setgeomtype$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_filepoint_setgeomtype AFTER INSERT ON file_point
    FOR EACH ROW EXECUTE PROCEDURE func_filepoint_setgeomtype();

--------------
-- Automatically set the file.geomtype whenever a file is linked to file_line
CREATE OR REPLACE FUNCTION func_fileline_setgeomtype() RETURNS trigger AS $func_fileline_setgeomtype$
    BEGIN
	UPDATE file SET geomtype = 1 WHERE id = NEW.fileid;
        RETURN NEW;
    END;
$func_fileline_setgeomtype$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_fileline_setgeomtype AFTER INSERT ON file_line
    FOR EACH ROW EXECUTE PROCEDURE func_fileline_setgeomtype();    
 
