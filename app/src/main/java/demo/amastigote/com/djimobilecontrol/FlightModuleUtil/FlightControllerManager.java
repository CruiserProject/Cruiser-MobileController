package demo.amastigote.com.djimobilecontrol.FlightModuleUtil;

import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * Created by hwding on 4/12/17.
 */

public class FlightControllerManager {
    private static final Object lock = new Object();
    private static FlightController fc = null;

    private FlightControllerManager() {
    }

    public synchronized static FlightController getInstance(BaseProduct baseProduct) {
        // TODO: 2017/4/21 连接另一台飞机后报错。。 
        if (fc == null) {
            synchronized (lock) {
                if (fc == null) {
                    fc = ((Aircraft) baseProduct).getFlightController();
                }
            }
        }

        return fc;
    }

    synchronized static FlightController getInstance() {
        return fc;
    }
}
