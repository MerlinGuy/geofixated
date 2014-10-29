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
'429e1abc69fc863a6bba5c9d7b10af8bcd976def',
'685b07daaa8f73e849f77cebb89f7b0fdf46c585f8c1d495bbae01a2901d34fec704b4ee5273aac4cdb076b34770286b8cf6ab340d8e5a128817b9c63a8a6c94',
'6d29b6249d92e44afb28caa12f7da44193',
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
INSERT INTO serverconfig VALUES ('maxdbconnections', '25', 'Maximum number of allowed database connections');
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
-- Table: entity
-- DROP TABLE entity;
CREATE TABLE entity (
	id	serial,
	name	varchar(80),
	description character varying(256),
	loadtime	smallint DEFAULT 0,
	status 		int DEFAULT 0,
	entitytype 	smallint DEFAULT 0,
	indatabase int DEFAULT 1,
	PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE entity OWNER TO geof;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.6
-- Dumped by pg_dump version 9.1.9
-- Started on 2013-08-23 15:34:24 MDT

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- TOC entry 2891 (class 0 OID 18591)
-- Dependencies: 170 2892
-- Data for Name: entity; Type: TABLE DATA; Schema: public; Owner: geof
--

INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (73, 0, 1, 1, 0, 'permission', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (74, 0, 1, 1, 0, 'link', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (75, 0, 1, 0, 1, 'usr_notification', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (76, 0, 1, 1, 0, 'version', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (77, 0, 1, 1, 0, 'dbpool', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (78, 0, 1, 1, 0, 'request', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (70, 0, 1, 1, 1, 'ugroup', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (71, 0, 1, 1, 1, 'ugroup_entity', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (72, 0, 1, 1, 1, 'usr_ugroup', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (67, 0, 1, 1, 1, 'notification', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (79, 0, 1, 1, 0, 'configuration', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (80, 0, 1, 1, 1, 'annotation', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (81, 0, 1, 1, 1, 'notification_annotation', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (1, 0, 1, 0, 1, 'usr', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (2, 0, 1, 0, 1, 'serverconfig', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (3, 0, 1, 0, 1, 'entityfield', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (4, 0, 1, 0, 1, 'entity', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (5, 0, 1, 0, 1, 'storageloc', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (12, 0, 1, 0, 1, 'keyword', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (13, 0, 1, 0, 1, 'project', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (15, 0, 1, 0, 1, 'search_keyword', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (16, 0, 1, 0, 1, 'search_project', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (21, 0, 1, 0, 1, 'entitylink', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (23, 0, 1, 0, 1, 'usrconfig', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (30, 0, 1, 0, 1, 'search', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (37, 0, 1, 0, 1, 'dbaction', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (40, 0, 1, 0, 1, 'requestaudit', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (46, 0, 1, 0, 1, 'entityignore', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (48, 0, 1, 0, 1, 'authcode', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (50, 0, 1, 0, 1, 'file', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (51, 0, 1, 0, 1, 'file_keyword', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (52, 0, 1, 0, 1, 'file_project', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (53, 0, 1, 0, 1, 'point', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (54, 0, 1, 0, 1, 'line', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (55, 0, 1, 0, 1, 'linepoint', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (56, 0, 1, 1, 0, 'upload', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (62, 0, 1, 0, 1, 'pulleyupload', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (61, 0, 1, 0, 1, 'file_point', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (60, 0, 1, 0, 1, 'file_line', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (59, 0, 1, 0, 1, 'entitychildren', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (63, 0, 1, 1, 0, 'table', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (64, 0, 1, 1, 0, 'session', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (65, 0, 1, 1, 0, 'logger', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (66, 0, 1, 1, 0, 'taskmgr', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (68, 0, 1, 1, 0, 'encrypt', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (69, 0, 1, 0, 1, 'rsaencryption', NULL);


--
-- TOC entry 2896 (class 0 OID 0)
-- Dependencies: 169
-- Name: entity_entityid_seq; Type: SEQUENCE SET; Schema: public; Owner: geof
--

--SELECT pg_catalog.setval('entity_entityid_seq', 83, true);



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

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.1.6
-- Dumped by pg_dump version 9.1.9
-- Started on 2013-08-23 15:33:00 MDT

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- TOC entry 2892 (class 0 OID 18600)
-- Dependencies: 171 2893
-- Data for Name: entityfield; Type: TABLE DATA; Schema: public; Owner: geof
--

INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (35, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (35, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (35, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (36, 'requestid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (36, 'action', true, true, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (38, 'dbactionid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (38, 'entityid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (38, 'permissionid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (75, 'notificationid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (75, 'usrid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (75, 'readdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (70, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (70, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (70, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'deleteable', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'readable', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'createable', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'ugroupid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'updateable', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'entityid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (71, 'executable', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (72, 'ugroupid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (72, 'usrid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'message', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'level', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'notificationid', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'usrid', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (67, 'type', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'registeredby', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'title', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'registerdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'endoffset', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'longitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'fileid', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'latitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'startoffset', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'type', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'ratioy', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (80, 'ratiox', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (81, 'annotationid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (81, 'notificationid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'clearcode', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (82, 'annotationid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (82, 'fileid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'lastname', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'digest', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'statusid', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'firstname', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'lastattempt', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'loginname', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'initials', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'email', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'attempts', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'notes', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (1, 'salt', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (2, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (2, 'name', true, true, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (2, 'value', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'ispkey', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'isspatial', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'isdefault', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'isauto', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'entityid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'isrequired', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'datatype', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'istemporal', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (3, 'fieldname', true, true, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'id', true, false, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'loadtime', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'status', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'indatabase', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (4, 'entitytype', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'quota', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'filecount', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'canstream', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'systemdir', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'active', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (5, 'usedspace', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'status', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'keyword', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (12, 'createdby', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'status', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (13, 'createdby', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (15, 'searchid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (15, 'keywordid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (16, 'searchid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (16, 'projectid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (21, 'fromentityid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (21, 'entitymapid', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (21, 'toentityid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (23, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (23, 'name', true, true, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (23, 'value', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (23, 'usrid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'minlat', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'minregdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'usekeywords', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'registeredby', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'distance', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'spatialtype', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'mindatetime', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'usetemporal', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'maxlat', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'useprojects', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'status', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'maxlon', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'minlon', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'keywordtype', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'temporaltype', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'registerdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'maxregdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'useworkgroups', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'unitofmeasure', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'maxdatetime', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'usespatial', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (30, 'originalname', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (37, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (37, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (37, 'name', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'result', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'completedate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'actionas', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'rundate', true, false, 93, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'usrid', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'sessionid', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'actionname', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (40, 'requestname', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (46, 'tablename', true, true, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'guid', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'startdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'usedcount', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'lastused', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'maxuses', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'createdby', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'usrid', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (48, 'enddate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'fileext', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'storagelocid', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'viewid', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'status', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'geomtype', true, false, 5, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'registeredby', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'duration', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'filesize', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'registerdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'filename', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'filetype', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'notes', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'checksumval', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (50, 'originalname', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (51, 'keywordid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (51, 'confidence', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (51, 'fileid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (52, 'projectid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (52, 'fileid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'registeredby', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'altitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'geom', true, false, 1111, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'utcdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'longitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'azimuth', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (53, 'latitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'registeredby', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'minlat', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'startdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'description', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'maxlat', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'geom', true, false, 1111, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'enddate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'maxlon', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'minlon', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (54, 'pointcount', true, false, 4, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'distance', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'lineid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'altitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'ordernum', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'geom', true, false, 1111, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'utcdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'longitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'azimuth', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'latitude', true, false, 2, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (55, 'timeoffset', true, false, 7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'guid', true, false, 12, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'filename', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'uploaddate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'filetype', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'usrid', true, false, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'notes', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'success', true, false, -7, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (62, 'originalname', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (61, 'pointid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (61, 'fileid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (60, 'lineid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (60, 'fileid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (59, 'childid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (59, 'parentid', true, true, 4, true, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'id', true, true, 4, true, true, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'pexponent', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'q', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'exponent', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'p', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'createdate', true, false, 93, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'qinv', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'dq', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'dp', true, false, 12, false, false, false, false);
INSERT INTO entityfield (entityid, fieldname, isdefault, ispkey, datatype, isrequired, isauto, isspatial, istemporal) VALUES (69, 'modulus', true, false, 12, false, false, false, false);


-- Table: entityignore
-- DROP TABLE entityignore;
CREATE TABLE entityignore (
	tablename character varying(256),
	primary key (tablename)
)
WITH (OIDS=FALSE);
ALTER TABLE entityignore OWNER TO geof;

INSERT INTO entityignore (tablename) VALUES ('geometry_columns');
INSERT INTO entityignore (tablename) VALUES ('spatial_ref_sys');

-----------------------------------------------------
------------  File Storage Tables  ------------------
-- Table: storageloc
-- DROP TABLE storageloc;
CREATE TABLE storageloc
(
  id serial NOT NULL,
  name character varying(80),
  systemdir character varying(512),
  active boolean default false,
  description character varying(512) default ''::character varying,
  quota integer default 0,
  usedspace numeric(10,3) default 0.0,
  filecount integer default 0,
  canstream boolean default false,
  CONSTRAINT storageloc_pkey PRIMARY KEY (id),
  CONSTRAINT storageloc_uniq_name UNIQUE (name)
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
  id serial NOT NULL,
  guid character varying(40) NOT NULL,
  usrid integer NOT NULL,
  filetype integer NOT NULL,
  uploaddate timestamp with time zone DEFAULT now(),
  originalname character varying(80),
  filename character varying(512),
  notes character varying(512),
  success boolean,
  CONSTRAINT pulleyupload_pkey PRIMARY KEY (id ),
  CONSTRAINT idx_pulleyupload_guid UNIQUE (guid )
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
  id serial NOT NULL,
  name character varying(80),
  minlat numeric(13,8),
  minlon numeric(13,8),
  maxlat numeric(13,8),
  maxlon numeric(13,8),
  mindatetime timestamp without time zone,
  maxdatetime timestamp without time zone,
  distance numeric(12,3),
  unitofmeasure integer,
  status integer NOT NULL,
  description character varying(512),
  spatialtype smallint,
  temporaltype smallint,
  usespatial boolean,
  usetemporal boolean,
  usekeywords boolean,
  keywordtype smallint,
  useprojects boolean,
  useworkgroups boolean,
  registerdate timestamp without time zone DEFAULT now(),
  registeredby integer NOT NULL,
  originalname character varying(80),
  minregdate timestamp without time zone,
  maxregdate timestamp without time zone,
  CONSTRAINT search_pkey PRIMARY KEY (id ),
  CONSTRAINT search_unq_registeredby UNIQUE (name , registeredby )
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
  id serial NOT NULL,
  keyword character varying(512),
  status integer default 1,
  createdate timestamp without time zone default CURRENT_TIMESTAMP,
  createdby integer NOT NULL,
  description character varying(512),
  CONSTRAINT keyword_pkey PRIMARY KEY (id),
  CONSTRAINT keyword_uniq UNIQUE (keyword)
)
WITH (OIDS=FALSE);
ALTER TABLE keyword OWNER TO geof;

-- Table: search_keyword
-- DROP TABLE search_keyword;
CREATE TABLE search_keyword 
(
	searchid	integer,
	keywordid	integer,
	CONSTRAINT search_keyword_pkey PRIMARY KEY (searchid, keywordid)
)
WITH (OIDS=FALSE);
ALTER TABLE search_keyword OWNER TO geof;

-------------------------------------------
-------------------------------------------
-- Table: project
-- DROP TABLE project;
CREATE TABLE project
(
  id serial NOT NULL,
  name character varying(128),
  status integer default 1,
  createdate timestamp without time zone default CURRENT_TIMESTAMP,
  createdby integer NOT NULL,
  description character varying(512),
  CONSTRAINT project_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE project OWNER TO geof;


-- Table: search_project
-- DROP TABLE search_project;
CREATE TABLE search_project 
(
	searchid	integer,
	projectid	integer,
	CONSTRAINT search_project_pkey PRIMARY KEY (searchid, projectid)
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
	requestname character varying(128) NOT NULL,
	actionname  character varying(64) NOT NULL,
	actionas  character varying(64) NOT NULL,
	usrid integer NOT NULL,
	sessionid character varying(128) NOT NULL,
	rundate timestamp DEFAULT current_timestamp NOT NULL,
	completedate timestamp,
	result smallint
)
WITH (OIDS=FALSE);
ALTER TABLE requestaudit OWNER TO geof;

CREATE index requestaudit_rundate_indx on requestaudit (rundate);

------------ Spatial Tables  ---------------------

-- Table: point
-- DROP TABLE point;
CREATE TABLE point
(
  id serial NOT NULL,
  longitude numeric(13,8),
  latitude numeric(13,8),
  utcdate timestamp without time zone,
  altitude numeric(10,3),
  azimuth numeric(10,3),
  registeredby integer default -1,
  CONSTRAINT point_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE point OWNER TO geof;

SELECT AddGeometryColumn('point', 'geom', 4326, 'POINT', 2);

CREATE INDEX point_spatial ON point USING GIST ( geom ); 

--------------
-------- 
CREATE OR REPLACE FUNCTION func_point_validgps() RETURNS trigger AS $func_point_validgps$
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
$func_point_validgps$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_point_validgps BEFORE INSERT OR UPDATE ON point
    FOR EACH ROW EXECUTE PROCEDURE func_point_validgps();

------------------------------------------------
-- Table: line
-- DROP TABLE line;
CREATE TABLE line
(
  id serial NOT NULL,
  pointcount integer default 0,
  minlat numeric(13,8) default 0.0,
  minlon numeric(13,8) default 0.0,
  maxlat numeric(13,8) default 0.0,
  maxlon numeric(13,8) default 0.0,
  startdate timestamp without time zone,
  enddate timestamp without time zone,
  registeredby integer default -1,
  description character varying(512),
  CONSTRAINT line_pkey PRIMARY KEY (id)
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
  lineid integer NOT NULL,
  ordernum integer NOT NULL,
  longitude numeric(13,8),
  latitude numeric(13,8),
  utcdate timestamp without time zone,
  timeoffset real default 0.0,
  altitude numeric(10,3),
  distance numeric(13,3),
  azimuth numeric(10,3),
  geom geometry,
  CONSTRAINT linepoint_pkey PRIMARY KEY (lineid, ordernum),
  CONSTRAINT enforce_dims_geom CHECK (ndims(geom) = 2),
  CONSTRAINT enforce_geotype_geom CHECK (geometrytype(geom) = 'POINT'::text OR geom IS NULL),
  CONSTRAINT enforce_srid_geom CHECK (srid(geom) = 4326)
)
WITH (OIDS=FALSE);
ALTER TABLE linepoint OWNER TO geof;


---------------------------------------------------------
-- Table: file_keyword
-- DROP TABLE file_keyword;
CREATE TABLE file_keyword 
(
	fileid	integer NOT NULL,
	keywordid integer NOT NULL,
	confidence numeric(5,4) default 1.0,
	CONSTRAINT file_keyword_pkey PRIMARY KEY (fileid, keywordid)
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
	CONSTRAINT file_point_pkey PRIMARY KEY (fileid, pointid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_point OWNER TO geof;

---------------------------------------------------------
-- Table: file_line
-- DROP TABLE file_line;

CREATE TABLE file_line
(
	fileid		integer,
	lineid	integer,
	CONSTRAINT file_line_pkey PRIMARY KEY (fileid, lineid)
)
WITH (OIDS=FALSE);
ALTER TABLE file_line OWNER TO geof;

---------------------------------------------------------
-- Table: rsaencryption
-- DROP TABLE rsaencryption;

CREATE TABLE rsaencryption
(
	id serial NOT NULL,
	modulus text,
	exponent character varying(64),
	pexponent text,
	p text,
	q text,
	dp text,
	dq text,
	qinv text,
	createdate timestamp without time zone DEFAULT now(),
	CONSTRAINT rsaencryption_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE rsaencryption OWNER TO geof;

---------------------------------------------------------
-- Table: notification
-- DROP TABLE notification;

CREATE TABLE notification
(
	id serial NOT NULL,
	message text,
	level smallint default 0,
	usrid integer,
	notificationid integer,
	type smallint default 0,
	createdate timestamp without time zone DEFAULT now(),
	CONSTRAINT notification_pkey PRIMARY KEY (id)
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
	readdate timestamp without time zone,
	CONSTRAINT usr_notification_pkey PRIMARY KEY (usrid, notificationid)
)
WITH (OIDS=FALSE);
ALTER TABLE usr_notification OWNER TO geof;

---------------------------------------------------------
-- Table: annotation
-- DROP TABLE annotation;
CREATE TABLE annotation
(
	id serial NOT NULL,
	fileid integer NOT NULL,
	title character varying(256) NOT NULL,
	description character varying(4000),
	registerdate timestamp without time zone DEFAULT now(),
	registeredby integer default -1,
    longitude numeric(13,8),
    latitude numeric(13,8),
    startoffset numeric(13,8),
    endoffset numeric(13,8),
    ratiox numeric(13,8),
    ratioy numeric(13,8),
    type smallint default 0,
	CONSTRAINT annotation_pkey PRIMARY KEY (id)
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
	CONSTRAINT notification_annotation_pkey PRIMARY KEY (notificationid, annotationid)
)
WITH (OIDS=FALSE);
ALTER TABLE notification_annotation OWNER TO geof;


---------------------------------------------------------
-- Table: ugroup
-- DROP TABLE ugroup;
CREATE TABLE ugroup
(
	id serial NOT NULL,
	name character varying(128),
	description character varying(512),
	CONSTRAINT ugroup_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE ugroup OWNER TO geof;


INSERT INTO ugroup (id, name, description) VALUES (2, 'BaseUser', 'Role which allows the user to login into the system.');
INSERT INTO ugroup (id, name, description) VALUES (3, 'Sys Admin', 'Role which allows the user to login into the system.');
INSERT INTO ugroup (id, name, description) VALUES (1, 'Administrator', 'test');

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

INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 48, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 79, false, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 37, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 77, false, true, false, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 68, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 4, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 59, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 3, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 46, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 21, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 50, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 51, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 60, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 61, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 52, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 12, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 54, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 55, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 74, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 65, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 67, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 73, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 53, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 13, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 62, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 78, false, true, false, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 40, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 69, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 30, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 15, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 16, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 2, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 64, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 5, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 63, true, false, false, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 66, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 70, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 71, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 56, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 1, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 23, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 72, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (1, 76, false, true, false, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (3, 77, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (3, 62, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (3, 69, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (3, 5, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (3, 66, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 75, true, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 67, true, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 80, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 81, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 12, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 13, true, true, false, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 15, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 16, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 30, true, true, true, true, true);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 48, false, true, false, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 50, true, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 51, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 52, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 53, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 54, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 55, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 56, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 61, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 60, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 64, true, true, false, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 1, false, true, true, false, false);

---------------------------------------------------------
-- Table: usr_ugroup
-- DROP TABLE usr_ugroup;
CREATE TABLE usr_ugroup
(
	usrid		integer,
	ugroupid	integer,
	CONSTRAINT usr_ugroup_pkey PRIMARY KEY (usrid, ugroupid)
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
	id serial NOT NULL,
	filename character varying(128) NOT NULL,
	fileext character varying(25),
	filesize integer NOT NULL,
	originalname character varying(500),
	filetype integer NOT NULL,
	status integer NOT NULL,
	checksumval character varying(50),
	createdate timestamp without time zone,
	duration integer default 0,
	registerdate timestamp without time zone DEFAULT now(),
	registeredby integer default -1,
	notes character varying(4000),
	storagelocid integer,
	viewid integer default -1,
	geomtype smallint default 0,
	CONSTRAINT file_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE file OWNER TO geof;


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


---------------------------------------------------------
---------------------------------------------------------
--- Create Functions
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
	
-- Automatically set the file.geomtype whenever a file is linked to file_point
CREATE OR REPLACE FUNCTION func_filepoint_setgeomtype() RETURNS trigger AS $func_filepoint_setgeomtype$
    BEGIN
	UPDATE file SET geomtype = 0 WHERE id = NEW.fileid;
        RETURN NEW;
    END;
$func_filepoint_setgeomtype$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_filepoint_setgeomtype AFTER INSERT ON file_point
    FOR EACH ROW EXECUTE PROCEDURE func_filepoint_setgeomtype();


-- Automatically set the file.geomtype whenever a file is linked to file_line
CREATE OR REPLACE FUNCTION func_fileline_setgeomtype() RETURNS trigger AS $func_fileline_setgeomtype$
    BEGIN
	UPDATE file SET geomtype = 1 WHERE id = NEW.fileid;
        RETURN NEW;
    END;
$func_fileline_setgeomtype$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_fileline_setgeomtype AFTER INSERT ON file_line
    FOR EACH ROW EXECUTE PROCEDURE func_fileline_setgeomtype();    
 
