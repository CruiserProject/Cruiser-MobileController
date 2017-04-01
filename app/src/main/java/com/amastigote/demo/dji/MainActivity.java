package com.amastigote.demo.dji;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;

import java.util.HashSet;
import java.util.Set;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.products.Aircraft;
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
    private RelativeLayout relativeLayoutMain;
    private TextureView videoTextureView;

    private MapView mapView;
    private LinearLayout linearLayoutForMapView;
    private LinearLayout mapViewPanel;
    private boolean isMapPanelFocused;
    private Button mapPanelCancelButton;
    private Button mapPanelUndoButton;
    private Button mapPanelStartButton;
    private Button mapPanelStopButton;
    private Set<Button> mapPanelButtonSet;
    private BaiduMap baiduMap;

    private FlightController flightController;
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
                initFlightController();

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
                    SimpleAlertDialog.showException(MainActivity.this, e);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
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
                            Manifest.permission.WRITE_SETTINGS,
                            Manifest.permission.GET_TASKS
                    }
                    , 1);
        }

        linearLayoutForMapView = (LinearLayout) findViewById(R.id.ll_for_map);
        mapViewPanel = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.map_panel, linearLayoutForMapView);
        mapViewPanel.setOnClickListener((view) -> switchMapPanelFocus());
        mapView = (MapView) mapViewPanel.findViewById(R.id.mv_mapview);
        mapPanelCancelButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_cancel);
        mapPanelUndoButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_undo);
        mapPanelStartButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_start);
        mapPanelStopButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_stop);
        mapPanelButtonSet = new HashSet<Button>() {{
            add(mapPanelCancelButton);
            add(mapPanelUndoButton);
            add(mapPanelStartButton);
            add(mapPanelStopButton);
        }};

        isMapPanelFocused = false;

        takeOffButton = (Button) findViewById(R.id.takeoff_btn);
        landButton = (Button) findViewById(R.id.land_btn);
        captureButton = (Button) findViewById(R.id.capture_btn);
        recordToggleButton = (ToggleButton) findViewById(R.id.record_tgbtn);

        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mapView.showZoomControls(false);
        mapView.showScaleControl(false);
        baiduMap.setMyLocationEnabled(true);

        UiSettings uiSettings = baiduMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setAllGesturesEnabled(false);

        // zoom the map 4 times to ensure it is large enough
        for (int i = 0; i < 4; i++)
            baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());

        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null));

        takeOffButton.setOnClickListener((view -> {
            // todo check whether there are any timeline elements in the timeline
//            missionControl.scheduleElement(new TakeOffAction());
            missionControl.startElement(new TakeOffAction());
        }));

        landButton.setOnClickListener((view -> {
            // todo check whether there are any timeline elements in the timeline
            missionControl.startElement(new GoHomeAction());

        }));

        captureButton.setOnClickListener((view -> {
            baseProduct.getCamera()
                    .setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, e -> {
                        if (e != null) {
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
            baseProduct.getCamera()
                    .startShootPhoto(e -> {
                        if (e != null) {
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
        }));

        recordToggleButton.setOnClickListener((view -> {
            if (recordToggleButton.isChecked()) {
                baseProduct.getCamera().startRecordVideo(e -> {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(this, e);
                    }
                });
            } else {
                baseProduct.getCamera().stopRecordVideo(e -> {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(this, e);
                    }
                });
            }
        }));

        relativeLayoutMain = (RelativeLayout) findViewById(R.id.rl_main);
        videoTextureView = new TextureView(this);
        videoTextureView.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT)
        );
        relativeLayoutMain.addView(videoTextureView);
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

    public void initFlightController() {
        flightController = ((Aircraft) baseProduct).getFlightController();
        flightController.setStateCallback(flightControllerState -> {
            MyLocationData locData = new MyLocationData.Builder()
                    .latitude(flightControllerState.getAircraftLocation().getLatitude())
                    .longitude(flightControllerState.getAircraftLocation().getLongitude())
                    .build();
            baiduMap.setMyLocationData(locData);
        });
    }

    private void switchMapPanelFocus() {
        if (!isMapPanelFocused) {
            mapViewPanel.setOnClickListener(null);
            videoTextureView.setOnClickListener((view) -> switchMapPanelFocus());
            relativeLayoutMain.removeView(videoTextureView);
            linearLayoutForMapView.removeView(mapViewPanel);
            relativeLayoutMain.addView(mapViewPanel);
            linearLayoutForMapView.addView(videoTextureView);
            mapPanelButtonSet.parallelStream().forEach(e -> e.setVisibility(View.VISIBLE));
        } else {
            videoTextureView.setOnClickListener(null);
            mapViewPanel.setOnClickListener((view) -> switchMapPanelFocus());
            relativeLayoutMain.removeView(mapViewPanel);
            linearLayoutForMapView.removeView(videoTextureView);
            relativeLayoutMain.addView(videoTextureView);
            linearLayoutForMapView.addView(mapViewPanel);
            mapPanelButtonSet.parallelStream().forEach(e -> e.setVisibility(View.GONE));
        }
        isMapPanelFocused = !isMapPanelFocused;
    }
}
