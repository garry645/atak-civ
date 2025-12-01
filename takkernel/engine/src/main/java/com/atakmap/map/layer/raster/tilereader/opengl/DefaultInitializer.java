package com.atakmap.map.layer.raster.tilereader.opengl;

import com.atakmap.map.layer.raster.DefaultDatasetProjection2;
import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.map.layer.raster.PrecisionImagery;
import com.atakmap.map.layer.raster.PrecisionImageryFactory;
import com.atakmap.map.layer.raster.gdal.GdalDatasetProjection2;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;

import org.gdal.gdal.Dataset;

public class DefaultInitializer implements NodeInitializer
{
    @Override
    public Result init(ImageInfo info, TileReaderFactory.Options opts)
    {
        Result retval = new Result();
        try
        {
            retval.reader = TileReaderFactory.create(info.path, opts);
            if (retval.reader == null)
                throw new IllegalArgumentException("Image not supported");
            do {
                final Dataset ds = retval.reader.getControl(Dataset.class);
                if(ds != null) {
                    retval.imprecise = GdalDatasetProjection2.getInstance(ds);
                    if(retval.imprecise != null)
                        break;
                }

                retval.imprecise = new DefaultDatasetProjection2(
                        info.srid,
                        info.width,
                        info.height,
                        info.upperLeft,
                        info.upperRight,
                        info.lowerRight,
                        info.lowerLeft
                );
            } while(false);
            if (info.precisionImagery)
            {
                PrecisionImagery precise = PrecisionImageryFactory.create(info.path);
                if (precise != null)
                    retval.precise = precise.getDatasetProjection();
            }
        } catch (Throwable t)
        {
            retval.error = t;
        }
        return retval;
    }

    @Override
    public void dispose(Result result)
    {
        if (result == null)
            return;
        if (result.reader != null)
            result.reader.dispose();
        if (result.imprecise != null)
            result.imprecise.release();
        if (result.precise != null)
            result.precise.release();
    }
}
