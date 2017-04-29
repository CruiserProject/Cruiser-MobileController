package demo.amastigote.com.djimobilecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.UiSettings;

import java.util.List;
import java.util.Locale;

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
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.sdkmanager.DJISDKManager;


public class MainActivity extends Activity {
    private static BaseProduct baseProduct;


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

    private SimpleProgressDialog startUpInfoDialog;

    private TextureView videoTextureView;
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

    /*
        baidu map
     */
    private LinearLayout linearLayoutForMap;
    private MapView mapView;
    private RelativeLayout mapViewPanel;
    private BaiduMap baiduMap;

    /*
        DJI sdk
     */
    private int currentBatteryInPercent = -1;
    private boolean isMapPanelFocused = false;
    private boolean isRecording = false;
    private SettingsDefinitions.CameraMode curCameraMode = SettingsDefinitions.CameraMode.UNKNOWN;
    private Camera camera;
    private DJICodecManager djiCodecManager;
    private Battery battery;
    private FlightController flightController;
    private MissionControl missionControl;
    private FlightController.OnboardSDKDeviceDataCallback onboardSDKDeviceDataCallback
            = new FlightController.OnboardSDKDeviceDataCallback() {
        @Override
        public void onReceive(byte[] bytes) {
            SideToast.makeText(MainActivity.this,"成功收到消息:" + bytes.toString(),SideToast.LENGTH_SHORT);
            Log.e("Onboard device message",">> " + bytes);
        }
    };
    private BaseProduct.BaseProductListener baseProductListener
            = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {

        }

        @Override
        public void onConnectivityChange(final boolean b) {
            if(b){
                SideToast.makeText(MainActivity.this,"飞行器已连接",SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aircraftTextView.setText(baseProduct.getModel().toString());
                    }
                });
                changeCameraState();
            }else{
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
    private FlightControllerState.Callback fcsCallback
            = new FlightControllerState.Callback() {
        @Override
        public void onUpdate(@NonNull final FlightControllerState flightControllerState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stateAltitudeTextView.setText(String.format(Locale.CHINA, "Altitude: %.1f", flightControllerState.getAircraftLocation().getAltitude()));
                    float velocity = (float) Math.sqrt(Math.pow(flightControllerState.getVelocityX(), 2) + Math.pow(flightControllerState.getVelocityY(), 2));
                    stateVelocityTextView.setText(String.format(Locale.CHINA, "Velocity: %.1f", velocity));
                }
            });
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        initPermissionRequest();
        initUI();
        initBaiduMap();
        initVideoTextureView();
        initOnClickListener();
        initSendDataOnClickListener();

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
        mapPanelCreateButton = (Button) findViewById(R.id.mv_btn_create);
        mapPanelUndoButton = (Button) findViewById(R.id.mv_btn_undo);
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
        if(baseProduct != null){
            camera = baseProduct.getCamera();
        }
    }

    private void initMissionControl() {
        missionControl = MissionControl.getInstance();
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
                    switch (curCameraMode){
                        case SHOOT_PHOTO:
                            camera.startShootPhoto(null);
                            break;
                        case RECORD_VIDEO:
                            if(isRecording){
                                camera.stopRecordVideo(null);
                                isRecording = false;
                            }else{
                                camera.startRecordVideo(null);
                                isRecording = true;
                            }
                            break;
                        case UNKNOWN:
                            SideToast.makeText(MainActivity.this,"相机连接错误",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
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
                }else{
                    switch (curCameraMode){
                        case SHOOT_PHOTO:
                            camera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if(djiError  == null){
                                        curCameraMode = SettingsDefinitions.CameraMode.RECORD_VIDEO;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_record));
                                            }
                                        });
                                    }else{
                                        SideToast.makeText(MainActivity.this,"相机切换状态失败",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR).show();
                                    }
                                }
                            });
                            break;
                        case RECORD_VIDEO:
                            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if(djiError == null){
                                        curCameraMode = SettingsDefinitions.CameraMode.SHOOT_PHOTO;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_take));
                                            }
                                        });
                                    }else{
                                        SideToast.makeText(MainActivity.this,"相机切换状态失败",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR).show();
                                    }
                                }
                            });
                            break;
                        case UNKNOWN:
                            SideToast.makeText(MainActivity.this,"相机连接错误",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
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

    }

    private void updateBatteryState(int remainingBattery) {
        if (currentBatteryInPercent == -1) {
            currentBatteryInPercent = remainingBattery;
            final int temp[] = new int[1];
            if (currentBatteryInPercent <= 10) {
                temp[0] = R.mipmap.battery_0;
            } else {
                if (currentBatteryInPercent <= 30) {
                    temp[0] = R.mipmap.battery_10;
                } else {
                    if (currentBatteryInPercent <= 50) {
                        temp[0] = R.mipmap.battery_30;
                    } else {
                        if (currentBatteryInPercent <= 70) {
                            temp[0] = R.mipmap.battery_50;
                        } else {
                            if (currentBatteryInPercent <= 85) {
                                temp[0] = R.mipmap.battery_70;
                            } else {
                                temp[0] = R.mipmap.battery_100;
                            }
                        }
                    }
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remainingBatteryImageView.setImageDrawable(MainActivity.this.getDrawable(temp[0]));
                }
            });
        } else {
            final int temp[] = new int[1];
            if (remainingBattery <= 10 && currentBatteryInPercent > 10) {
                temp[0] = R.mipmap.battery_0;
                currentBatteryInPercent = remainingBattery;
                SideToast.makeText(MainActivity.this, "电池电量低", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
            } else {
                if (remainingBattery <= 30 && currentBatteryInPercent > 30) {
                    temp[0] = R.mipmap.battery_10;
                    currentBatteryInPercent = remainingBattery;
                } else {
                    if (remainingBattery <= 50 && currentBatteryInPercent > 50) {
                        temp[0] = R.mipmap.battery_30;
                        currentBatteryInPercent = remainingBattery;
                    } else {
                        if (remainingBattery <= 70 && currentBatteryInPercent > 70) {
                            temp[0] = R.mipmap.battery_50;
                            currentBatteryInPercent = remainingBattery;
                        } else {
                            if (remainingBattery <= 85 && currentBatteryInPercent > 85) {
                                temp[0] = R.mipmap.battery_70;
                                currentBatteryInPercent = remainingBattery;
                            } else {
                                return;
                            }
                        }
                    }
                }

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remainingBatteryImageView.setImageDrawable(MainActivity.this.getDrawable(temp[0]));
                }
            });

        }
    }

    private void changeCameraState(){
        camera.getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.CameraMode cameraMode) {
                if(cameraMode.equals(SettingsDefinitions.CameraMode.SHOOT_PHOTO)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraShootImageView.setImageDrawable(MainActivity.this.getDrawable(R.mipmap.camera_take));
                            curCameraMode = SettingsDefinitions.CameraMode.SHOOT_PHOTO;
                        }
                    });
                }else{
                    if(cameraMode.equals(SettingsDefinitions.CameraMode.RECORD_VIDEO)){
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
                Log.e("Camera State error",">>" + djiError.toString());
//                        SideToast.makeText(MainActivity.this,"获取相机状态失败",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
                curCameraMode = SettingsDefinitions.CameraMode.UNKNOWN;
            }
        });
    }

    private void initSendDataOnClickListener(){
        sendDataToOnBoardSDKDeviceButton = (Button)findViewById(R.id.test_send_data_btn);
        sendDataToOnBoardSDKDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(baseProduct == null || !baseProduct.isConnected()){
                    SideToast.makeText(MainActivity.this,"发送失败：飞行器未连接",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR).show();
                }else{
                    byte arr[] = new byte[10];
                    arr[0] = 2 + '0';
                    arr[1] = 3 + '0';
                    arr[2] = 3 + '0';
                    arr[3] = 3 + '0';
                    arr[4] = 3 + '0';
                    arr[5] = 3 + '0';
                    arr[6] = 3 + '0';
                    arr[7] = 3 + '0';
                    arr[8] = 3 + '0';
                    arr[9] = '\0';
                    flightController.sendDataToOnboardSDKDevice(arr, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if(djiError == null){
                                SideToast.makeText(MainActivity.this,"发送成功",SideToast.LENGTH_SHORT).show();
                            }else{
                                SideToast.makeText(MainActivity.this,"发送失败: 请查看日志",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR).show();
                                Log.e("SendDataToOnBoard",">> " + djiError);
                            }
                        }
                    });

                }

            }
        });
    }

    private void switchMapPanelFocus() {
        if(!isMapPanelFocused){
            relativeLayoutMain.removeView(cameraPlayImageView);
            relativeLayoutMain.removeView(cameraShootImageView);
            relativeLayoutMain.removeView(cameraSwitchImageView);
            linearLayoutForMap.removeView(mapViewPanel);
            videoTextureViewFrameLayout.removeView(videoTextureView);
            videoTextureViewFrameLayout.addView(mapViewPanel);
            linearLayoutForMap.addView(videoTextureView);
            mapPanelUndoButton.setVisibility(View.VISIBLE);
            mapPanelCreateButton.setVisibility(View.VISIBLE);
        }else{
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

}
