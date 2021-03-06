package com.camptocamp.android.gis.proj;

import java.util.HashMap;

import com.nutiteq.components.MapPos;
import com.nutiteq.components.Point;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.maps.projections.Projection;

/**
 * Abstract class for doing WGS84 coordinates calculations to map pixels in
 * CH-1903 (EPSG:4149, SwissGrid) and back.
 * 
 */
// http://www.swisstopo.admin.ch/internet/swisstopo/en/home/products/software/products/skripts.html
// http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/swiss_grid.html
// http://spatialreference.org/ref/epsg/4149/

public class CH1903 implements Projection {

    // private static final String TAG = Map.D + "CH1903";
    protected static double MIN_X = 485869.5728;
    protected static double MAX_X = 837076.5648;
    protected static double MIN_Y = 76443.1884;
    protected static double MAX_Y = 299941.7864;

    protected final HashMap<Integer, Double> resolutions;

    protected double yShift;

    
    public CH1903(HashMap<Integer, Double> resolutions) {
        this.resolutions = resolutions;
    }
    
    public void setyShift(double yShift) {
        this.yShift = yShift;
    }

    public double CHxtoPIX(double pt, int zoom) {
        return (pt - MIN_X) / resolutions.get(zoom);
    }

    public double CHytoPIX(double pt, int zoom) {
        return ((MAX_Y - pt) / resolutions.get(zoom)) + yShift;
    }

    public double PIXtoCHx(double px, int zoom) {
        return MIN_X + (px * resolutions.get(zoom));
    }

    public double PIXtoCHy(double px, int zoom) {
        return MAX_Y - ((px - yShift) * resolutions.get(zoom));
    }
    
    public double getResolution(int zoom) {
        return resolutions.get(zoom);
    }
    
    public Point mapPosToWgs(MapPos pos) {
        // Convert from CH1903 to pixel
        // (X and Y are inverted in the CH1903 notation)
        double y_aux = PIXtoCHx((double) pos.getX(), pos.getZoom());
        double x_aux = PIXtoCHy((double) pos.getY(), pos.getZoom());

        // Converts militar to civil and to unit = 1000km
        // Axiliary values (% Bern)
        y_aux = (y_aux - 600000) / 1000000;
        x_aux = (x_aux - 200000) / 1000000;

        // Process lat/long
        double _lat = 16.9023892 + 3.238272 * x_aux - 0.270978 * Math.pow(y_aux, 2) - 0.002528
                * Math.pow(x_aux, 2) - 0.0447 * Math.pow(y_aux, 2) * x_aux - 0.0140
                * Math.pow(x_aux, 3);
        double _long = 2.6779094 + 4.728982 * y_aux + 0.791484 * y_aux * x_aux + 0.1306 * y_aux
                * Math.pow(x_aux, 2) - 0.0436 * Math.pow(y_aux, 3);

        // Unit 10000'' to 1'' and converts seconds to degrees (dec)
        _lat = _lat * 100 / 36;
        _long = _long * 100 / 36;

        // Log.i(TAG + ":mapPosToWgs", "lat=" + _lat + ", long=" + _long);
        return new WgsPoint(_long, _lat).toInternalWgs();
    }

    public MapPos wgsToMapPos(Point pt, int zoom) {
        // Converts degrees dec to sex
        WgsPoint wgs = pt.toWgsPoint();
        double _lat = DECtoSEX(wgs.getLat());
        double _long = DECtoSEX(wgs.getLon());

        // Converts degrees to seconds (sex)
        _lat = SEXtoSEC(_lat);
        _long = SEXtoSEC(_long);

        // Axiliary values (% Bern)
        double lat_aux = (_lat - 169028.66) / 10000;
        double lng_aux = (_long - 26782.5) / 10000;

        // Process X/Y
        double y = 600072.37 + 211455.93 * lng_aux - 10938.51 * lng_aux * lat_aux - 0.36 * lng_aux
                * Math.pow(lat_aux, 2) - 44.54 * Math.pow(lng_aux, 3);
        double x = 200147.07 + 308807.95 * lat_aux + 3745.25 * Math.pow(lng_aux, 2) + 76.63
                * Math.pow(lat_aux, 2) - 194.56 * Math.pow(lng_aux, 2) * lat_aux + 119.79
                * Math.pow(lat_aux, 3);

        // Convert from CH1903 to pixel
        // (X and Y are inverted in CH1903 notation)
        int X = (int) Math.ceil(CHxtoPIX(y, zoom));
        int Y = (int) Math.ceil(CHytoPIX(x, zoom));

        // Log.i(TAG + ":wgsToMapPos", "x=" + X + ", y=" + Y);
        return new MapPos(X, Y, zoom);
    }

    // Convert DEC angle to SEX DMS
    private double DECtoSEX(double angle) {
        // Extract DMS
        double deg = Math.floor(angle);
        double min = Math.floor((angle - deg) * 60);
        double sec = (((angle - deg) * 60) - min) * 60;

        // Result in degrees sex (dd.mmss)
        return deg + (double) min / 100 + (double) sec / 10000;
    }

    // Convert Degrees to seconds
    private double SEXtoSEC(double angle) {
        // Extract DMS
        double deg = Math.floor(angle);
        double min = Math.floor((angle - deg) * 100);
        double sec = (((angle - deg) * 100) - min) * 100;

        // Result in degrees sex (dd.mmss)
        return sec + (double) min * 60 + (double) deg * 3600;
    }
}
