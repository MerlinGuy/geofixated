
\connect geofdb;

\i /usr/share/postgresql/9.1/contrib/postgis-1.5/postgis.sql;
\i /usr/share/postgresql/9.1/contrib/postgis_comments.sql;
\i /usr/share/postgresql/9.1/contrib/postgis-1.5/spatial_ref_sys.sql;
\i ./create_geofdb_tables.sql

GRANT ALL PRIVILEGES ON geometry_columns TO geof;
GRANT ALL PRIVILEGES ON spatial_ref_sys to geof;

