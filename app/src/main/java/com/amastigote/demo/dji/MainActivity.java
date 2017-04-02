package com.amastigote.demo.dji;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
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
    private Button switchButton;
    private Button flightAssistButton;
    private ToggleButton recordToggleButton;
    private SimpleProgressDialog startUpInfoDialog;
    private RelativeLayout relativeLayoutMain;
    private TextureView videoTextureView;

    private MapView mapView;
    private LinearLayout linearLayoutForMapView;
    private RelativeLayout mapViewPanel;
    private boolean isMapPanelFocused;
    private Button mapPanelUndoButton;
    private Button mapPanelStartButton;
    private Button mapPanelStopButton;
    private Set<Button> mapPanelButtonSet;
    private BaiduMap baiduMap;

    private List<Waypoint> waypointList = new ArrayList<>();

    private WaypointMissionOperatorListener wmoListener;
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
                    // todo be aware of components change
                    baseProduct.setBaseProductListener(new BaseProduct.BaseProductListener() {
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {

                        }

                        @Override
                        public void onConnectivityChange(boolean b) {

                        }
                    });

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
        mapViewPanel = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.map_panel, null);
        linearLayoutForMapView.addView(mapViewPanel);
        mapViewPanel.setOnClickListener((view) -> switchMapPanelFocus());
        mapView = (MapView) mapViewPanel.findViewById(R.id.mv_mapview);
        mapPanelUndoButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_undo);
        mapPanelStartButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_start);
        mapPanelStopButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_stop);
        mapPanelButtonSet = new HashSet<Button>() {{
            add(mapPanelUndoButton);
            add(mapPanelStartButton);
            add(mapPanelStopButton);
        }};

        mapPanelUndoButton.setOnClickListener(view -> {
            waypointList.clear();
            baiduMap.clear();
        });
        mapPanelStartButton.setOnClickListener(view -> {
            if (!waypointList.isEmpty()) {
                executeWayPointMission(waypointList);
            }
        });

        isMapPanelFocused = false;

        takeOffButton = (Button) findViewById(R.id.takeoff_btn);
        switchButton = (Button) findViewById(R.id.btn_switch);
        landButton = (Button) findViewById(R.id.land_btn);
        flightAssistButton = (Button) findViewById(R.id.btn_flight_assist);
        captureButton = (Button) findViewById(R.id.capture_btn);
        recordToggleButton = (ToggleButton) findViewById(R.id.record_tgbtn);

        /*
        initialize WaypointMissionOperatorListener
         */
        wmoListener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {

            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

            }

            @Override
            public void onExecutionStart() {

            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                if (djiError != null) {
                    SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                } else {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            false,
                            "GoHome Confirmation",
                            "WayPoint Mission Completed.Go Home Immediately?",
                            new SimpleDialogButton("ok", (dialogInterface, i) -> {
                                missionControl.startElement(new GoHomeAction());
                            }));
                }
            }
        };

        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mapView.showZoomControls(false);
        mapView.showScaleControl(false);
        baiduMap.setMyLocationEnabled(true);

        UiSettings uiSettings = baiduMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setAllGesturesEnabled(false);

        // zoom the map 4 times to ensure it is large enough :)
        for (int i = 0; i < 4; i++)
            baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());

        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null));

        switchButton.setOnClickListener(e -> switchMapPanelFocus());

        takeOffButton.setOnClickListener((view -> {
            missionControl.startElement(new TakeOffAction());
        }));

        landButton.setOnClickListener((view -> {
            missionControl.startElement(new GoHomeAction());

        }));

        flightAssistButton.setOnClickListener(view -> {
            View dialog_content = LayoutInflater.from(this).inflate(R.layout.dialog_flight_assist_conf, null);
            Switch switch_ca = (Switch) dialog_content.findViewById(R.id.sw_ca);
            Switch switch_ua = (Switch) dialog_content.findViewById(R.id.sw_ua);
            Switch switch_aoa = (Switch) dialog_content.findViewById(R.id.sw_aoa);
            Switch switch_vap = (Switch) dialog_content.findViewById(R.id.sw_vap);
            Switch switch_pl = (Switch) dialog_content.findViewById(R.id.sw_pl);
            Switch switch_lp = (Switch) dialog_content.findViewById(R.id.sw_lp);

            // update switch states
            FlightAssistant fa = flightController.getFlightAssistant();
            if (fa != null) {
                fa.getCollisionAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_ca.setEnabled(true);
                        switch_ca.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_ca.setEnabled(false);
                    }
                });
                fa.getUpwardsAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_ua.setEnabled(true);
                        switch_ua.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_ua.setEnabled(false);
                    }
                });
                fa.getActiveObstacleAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_aoa.setEnabled(true);
                        switch_aoa.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_aoa.setEnabled(false);
                    }
                });
                fa.getVisionAssistedPositioningEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_vap.setEnabled(true);
                        switch_vap.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_vap.setEnabled(false);
                    }
                });
                fa.getPrecisionLandingEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_pl.setEnabled(true);
                        switch_pl.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_pl.setEnabled(false);
                    }
                });
                fa.getLandingProtectionEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        switch_lp.setEnabled(true);
                        switch_lp.setChecked(aBoolean);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        switch_lp.setEnabled(false);
                    }
                });

                switch_ca.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setCollisionAvoidanceEnabled(b, e -> {
                        if (e == null)
                            switch_ca.setEnabled(true);
                        else {
                            switch_ca.setEnabled(true);
                            switch_ca.setChecked(!switch_ca.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
                switch_ua.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setUpwardsAvoidanceEnabled(b, e -> {
                        if (e == null)
                            switch_ua.setEnabled(true);
                        else {
                            switch_ua.setEnabled(true);
                            switch_ua.setChecked(!switch_ua.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
                switch_aoa.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setActiveObstacleAvoidanceEnabled(b, e -> {
                        if (e == null)
                            switch_aoa.setEnabled(true);
                        else {
                            switch_aoa.setEnabled(true);
                            switch_aoa.setChecked(!switch_aoa.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
                switch_vap.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setVisionAssistedPositioningEnabled(b, e -> {
                        if (e == null)
                            switch_vap.setEnabled(true);
                        else {
                            switch_vap.setEnabled(true);
                            switch_vap.setChecked(!switch_vap.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
                switch_pl.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setPrecisionLandingEnabled(b, e -> {
                        if (e == null)
                            switch_pl.setEnabled(true);
                        else {
                            switch_pl.setEnabled(true);
                            switch_pl.setChecked(!switch_pl.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
                switch_lp.setOnCheckedChangeListener((v, b) -> {
                    v.setEnabled(false);
                    fa.setLandingProtectionEnabled(b, e -> {
                        if (e == null)
                            switch_lp.setEnabled(true);
                        else {
                            switch_lp.setEnabled(true);
                            switch_lp.setChecked(!switch_lp.isChecked());
                            SimpleAlertDialog.showDJIError(this, e);
                        }
                    });
                });
            } else {
                SimpleAlertDialog.showException(this, new Exception("No flight assistant available!"));
            }

            new AlertDialog.Builder(this)
                    .setTitle("Configure Flight Assistant")
                    .setCancelable(false)
                    .setPositiveButton("ok", null)
                    .setView(dialog_content)
                    .show();
        });

        captureButton.setOnClickListener((view -> baseProduct.getCamera().setMode(
                SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                e -> {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(this, e);
                    } else {
                        baseProduct.getCamera()
                                .setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, error -> {
                                    if (error != null) {
                                        SimpleAlertDialog.showDJIError(this, error);
                                    }
                                });
                        baseProduct.getCamera()
                                .startShootPhoto(error -> {
                                    if (error != null) {
                                        SimpleAlertDialog.showDJIError(this, error);
                                    }
                                });
                    }
                })));

        recordToggleButton.setOnClickListener((view -> {
            if (recordToggleButton.isChecked()) {
                baseProduct.getCamera().setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, e -> {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(this, e);
                    } else {
                        baseProduct.getCamera().startRecordVideo(error -> {
                            if (error != null) {
                                SimpleAlertDialog.showDJIError(this, error);
                            }
                        });
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
        videoTextureView.setClickable(true);
        videoTextureView.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT)
        );
        videoTextureView.setElevation(0);
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
            baiduMap.setOnMapLongClickListener(latLng -> {
                waypointList.add(new Waypoint(latLng.latitude, latLng.longitude, 50F));
                baiduMap.addOverlay(new MarkerOptions().position(latLng).title("POINT TEST").animateType(MarkerOptions.MarkerAnimateType.grow).flat(true));
            });
            relativeLayoutMain.removeView(videoTextureView);
            linearLayoutForMapView.removeView(mapViewPanel);
            relativeLayoutMain.addView(mapViewPanel);
            linearLayoutForMapView.addView(videoTextureView);
            mapPanelButtonSet.parallelStream().forEach(e -> e.setVisibility(View.VISIBLE));
        } else {
            waypointList.clear();
            baiduMap.clear();
            baiduMap.setOnMapLongClickListener(null);
            relativeLayoutMain.removeView(mapViewPanel);
            linearLayoutForMapView.removeView(videoTextureView);
            relativeLayoutMain.addView(videoTextureView);
            linearLayoutForMapView.addView(mapViewPanel);
            mapPanelButtonSet.parallelStream().forEach(e -> e.setVisibility(View.GONE));
        }
        isMapPanelFocused = !isMapPanelFocused;
    }

    private void executeWayPointMission(List<Waypoint> wayPointList) {
        WaypointMissionOperator waypointMissionOperator = missionControl.getWaypointMissionOperator();

        waypointMissionOperator.addListener(wmoListener);

        WaypointMission.Builder builder = new WaypointMission.Builder();
        builder.waypointList(wayPointList);

        if (waypointMissionOperator.loadMission(builder.build()) != null) {
            Log.e("DJIError", "Uploading failed.Retrying");
            waypointMissionOperator.retryUploadMission(djiError -> {
                if (djiError != null) {
                    SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                    Log.e("DJIError", "Uploading still failed");
                } else {
                    waypointMissionOperator.startMission(djiError1 -> {
                        if (djiError1 != null) {
                            SimpleAlertDialog.showDJIError(MainActivity.this, djiError1);
                        }
                    });
                }
            });
        } else {
            waypointMissionOperator.startMission(djiError -> {
                if (djiError != null) {
                    SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                }
            });

        }




    }
}
