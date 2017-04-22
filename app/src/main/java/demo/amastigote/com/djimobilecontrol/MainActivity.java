package demo.amastigote.com.djimobilecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
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

import demo.amastigote.com.djimobilecontrol.FlightModuleUtil.FlightControllerManager;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SideToast;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleAlertDialog;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleDialogButton;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.SimpleProgressDialog;
import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.products.Aircraft;
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
    private TextView aircraftTextView;
    private TextView statusDescriptionTextView;
    private TextView satelliteNumberTextView;
    private TextView stateAltitudeTextView;
    private TextView stateVelocityTextView;

    private SimpleProgressDialog startUpInfoDialog;

    private TextureView videoTextureView;
    private TextureView.SurfaceTextureListener textureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if(djiCodecManager == null){
                djiCodecManager = new DJICodecManager(MainActivity.this,surface,width,height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if(djiCodecManager != null){
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
    private DJICodecManager djiCodecManager;
    private FlightController flightController;
    private MissionControl missionControl;
    private BaseProduct.BaseProductListener baseProductListener
            = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {

        }

        @Override
        public void onConnectivityChange(final boolean b) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (b) {
                        SideToast.makeText(MainActivity.this, "飞行器已连接", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                        aircraftTextView.setText(baseProduct.getModel().toString());
                    } else {
                        SideToast.makeText(MainActivity.this, "飞行器已断开连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                        aircraftTextView.setText("飞行器未连接");
                        stateVelocityTextView.setText("");
                        stateAltitudeTextView.setText("");
                    }
                }
            });
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SideToast.makeText(MainActivity.this, "飞行器已连接", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                    aircraftTextView.setText(baseProduct.getModel().toString());
                }
            });

            initFlightController();
            initMissionControl();
            baseProduct.setBaseProductListener(baseProductListener);


            List<VideoFeeder.VideoFeed> videoFeeds = VideoFeeder.getInstance().getVideoFeeds();
            if(videoFeeds.size() != 0){
                videoFeeds.get(0).setCallback(new VideoFeeder.VideoDataCallback() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {
                        if(djiCodecManager != null){
                            djiCodecManager.sendDataToDecoder(videoBuffer,size);
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
                    stateAltitudeTextView.setText(String.format(Locale.CHINA,"Altitude: %.1f",flightControllerState.getAircraftLocation().getAltitude()));
                    float velocity = (float)Math.sqrt(Math.pow(flightControllerState.getVelocityX(),2) + Math.pow(flightControllerState.getVelocityY(),2));
                    stateVelocityTextView.setText(String.format(Locale.CHINA,"Velocity: %.1f",velocity));
                }
            });
        }
    };
    private BatteryState.Callback batteryCallback
            = new BatteryState.Callback() {
        @Override
        public void onUpdate(final BatteryState batteryState) {
            final int remainingBatteryInPercent = batteryState.getChargeRemainingInPercent();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO: 2017/4/22 battery state update 
                }
            });
        }
    }
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

    private void initVideoTextureView(){
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

    private void initFlightController(){
        flightController = FlightControllerManager.getInstance(baseProduct);
        flightController.setStateCallback(fcsCallback);
    }

    private void initMissionControl(){
        missionControl = MissionControl.getInstance();
    }

    private void initOnClickListener() {
        takeOffImageView.setClickable(true);
        takeOffImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!baseProduct.isConnected() || baseProduct == null){
                    SideToast.makeText(MainActivity.this,"无效的操作:起飞",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
                }else{
                    missionControl.startElement(new TakeOffAction());
                }
            }
        });

        landImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!baseProduct.isConnected() || baseProduct == null){
                    SideToast.makeText(MainActivity.this,"无效的操作：返航",SideToast.LENGTH_SHORT,SideToast.TYPE_ERROR);
                }else{
                    missionControl.startElement(new GoHomeAction());
                }
            }
        });
    }


}
