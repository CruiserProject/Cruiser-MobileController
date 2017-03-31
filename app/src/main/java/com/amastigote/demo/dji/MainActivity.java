package com.amastigote.demo.dji;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ToggleButton;

import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener {
    private static BaseProduct baseProduct;

    /*
        UI components
     */
    private Button takeOffButton;
    private Button landButton;
    private Button captureButton;
    private ToggleButton recordToggleButton;
    private SimpleProgressDialog startUpInfoDialog;
    private TextureView videoTextureView;

    private MissionControl missionControl;
    private DJICodecManager djiCodecManager;
    private DJISDKManager.SDKManagerCallback sdkManagerCallback
            = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError djiError) {
            if (djiError.toString().equals(DJISDKError.REGISTRATION_SUCCESS.toString())) {
                startUpInfoDialog.switchMessage("Waiting for aircraft");
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                SimpleAlertDialog.show(
                        MainActivity.this,
                        false,
                        "API Key Validation Error",
                        "Contact the developers or check your network connection",
                        new SimpleDialogButton("quit", (x, y) -> System.exit(0))
                );
            }
        }

        @Override
        public void onProductChange(BaseProduct previousProd, BaseProduct presentProd) {
            baseProduct = presentProd;
            if (baseProduct != null && baseProduct.isConnected()) {
//                baseProduct.setDJIBaseProductListener(...);
                startUpInfoDialog.dismiss();

                initMissionControl();

                SimpleAlertDialog.show(
                        MainActivity.this,
                        false,
                        "Product Connected",
                        "Present product is " + baseProduct.getModel().getDisplayName(),
                        new SimpleDialogButton("ok", null)
                );

                try {
                    VideoFeeder.getInstance()
                            .getVideoFeeds().get(0)
                            .setCallback((videoBuffer, size) -> {
                                if (djiCodecManager != null)
                                    djiCodecManager.sendDataToDecoder(videoBuffer, size);
                            });
                } catch (Exception e) {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            false,
                            "Exception",
                            e.toString(),
                            new SimpleDialogButton("ok", null)
                    );
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        takeOffButton = (Button) findViewById(R.id.takeoff_btn);
        landButton = (Button) findViewById(R.id.land_btn);
        captureButton = (Button) findViewById(R.id.capture_btn);
        recordToggleButton = (ToggleButton) findViewById(R.id.record_tgbtn);

        takeOffButton.setOnClickListener((view -> {
            //todo check whether there are any timeline elements in the timeline
//            missionControl.scheduleElement(new TakeOffAction());
            missionControl.startElement(new TakeOffAction());
        }));

        landButton.setOnClickListener((view -> {
            //todo check whether there are any timeline elements in the timeline
            missionControl.startElement(new GoHomeAction());

        }));

        captureButton.setOnClickListener((view -> {
            baseProduct.getCamera()
                    .setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, null);
            baseProduct.getCamera()
                    .startShootPhoto(null);
        }));

        recordToggleButton.setOnClickListener((view -> {
            if (recordToggleButton.isChecked()) {
                baseProduct.getCamera().startRecordVideo(null);
            } else {
                baseProduct.getCamera().stopRecordVideo(null);
            }
        }));

        videoTextureView = (TextureView) findViewById(R.id.texture_view);
        videoTextureView.setSurfaceTextureListener(this);

        startUpInfoDialog = new SimpleProgressDialog(this, "Validating API key");

        startUpInfoDialog.show();
        DJISDKManager.getInstance().registerApp(this, sdkManagerCallback);
    }

    /*
        implements TextureView.SurfaceTextureListener
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (djiCodecManager == null)
            djiCodecManager = new DJICodecManager(this, surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (djiCodecManager != null) {
            djiCodecManager.cleanSurface();
            djiCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void initMissionControl() {
        missionControl = MissionControl.getInstance();
//        missionControl = DJISDKManager.getInstance().getMissionControl();
    }
}
