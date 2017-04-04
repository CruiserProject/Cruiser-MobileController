package com.amastigote.demo.dji.CoordinationUtil;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

/**
 * Created by hwding on 4/4/17.
 */

public class CoordinationConverter {
    private static final CoordinateConverter coordinateConverter = new CoordinateConverter();
    private static final double PI = 3.1415926535897932384626;
    private static final double XPI = PI * 3000 / 180;
    private static double A = 6378245.0;
    private static double EE = 0.00669342162296594323;

    private CoordinationConverter() {
    }

    public synchronized static LatLng GPS2BD09(LatLng latLng) {
        synchronized (coordinateConverter) {
            return coordinateConverter.from(CoordinateConverter.CoordType.GPS).coord(latLng).convert();
        }
    }

    public static LatLng BD092GPS84(LatLng latLng) {
        return GCJ022GPS84(BD092GCJ02(latLng));
    }

    public static void init() {
        coordinateConverter.from(CoordinateConverter.CoordType.GPS);
    }

    public static LatLng BD092GCJ02(LatLng latLng) {
        double x = latLng.longitude - 0.0065, y = latLng.latitude - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * XPI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * XPI);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return new LatLng(gg_lat, gg_lon);
    }

    private static LatLng GCJ022GPS84(LatLng latLng) {
        LatLng latLngTransformed;

        if (isOutOfChina(latLng.latitude, latLng.longitude)) {
            latLngTransformed = new LatLng(latLng.latitude, latLng.longitude);
        } else {
            double dLat = transformLat(latLng.longitude - 105.0, latLng.latitude - 35.0);
            double dLon = transformLon(latLng.longitude - 105.0, latLng.latitude - 35.0);
            double radLat = latLng.latitude / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
            double mgLat = latLng.latitude + dLat;
            double mgLon = latLng.longitude + dLon;
            latLngTransformed = new LatLng(mgLat, mgLon);
        }

        double longitude = latLng.longitude * 2 - latLngTransformed.longitude;
        double latitude = latLng.latitude * 2 - latLngTransformed.latitude;
        return new LatLng(latitude, longitude);
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    private static boolean isOutOfChina(double lat, double lon) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }
}
