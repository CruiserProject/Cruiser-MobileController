package com.amastigote.demo.dji;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.amastigote.demo.dji.CoordinationUtil.CoordinationConverter;
import com.amastigote.demo.dji.FlightModuleUtil.FlightAssistantManager;
import com.amastigote.demo.dji.FlightModuleUtil.FlightControllerManager;
import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener {
    private static BaseProduct baseProduct;
    private final AtomicInteger atomicInteger = new AtomicInteger();
    /*
        UI components
     */
    @BindView(R.id.vx_text)
    protected TextView velocityXTextView;
    @BindView(R.id.vy_text)
    protected TextView velocityYTextView;
    @BindView(R.id.vz_text)
    protected TextView velocityZTextView;
    @BindView(R.id.altitude_text)
    protected TextView altitudeTextView;
    @BindView(R.id.flight_time_text)
    protected TextView flightTimeTextView;
    @BindView(R.id.rl_main)
    protected RelativeLayout relativeLayoutMain;
    @BindView(R.id.ll_for_map)
    protected LinearLayout linearLayoutForMapView;

    protected Button mapPanelUndoButton;
    protected Button mapPanelStartButton;
    protected Button mapPanelStopButton;
    protected MapView mapView;
    private SimpleProgressDialog startUpInfoDialog;
    private TextureView videoTextureView;
    private RelativeLayout mapViewPanel;
    private boolean isMapPanelFocused;
    private Set<Button> mapPanelButtonSet;
    private BaiduMap baiduMap;
    private List<Waypoint> wayPointList = new ArrayList<>();
    private AtomicInteger previousWayPointIndex = new AtomicInteger();
    private AtomicBoolean isCompletedByStopping = new AtomicBoolean();
    private WaypointMissionOperator waypointMissionOperator;
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
                        new SimpleDialogButton("quit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface x, int y) {
                                System.exit(0);
                            }
                        })
                );
            }
        }

        @Override
        public void onProductChange(BaseProduct previousProd, BaseProduct presentProd) {
            baseProduct = presentProd;
            if (baseProduct != null && baseProduct.isConnected()) {
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
                    baseProduct.setBaseProductListener(new BaseProduct.BaseProductListener() {
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {
                        }

                        @Override
                        public void onConnectivityChange(boolean b) {
                        }
                    });

                    if (VideoFeeder.getInstance().getVideoFeeds().size() != 0) {
                        VideoFeeder.getInstance()
                                .getVideoFeeds().get(0)
                                .setCallback(new VideoFeeder.VideoDataCallback() {
                                    @Override
                                    public void onReceive(byte[] videoBuffer, int size) {
                                        if (djiCodecManager != null)
                                            djiCodecManager.sendDataToDecoder(videoBuffer, size);
                                    }
                                });
                    }

                    waypointMissionOperator = missionControl.getWaypointMissionOperator();
                    waypointMissionOperator.addListener(
                            new WaypointMissionOperatorListener() {
                                @Override
                                public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

                                }

                                @Override
                                public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
                                    if (waypointMissionUploadEvent.getProgress() != null
                                            && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == waypointMissionUploadEvent.getProgress().totalWaypointCount - 1
                                            && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                                            && previousWayPointIndex.get() != waypointMissionUploadEvent.getProgress().uploadedWaypointIndex) {
                                        previousWayPointIndex.set(waypointMissionUploadEvent.getProgress().uploadedWaypointIndex);
                                        SimpleAlertDialog.show(
                                                MainActivity.this,
                                                false,
                                                "Mission Uploaded",
                                                "Start mission immediately?",
                                                new SimpleDialogButton("yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                                                            @Override
                                                            public void onResult(DJIError djiError) {
                                                                if (djiError != null) {
                                                                    SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                                                                }
                                                            }
                                                        });
                                                    }
                                                }));
                                    }

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
                                                true,
                                                "WayPoint Mission " + (isCompletedByStopping.get() ? "Stopped" : "Completed"),
                                                "Go home immediately?",
                                                new SimpleDialogButton("yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        missionControl.startElement(new GoHomeAction());
                                                    }
                                                }));
                                    }
                                }
                            });
                } catch (Exception e) {
                    SimpleAlertDialog.showException(MainActivity.this, e);
                }
            }
        }
    };

    @OnClick(R.id.takeoff_btn)
    void tob_oc() {
        missionControl.startElement(new TakeOffAction());
    }

    @OnClick(R.id.land_btn)
    void lb_oc() {
        missionControl.startElement(new GoHomeAction());
    }

    @OnClick(R.id.capture_btn)
    void cb_oc() {
        baseProduct.getCamera().setMode(
                SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError e) {
                        if (e != null) {
                            SimpleAlertDialog.showDJIError(MainActivity.this, e);
                        } else {
                            baseProduct.getCamera()
                                    .setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError error) {
                                            if (error != null) {
                                                SimpleAlertDialog.showDJIError(MainActivity.this, error);
                                            }
                                        }
                                    });
                            baseProduct.getCamera()
                                    .startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError error) {
                                            if (error != null) {
                                                SimpleAlertDialog.showDJIError(MainActivity.this, error);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    @OnClick(R.id.btn_switch)
    void sb_oc() {
        MainActivity.this.switchMapPanelFocus();
    }

    @OnClick(R.id.btn_flight_assist)
    void fab_oc() {
        @SuppressLint("InflateParams") View dialog_content = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_flight_assist_conf, null);
        final Switch switch_ca = ButterKnife.findById(dialog_content, R.id.sw_ca);
        final Switch switch_ua = ButterKnife.findById(dialog_content, R.id.sw_ua);
        final Switch switch_aoa = ButterKnife.findById(dialog_content, R.id.sw_aoa);
        final Switch switch_vap = ButterKnife.findById(dialog_content, R.id.sw_vap);
        final Switch switch_pl = ButterKnife.findById(dialog_content, R.id.sw_pl);
        final Switch switch_lp = ButterKnife.findById(dialog_content, R.id.sw_lp);

        switch_ca.setTag(FlightAssistantManager.Type.CA);
        switch_ua.setTag(FlightAssistantManager.Type.UA);
        switch_aoa.setTag(FlightAssistantManager.Type.AOA);
        switch_vap.setTag(FlightAssistantManager.Type.VAP);
        switch_pl.setTag(FlightAssistantManager.Type.PL);
        switch_lp.setTag(FlightAssistantManager.Type.LP);

        Set<Switch> switches = new HashSet<Switch>() {{
            add(switch_ca);
            add(switch_ua);
            add(switch_aoa);
            add(switch_vap);
            add(switch_pl);
            add(switch_lp);
        }};

        for (Switch e : switches) {
            e.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FlightAssistantManager.handleSettingsChange((Switch) v, MainActivity.this);
                }
            });
        }

        FlightAssistantManager.initSettings(switches, MainActivity.this, dialog_content);
    }

    @OnClick(R.id.record_tgbtn)
    void rtb_oc(final View view) {
        final boolean b = ((CompoundButton) view).isChecked();
        if (b) {
            baseProduct.getCamera().setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError e) {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(MainActivity.this, e);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((CompoundButton) view).setChecked(false);
                            }
                        });
                    } else {
                        baseProduct.getCamera().startRecordVideo(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                if (error != null) {
                                    SimpleAlertDialog.showDJIError(MainActivity.this, error);
                                } else
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((CompoundButton) view).setChecked(false);
                                        }
                                    });
                            }
                        });
                    }
                }
            });
        } else {
            baseProduct.getCamera().stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError e) {
                    if (e != null) {
                        SimpleAlertDialog.showDJIError(MainActivity.this, e);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((CompoundButton) view).setChecked(true);
                            }
                        });
                    }
                }
            });
        }
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        CoordinationConverter.init();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //noinspection deprecation
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
                            Manifest.permission.GET_TASKS,
                            Manifest.permission.CHANGE_CONFIGURATION,
                    }
                    , Integer.MAX_VALUE);
        }

        mapViewPanel = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.map_panel, null);
        mapView = (MapView) mapViewPanel.findViewById(R.id.mv_mapview);
        mapPanelUndoButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_undo);
        mapPanelStartButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_start);
        mapPanelStopButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_stop);
        linearLayoutForMapView.addView(mapViewPanel);
        mapViewPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.switchMapPanelFocus();
            }
        });
        mapPanelButtonSet = new HashSet<Button>() {{
            add(mapPanelUndoButton);
            add(mapPanelStartButton);
            add(mapPanelStopButton);
        }};

        mapPanelUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wayPointList.clear();
                baiduMap.clear();
            }
        });
        mapPanelStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!wayPointList.isEmpty()) {
                    MainActivity.this.executeWayPointMission(wayPointList);
                }
            }
        });
        mapPanelStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCompletedByStopping.set(true);
                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null)
                            SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                    }
                });
            }
        });

        isMapPanelFocused = false;

        /*
            initialize WaypointMissionOperatorListener
         */
        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mapView.setClickable(true);
        mapView.showZoomControls(false);
        mapView.showScaleControl(false);
        baiduMap.setMyLocationEnabled(true);

        UiSettings uiSettings = baiduMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setAllGesturesEnabled(false);
        uiSettings.setZoomGesturesEnabled(true);

        // zoom the map 6 times to ensure it is large enough :)
        for (int i = 0; i < 6; i++)
            baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());

        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null));

        videoTextureView = new TextureView(this);
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
    }

    public void initFlightController() {
        FlightController flightController = FlightControllerManager.getInstance(baseProduct);
        flightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull final FlightControllerState flightControllerState) {
                // Drop the callback data at a fixed rate to prevent
                // the SDK from shitting its pants.
                if (atomicInteger.getAndAdd(1) > 2) {
                    atomicInteger.set(0);
                    return;
                }

                //update params of FlightControllerState
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        velocityXTextView.setText(String.format(Locale.CHINA, "VelocityX: %f m/s", flightControllerState.getVelocityX()));
                        velocityYTextView.setText(String.format(Locale.CHINA, "VelocityY: %f m/s", flightControllerState.getVelocityY()));
                        velocityZTextView.setText(String.format(Locale.CHINA, "VelocityZ: %f m/s", flightControllerState.getVelocityZ()));
                        altitudeTextView.setText(String.format(Locale.CHINA, "Altitude: %f m", flightControllerState.getAircraftLocation().getAltitude()));
                        flightTimeTextView.setText(String.format(Locale.CHINA, "FlightTime: %d s", flightControllerState.getFlightTimeInSeconds()));
                    }
                });

                LatLng cvLatLong = CoordinationConverter.GPS2BD09(
                        new LatLng(
                                flightControllerState.getAircraftLocation().getLatitude(),
                                flightControllerState.getAircraftLocation().getLongitude()
                        )
                );
                MyLocationData locationData = new MyLocationData.Builder()
                        .latitude(cvLatLong.latitude)
                        .longitude(cvLatLong.longitude)
                        .direction(flightControllerState.getAircraftHeadDirection())
                        .build();
                baiduMap.setMyLocationData(locationData);
            }
        });
    }

    private void switchMapPanelFocus() {
        if (!isMapPanelFocused) {
            baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    LatLng latLngGPS84 = CoordinationConverter.BD092GPS84(latLng);
                    wayPointList.add(new Waypoint(
                            latLngGPS84.latitude,
                            latLngGPS84.longitude,
                            30F)
                    );
                    baiduMap.addOverlay(new MarkerOptions()
                            .position(latLng)
                            .animateType(MarkerOptions.MarkerAnimateType.grow)
                            .flat(true)
                            .anchor(0.5F, 0.5F)
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker))
                            .draggable(false));
                    final int pointListSize = wayPointList.size();
                    if (pointListSize > 1) {
                        baiduMap.addOverlay(new PolylineOptions()
                                .points(new ArrayList<LatLng>() {
                                    {
                                        add(CoordinationConverter.GPS2BD09(new LatLng(
                                                wayPointList.get(pointListSize - 1).coordinate.getLatitude(),
                                                wayPointList.get(pointListSize - 1).coordinate.getLongitude()
                                        )));
                                        add(CoordinationConverter.GPS2BD09(new LatLng(
                                                wayPointList.get(pointListSize - 2).coordinate.getLatitude(),
                                                wayPointList.get(pointListSize - 2).coordinate.getLongitude()
                                        )));
                                    }
                                })
                                .color(R.color.purple)
                                .dottedLine(true)
                        );
                    }
                }
            });
            relativeLayoutMain.removeView(videoTextureView);
            linearLayoutForMapView.removeView(mapViewPanel);
            relativeLayoutMain.addView(mapViewPanel);
            linearLayoutForMapView.addView(videoTextureView);
            for (View v : mapPanelButtonSet)
                v.setVisibility(View.VISIBLE);
        } else {
            baiduMap.setOnMapLongClickListener(null);
            relativeLayoutMain.removeView(mapViewPanel);
            linearLayoutForMapView.removeView(videoTextureView);
            relativeLayoutMain.addView(videoTextureView);
            linearLayoutForMapView.addView(mapViewPanel);
            for (View v : mapPanelButtonSet)
                v.setVisibility(View.GONE);
        }
        mapView.onResume();
        isMapPanelFocused = !isMapPanelFocused;
    }

    private void executeWayPointMission(List<Waypoint> wayPointList) {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        DJIError djiParameterError;
        if ((djiParameterError =
                builder
                        .waypointList(wayPointList)
                        .maxFlightSpeed(3.0F)
                        .waypointCount(wayPointList.size())
                        .autoFlightSpeed(2.0F)
                        .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                        .finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                        .headingMode(WaypointMissionHeadingMode.AUTO)
                        .gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
                        .checkParameters()) != null) {
            SimpleAlertDialog.showDJIError(this, djiParameterError);
        } else {
            previousWayPointIndex.set(0);
            atomicInteger.set(0);
            isCompletedByStopping.set(false);

            DJIError djiErrorFirst = waypointMissionOperator.loadMission(builder.build());
            if (djiErrorFirst != null) {
                SimpleAlertDialog.showDJIError(MainActivity.this, djiErrorFirst);
            } else {
                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        SimpleAlertDialog.showDJIError(MainActivity.this, djiError);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}
