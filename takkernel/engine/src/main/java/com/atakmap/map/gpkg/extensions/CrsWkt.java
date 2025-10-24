package com.atakmap.map.gpkg.extensions;

import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.lang.Objects;
import com.atakmap.map.gpkg.GeoPackage;

public final class CrsWkt {
    public final static String NAME = "gpkg_crs_wkt";
    public final static String DEFINITION = "http://www.geopackage.org/spec120/#extension_crs_wkt";
    public final static String COLUMN = "definition_12_063";

    public static class CRS extends GeoPackage.SRS {
        public String definition_12_063 = "undefined";
    }

    CrsWkt() {}

    public static void insertExtension(GeoPackage gpkg) {
        // check if extension already exists
        final CrsWkt extension = getPackageExtension(gpkg);
        if(extension != null)
            return;

        // insert the extension
        gpkg.insertExtension(new GeoPackage.ExtensionsRow.Builder(NAME, DEFINITION)
                        .setTable("gpkg_spatial_ref_sys")
                        .setColumnName(COLUMN)
                        .setScope(GeoPackage.ScopeType.READ_WRITE)
                .build(),
                false);

        // insert the column
        StatementIface stmt = null;
        try {
            stmt = gpkg.getDatabase().compileStatement("ALTER TABLE gpkg_spatial_ref_sys ADD COLUMN " + COLUMN + " TEXT DEFAULT 'undefined'");

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    public static CrsWkt getPackageExtension(GeoPackage gpkg) {
        for(GeoPackage.ExtensionsRow extension : gpkg.getPackageExtensions()) {
            if(Objects.equals(extension.extension_name, NAME) &&
                    Objects.equals(extension.table_name, "gpkg_spatial_ref_sys") &&
                    Objects.equals(extension.column_name, COLUMN) &&
                    Objects.equals(extension.definition, extension.definition)) {

                return new CrsWkt();
            }
        }
        return null;
    }

    public boolean insertSRS(GeoPackage gpkg, CrsWkt.CRS crs) {
        QueryIface query;

        // check for already existing
        query = null;
        try
        {
            query = gpkg.getDatabase().compileQuery("SELECT 1 FROM gpkg_spatial_ref_sys WHERE srs_id = ? LIMIT 1");
            query.bind(1, crs.srs_id);
            if (query.moveToNext())
                return false;
        } finally
        {
            if (query != null)
                query.close();
        }

        StatementIface stmt = null;
        try
        {
            stmt = gpkg.getDatabase().compileStatement("INSERT INTO gpkg_spatial_ref_sys (srs_id, definition, description, organization, organization_coordsys_id, srs_name, " + COLUMN + ") VALUES(?, ?, ?, ?, ?, ?, ?)");
            stmt.bind(1, crs.srs_id);
            stmt.bind(2, crs.definition);
            stmt.bind(3, crs.description);
            stmt.bind(4, crs.organization);
            stmt.bind(5, crs.organization_coordsys_id);
            stmt.bind(6, crs.srs_name);
            stmt.bind(7, crs.definition_12_063);

            stmt.execute();

            return true;
        } finally
        {
            if (stmt != null)
                stmt.close();
        }
    }
}
