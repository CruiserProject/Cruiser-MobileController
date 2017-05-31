package demo.amastigote.com.djimobilecontrol.ConverterUtil;

/**
 * Created by Trafalgar on 2017/5/31.
 */

public class LLDistanceConverter {
    private static final double R = 6370996.81;

    public static double LL2Distance(double lat1, double lng1, double lat2, double lng2) {
        return R * Math.acos(Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(lng1 * Math.PI / 180 - lng2 * Math.PI / 180) +
                Math.sin(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180));
    }
}
