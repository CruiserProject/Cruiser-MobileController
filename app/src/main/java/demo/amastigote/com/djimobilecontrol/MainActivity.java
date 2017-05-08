package demo.amastigote.com.djimobilecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import demo.amastigote.com.djimobilecontrol.ConverterUtil.CoordinationConverter;
import demo.amastigote.com.djimobilecontrol.ConverterUtil.DensityUtil;
import demo.amastigote.com.djimobilecontrol.DataUtil.WaypointMissionParams;
import demo.amastigote.com.djimobilecontrol.FlightModuleUtil.BatteryManager;
import demo.amastigote.com.djimobilecontrol.FlightModuleUtil.FlightControllerManager;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SideToast;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleAlertDialog;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleDialogButton;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleProgressDialog;
import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
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
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;


public class MainActivity extends Activity {
    private static BaseProduct baseProduct;
    private final AtomicInteger atomicInteger = new AtomicInteger();
    /*
        UI components
     */
    private RelativeLayout relativeLayoutMain;
    private FrameLayout videoTextureViewFrameLayout;
    private ImageView statusIndicatorImageView;
    private ImageView gpsSignalLevelImageView;
    private ImageView rcSignalLevelImageView;
    private ImageView remainingBatteryImageView;
    private ImageView takeOffImageView;
    private ImageView landImageView;
    private ImageView cameraShootImageView;
    private ImageView cameraSwitchImageView;
    private ImageView cameraPlayImageView;
    private ImageView switchPanelImageView;
    private TextView aircraftTextView;
    private TextView statusDescriptionTextView;
    private TextView satelliteNumberTextView;
    private TextView stateAltitudeTextView;
    private TextView stateVelocityTextView;

    // a test for SendDataToOnBoardSDKDevice
    private Button sendDataToOnBoardSDKDeviceButton;
    private Button mapPanelUndoButton;
    private Button mapPanelCreateButton;
    private Button mapPanelStartMissionButton;
    private Button mapPanelCancelMissionButton;
    private Button missionConfigurationPanelOKButton;
    private Button missionConfigurationPanelCancelButton;

    private RadioGroup radioGroupMissionStartAction;
    private RadioGroup radioGroupMissionFinishAction;
    private RadioGroup radioGroupPathMode;
    private RadioGroup radioGroupHeading;

    private TextView textViewAutoFlightSpeed;
    private TextView textViewMaxFlightSpeed;

    private SimpleProgressDialog startUpInfoDialog;

    private TextureView videoTextureView;
    /*
        baidu map
     */
    private LinearLayout linearLayoutForMap;
    private MapView mapView;
    private RelativeLayout mapViewPanel;
    private RelativeLayout missionConfigurationPanel;
    private BaiduMap baiduMap;
    /*
        data
     */
    private AtomicInteger previousWayPointIndex = new AtomicInteger();
    private AtomicBoolean isCompletedByStopping = new AtomicBoolean();
    /*
        DJI sdk
     */
    private int currentBatteryInPercent = -1;
    private int currentSatellitesCount = -1;
    private boolean isMapPanelFocused = false;
    private boolean isRecording = false;
    private SettingsDefinitions.CameraMode curCameraMode = SettingsDefinitions.CameraMode.UNKNOWN;
    private Camera camera;
    private DJICodecManager djiCodecManager;
    private TextureView.SurfaceTextureListener textureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (djiCodecManager == null) {
                djiCodecManager = new DJICodecManager(MainActivity.this, surface, width, height);
            }
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
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private Battery battery;
    private FlightController flightController;
    private MissionControl missionControl;
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMissionParams waypointMissionParams;
    private List<Waypoint> wayPointList = new ArrayList<>();
    private BaiduMap.OnMapLongClickListener onMapLongClickListener
            = new BaiduMap.OnMapLongClickListener() {
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
    };

    private View.OnClickListener newMissionOnClickListener
            = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mapPanelCreateButton.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(DensityUtil.dip2px(MainActivity.this, 200.0f), ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            mapViewPanel.addView(missionConfigurationPanel, params);

            linearLayoutForMap.setVisibility(View.GONE);
            switchPanelImageView.setVisibility(View.GONE);

        }
    };


    private FlightController.OnboardSDKDeviceDataCallback onboardSDKDeviceDataCallback
            = new FlightController.OnboardSDKDeviceDataCallback() {
        @Override
        public void onReceive(final byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SideToast.makeText(MainActivity.this, "成功收到消息: " + bytes.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
                    int len = bytes.length;
                    StringBuilder stringBuilder = new StringBuilder();
                    for (byte aByte : bytes) {
                        stringBuilder.append((char) aByte);
                    }
                    Log.e(">> Onboard Message", stringBuilder.toString());

                }
            });
//            SideToast.makeText(MainActivity.this, "成功收到消息:" + bytes.toString(), SideToast.LENGTH_SHORT).show();
        }
    };
    private BaseProduct.BaseProductListener baseProductListener
            = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {

        }

        @Override
        public void onConnectivityChange(final boolean b) {
            if (b) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SideToast.makeText(MainActivity.this, "飞行器已连接", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                        aircraftTextView.setText(baseProduct.getModel().toString());
                    }
                });
                changeCameraState();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SideToast.makeText(MainActivity.this, "飞行器已断开连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                        aircraftTextView.setText("飞行器未连接");
                        stateVelocityTextView.setText("");
                        stateAltitudeTextView.setText("");
                        currentBatteryInPercent = -1;
                        curCameraMode = SettingsDefinitions.CameraMode.UNKNOWN;
                    }
                });
            }
        }
    };
    private FlightControllerState.Callback fcsCallback
            = new FlightControllerState.Callback() {
        @Override
        public void onUpdate(@NonNull final FlightControllerState flightControllerState) {
            updateVelocity(flightControllerState);

            updateBaiduMapMyLocation(flightControllerState);

            updateSatellitesCount(flightControllerState.getSatelliteCount());
        }
    };
    private BatteryState.Callback batteryCallback
            = new BatteryState.Callback() {
        @Override
        public void onUpdate(BatteryState batteryState) {
            int remainingBatteryInPercent = batteryState.getChargeRemainingInPercent();
            updateBatteryState(remainingBatteryInPercent);

        }
    };
    private DJISDKManager.SDKManagerCallback sdkManagerCallback
            = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError djiError) {
            if (djiError.toString().equals(DJISDKError.REGISTRATION_SUCCESS.toString())) {
                startUpInfoDialog.dismiss();
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
            if (baseProduct == null) {
                return;
            }

            currentBatteryInPercent = -1;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SideToast.makeText(MainActivity.this, "飞行器已连接", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                    aircraftTextView.setText(baseProduct.getModel().toString());
                }
            });

            initFlightController();
            initBattery();
            initCamera();
            initMissionControl();
            changeCameraState();
            baseProduct.setBaseProductListener(baseProductListener);


            List<VideoFeeder.VideoFeed> videoFeeds = VideoFeeder.getInstance().getVideoFeeds();
            if (videoFeeds.size() != 0) {
                videoFeeds.get(0).setCallback(new VideoFeeder.VideoDataCallback() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {
                        if (djiCodecManager != null) {
                            djiCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                    }
                });
            }

        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        getWindow().getAttributes().systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        initPermissionRequest();
        initUI();
        initBaiduMap();
        initVideoTextureView();
        initOnClickListener();
        initSendDataOnClickListener();

        waypointMissionParams = new WaypointMissionParams();

        startUpInfoDialog = new SimpleProgressDialog(MainActivity.this, "Validating API key");

        startUpInfoDialog.show();
        DJISDKManager.getInstance().registerApp(this, sdkManagerCallback);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mapView.onResume();
    }

    private void initBaiduMap() {
        mapViewPanel = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.map_panel, null);
        linearLayoutForMap = (LinearLayout) findViewById(R.id.ll_for_map);
        mapView = (MapView) mapViewPanel.findViewById(R.id.mv_mapview);
        mapPanelCreateButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_create);
        mapPanelUndoButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_undo);
        mapPanelCancelMissionButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_cancel);
        mapPanelStartMissionButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_start);
        linearLayoutForMap.addView(mapViewPanel);

        /*
            configure Baidu MapView
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
        
        /*
            init mission configuration
         */
        missionConfigurationPanel = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.waypoint_configuration, null);
        missionConfigurationPanelOKButton = (Button) missionConfigurationPanel.findViewById(R.id.control_mission_ok_btn);
        missionConfigurationPanelCancelButton = (Button) missionConfigurationPanel.findViewById(R.id.control_mission_cancel_btn);
        radioGroupMissionStartAction = (RadioGroup) missionConfigurationPanel.findViewById(R.id.rg_mission_start_acton);
        radioGroupMissionFinishAction = (RadioGroup) missionConfigurationPanel.findViewById(R.id.rg_mission_finish_action);
        radioGroupPathMode = (RadioGroup) missionConfigurationPanel.findViewById(R.id.rg_path_mode);
        radioGroupHeading = (RadioGroup) missionConfigurationPanel.findViewById(R.id.rg_heading);
        textViewMaxFlightSpeed = (TextView) missionConfigurationPanel.findViewById(R.id.edtxt_max_flight_speed);
        textViewAutoFlightSpeed = (TextView) missionConfigurationPanel.findViewById(R.id.edtxt_auto_flight_speed);
    }

    private void initVideoTextureView() {
        videoTextureView = new TextureView(this);
        videoTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        videoTextureView.setElevation(0);
        videoTextureView.setSurfaceTextureListener(textureListener);
        videoTextureViewFrameLayout.addView(videoTextureView);

    }

    private void initUI() {
        videoTextureViewFrameLayout = (FrameLayout) findViewById(R.id.videoTextureViewLayout);
        relativeLayoutMain = (RelativeLayout) findViewById(R.id.rl_main);
        statusIndicatorImageView = (ImageView) findViewById(R.id.status_indicator_img);
        gpsSignalLevelImageView = (ImageView) findViewById(R.id.gps_signal);
        rcSignalLevelImageView = (ImageView) findViewById(R.id.rc_signal);
        remainingBatteryImageView = (ImageView) findViewById(R.id.remaining_battery);
        takeOffImageView = (ImageView) findViewById(R.id.takeoff);
        landImageView = (ImageView) findViewById(R.id.land);
        cameraShootImageView = (ImageView) findViewById(R.id.camera_take);
        cameraSwitchImageView = (ImageView) findViewById(R.id.camera_switch);
        cameraPlayImageView = (ImageView) findViewById(R.id.camera_play);
        switchPanelImageView = (ImageView) findViewById(R.id.switch_panel);
        aircraftTextView = (TextView) findViewById(R.id.status_aircraft);
        statusDescriptionTextView = (TextView) findViewById(R.id.status_description_txt);
        satelliteNumberTextView = (TextView) findViewById(R.id.satellite_number_txt);
        stateAltitudeTextView = (TextView) findViewById(R.id.state_altitude);
        stateVelocityTextView = (TextView) findViewById(R.id.state_velocity);
    }

    private void initPermissionRequest() {
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
    }

    private void initFlightController() {
        flightController = FlightControllerManager.getInstance(baseProduct);
        flightController.setStateCallback(fcsCallback);
        flightController.setOnboardSDKDeviceDataCallback(onboardSDKDeviceDataCallback);
    }

    private void initBattery() {
        battery = BatteryManager.getInstance(baseProduct);
        battery.setStateCallback(batteryCallback);
    }

    private void initCamera() {
        if (baseProduct != null) {
            camera = baseProduct.getCamera();
        }
    }

    private void initMissionControl() {
        missionControl = MissionControl.getInstance();
        waypointMissionOperator = missionControl.getWaypointMissionOperator();
        waypointMissionOperator.addListener(new WaypointMissionOperatorListener() {
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
                                                SideToast.makeText(MainActivity.this, "任务执行失败:" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
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

            }
        });
    }

    private void initOnClickListener() {
        takeOffImageView.setClickable(true);
        takeOffImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作:飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                } else {
                    flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                SideToast.makeText(MainActivity.this, "起飞时出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                            } else {
                                SideToast.makeText(MainActivity.this, "成功起飞", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        }
                    });
                }

            }
        });

        landImageView.setClickable(true);
        landImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                } else {
                    flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                SideToast.makeText(MainActivity.this, "返航时出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                            } else {
                                SideToast.makeText(MainActivity.this, "成功返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        }
                    });
                }
            }
        });

        cameraShootImageView.setClickable(true);
        cameraShootImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                } else {
                    switch (curCameraMode) {
                        case SHOOT_PHOTO:
                            camera.startShootPhoto(null);
                            break;
                        case RECORD_VIDEO:
                            if (isRecording) {
                                camera.stopRecordVideo(null);
                                isRecording = false;
                            } else {
                                camera.startRecordVideo(null);
                                isRecording = true;
                            }
                            break;
                        case UNKNOWN:
                            SideToast.makeText(MainActivity.this, "相机连接错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                            break;
                    }
                }
            }
        });

        cameraSwitchImageView.setClickable(true);
        cameraSwitchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                } else {
                    switch (curCameraMode) {
                        case SHOOT_PHOTO:
                            camera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null) {
                                        curCameraMode = SettingsDefinitions.CameraMode.RECORD_VIDEO;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_record));
                                            }
                                        });
                                    } else {
                                        SideToast.makeText(MainActivity.this, "相机切换状态失败", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    }
                                }
                            });
                            break;
                        case RECORD_VIDEO:
                            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null) {
                                        curCameraMode = SettingsDefinitions.CameraMode.SHOOT_PHOTO;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_take));
                                            }
                                        });
                                    } else {
                                        SideToast.makeText(MainActivity.this, "相机切换状态失败", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    }
                                }
                            });
                            break;
                        case UNKNOWN:
                            SideToast.makeText(MainActivity.this, "相机连接错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                            break;
                    }
                }
            }
        });

        switchPanelImageView.setClickable(true);
        switchPanelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMapPanelFocus();
            }
        });

        mapPanelCreateButton.setOnClickListener(newMissionOnClickListener);

        mapPanelStartMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleAlertDialog.show(
                        MainActivity.this,
                        false,
                        "Mission Confirm",
                        "Start mission immediately?",
                        new SimpleDialogButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapPanelCreateButton.setVisibility(View.VISIBLE);
                                linearLayoutForMap.setVisibility(View.VISIBLE);
                                switchPanelImageView.setVisibility(View.VISIBLE);
                                mapPanelStartMissionButton.setVisibility(View.GONE);
                                mapPanelCancelMissionButton.setVisibility(View.GONE);
                                if (flightController == null || !flightController.isConnected()) {
                                    SideToast.makeText(MainActivity.this, "任务执行错误：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    return;
                                }
                                LocationCoordinate3D homeLocation = flightController.getState().getAircraftLocation();
                                wayPointList.add(new Waypoint(homeLocation.getLatitude(), homeLocation.getLongitude(), 30.0f));
                                executeWaypointMission(wayPointList, waypointMissionParams);
                            }
                        }));
            }
        });

        mapPanelCancelMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapPanelStartMissionButton.setVisibility(View.GONE);
                mapPanelCancelMissionButton.setVisibility(View.GONE);
                baiduMap.setOnMapLongClickListener(null);
                wayPointList.clear();
                baiduMap.clear();
                mapViewPanel.addView(missionConfigurationPanel);
            }
        });


        missionConfigurationPanelCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapViewPanel.removeView(missionConfigurationPanel);
                mapPanelCreateButton.setVisibility(View.VISIBLE);
                linearLayoutForMap.setVisibility(View.VISIBLE);
                switchPanelImageView.setVisibility(View.VISIBLE);
            }
        });

        missionConfigurationPanelOKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baiduMap.setOnMapLongClickListener(onMapLongClickListener);
                WaypointMissionFinishedAction waypointMissionFinishedAction;
                WaypointMissionFlightPathMode waypointMissionFlightPathMode;
                WaypointMissionGotoWaypointMode waypointMissionGotoWaypointModel;
                WaypointMissionHeadingMode waypointMissionHeadingMode;

                switch (radioGroupMissionFinishAction.getCheckedRadioButtonId()) {
                    case R.id.rb_c:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        break;
                    case R.id.rb_d:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                        break;
                    case R.id.rb_e:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                        break;
                    default:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        break;
                }

                switch (radioGroupPathMode.getCheckedRadioButtonId()) {
                    case R.id.rb_f:
                        waypointMissionFlightPathMode = WaypointMissionFlightPathMode.NORMAL;
                        break;
                    case R.id.rb_g:
                        waypointMissionFlightPathMode = WaypointMissionFlightPathMode.CURVED;
                        break;
                    default:
                        waypointMissionFlightPathMode = WaypointMissionFlightPathMode.NORMAL;
                        break;
                }

                switch (radioGroupMissionStartAction.getCheckedRadioButtonId()) {
                    case R.id.rb_a:
                        waypointMissionGotoWaypointModel = WaypointMissionGotoWaypointMode.SAFELY;
                        break;
                    case R.id.rb_b:
                        waypointMissionGotoWaypointModel = WaypointMissionGotoWaypointMode.POINT_TO_POINT;
                        break;
                    default:
                        waypointMissionGotoWaypointModel = WaypointMissionGotoWaypointMode.SAFELY;
                        break;
                }

                switch (radioGroupHeading.getCheckedRadioButtonId()) {
                    case R.id.rb_h:
                        waypointMissionHeadingMode = WaypointMissionHeadingMode.AUTO;
                        break;
                    case R.id.rb_i:
                        waypointMissionHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                        break;
                    case R.id.rb_j:
                        waypointMissionHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                        break;
                    default:
                        waypointMissionHeadingMode = WaypointMissionHeadingMode.AUTO;
                        break;
                }

                waypointMissionParams.setMissionFinishedAction(waypointMissionFinishedAction);
                waypointMissionParams.setMissionFlightPathMode(waypointMissionFlightPathMode);
                waypointMissionParams.setMissionGotoWaypointMode(waypointMissionGotoWaypointModel);
                waypointMissionParams.setMissionHeadingMode(waypointMissionHeadingMode);
                String maxSpeedString = textViewMaxFlightSpeed.getText().toString().trim();
                String autoSpeedString = textViewAutoFlightSpeed.getText().toString().trim();
                waypointMissionParams.setMaxFlightSpeed(Float.valueOf("".equals(maxSpeedString) ? "5" : maxSpeedString));
                waypointMissionParams.setAutoFlightSpeed(Float.valueOf("".equals(autoSpeedString) ? "2" : autoSpeedString));
                mapViewPanel.removeView(missionConfigurationPanel);
                mapPanelStartMissionButton.setVisibility(View.VISIBLE);
                mapPanelCancelMissionButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateBatteryState(int remainingBattery) {
        if (currentBatteryInPercent == -1) {
            final int temp[] = new int[1];
            switch ((remainingBattery + 10) / 20) {
                case 0:
                    temp[0] = R.mipmap.battery_0;
                    currentBatteryInPercent = 0;
                    break;
                case 1:
                    currentBatteryInPercent = 1;
                    temp[0] = R.mipmap.battery_10;
                    break;
                case 2:
                    currentBatteryInPercent = 2;
                    temp[0] = R.mipmap.battery_30;
                    break;
                case 3:
                    currentBatteryInPercent = 3;
                    temp[0] = R.mipmap.battery_50;
                    break;
                case 4:
                    currentBatteryInPercent = 4;
                    temp[0] = R.mipmap.battery_70;
                    break;
                case 5:
                    currentBatteryInPercent = 5;
                    temp[0] = R.mipmap.battery_100;
                    break;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remainingBatteryImageView.setImageDrawable(MainActivity.this.getDrawable(temp[0]));
                }
            });
        } else {
            int flag = (remainingBattery + 10) / 20;
            if (flag != currentBatteryInPercent) {
                final int temp[] = new int[1];
                switch (flag) {
                    case 0:
                        temp[0] = R.mipmap.battery_0;
                        currentBatteryInPercent = 0;
                        break;
                    case 1:
                        currentBatteryInPercent = 1;
                        temp[0] = R.mipmap.battery_10;
                        break;
                    case 2:
                        currentBatteryInPercent = 2;
                        temp[0] = R.mipmap.battery_30;
                        break;
                    case 3:
                        currentBatteryInPercent = 3;
                        temp[0] = R.mipmap.battery_50;
                        break;
                    case 4:
                        currentBatteryInPercent = 4;
                        temp[0] = R.mipmap.battery_70;
                        break;
                    case 5:
                        currentBatteryInPercent = 5;
                        temp[0] = R.mipmap.battery_100;
                        break;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remainingBatteryImageView.setImageDrawable(MainActivity.this.getDrawable(temp[0]));
                    }
                });
            }


        }
    }

    private void updateSatellitesCount(final int satellitesCount) {
        if (satellitesCount != currentSatellitesCount) {
            currentSatellitesCount = satellitesCount;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    satelliteNumberTextView.setText(String.valueOf(satellitesCount));
                }
            });
        }
    }

    private void updateVelocity(final FlightControllerState flightControllerState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stateAltitudeTextView.setText(String.format(Locale.CHINA, "Altitude: %.1f", flightControllerState.getAircraftLocation().getAltitude()));
                float velocity = (float) Math.sqrt(Math.pow(flightControllerState.getVelocityX(), 2) + Math.pow(flightControllerState.getVelocityY(), 2));
                stateVelocityTextView.setText(String.format(Locale.CHINA, "Velocity: %.1f", velocity));
            }
        });
    }

    private void updateBaiduMapMyLocation(FlightControllerState flightControllerState) {
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

    private void changeCameraState() {
        camera.getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.CameraMode cameraMode) {
                if (cameraMode.equals(SettingsDefinitions.CameraMode.SHOOT_PHOTO)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_take));
                            curCameraMode = SettingsDefinitions.CameraMode.SHOOT_PHOTO;
                        }
                    });
                } else {
                    if (cameraMode.equals(SettingsDefinitions.CameraMode.RECORD_VIDEO)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_record));
                                curCameraMode = SettingsDefinitions.CameraMode.RECORD_VIDEO;
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e("Camera State error", ">>" + djiError.toString());
//                        SideToast.makeText(MainActivity.this,"获取相机状态失败",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
                curCameraMode = SettingsDefinitions.CameraMode.UNKNOWN;
            }
        });
    }

    private void initSendDataOnClickListener() {
        sendDataToOnBoardSDKDeviceButton = (Button) findViewById(R.id.test_send_data_btn);
        sendDataToOnBoardSDKDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "发送失败：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                } else {
                    byte arr[] = new byte[1];
                    arr[0] = 2 + '0';
                    flightController.sendDataToOnboardSDKDevice(arr, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                SideToast.makeText(MainActivity.this, "发送成功", SideToast.LENGTH_SHORT).show();
                            } else {
                                SideToast.makeText(MainActivity.this, "发送失败: 请查看日志", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                Log.e("SendDataToOnBoard", ">> " + djiError);
                            }
                        }
                    });

                }

            }
        });
    }

    private void switchMapPanelFocus() {
        if (!isMapPanelFocused) {
            relativeLayoutMain.removeView(cameraPlayImageView);
            relativeLayoutMain.removeView(cameraShootImageView);
            relativeLayoutMain.removeView(cameraSwitchImageView);
            linearLayoutForMap.removeView(mapViewPanel);
            videoTextureViewFrameLayout.removeView(videoTextureView);
            videoTextureViewFrameLayout.addView(mapViewPanel);
            linearLayoutForMap.addView(videoTextureView);
            mapPanelUndoButton.setVisibility(View.VISIBLE);
            mapPanelCreateButton.setVisibility(View.VISIBLE);
        } else {
            mapPanelCreateButton.setVisibility(View.GONE);
            mapPanelUndoButton.setVisibility(View.GONE);
            videoTextureViewFrameLayout.removeView(mapViewPanel);
            linearLayoutForMap.removeView(videoTextureView);
            linearLayoutForMap.addView(mapViewPanel);
            videoTextureViewFrameLayout.addView(videoTextureView);
            relativeLayoutMain.addView(cameraShootImageView);
            relativeLayoutMain.addView(cameraPlayImageView);
            relativeLayoutMain.addView(cameraSwitchImageView);

        }

        mapView.onResume();
        isMapPanelFocused = !isMapPanelFocused;
    }

    private void executeWaypointMission(List<Waypoint> wayPointList, WaypointMissionParams params) {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        DJIError djiParameterError;
        if ((djiParameterError =
                builder
                        .waypointList(wayPointList)
                        .maxFlightSpeed(params.getMaxFlightSpeed())
                        .waypointCount(wayPointList.size())
                        .autoFlightSpeed(params.getAutoFlightSpeed())
                        .flightPathMode(params.getMissionFlightPathMode())
                        .finishedAction(params.getMissionFinishedAction())
                        .headingMode(params.getMissionHeadingMode())
                        .gotoFirstWaypointMode(params.getMissionGotoWaypointMode())
                        .checkParameters()) != null) {
            SideToast.makeText(MainActivity.this, djiParameterError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
        } else {
            previousWayPointIndex.set(0);
            atomicInteger.set(0);
            isCompletedByStopping.set(false);

            DJIError djiErrorFirst = waypointMissionOperator.loadMission(builder.build());
            if (djiErrorFirst != null) {
                SideToast.makeText(MainActivity.this, djiErrorFirst.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
            } else {
                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        SideToast.makeText(MainActivity.this, djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
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
