package demo.amastigote.com.djimobilecontrol.FlightModuleUtil;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Switch;

import java.util.Set;

import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleAlertDialog;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightAssistant;

/**
 * Created by hwding on 4/12/17.
 */

public class FlightAssistantManager {
    private static final Object lock = new Object();
    private static FlightAssistant fa;

    private FlightAssistantManager() {
    }

    public synchronized static void handleSettingsChange(Switch aSwitch, Context context) {
        aSwitch.setEnabled(false);
        boolean b = aSwitch.isChecked();

        switch ((Type) aSwitch.getTag()) {
            case CA:
                fa.setCollisionAvoidanceEnabled(b, new CompletionCallback(aSwitch, context));
                break;
            case UA:
                fa.setUpwardsAvoidanceEnabled(b, new CompletionCallback(aSwitch, context));
                break;
            case AOA:
                fa.setActiveObstacleAvoidanceEnabled(b, new CompletionCallback(aSwitch, context));
                break;
            case VAP:
                fa.setVisionAssistedPositioningEnabled(b, new CompletionCallback(aSwitch, context));
                break;
            case PL:
                fa.setPrecisionLandingEnabled(b, new CompletionCallback(aSwitch, context));
                break;
            case LP:
                fa.setLandingProtectionEnabled(b, new CompletionCallback(aSwitch, context));
                break;
        }
    }

    public synchronized static void initSettings(Set<Switch> switches, Context context, View dialog_content) {
        if (fa == null) {
            synchronized (lock) {
                if (fa == null) {
                    fa = FlightControllerManager.getInstance().getFlightAssistant();
                }
            }
        }

        if (fa == null) {
            SimpleAlertDialog.showException(context, new Exception("No flight assistant available!"));
        } else {
            for (final Switch e : switches) {
                switch ((Type) e.getTag()) {
                    case CA:
                        fa.getCollisionAvoidanceEnabled(new CompletionBooleanCallback(e));
                        break;
                    case UA:
                        fa.getUpwardsAvoidanceEnabled(new CompletionBooleanCallback(e));
                        break;
                    case AOA:
                        fa.getActiveObstacleAvoidanceEnabled(new CompletionBooleanCallback(e));
                        break;
                    case VAP:
                        fa.getVisionAssistedPositioningEnabled(new CompletionBooleanCallback(e));
                        break;
                    case PL:
                        fa.getPrecisionLandingEnabled(new CompletionBooleanCallback(e));
                        break;
                    case LP:
                        fa.getLandingProtectionEnabled(new CompletionBooleanCallback(e));
                }
            }

            new AlertDialog.Builder(context)
                    .setTitle("Configure Flight Assistant")
                    .setCancelable(false)
                    .setPositiveButton("ok", null)
                    .setView(dialog_content)
                    .show();
        }
    }

    public enum Type {
        CA, UA, AOA, VAP, PL, LP
    }

    private static class CompletionBooleanCallback implements CommonCallbacks.CompletionCallbackWith<Boolean> {

        private Switch aSwitch;

        CompletionBooleanCallback(Switch aSwitch) {
            this.aSwitch = aSwitch;
        }

        @Override
        public void onSuccess(Boolean aBoolean) {
            aSwitch.setEnabled(true);
            aSwitch.setChecked(aBoolean);
        }

        @Override
        public void onFailure(DJIError djiError) {
            aSwitch.setEnabled(false);
        }
    }

    private static class CompletionCallback implements CommonCallbacks.CompletionCallback {
        Switch aSwitch;
        Context context;

        CompletionCallback(Switch aSwitch, Context context) {
            this.aSwitch = aSwitch;
            this.context = context;
        }

        @Override
        public void onResult(DJIError e) {
            if (e == null)
                aSwitch.setEnabled(true);
            else {
                aSwitch.setEnabled(true);
                aSwitch.setChecked(!aSwitch.isChecked());
                SimpleAlertDialog.showDJIError(context, e);
            }
        }
    }
}
