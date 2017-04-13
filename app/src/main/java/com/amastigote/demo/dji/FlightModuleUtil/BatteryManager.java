package com.amastigote.demo.dji.FlightModuleUtil;

import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.products.Aircraft;

/**
 * Created by hwding on 4/13/17.
 */

public class BatteryManager {
    private static final Object lock = new Object();
    private static Battery battery = null;

    private BatteryManager() {
    }

    public synchronized static Battery getInstance(BaseProduct baseProduct) {
        if (battery == null) {
            synchronized (lock) {
                if (battery == null) {
                    battery = ((Aircraft) baseProduct).getBattery();
                }
            }
        }

        return battery;
    }
}
