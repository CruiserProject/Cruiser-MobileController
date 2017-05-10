package demo.amastigote.com.djimobilecontrol.DataUtil;

/**
 * Created by Trafalgar on 2017/5/10.
 */

public class OnboardDataEncoder {
    static byte[] data = new byte[10];

    private static void clearData() {
        for (byte datum : data) {
            datum = 0x00;
        }
    }

    public static byte[] encode(DataType dataType, byte[] coordinations) {
        clearData();
        switch (dataType) {
            case VISUAL_LANDING_START:
                data[0] = 0x01;
                data[1] = 0x01;
                break;
            case VISUAL_LANDING_STOP:
                data[0] = 0x01;
                data[1] = 0x03;
                break;
            case OBJECT_TRACKING_START:
                data[0] = 0x02;
                data[1] = 0x01;
                break;
            case OBJECT_TRACKING_STOP:
                data[0] = 0x02;
                data[1] = 0x03;
                break;
            case OBJECT_TRACKING_VALUE:
                data[0] = 0x02;
                data[1] = 0x11;
                data[2] = coordinations[0];
                data[3] = coordinations[1];
                data[4] = coordinations[2];
                data[5] = coordinations[3];
                break;
        }

        return data;
    }

    public enum DataType {
        VISUAL_LANDING_START,
        VISUAL_LANDING_STOP,
        OBJECT_TRACKING_START,
        OBJECT_TRACKING_STOP,
        OBJECT_TRACKING_VALUE
    }
}
