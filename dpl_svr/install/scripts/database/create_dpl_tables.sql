\connect dopple;

---------------------------------------------------------
-- Table holds image file information
--DROP TABLE image;

CREATE TABLE image
(
	id 				serial NOT NULL,
	name 			character varying(255) NOT NULL,
	statusid	 	smallint,
	description		character varying(512),
	createdate		timestamp without time zone DEFAULT NOW() ,
	lastcloned		timestamp without time zone, 
	CONSTRAINT 		image_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE image OWNER TO dpladmin;

---------------------------------------------------------
-- Table: buildplan
-- DROP TABLE buildplan;

CREATE TABLE buildplan
(
	id 				serial NOT NULL,
	name 			character varying(255) NOT NULL,
	description		character varying(512),
	status			smallint,
	imageid			integer,
	storagelocid	integer,
	domainname		character varying(255),
	baseip			character varying(16),
	antcmd			character varying(512),	
	imgcfg			text, -- replaces image_changes.cfg,
	createdate		timestamp without time zone DEFAULT now (), 
	CONSTRAINT 		buildplan_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
ALTER TABLE buildplan OWNER TO dpladmin;

---------------------------------------------------------
-- Table: demo_buildplan
-- DROP TABLE demo_buildplan;

CREATE TABLE demo_buildplan
(
	buildplanid 	integer NOT NULL,
	startdate		timestamp without time zone DEFAULT now(),
	enddate			timestamp without time zone,
	createdate		timestamp without time zone DEFAULT now (), 
	CONSTRAINT 		demo_buildplan_pkey PRIMARY KEY (buildplanid)
)
WITH (OIDS=FALSE);
ALTER TABLE demo_buildplan OWNER TO dpladmin;

---------------------------------------------------------
-- Table holds Virtual Machine info
--DROP TABLE domain;

CREATE TABLE domain
(
	id				serial NOT NULL,
	name			character varying(255) NOT NULL,
	description		character varying(4000),
	xmlpath			character varying(512),
	imageid			integer,
	buildplanid		integer,
	status			smallint default 0,
	type			smallint default -1,
	runnable		boolean default true,
	startdate		timestamp without time zone DEFAULT now(),
	enddate			timestamp without time zone,
	createdate		timestamp without time zone DEFAULT now(),
	ipaddress		character varying(64),
  dirname     character varying(255),
  filename    character varying(255),
	CONSTRAINT		domain_pkey PRIMARY KEY (id),
	CONSTRAINT		domain_unq_name UNIQUE (name)
)
WITH (OIDS=FALSE);
ALTER TABLE domain OWNER TO dpladmin;

---------------------------------------------------------
-- Table: image_file
-- DROP TABLE image_file;

CREATE TABLE image_file
(
	fileid			integer,
	imageid			integer,
	createdate		timestamp without time zone DEFAULT NOW(), 
	CONSTRAINT		image_file_pkey PRIMARY KEY (fileid, imageid)
)
WITH (OIDS=FALSE);
ALTER TABLE image_file OWNER TO dpladmin;


---------------------------------------------------------
-- Table: buildplan_file
-- DROP TABLE buildplan_file;
-- file types (build.xml, image upload, domain rcps )

CREATE TABLE buildplan_file
(
  buildplanid		integer NOT NULL,
  fileid			integer NOT NULL,
  createdate		timestamp without time zone DEFAULT NOW() ,
  CONSTRAINT buildplan_file_pkey PRIMARY KEY (buildplanid, fileid)
)
WITH (OIDS=FALSE);
ALTER TABLE buildplan_file OWNER TO dpladmin;


---------------------------------------------------------
--DROP TABLE macip;
-- Table holds mac address and ip information

-- DROP TABLE macip;

CREATE TABLE macip
(
  id			serial NOT NULL,
  ipaddress		character varying(32) NOT NULL,
  macaddress	character varying(32) NOT NULL,
  domain		character varying(255),
  status		smallint NOT NULL DEFAULT 0,
  fwport 		integer, 
  CONSTRAINT 	macip_pkey PRIMARY KEY (id),
  CONSTRAINT 	macip_unq_ip UNIQUE (ipaddress),
  CONSTRAINT	domain_unq_name UNIQUE (domain)  
)
WITH (OIDS=FALSE);
ALTER TABLE macip OWNER TO dpladmin;

---------------------------------------------------------
-- Table: usr_domain
-- DROP TABLE usr_domain;

CREATE TABLE usr_domain
(
  usrid integer NOT NULL,
  domainid integer NOT NULL,
  createdate timestamp without time zone DEFAULT now(),
  CONSTRAINT usr_domain_pkey PRIMARY KEY (usrid, domainid)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE usr_domain
  OWNER TO dpladmin;



---------------------------------------------------------

INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (82, 0, 1, 0, 1, 'domain', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (83, 0, 1, 0, 1, 'image', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (85, 0, 1, 0, 1, 'buildplan', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (86, 0, 1, 0, 1, 'buildplan_file', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (87, 0, 1, 0, 1, 'macip', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (90, 0, 1, 0, 1, 'image_file', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (92, 0, 1, 0, 1, 'usr_domain', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (95, 0, 1, 0, 0, 'tstate', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (96, 0, 1, 0, 1, 'domain_image', NULL);


INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 82, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 83, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 84, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 85, true, true, true, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 86, true, true, false, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 87, false, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 90, false, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (2, 96, true, true, true, true, false);

--- Create the Tenant and Guest links
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (93, 0, 1, 0, 0, 'guest', NULL);
INSERT INTO entity (id, loadtime, status, entitytype, indatabase, name, description) VALUES (94, 0, 1, 0, 0, 'tenant', NULL);
INSERT INTO ugroup (id, name, description) VALUES (4, 'Demo Account', 'Account for users to use the Dopple demo system');

INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (4, 92, false, true, true, false, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (4, 93, true, true, false, true, false);
INSERT INTO ugroup_entity (ugroupid, entityid, createable, readable, updateable, deleteable, executable) VALUES (4, 94, true, true, true, true, false);

SELECT setval('entity_id_seq', (SELECT MAX(id+1) FROM entity));

