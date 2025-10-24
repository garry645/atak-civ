package com.atakmap.map.layer.raster.nativeimagery;

import com.atakmap.map.layer.raster.mosaic.FilterMosaicDatabase2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;

final class LegacyQueryResAdjustingMosaicDatabase extends FilterMosaicDatabase2
{

    LegacyQueryResAdjustingMosaicDatabase(MosaicDatabase2 impl)
    {
        super(impl);
    }

    @Override
    public Cursor query(QueryParameters params)
    {
        return super.query(filterQueryParams(params));
    }

    static QueryParameters filterQueryParams(QueryParameters params)
    {
        if (params == null)
            return null;
        QueryParameters filtered = new QueryParameters(params);
        filtered.maxGsd /= 2d;
        return filtered;
    }
}
