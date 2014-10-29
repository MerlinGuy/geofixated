/* Accepts table name, verifies not in exclude list, deletes entry by ID value */
CREATE OR REPLACE FUNCTION f_delete_entity(p_tab_nm TEXT) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	e.id, t.table_name, t.table_schema FROM information_schema.tables t
	INNER JOIN entity e
		ON (t.table_name = e.name)
	WHERE ((t.table_schema NOT IN ('pg_catalog', 'information_schema'))
		AND (t.table_name NOT IN (SELECT tablename from entityignore))
		AND t.table_name = p_tab_nm)
	LOOP
	BEGIN
		DELETE FROM entity 
			WHERE id=c_meta.id 
			AND name=c_meta.table_name;

		BEGIN
			DELETE FROM ugroup_entity WHERE entityid=c_meta.id;

			EXCEPTION WHEN OTHERS THEN
				RETURN 'Failed to delete '||c_meta.table_name||' from ugroup_entity : '||sqlerrm;
		END;

		EXCEPTION WHEN OTHERS THEN
			RETURN 'Failed to delete '||c_meta.table_name||' from entity : '||sqlerrm;
	END;
  END LOOP;

	RETURN NULL;
  	
END;
$$ LANGUAGE plpgsql;


/* Accepts table name, verifies not in exclude list, inserts if entry does not already exist */
CREATE OR REPLACE FUNCTION f_insert_entity(p_tab_nm TEXT) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	e.id, t.table_name, t.table_schema FROM information_schema.tables t
	LEFT OUTER JOIN entity e
		ON (t.table_name = e.name)
	WHERE ((t.table_schema NOT IN ('pg_catalog', 'information_schema'))
		AND (t.table_name NOT IN (SELECT tablename from entityignore))
		AND t.table_name = p_tab_nm)
	LOOP

	IF c_meta.id IS NULL and c_meta.table_name IS NOT NULL THEN
		BEGIN
			INSERT INTO entity 
				(name, description) 
				VALUES (p_tab_nm, NULL);

			EXCEPTION WHEN OTHERS THEN
				RETURN 'Failed to insert '||c_meta.table_name||' into entity : '||sqlerrm;
		END;
  END IF;

  END LOOP;

	RETURN NULL;
  	
END;
$$ LANGUAGE plpgsql;


/* Accepts table name and column values, verifies not in exclude list, */
/* inserts entry with passed values or default if NULL */
CREATE OR REPLACE FUNCTION f_insert_entity_special(p_tab_nm TEXT, p_loadtime INTEGER, p_status INTEGER, p_entitytype INTEGER, p_indatabase INTEGER) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	tab_nm FROM
	(SELECT CAST(p_tab_nm as TEXT) tab_nm) AS holdtable
	LEFT OUTER JOIN information_schema.tables t
		ON (t.table_name = tab_nm)
	LOOP

	IF c_meta.tab_nm NOT IN ('pg_catalog', 'information_schema')
		AND (c_meta.tab_nm NOT IN (SELECT tablename from entityignore)) THEN
		BEGIN
			INSERT INTO entity 
				(name, description, loadtime, status, entitytype, indatabase) 
				SELECT p_tab_nm, NULL, coalesce(p_loadtime,0), coalesce(p_status,0), coalesce(p_entitytype,0), coalesce(p_indatabase,0);

			EXCEPTION WHEN OTHERS THEN
				RETURN 'Failed to insert '||p_tab_nm||' into entity : '||sqlerrm;
		END;
	END IF;

  END LOOP;

	RETURN NULL;
  	
END;
$$ LANGUAGE plpgsql;


/* Queries for geof tables, loops through cursor and calls f_insert_entity */
CREATE OR REPLACE FUNCTION f_populate_entity() 
RETURNS TEXT AS $$
DECLARE

	c_meta record;
	v_ins_result text;

BEGIN
	FOR c_meta IN SELECT
	t.table_name, t.table_schema 
	FROM information_schema.tables t
	WHERE (t.table_schema NOT IN ('pg_catalog', 'information_schema'))
	ORDER BY t.table_name
	LOOP

		SELECT f_insert_entity(c_meta.table_name) INTO v_ins_result;

  END LOOP;

	RETURN NULL;
  
END;
$$ LANGUAGE plpgsql;


/* Accepts table name, verifies not in exclude list, deletes all matching entityid's */
CREATE OR REPLACE FUNCTION f_force_delete_entityf(p_tab_nm TEXT) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	e.id, e.name
	FROM entity e
	INNER JOIN information_schema.tables t
		ON (t.table_name = e.name)
	WHERE e.name = p_tab_nm
		AND ((t.table_schema NOT IN ('pg_catalog', 'information_schema'))
		AND (t.table_name NOT IN (SELECT tablename from entityignore)))
	LOOP
	
	IF c_meta.id IS NOT NULL THEN
		BEGIN
			DELETE FROM entityfield WHERE entityid=c_meta.id;

			EXCEPTION WHEN OTHERS THEN
				RETURN 'Failed to delete from '||c_meta.name||' : '||sqlerrm;
		END;
  END IF;

  END LOOP;

	RETURN NULL;

END;
$$ LANGUAGE plpgsql;


/* Accepts table name, verifies not in exclude list and column physically exists, */
/* deletes entry by entityid and fieldname */
CREATE OR REPLACE FUNCTION f_delete_entityf(p_tab_nm TEXT) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;
	c_colmeta record;

BEGIN
	FOR c_meta IN SELECT
	e.id, e.name, t.table_schema
	FROM entity e
	LEFT OUTER JOIN information_schema.tables t
		ON (e.name = t.table_name)
	WHERE e.name = p_tab_nm
	AND (e.name NOT IN (SELECT tablename from entityignore))
	LOOP

	IF c_meta.id IS NOT NULL 
		AND coalesce(c_meta.table_schema,'NOTFOUND') NOT IN ('pg_catalog', 'information_schema') THEN
			FOR c_colmeta IN SELECT
			cf.fieldname, c.column_name
			FROM entityfield cf
			INNER JOIN entity e
				ON (e.id = cf.entityid)
			LEFT OUTER JOIN information_schema.columns c
				ON (e.name=c.table_name
				AND c.column_name=cf.fieldname)
			WHERE e.name=c_meta.name
			LOOP

			IF c_colmeta.fieldname IS NOT NULL AND c_colmeta.column_name IS NULL THEN
				BEGIN
					DELETE FROM entityfield WHERE entityid=c_meta.id
						AND fieldname=c_colmeta.fieldname;

				EXCEPTION WHEN OTHERS THEN
					RETURN 'Failed to delete '||c_meta.table_name||' from entity : %'||sqlerrm;
				END;		
			END IF;
			
			END LOOP;
		
  END IF;

  END LOOP;

	RETURN NULL;

END;
$$ LANGUAGE plpgsql;


/* Queries entityfieldname rows with no matching entity.id and deletes entries */
CREATE OR REPLACE FUNCTION f_purge_entityf_orphans() 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	 f.entityid
		FROM entityfield f
		WHERE NOT EXISTS
			(SELECT e.id FROM entity e WHERE f.entityid=e.id)
	LOOP
		BEGIN
			DELETE FROM entityfield
			WHERE entityid=c_meta.entityid;
			
			EXCEPTION WHEN OTHERS THEN
				RETURN 'Failed to purge '||c_meta.entityid||' from entityfield: '||sqlerrm;
		END;
  END LOOP;

	RETURN NULL;
  
END;
$$ LANGUAGE plpgsql;


/* Accepts table name, collects meta-data, inserts if entry does not already exist */
CREATE OR REPLACE FUNCTION f_insert_entityf(p_tab_nm TEXT) 
RETURNS TEXT AS $$
DECLARE

	c_meta record;

BEGIN
	FOR c_meta IN SELECT
	 e.id,
	 c.column_name fieldname,
	 CASE WHEN k.ordinal_position is NULL THEN FALSE ELSE TRUE END isdefault,
	 CASE WHEN k.constraint_name like '%pkey%' THEN TRUE ELSE FALSE END ispkey,
	 data_type,
	 jenum, 
	 CASE WHEN is_nullable = 'YES' THEN FALSE ELSE TRUE END isrequired,
	 CASE WHEN (is_nullable = 'NO' AND data_type like '%int%' AND column_default like '%nextval%') THEN TRUE ELSE FALSE END isauto,
	 FALSE isspatial,
	 FALSE istemporal
		FROM information_schema.columns c
		LEFT OUTER JOIN jdbc_datatype d
			ON (c.data_type = d.pname)
		LEFT OUTER JOIN entity e
			ON (c.table_name = e.name)
		LEFT OUTER JOIN information_schema.key_column_usage k
			ON (c.table_name=k.table_name and c.column_name=k.column_name)
		WHERE c.table_name = p_tab_nm
			AND ((c.table_schema NOT IN ('pg_catalog', 'information_schema'))
			AND (c.table_name NOT IN (SELECT tablename from entityignore)))
	LOOP

		IF c_meta.id IS NOT NULL THEN
			BEGIN
					INSERT INTO entityfield
					(entityid, fieldname, isdefault, ispkey, datatype,
					 isrequired, isauto, isspatial, istemporal) 
					values(c_meta.id, c_meta.fieldname, c_meta.isdefault, c_meta.ispkey,
					c_meta.jenum, c_meta.isrequired, c_meta.isauto, 
					FALSE, FALSE);

				EXCEPTION WHEN OTHERS THEN
					RETURN 'Failed to insert ID '||c_meta.id||' into entityfield '||sqlerrm;		
			END;
		END IF;

  END LOOP;

	RETURN NULL;
  
END;
$$ LANGUAGE plpgsql;


/* Queries for geof tables, loops through cursor and calls f_insert_entityf */
CREATE OR REPLACE FUNCTION f_populate_entityf() 
RETURNS TEXT AS $$
DECLARE

	c_meta record;
	v_ins_result text;

BEGIN
	FOR c_meta IN SELECT
	e.name 
	FROM entity e
	INNER JOIN information_schema.tables t
	ON (e.name = t.table_name)
	WHERE (t.table_schema NOT IN ('pg_catalog', 'information_schema'))
	ORDER BY t.table_name
	LOOP

		SELECT f_insert_entityf(c_meta.name) INTO v_ins_result;

  END LOOP;

	RETURN NULL;
  
END;
$$ LANGUAGE plpgsql;


/* Accepts user group name, entity name, and CRUDE arguments and inserts entry into ugroup_entity */
CREATE OR REPLACE FUNCTION f_insert_ugroup_entity(p_ugroup TEXT, p_entity TEXT, 
p_cr BOOLEAN, p_read BOOLEAN, p_upd BOOLEAN, p_del BOOLEAN, p_exec BOOLEAN) 
RETURNS TEXT AS $$
DECLARE

BEGIN

	BEGIN
		INSERT INTO ugroup_entity 
			(ugroupid, entityid, createable, readable, updateable, deleteable, executable) 
			SELECT (SELECT id FROM ugroup WHERE name=p_ugroup), (SELECT id FROM entity WHERE name=p_entity), p_cr, p_read, p_upd, p_del, p_exec;

		EXCEPTION WHEN OTHERS THEN
			RETURN 'Failed to insert into ugroup_entity : ugroup '||p_ugroup||' and entity '||p_entity||' : '||sqlerrm;	
	END;
		
	RETURN NULL;
  	
END;
$$ LANGUAGE plpgsql;



