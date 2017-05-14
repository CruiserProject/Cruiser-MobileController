package demo.amastigote.com.djimobilecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
import demo.amastigote.com.djimobilecontrol.ConverterUtil.ScreenSizeConverter;
import demo.amastigote.com.djimobilecontrol.DataUtil.OnboardDataEncoder;
import demo.amastigote.com.djimobilecontrol.DataUtil.WaypointMissionParams;
import demo.amastigote.com.djimobilecontrol.FlightModuleUtil.BatteryManager;
import demo.amastigote.com.djimobilecontrol.FlightModuleUtil.FlightControllerManager;
import demo.amastigote.com.djimobilecontrol.UIComponentUtil.RectView;
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
    private LinearLayout takeOffLinearLayout;
    private LinearLayout landLinearLayout;
    private LinearLayout followLinearLayout;
    private LinearLayout logLinearLayout;
    private ScrollView logScrollView;
    private RelativeLayout developerOptionsRelativeLayout;
    private ImageView remainingBatteryImageView;
    private ImageView gpsSignalLevelImageView;
    private ImageView rcSignalLevelImageView;
    private ImageView cameraShootImageView;
    private ImageView cameraSwitchImageView;
    private ImageView switchPanelImageView;
    private ImageView followStateImageView;
    private ImageView developOptionImageView;
    private TextView aircraftTextView;
    private TextView satelliteNumberTextView;
    private TextView statusAltitudeTextView;
    private TextView statusVelocityTextView;
    private TextView statusLandingTextView;
    private TextView statusTrackingTextView;
    private TextView followStateTextView;

    // a test for SendDataToOnBoardSDKDevice
    private Button sendDataLandingButton;

    private Button mapPanelUndoButton;
    private LinearLayout mapPanelCreateButton;
    private Button mapPanelStartMissionButton;
    private Button mapPanelCancelMissionButton;
    private Button mapPanelStopMissionButton;
    private Button missionConfigurationPanelOKButton;
    private Button missionConfigurationPanelCancelButton;
    private Button debugConfigurationExitButton;

    private RadioGroup radioGroupMissionStartAction;
    private RadioGroup radioGroupMissionFinishAction;
    private RadioGroup radioGroupPathMode;
    private RadioGroup radioGroupHeading;

    private Switch[] developSwitchGroup;


    private TextView textViewAutoFlightSpeed;
    private TextView textViewMaxFlightSpeed;
    private TextView textLogTitle;

    private SimpleProgressDialog startUpInfoDialog;
    private SimpleProgressDialog uploadInfoDialog;

    private TextureView videoTextureView;

    private RectView rectView;

    private ScreenSizeConverter screenSizeConverter;


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
    private byte[] coordinations = new byte[4];

    private AtomicInteger previousWayPointIndex = new AtomicInteger();
    private AtomicBoolean isCompletedByStopping = new AtomicBoolean();
    private AtomicBoolean isUsingPreciselyLanding = new AtomicBoolean(false);
    private AtomicBoolean isUsingObjectFollow = new AtomicBoolean(false);
    private AtomicBoolean isExectuingMission = new AtomicBoolean(false);

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

    private View.OnTouchListener videoTextureViewFrameLayoutOnTouchListener
            = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    rectView.setX1(event.getX());
                    rectView.setY1(event.getY());
                    rectView.setX2(event.getX());
                    rectView.setY2(event.getY());
                    rectView.setVisibility(View.VISIBLE);
                    break;
                case MotionEvent.ACTION_MOVE:
                    rectView.setX2(event.getX());
                    rectView.setY2(event.getY());
                    rectView.invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    videoTextureViewFrameLayout.setOnTouchListener(null);
                    rectView.setVisibility(View.GONE);
                    followLinearLayout.setVisibility(View.VISIBLE);

                    coordinations[0] = screenSizeConverter.convertX2XPercent(rectView.getX1());
                    coordinations[1] = screenSizeConverter.convertY2YPercent(rectView.getY1());
                    coordinations[2] = screenSizeConverter.convertX2XPercent(rectView.getX2());
                    coordinations[3] = screenSizeConverter.convertY2YPercent(rectView.getY2());

                    byte[] data_start = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.OBJECT_TRACKING_START, null);

                    if (baseProduct != null && baseProduct.isConnected() && flightController != null) {
                        flightController.sendDataToOnboardSDKDevice(data_start, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "正在请求启用目标追踪", SideToast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "请求启用目标追踪失败", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        SideToast.makeText(MainActivity.this, "无效操作：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                    }

                    break;

            }
            return true;
        }
    };


    private FlightController.OnboardSDKDeviceDataCallback onboardSDKDeviceDataCallback
            = new FlightController.OnboardSDKDeviceDataCallback() {
        @Override
        public void onReceive(final byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView txt = new TextView(MainActivity.this);
                    txt.setTextColor(Color.YELLOW);
                    txt.setShadowLayer(4.0f, 0.0f, 0.0f, Color.BLACK);
                    txt.setText(String.format(Locale.CHINA, "Received:  %x %x //  %x %x %x %x", bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]));
                    logLinearLayout.addView(txt);
                }
            });
            if (bytes[0] == 0x01) {
                switch (bytes[1]) {
                    case 0x02:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "精准辅助降落已启用", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x04:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "精准辅助降落已取消", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x06:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "开始垂直下降", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                final byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_STOP, null);
                                flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onResult(DJIError djiError) {
                                                    if (djiError != null) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                SideToast.makeText(MainActivity.this, "视觉辅助终止失败", SideToast.LENGTH_SHORT, SideToast.LENGTH_SHORT);
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                        break;
                    case 0x08:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "已成功降落", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x42:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusLandingTextView.setText(String.format(Locale.CHINA, "L:  deltaX: %d.%d   deltaY: %d.%d", (int) bytes[2], (int) bytes[3], (int) bytes[4], (int) bytes[5]));
                            }
                        });
                        break;
                    case 0x44:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //// TODO: 2017/5/12 update circle and radius
                            }
                        });
                        break;
                    default:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "不可识别的消息", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                            }
                        });
                        break;
                }
                return;
            }

            if (bytes[0] == 0x02) {
                switch (bytes[1]) {
                    case 0x02:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (flightController != null && baseProduct != null && baseProduct.isConnected()) {
                                    byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.OBJECT_TRACKING_VALUE, coordinations);
                                    flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SideToast.makeText(MainActivity.this, "正在上传目标信息", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                                    }
                                                });
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        SideToast.makeText(MainActivity.this, "上传目标信息失败", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                                    }
                                                });
                                            }
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "上传目标信息失败：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                        }
                                    });
                                }
//                                followStateTextView.setTextColor(Color.rgb(18,150,219));
                                followStateTextView.setText("目标跟踪 ON");
                                followStateImageView.setImageDrawable(getDrawable(R.mipmap.follow_on));
                                isUsingObjectFollow.set(true);

                                rectView.setX1(0.0f);
                                rectView.setY1(0.0f);
                                rectView.setX2(0.0f);
                                rectView.setY2(0.0f);

                                rectView.setVisibility(View.VISIBLE);
                            }
                        });
                        break;
                    case 0x04:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "目标跟踪已取消", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                followStateImageView.setImageDrawable(getDrawable(R.mipmap.follow));
//                                followStateTextView.setTextColor(followStateTextView.getTextColors().getDefaultColor());
                                followStateTextView.setText("目标跟踪 OFF");
                                isUsingObjectFollow.set(false);

                                rectView.setVisibility(View.GONE);

                            }
                        });
                        break;
                    case 0x42:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusTrackingTextView.setText(String.format(Locale.CHINA, "T:  deltaX: %d.%d   deltaY: %d.%d", (int) bytes[2], (int) bytes[3], (int) bytes[4], (int) bytes[5]));
                            }
                        });
                        break;
                    case 0x44:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rectView.setX1(screenSizeConverter.convertXPercent2X(bytes[2]));
                                rectView.setY1(screenSizeConverter.convertYPercent2Y(bytes[3]));
                                rectView.setX2(screenSizeConverter.convertXPercent2X(bytes[4]));
                                rectView.setY2(screenSizeConverter.convertYPercent2Y(bytes[5]));

                                rectView.invalidate();
                            }
                        });
                        break;
                    default:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "不可识别的消息", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR);
                            }
                        });
                        break;
                }
            }

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
                        statusVelocityTextView.setText("V: n/a");
                        statusAltitudeTextView.setText("A: n/a");
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

            if (baseProduct.isConnected()) {
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
        mapPanelCreateButton = (LinearLayout) mapViewPanel.findViewById(R.id.mv_btn_create);
        mapPanelUndoButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_undo);
        mapPanelCancelMissionButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_cancel);
        mapPanelStartMissionButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_start);
        mapPanelStopMissionButton = (Button) mapViewPanel.findViewById(R.id.mv_btn_stop_mission);
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
        developOptionImageView = (ImageView) findViewById(R.id.develop_option);
        gpsSignalLevelImageView = (ImageView) findViewById(R.id.gps_signal);
        rcSignalLevelImageView = (ImageView) findViewById(R.id.rc_signal);
        remainingBatteryImageView = (ImageView) findViewById(R.id.remaining_battery);
        cameraShootImageView = (ImageView) findViewById(R.id.camera_take);
        cameraSwitchImageView = (ImageView) findViewById(R.id.camera_switch);
        switchPanelImageView = (ImageView) findViewById(R.id.switch_panel);
        followStateImageView = (ImageView) findViewById(R.id.follow_img);
        followStateTextView = (TextView) findViewById(R.id.follow_txt);
        aircraftTextView = (TextView) findViewById(R.id.status_aircraft);
        satelliteNumberTextView = (TextView) findViewById(R.id.satellite_number_txt);
        statusAltitudeTextView = (TextView) findViewById(R.id.altitude_txt);
        statusVelocityTextView = (TextView) findViewById(R.id.velocity_txt);
        statusLandingTextView = (TextView) findViewById(R.id.landing_status_txt);
        statusTrackingTextView = (TextView) findViewById(R.id.tracking_status_txt);
        textLogTitle = (TextView) findViewById(R.id.log_title);
        takeOffLinearLayout = (LinearLayout) findViewById(R.id.takeoff);
        landLinearLayout = (LinearLayout) findViewById(R.id.land);
        followLinearLayout = (LinearLayout) findViewById(R.id.folllow_ll);

        logLinearLayout = (LinearLayout) findViewById(R.id.log_ll);
        logScrollView = (ScrollView) findViewById(R.id.log_sv);

        logScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                logLinearLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        logScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });

        rectView = new RectView(MainActivity.this);
        rectView.setX1(0.0f);
        rectView.setY1(0.0f);
        rectView.setX2(0.0f);
        rectView.setY2(0.0f);

        rectView.setElevation(Integer.MAX_VALUE);

        statusTrackingTextView.setVisibility(View.GONE);
        statusLandingTextView.setVisibility(View.GONE);
        textLogTitle.setVisibility(View.GONE);
        logLinearLayout.setVisibility(View.GONE);

        videoTextureViewFrameLayout.addView(rectView);

        developSwitchGroup = new Switch[7];

        developerOptionsRelativeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.debug_configuration, null);
        debugConfigurationExitButton = (Button) developerOptionsRelativeLayout.findViewById(R.id.debug_exit_btn);

        developSwitchGroup[0] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch1);
        developSwitchGroup[1] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch2);
        developSwitchGroup[2] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch3);
        developSwitchGroup[3] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch4);
        developSwitchGroup[4] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch5);
        developSwitchGroup[5] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch6);
        developSwitchGroup[6] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch7);

        //// TODO: 2017/5/12 delete this Button
        sendDataLandingButton = (Button) developerOptionsRelativeLayout.findViewById(R.id.send_data_test_btn);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(500, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        developerOptionsRelativeLayout.setElevation(Integer.MAX_VALUE);
        developerOptionsRelativeLayout.setLayoutParams(params);
        relativeLayoutMain.addView(developerOptionsRelativeLayout);
        developerOptionsRelativeLayout.setVisibility(View.GONE);


        screenSizeConverter = new ScreenSizeConverter(MainActivity.this);
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
                    uploadInfoDialog.dismiss();
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
                                            } else {
                                                isExectuingMission.set(true);
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
                isExectuingMission.set(false);

                if (djiError != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SideToast.makeText(MainActivity.this, "任务出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();

                            if (isMapPanelFocused) {
                                mapPanelStopMissionButton.setVisibility(View.GONE);
                                mapPanelCreateButton.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapPanelStopMissionButton.setVisibility(View.GONE);
                        mapPanelCreateButton.setVisibility(View.VISIBLE);
                    }
                });
                if (isUsingPreciselyLanding.get()) {
                    if (!isCompletedByStopping.get()) {
                        byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_START, null);
                        flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(final DJIError djiError) {
                                if (djiError == null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "正在请求视觉辅助精准降落", SideToast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "请求精准降落失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                        }
                                    });
                                }
                            }
                        });

                    } else {
                        flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "返航时出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "成功返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                } else {
                    flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SideToast.makeText(MainActivity.this, "返航时出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SideToast.makeText(MainActivity.this, "成功返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }


    private void initOnClickListener() {
        /*
            test sendData
         */
        sendDataLandingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flightController == null || !flightController.isConnected()) {
                    SideToast.makeText(MainActivity.this, "发送失败：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                    return;
                }
                byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_START, null);
                flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(final DJIError djiError) {
                        if (djiError == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SideToast.makeText(MainActivity.this, "正在请求视觉辅助精准降落", SideToast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SideToast.makeText(MainActivity.this, "请求精准降落失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                }
                            });
                        }
                    }
                });
            }
        });

        debugConfigurationExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (developSwitchGroup[0].isChecked()) {
                    statusVelocityTextView.setVisibility(View.VISIBLE);
                } else {
                    statusVelocityTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[1].isChecked()) {
                    statusAltitudeTextView.setVisibility(View.VISIBLE);
                } else {
                    statusAltitudeTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[2].isChecked()) {
                    statusLandingTextView.setVisibility(View.VISIBLE);
                } else {
                    statusLandingTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[3].isChecked()) {
                    statusTrackingTextView.setVisibility(View.VISIBLE);
                } else {
                    statusTrackingTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[6].isChecked()) {
                    logLinearLayout.setVisibility(View.VISIBLE);
                    textLogTitle.setVisibility(View.VISIBLE);
                } else {
                    logLinearLayout.setVisibility(View.GONE);
                    textLogTitle.setVisibility(View.GONE);
                }

                developerOptionsRelativeLayout.setVisibility(View.GONE);
            }
        });

        developOptionImageView.setClickable(true);
        developOptionImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                developerOptionsRelativeLayout.setVisibility(View.VISIBLE);
            }
        });

        followLinearLayout.setClickable(true);
        followLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isUsingObjectFollow.get()) {
                    SideToast.makeText(MainActivity.this, "在屏幕上滑动以框定目标", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
                    followLinearLayout.setVisibility(View.GONE);
                    videoTextureViewFrameLayout.setOnTouchListener(videoTextureViewFrameLayoutOnTouchListener);

                } else {
                    byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.OBJECT_TRACKING_STOP, null);
                    flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(final DJIError djiError) {
                            if (djiError == null) {
                                SideToast.makeText(MainActivity.this, "正在请求终止目标跟踪", SideToast.LENGTH_SHORT).show();
                            } else {
                                SideToast.makeText(MainActivity.this, "请求终止目标跟踪失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                            }
                        }
                    });
                }
            }
        });

        takeOffLinearLayout.setClickable(true);
        takeOffLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                } else {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            true,
                            "",
                            "确认起飞",
                            new SimpleDialogButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
                            }));
                }


            }
        });

        landLinearLayout.setClickable(true);
        landLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseProduct == null || !baseProduct.isConnected()) {
                    SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                } else {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            true,
                            "",
                            "确认降落",
                            new SimpleDialogButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
                            })
                    );
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
                                if (flightController == null || !flightController.isConnected()) {
                                    SideToast.makeText(MainActivity.this, "任务执行错误：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    return;
                                }
                                mapPanelStopMissionButton.setVisibility(View.VISIBLE);
                                linearLayoutForMap.setVisibility(View.VISIBLE);
                                switchPanelImageView.setVisibility(View.VISIBLE);
                                mapPanelStartMissionButton.setVisibility(View.GONE);
                                mapPanelCancelMissionButton.setVisibility(View.GONE);
                                mapPanelUndoButton.setVisibility(View.GONE);
                                LocationCoordinate3D homeLocation = flightController.getState().getAircraftLocation();
                                wayPointList.add(new Waypoint(homeLocation.getLatitude(), homeLocation.getLongitude(), 30.0f));

                                final int pointListSize = wayPointList.size();
                                LatLng latLngBD09 = CoordinationConverter.GPS2BD09(new LatLng(homeLocation.getLatitude(), homeLocation.getLongitude()));
                                baiduMap.addOverlay(new MarkerOptions()
                                        .position(latLngBD09)
                                        .animateType(MarkerOptions.MarkerAnimateType.grow)
                                        .flat(true)
                                        .anchor(0.5F, 0.5F)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker))
                                        .draggable(false));

                                baiduMap.addOverlay(new PolylineOptions()
                                        .points(new ArrayList<LatLng>() {
                                            {
                                                add(CoordinationConverter.GPS2BD09(new LatLng(
                                                        wayPointList.get(0).coordinate.getLatitude(),
                                                        wayPointList.get(0).coordinate.getLongitude()
                                                )));
                                                add(CoordinationConverter.GPS2BD09(new LatLng(
                                                        wayPointList.get(pointListSize - 1).coordinate.getLatitude(),
                                                        wayPointList.get(pointListSize - 1).coordinate.getLongitude()
                                                )));
                                            }
                                        })
                                        .color(R.color.purple)
                                        .dottedLine(true)
                                );

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
                mapPanelUndoButton.setVisibility(View.GONE);
                baiduMap.setOnMapLongClickListener(null);
                mapViewPanel.addView(missionConfigurationPanel);
            }
        });

        mapPanelStopMissionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SideToast.makeText(MainActivity.this, "成功取消任务", SideToast.LENGTH_SHORT).show();
                                    mapPanelStopMissionButton.setVisibility(View.GONE);
                                    mapPanelCreateButton.setVisibility(View.VISIBLE);
                                }
                            });
                            isCompletedByStopping.set(true);

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SideToast.makeText(MainActivity.this, "未能成功取消任务", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                }
                            });
                        }
                    }
                });
                return true;
            }
        });

        mapPanelStopMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SideToast.makeText(MainActivity.this, "长按以取消任务", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
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
                isCompletedByStopping.set(false);
                isUsingPreciselyLanding.set(false);

                wayPointList.clear();
                baiduMap.clear();
                baiduMap.setOnMapLongClickListener(onMapLongClickListener);
                WaypointMissionFinishedAction waypointMissionFinishedAction;
                WaypointMissionFlightPathMode waypointMissionFlightPathMode;
                WaypointMissionGotoWaypointMode waypointMissionGotoWaypointModel;
                WaypointMissionHeadingMode waypointMissionHeadingMode;

                switch (radioGroupMissionFinishAction.getCheckedRadioButtonId()) {
                    case R.id.rb_c:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        isUsingPreciselyLanding.set(false);
                        break;
                    case R.id.rb_d:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        isUsingPreciselyLanding.set(false);
                        break;
                    case R.id.rb_e:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        isUsingPreciselyLanding.set(true);
                        break;
                    default:
                        isUsingPreciselyLanding.set(false);
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
                mapPanelUndoButton.setVisibility(View.VISIBLE);
            }
        });

        mapPanelUndoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                baiduMap.clear();
                wayPointList.clear();
                return true;
            }
        });

        mapPanelUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SideToast.makeText(MainActivity.this, "长按以清空路径点", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
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
                statusAltitudeTextView.setText(String.format(Locale.CHINA, "A: %.1f", flightControllerState.getAircraftLocation().getAltitude()));
                float velocity = (float) Math.sqrt(Math.pow(flightControllerState.getVelocityX(), 2) + Math.pow(flightControllerState.getVelocityY(), 2));
                statusVelocityTextView.setText(String.format(Locale.CHINA, "V: %.1f", velocity));
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

    private void switchMapPanelFocus() {
        if (!isMapPanelFocused) {
            if (isExectuingMission.get()) {
                mapPanelStopMissionButton.setVisibility(View.VISIBLE);
            } else {
                mapPanelCreateButton.setVisibility(View.VISIBLE);
            }
            relativeLayoutMain.removeView(cameraShootImageView);
            relativeLayoutMain.removeView(cameraSwitchImageView);
            linearLayoutForMap.removeView(mapViewPanel);
            videoTextureViewFrameLayout.removeView(videoTextureView);
            videoTextureViewFrameLayout.addView(mapViewPanel);
            linearLayoutForMap.addView(videoTextureView);
            followLinearLayout.setVisibility(View.GONE);
        } else {
            if (isExectuingMission.get()) {
                mapPanelStopMissionButton.setVisibility(View.GONE);
            } else {
                mapPanelCreateButton.setVisibility(View.GONE);
            }
            videoTextureViewFrameLayout.removeView(mapViewPanel);
            linearLayoutForMap.removeView(videoTextureView);
            linearLayoutForMap.addView(mapViewPanel);
            videoTextureViewFrameLayout.addView(videoTextureView);
            relativeLayoutMain.addView(cameraShootImageView);
            relativeLayoutMain.addView(cameraSwitchImageView);
            followLinearLayout.setVisibility(View.VISIBLE);
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
            uploadInfoDialog = new SimpleProgressDialog(MainActivity.this, "正在上传数据…");
            uploadInfoDialog.show();
            if (djiErrorFirst != null) {
                SideToast.makeText(MainActivity.this, djiErrorFirst.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
            } else {
                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            uploadInfoDialog.switchMessage("正在重新上传数据…");
                            waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(final DJIError djiError) {
                                    if (djiError != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                SideToast.makeText(MainActivity.this, djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                                uploadInfoDialog.dismiss();
                                            }
                                        });
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
