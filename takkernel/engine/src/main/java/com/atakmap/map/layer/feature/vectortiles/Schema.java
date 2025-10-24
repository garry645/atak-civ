package com.atakmap.map.layer.feature.vectortiles;

import com.atakmap.util.Collections2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Schema
{
    final static Schema OMT;
    static {
        Set<String> nameFields = new HashSet<>(Arrays.asList(
            "name:mt",
            "name:pt",
            "name:az",
            "name:id",
            "name:cy",
            "name:rm",
            "name:ko",
            "name:kn",
            "name:ar",
            "name:cs",
            "name:co",
            "name:tr",
            "name_de",
            "name:ro",
            "name:am",
            "name:it",
            "name:ru",
            "name:ml",
            "name:pl",
            "name:ca",
            "name_int",
            "name:hu",
            "name:ka",
            "name:fi",
            "name:da",
            "name:de",
            "name:oc",
            "name:fr",
            "name:mk",
            "name:sl",
            "name:nonlatin",
            "name:fy",
            "name:zh",
            "name:ku",
            "name:ko_rm",
            "name:lv",
            "name:ja",
            "name:lt",
            "name:no",
            "name:kk",
            "name:sv",
            "name:he",
            "name:ja_rm",
            "name:ga",
            "name:br",
            "name:bs",
            "name:lb",
            "name:sr-Latn",
            "name:la",
            "name:sk",
            "name:uk",
            "name:hy",
            "name:be",
            "name_en",
            "name:bg",
            "name:hr",
            "name:sr",
            "name:sq",
            "name:el",
            "name:eo",
            "name:en",
            "name",
            "name:gd",
            "name:ja_kana",
            "name:is",
            "name:th",
            "name:latin",
            "name:eu",
            "name:et",
            "name:nl",
            "name:es"
        ));

        Map<String, Set<String>> omtSchema = new HashMap<>();
        omtSchema.put("water", new HashSet<>(Arrays.asList("brunnel", "class", "intermittent")));
        omtSchema.put("waterway", new HashSet<>(Arrays.asList("brunnel", "intermittent", "class")));
        omtSchema.get("waterway").addAll(nameFields);
        omtSchema.put("landcover", new HashSet<>(Arrays.asList("class", "subclass")));
        omtSchema.put("landuse", new HashSet<>(Arrays.asList("class")));
        omtSchema.put("mountain_peak", new HashSet<>(Arrays.asList("rank", "ele", "ele_ft", "class")));
        omtSchema.get("mountain_peak").addAll(nameFields);
        omtSchema.put("park", new HashSet<>(Arrays.asList("rank", "class")));
        omtSchema.get("park").addAll(nameFields);
        omtSchema.put("boundary", new HashSet<>(Arrays.asList("admin_level", "disputed_name", "disputed", "maritime", "claimed_by")));
        omtSchema.put("aeroway", new HashSet<>(Arrays.asList("ref", "class")));
        omtSchema.put("transportation", new HashSet<>(Arrays.asList("layer", "bicycle", "service", "level", "brunnel", "indoor", "ramp", "horse", "subclass", "surface", "oneway", "foot", "mtb_scale", "class")));
        omtSchema.put("building", new HashSet<>(Arrays.asList("render_min_height", "hide_3d", "colour", "render_height")));
        omtSchema.put("water_name", new HashSet<>(Arrays.asList("intermittent", "class")));
        omtSchema.get("water_name").addAll(nameFields);
        omtSchema.put("transportation_name", new HashSet<>(Arrays.asList("layer", "subclass", "indoor", "network", "ref", "level", "ref_length", "class")));
        omtSchema.get("transportation_name").addAll(nameFields);
        omtSchema.put("place", new HashSet<>(Arrays.asList("rank", "capital", "iso_a2", "class")));
        omtSchema.get("place").addAll(nameFields);
        omtSchema.put("housenumber", new HashSet<>(Arrays.asList("housenumber")));
        omtSchema.put("poi", new HashSet<>(Arrays.asList("layer", "rank", "subclass", "indoor", "level", "class", "agg_stop")));
        omtSchema.get("poi").addAll(nameFields);
        omtSchema.put("aerodrome_label", new HashSet<>(Arrays.asList("ele", "iata", "ele_ft", "icao", "class")));
        omtSchema.get("aerodrome_label").addAll(nameFields);
        OMT = new Schema(omtSchema);
    }

    final static Schema RBT_PHYSICAL;
    static {
        Map<String, Set<String>> schema = new HashMap<>();
        schema.put("builtuparea", new HashSet<>(Arrays.asList("class")));
        schema.put("contour", new HashSet<>(Arrays.asList("elevation", "negative", "nth_line")));
        schema.put("contour_glacier", new HashSet<>(Arrays.asList("elevation", "negative", "nth_line")));
        schema.put("dune", new HashSet<>(Arrays.asList("area", "name", "subclass")));
        schema.put("forest", new HashSet<>());
        schema.put("glacier", new HashSet<>(Arrays.asList("name", "source", "subclass")));
        schema.put("grassland", new HashSet<>());
        schema.put("inland_water_intermittent", new HashSet<>());
        schema.put("marsh", new HashSet<>());
        schema.put("park", new HashSet<>(Arrays.asList("access", "area", "iucn_level", "name", "name_en", "osm_id", "protect_class", "subclass")));
        schema.put("river", new HashSet<>());
        schema.put("swamp", new HashSet<>());
        schema.put("water", new HashSet<>());
        schema.put("waterway", new HashSet<>(Arrays.asList("geom_len", "intermittent", "name", "subclass")));
        RBT_PHYSICAL = new Schema(schema);
    }

    final static Schema RBT_CULTURAL;
    static {
        Map<String, Set<String>> schema = new HashMap<>();
        schema.put("adm0_labels", new HashSet<>(Arrays.asList("adm0_name", "adm0_name1", "area", "gns_deig_cd", "gns_full_name", "gns_name_rank", "ne_abbrev", "ne_formal_en", "ne_name", "ne_name_long", "status_cd", "status_nm")));
        schema.put("adm0_lines", new HashSet<>(Arrays.asList("cc1", "cc2", "country1", "country2", "fid_1", "label", "notes", "rank", "status", "tippecanoe_feature_density", "wld_date", "wld_update", "wld_view")));
        schema.put("adm1_labels", new HashSet<>(Arrays.asList("adm0_id", "adm0_name", "adm0_name1", "adm0_src", "adm1_id", "adm1_name", "adm1_name1", "adm1_name2", "adm1_src", "iso_2", "iso_3", "iso_3_grp", "iso_cd", "region1_cd", "region1_nm", "region2_cd", "region2_nm", "region3_cd", "region3_nm", "src_date", "src_grp", "src_lang", "src_lang1", "src_lang2", "src_lic", "src_lvl", "src_name", "src_name1", "src_update", "src_url", "status_cd", "status_nm", "wld_date", "wld_notes", "wld_update", "wld_view")));
        schema.put("adm1_lines", new HashSet<>(Arrays.asList("adm0_id", "adm0_name", "adm0_name1", "adm0_src", "iso_2", "iso_3", "iso_3_grp", "iso_cd", "region1_cd", "region1_nm", "region2_cd", "region2_nm", "region3_cd", "region3_nm", "src_date", "src_grp", "src_lang", "src_lang1", "src_lang2", "src_lic", "src_lvl", "src_name", "src_name1", "src_update", "src_url", "status_cd", "status_nm", "tippecanoe_feature_density", "wld_date", "wld_notes", "wld_update", "wld_view")));
        schema.put("adm2_labels", new HashSet<>(Arrays.asList("adm0_id", "adm0_name", "adm0_name1", "adm0_src", "adm1_id", "adm1_name", "adm1_name1", "adm1_name2", "adm1_src", "adm2_id", "adm2_name", "adm2_name1", "adm2_name2", "adm2_src", "iso_2", "iso_3", "iso_3_grp", "iso_cd", "region1_cd", "region1_nm", "region2_cd", "region2_nm", "region3_cd", "region3_nm", "src_date", "src_grp", "src_lang", "src_lang1", "src_lang2", "src_lic", "src_lvl", "src_name", "src_name1", "src_update", "src_url", "status_cd", "status_nm", "wld_date", "wld_notes", "wld_update", "wld_view")));
        schema.put("adm2_lines", new HashSet<>(Arrays.asList("adm0_id", "adm0_name", "adm0_name1", "adm0_src", "adm1_id", "adm1_name", "adm1_name1", "adm1_name2", "adm1_src", "iso_2", "iso_3", "iso_3_grp", "iso_cd", "region1_cd", "region1_nm", "region2_cd", "region2_nm", "region3_cd", "region3_nm", "src_date", "src_grp", "src_lang", "src_lang1", "src_lang2", "src_lic", "src_lvl", "src_name", "src_name1", "src_update", "src_url", "status_cd", "status_nm", "tippecanoe_feature_density", "wld_date", "wld_notes", "wld_update", "wld_view")));
        schema.put("aeroway_surface", new HashSet<>(Arrays.asList("aerodrome_type", "amenity", "area", "class", "ele", "faa_code", "iata", "icao", "military", "name", "name_en", "operator", "osm_id", "osm_runway_id", "ourairports_ident", "subclass", "surface")));
        schema.put("airports", new HashSet<>(Arrays.asList("airport_ident", "category", "continent", "elevation_ft", "gid", "iata", "icao", "iso_country", "iso_region", "keywords", "local_code", "municipality", "name", "osm_aerodrome", "osm_aerodrome_area", "osm_runway", "runway_closed", "runway_he_heading", "runway_he_ident", "runway_id", "runway_le_heading", "runway_le_ident", "runway_length_ft", "runway_lighted", "runway_surface", "runway_width_ft", "scheduled_service", "type")));
        schema.put("building", new HashSet<>(Arrays.asList("amenity", "area", "class", "denomination", "government", "height", "office", "osm_id", "religion", "subclass")));
        schema.put("cemetery", new HashSet<>(Arrays.asList("area", "area_part", "class", "contained", "name", "osm_id", "rank", "religion", "subclass")));
        schema.put("cemetery_label", new HashSet<>(Arrays.asList("area", "area_part", "class", "contained", "name", "osm_id", "rank", "religion", "subclass")));
        schema.put("dam_curve", new HashSet<>(Arrays.asList("fclass", "length", "name", "surface")));
        schema.put("dam_label", new HashSet<>(Arrays.asList("dam_srf_crv_intersect", "fclass", "fid", "name", "water_intersect")));
        schema.put("dam_surface", new HashSet<>(Arrays.asList("area", "fclass", "name", "surface")));
        schema.put("ferry", new HashSet<>(Arrays.asList("id", "is_bridge", "is_tunnel", "name", "name_en", "osm_id", "service", "short_name", "subclass", "tags", "usage")));
        schema.put("gadm1_name", new HashSet<>(Arrays.asList("area", "gid_0", "name_1")));
        schema.put("geonames_hydrographic", new HashSet<>(Arrays.asList("area", "class", "name", "osm_intersect")));
        schema.put("grain_elevator", new HashSet<>(Arrays.asList("area", "class", "content", "height", "name", "name_en", "osm_id", "subclass")));
        schema.put("grain_elevator_srf", new HashSet<>(Arrays.asList("area", "class", "content", "height", "name", "name_en", "osm_id", "subclass", "tags")));
        schema.put("heliports", new HashSet<>(Arrays.asList("airport_ident", "continent", "elevation_ft", "gid", "hospital", "iata", "icao", "iso_country", "iso_region", "keywords", "local_code", "municipality", "name", "scheduled_service", "type")));
        schema.put("lock_label", new HashSet<>(Arrays.asList("access", "class", "gate_category", "lock", "name", "name_en", "operator", "osm_id", "seamark_name", "service", "subclass", "tags")));
        schema.put("lock_way", new HashSet<>(Arrays.asList("class", "intermittent", "lock", "name", "name_en", "osm_id", "subclass", "tags")));
        schema.put("ne_water_label", new HashSet<>(Arrays.asList("featurecla", "name")));
        schema.put("osm_road", new HashSet<>(Arrays.asList("brunnel", "geom_len", "layer", "name", "name_len", "osm_id", "ref", "ref_len", "ref_multi", "ref_number", "ref_number_len", "route_type", "subclass")));
        schema.put("pipeline", new HashSet<>(Arrays.asList("location", "name", "osm_id", "substance", "usage")));
        schema.put("populated_places", new HashSet<>(Arrays.asList("capital", "class", "iso_a2", "name", "name_en", "rank")));
        schema.put("port_label", new HashSet<>(Arrays.asList("access", "area", "area_part", "cargo", "class", "contained", "fid", "industrial", "name", "osm_id", "overlap", "port", "port_type", "rank", "subclass")));
        schema.put("port_surface", new HashSet<>(Arrays.asList("access", "area", "area_part", "cargo", "class", "contained", "fid", "industrial", "name", "osm_id", "overlap", "port", "port_type", "rank", "subclass")));
        schema.put("power_station", new HashSet<>(Arrays.asList("area", "generator_method", "generator_output", "generator_plant", "generator_source", "generator_type", "id", "name", "name_en", "operator", "osm_id", "plant_method", "plant_output", "plant_source", "plant_storage", "subclass")));
        schema.put("power_station_label", new HashSet<>(Arrays.asList("area", "generator_method", "generator_output", "generator_plant", "generator_source", "generator_type", "id", "name", "name_en", "operator", "osm_id", "plant_method", "plant_output", "plant_source", "plant_storage", "subclass")));
        schema.put("powerline", new HashSet<>(Arrays.asList("cables", "class", "location", "name", "osm_id", "subclass", "usage", "wires")));
        schema.put("pumping_station", new HashSet<>(Arrays.asList("area", "id", "name", "name_en", "operator", "osm_id", "pumping_station", "subclass", "substance", "substation")));
        schema.put("pumping_station_label", new HashSet<>(Arrays.asList("area", "id", "name", "name_en", "operator", "osm_id", "pumping_station", "subclass", "substance", "substation")));
        schema.put("radar_label", new HashSet<>(Arrays.asList("access", "airmark", "area", "class", "description", "ele", "height", "military", "name", "name_en", "operator", "osm_id", "radar", "service", "subclass", "tower_construction", "tower_type")));
        schema.put("railway", new HashSet<>(Arrays.asList("brunnel", "dps_type", "electrified", "gauge", "geom_len", "name", "network", "osm_id", "ref", "service", "subclass", "tracks")));
        schema.put("railway_station", new HashSet<>(Arrays.asList("area", "class", "id", "name", "operator", "osm_id", "platforms", "service", "station", "subclass")));
        schema.put("railway_station_label", new HashSet<>(Arrays.asList("area", "class", "id", "name", "operator", "osm_id", "platforms", "service", "station", "subclass")));
        schema.put("runway_curve", new HashSet<>(Arrays.asList("aerodrome_id", "class", "ele", "faa_code", "iata", "icao", "length", "lit", "lit_centre", "military", "name", "operator", "osm_id", "ref", "subclass", "surface", "width")));
        schema.put("stadium_labels", new HashSet<>(Arrays.asList("area", "class", "name", "osm_id", "subclass")));
        schema.put("stadium_surface", new HashSet<>(Arrays.asList("area", "class", "name", "osm_id", "subclass")));
        schema.put("us_military_installations", new HashSet<>(Arrays.asList("area", "component", "country", "jointbase", "operstatus", "sitename", "state")));
        schema.put("us_military_installations_labels", new HashSet<>(Arrays.asList("area", "component", "country", "jointbase", "operstatus", "sitename", "state")));
        schema.put("utility_point", new HashSet<>(Arrays.asList("access", "capacity", "class", "content", "generator_method", "generator_plant", "generator_source", "generator_type", "height", "location", "mast_type", "name", "name_en", "operator", "osm_id", "plant_method", "plant_source", "pumping_station", "rotor_diameter", "seamark_name", "seamark_platform_category", "seamark_platform_height", "seamark_pylon_category", "service", "shore", "subclass", "substance", "substation", "tower_construction", "tower_type")));
        schema.put("yard_label", new HashSet<>(Arrays.asList("class", "name", "operator", "osm_id", "ref", "subclass", "usage", "yard_purpose", "yard_size")));
        RBT_CULTURAL = new Schema(schema);
    }

    final static Schema RBT = new Schema(Collections.emptyMap());

    Map<String, Set<String>> schema;

    Schema(Map<String, Set<String>> schema)
    {
        this.schema = new HashMap<>();
        for(Map.Entry<String, Set<String>> layer : schema.entrySet()) {
            this.schema.put(layer.getKey(), Collections.unmodifiableSet(new HashSet<>(layer.getValue())));
        }
    }

    Set<String> getLayerNames()
    {
        return Collections.unmodifiableSet(this.schema.keySet());
    }

    Set<String> getLayerAttributes(String layer)
    {
        return this.schema.get(layer);
    }

    boolean matches(Schema other, boolean matchOnIntersect)
    {
        int layerMatch = 0;
        int layerIntersect = 0;
        int layerMiss = 0;
        for(Map.Entry<String, Set<String>> layer : other.schema.entrySet()) {
            final Set<String> attrs = schema.get(layer.getKey());
            if(attrs == null) {
                layerMiss++;
                continue;
            }
            if(Collections2.containsAny(attrs, layer.getValue()))
                layerIntersect++;
            if(layer.getValue().containsAll(attrs))
                layerMatch++;
        }

        return matchOnIntersect ? (layerIntersect > 0 && layerMiss == 0) : (layerMatch == schema.size());
    }
}
