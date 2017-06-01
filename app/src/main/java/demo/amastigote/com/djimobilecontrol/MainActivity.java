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
import android.view.MotionEvent;
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
import android.widget.SeekBar;
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

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import demo.amastigote.com.djimobilecontrol.ConverterUtil.CoordinationConverter;
import demo.amastigote.com.djimobilecontrol.ConverterUtil.DensityUtil;
import demo.amastigote.com.djimobilecontrol.ConverterUtil.LLDistanceConverter;
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
import dji.common.model.LocationCoordinate2D;
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
    private final static String LINE_BROKER = "\r\n";
    private final AtomicInteger atomicInteger = new AtomicInteger();
    //// TODO: 2017/5/17 check static is necessary
    private BaseProduct baseProduct;
    /*
        UI components
     */
    private RelativeLayout relativeLayoutMain;
    private FrameLayout videoTextureViewFrameLayout;
    private LinearLayout takeOffLinearLayout;
    private LinearLayout landLinearLayout;
    private LinearLayout mapPanelCreateLinearLayout;
    private LinearLayout followLinearLayout;
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
    private TextView statusVerticaLDistanceTextView;
    private TextView statusHorizontalDistanceTextView;
    private TextView statusHorizontalVelocityTextView;
    private TextView statusVerticalVelocityTextView;
    private TextView statusLandingTextView;
    private TextView statusTrackingTextView;
    private TextView followStateTextView;
    private Button mapPanelUndoButton;
    private Button mapPanelStartMissionButton;
    private Button mapPanelCancelMissionButton;
    private Button mapPanelStopMissionButton;
    private Button missionConfigurationPanelOKButton;
    private Button missionConfigurationPanelCancelButton;
    private Button debugConfigurationExitButton;
    private SeekBar autoFlightSpeedSeekbar;
    private SeekBar flightHeightSeekbar;
    private TextView textViewAutoFlightSpeed;
    private TextView textViewflightHeight;
    // a test for SendDataToOnBoardSDKDevice
    private Button debugSendDataLandingButton;
    private Button debugSendDataLandingCancelButton;
    private RadioGroup radioGroupMissionStartAction;
    private RadioGroup radioGroupMissionFinishAction;
    private RadioGroup radioGroupPathMode;
    private RadioGroup radioGroupHeading;
    private Switch[] developSwitchGroup;

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
    private float waypointMissionFlightHeight = 30.0f;
    private byte[] coordinators = new byte[4];
    private AtomicInteger previousWayPointIndex = new AtomicInteger();
    private AtomicBoolean isCompletedByStopping = new AtomicBoolean();
    private AtomicInteger missionFinishMode = new AtomicInteger();
    private AtomicBoolean isUsingObjectFollow = new AtomicBoolean(false);
    private AtomicBoolean isExecutingMission = new AtomicBoolean(false);
    private AtomicBoolean isSocketConnected = new AtomicBoolean(false);
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BlockingQueue<String> logQ = new LinkedBlockingQueue<>();
    /*
        DJI sdk
     */
    private int currentBatteryInPercent = -1;
    private int currentSatellitesCount = -1;
    private boolean isDrawingLandingCircle = true;
    private boolean isDrawingFollowObject = true;
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
    private WaypointMission mission;

    private List<Waypoint> wayPointList = new ArrayList<>();

    private BaiduMap.OnMapLongClickListener onMapLongClickListener
            = new BaiduMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            LatLng latLngGPS84 = CoordinationConverter.BD092GPS84(latLng);
            wayPointList.add(new Waypoint(
                    latLngGPS84.latitude,
                    latLngGPS84.longitude,
                    waypointMissionFlightHeight)
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
            mapPanelCreateLinearLayout.setVisibility(View.GONE);
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

                    coordinators[0] = screenSizeConverter.convertX2XPercent(rectView.getX1());
                    coordinators[1] = screenSizeConverter.convertY2YPercent(rectView.getY1());
                    coordinators[2] = screenSizeConverter.convertX2XPercent(rectView.getX2());
                    coordinators[3] = screenSizeConverter.convertY2YPercent(rectView.getY2());

                    if (flightController != null && baseProduct != null && baseProduct.isConnected()) {
                        byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.OBJECT_TRACKING_VALUE, coordinators);
                        sendLogToServer(String.format(Locale.CHINA, "Sent: %d %d %d %d", data[2], data[3], data[4], data[5]));
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

                    rectView.setX1(0.0f);
                    rectView.setY1(0.0f);
                    rectView.setX2(0.0f);
                    rectView.setY2(0.0f);

                    rectView.setVisibility(View.VISIBLE);


                    break;

            }
            return true;
        }
    };
    private FlightController.OnboardSDKDeviceDataCallback onboardSDKDeviceDataCallback
            = new FlightController.OnboardSDKDeviceDataCallback() {
        @Override
        public void onReceive(final byte[] bytes) {
            sendLogToServer(String.format(Locale.CHINA, "Received:  %02x %02x %d %d %d %d %d %d", bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]));
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    TextView textView = new TextView(MainActivity.this);
//                    textView.setText(String.format(Locale.CHINA, "Received:  %02x %02x %02x %02x %02x %02x", bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]));
//                    textView.setTextColor(Color.RED);
//                    logTexts.add(textView);
//                    logLinearLayout.addView(textView);
//                }
//            });
            if (bytes[0] == 0x01) {
                switch (bytes[1]) {
                    case 0x02:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "视觉辅助降落已启用", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x04:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "视觉辅助降落已取消", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x06:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "开始垂直下降", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                            }
                        });
                        break;
                    case 0x08:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "已成功降落", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
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
                    case 0x42:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String str;
                                if (bytes[6] == -1 && bytes[7] == -1) {
                                    str = String.format(Locale.CHINA, "L:  deltaX: -%d.%d    deltaY: -%d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                } else {
                                    if (bytes[6] == -1) {
                                        str = String.format(Locale.CHINA, "L:  deltaX: -%d.%d    deltaY: %d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                    } else {
                                        if (bytes[7] == -1) {
                                            str = String.format(Locale.CHINA, "L:  deltaX: %d.%d    deltaY: -%d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                        } else {
                                            str = String.format(Locale.CHINA, "L:  deltaX: %d.%d    deltaY: %d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                        }
                                    }
                                }
                                statusLandingTextView.setText(str);
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
                                SideToast.makeText(MainActivity.this, "在屏幕上滑动以框定目标", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
                                isUsingObjectFollow.set(true);
                                followStateTextView.setText("目标跟踪 ON");
                                followStateImageView.setImageDrawable(getDrawable(R.mipmap.follow_on));
                                followLinearLayout.setVisibility(View.GONE);
                                videoTextureViewFrameLayout.setOnTouchListener(videoTextureViewFrameLayoutOnTouchListener);
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
                    case 0x12:
                        break;
                    case 0x42:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String str;
                                if (bytes[6] == -1 && bytes[7] == -1) {
                                    str = String.format(Locale.CHINA, "T:  deltaX: -%d.%d    deltaY: -%d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                } else {
                                    if (bytes[6] == -1) {
                                        str = String.format(Locale.CHINA, "T:  deltaX: -%d.%d    deltaY: %d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                    } else {
                                        if (bytes[7] == -1) {
                                            str = String.format(Locale.CHINA, "T:  deltaX: %d.%d    deltaY: -%d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                        } else {
                                            str = String.format(Locale.CHINA, "T:  deltaX: %d.%d    deltaY: %d.%d", bytes[2], bytes[3], bytes[4], bytes[5]);
                                        }
                                    }
                                }
                                statusTrackingTextView.setText(str);
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

//                                rectView.setElevation(Integer.MAX_VALUE - 1);

                                if (isDrawingFollowObject) {
                                    rectView.invalidate();
                                }
                            }
                        });
                        break;
                    default:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SideToast.makeText(MainActivity.this, "不可识别的消息", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
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
                        statusHorizontalVelocityTextView.setText("N/A");
                        statusHorizontalDistanceTextView.setText("N/A");
                        statusVerticaLDistanceTextView.setText("N/A");
                        statusVerticalVelocityTextView.setText("N/A");
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
            updateFlightParams(flightControllerState);
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
                        "验证失败",
                        "请检查网络连接",
                        new SimpleDialogButton("退出", new DialogInterface.OnClickListener() {
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

    private void sendLogToServer(final String str) {
        logQ.add(str + LINE_BROKER);
    }

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

        checkSocketConnection();

        waypointMissionParams = new WaypointMissionParams();

        startUpInfoDialog = new SimpleProgressDialog(MainActivity.this, "正在验证应用程序…");

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
        mapPanelCreateLinearLayout = (LinearLayout) mapViewPanel.findViewById(R.id.mv_btn_create);
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
        uiSettings.setScrollGesturesEnabled(true);
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
        autoFlightSpeedSeekbar = (SeekBar) missionConfigurationPanel.findViewById(R.id.afs_seekbar);
        flightHeightSeekbar = (SeekBar) missionConfigurationPanel.findViewById(R.id.fh_seekbar);
        textViewAutoFlightSpeed = (TextView) missionConfigurationPanel.findViewById(R.id.afs_txt);
        textViewflightHeight = (TextView) missionConfigurationPanel.findViewById(R.id.fh_txt);

        autoFlightSpeedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewAutoFlightSpeed.setText(String.format(Locale.CHINA, "飞行速度： %d m/s", progress + 2));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        flightHeightSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewflightHeight.setText(String.format(Locale.CHINA, "飞行高度： %d m", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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
        statusVerticaLDistanceTextView = (TextView) findViewById(R.id.altitude_txt);
        statusHorizontalDistanceTextView = (TextView) findViewById(R.id.horizontal_distance_txt);
//        //// TODO: 2017/5/30 test
//        statusHorizontalDistanceTextView.setVisibility(View.GONE);
        statusHorizontalVelocityTextView = (TextView) findViewById(R.id.velocity_txt);
        statusVerticalVelocityTextView = (TextView) findViewById(R.id.vertical_velocity_txt);
        statusLandingTextView = (TextView) findViewById(R.id.landing_status_txt);
        statusTrackingTextView = (TextView) findViewById(R.id.tracking_status_txt);
        takeOffLinearLayout = (LinearLayout) findViewById(R.id.takeoff);
        landLinearLayout = (LinearLayout) findViewById(R.id.land);
        followLinearLayout = (LinearLayout) findViewById(R.id.folllow_ll);


        rectView = new RectView(MainActivity.this);
        rectView.setX1(0.0f);
        rectView.setY1(0.0f);
        rectView.setX2(0.0f);
        rectView.setY2(0.0f);

        rectView.setElevation(Integer.MAX_VALUE);

        statusTrackingTextView.setVisibility(View.GONE);
        statusLandingTextView.setVisibility(View.GONE);

        videoTextureViewFrameLayout.addView(rectView);

        developSwitchGroup = new Switch[7];

        developerOptionsRelativeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.debug_configuration, null);
        debugConfigurationExitButton = (Button) developerOptionsRelativeLayout.findViewById(R.id.debug_exit_btn);

        developSwitchGroup[0] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch1);
        developSwitchGroup[1] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch2);
        developSwitchGroup[2] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch3);
        developSwitchGroup[3] = (Switch) developerOptionsRelativeLayout.findViewById(R.id.switch4);

        //// TODO: 2017/5/12 delete this Button
        debugSendDataLandingButton = (Button) developerOptionsRelativeLayout.findViewById(R.id.send_data_test_btn);
        debugSendDataLandingCancelButton = (Button) developerOptionsRelativeLayout.findViewById(R.id.send_data_test_cancel_btn);
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
        if (baseProduct != null && baseProduct.isConnected()) {
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
                Log.e(">> ", String.valueOf(waypointMissionUploadEvent.getProgress().uploadedWaypointIndex));
                if (waypointMissionUploadEvent.getProgress() != null
                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == waypointMissionUploadEvent.getProgress().totalWaypointCount - 1
                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                        && previousWayPointIndex.get() != waypointMissionUploadEvent.getProgress().uploadedWaypointIndex) {
                    previousWayPointIndex.set(waypointMissionUploadEvent.getProgress().uploadedWaypointIndex);
                    uploadInfoDialog.dismiss();
                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(final DJIError djiError) {
                            if (djiError != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SideToast.makeText(MainActivity.this, "任务执行失败:" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                        mapPanelStopMissionButton.setVisibility(View.GONE);
                                        linearLayoutForMap.setVisibility(View.VISIBLE);
                                        switchPanelImageView.setVisibility(View.VISIBLE);
                                        mapPanelStartMissionButton.setVisibility(View.GONE);
                                        mapPanelCancelMissionButton.setVisibility(View.GONE);
                                        mapPanelUndoButton.setVisibility(View.GONE);
                                        mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                                    }
                                });
                            } else {
                                isExecutingMission.set(true);

                            }
                        }
                    });


                }
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

            }

            @Override
            public void onExecutionStart() {
                Log.e(">> Status", "Execution Start");
                mission = null;
                System.gc();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapPanelStopMissionButton.setVisibility(View.VISIBLE);
                        mapPanelCreateLinearLayout.setVisibility(View.GONE);
                    }
                });

            }


            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                Log.e(">> Status", "Execution Finish");
                isExecutingMission.set(false);

                if (djiError != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SideToast.makeText(MainActivity.this, "任务出现错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();

                            if (isMapPanelFocused) {
                                mapPanelStopMissionButton.setVisibility(View.GONE);
                                mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isMapPanelFocused) {
                            mapPanelStopMissionButton.setVisibility(View.GONE);
                            mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });

                if (isCompletedByStopping.get()) {
                    if (missionFinishMode.get() != 0) {
                        goHome();
                    }
                    return;
                }

                switch (missionFinishMode.get()) {
                    case 0:
                        break;
                    case 1:
                        goHome();
                        break;
                    case 2:
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_START, null);
                                flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(final DJIError djiError) {
                                        if (djiError == null) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "正在请求视觉辅助降落", SideToast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "请求降落失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        };

                        Timer timer = new Timer();
                        timer.schedule(task, 10000);
                        break;
                    default:
                        goHome();
                        break;
                }

                /*if (isUsingPreciselyLanding.get()) {
                    if (!isCompletedByStopping.get()) {
                        byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_START, null);
                        flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(final DJIError djiError) {
                                if (djiError == null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "正在请求视觉辅助降落", SideToast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SideToast.makeText(MainActivity.this, "请求降落失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
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
                                            SideToast.makeText(MainActivity.this, "正在返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
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
                                        SideToast.makeText(MainActivity.this, "正在返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
                                    }
                                });
                            }
                        }
                    });
                }*/
            }
        });
    }

    private void goHome() {
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
                            SideToast.makeText(MainActivity.this, "正在返航", SideToast.LENGTH_SHORT, SideToast.TYPE_NORMAL).show();
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
        debugSendDataLandingButton.setOnClickListener(new View.OnClickListener() {
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

        debugSendDataLandingCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flightController == null || !flightController.isConnected()) {
                    SideToast.makeText(MainActivity.this, "发送失败：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                    return;
                }
                byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.VISUAL_LANDING_STOP, null);
                flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SideToast.makeText(MainActivity.this, "视觉辅助降落终止失败", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
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
                    statusLandingTextView.setVisibility(View.VISIBLE);
                } else {
                    statusLandingTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[1].isChecked()) {
                    statusTrackingTextView.setVisibility(View.VISIBLE);
                } else {
                    statusTrackingTextView.setVisibility(View.GONE);
                }

                if (developSwitchGroup[2].isChecked()) {
                    isDrawingLandingCircle = true;
                } else {
                    isDrawingLandingCircle = false;
                }

                if (developSwitchGroup[3].isChecked()) {
                    isDrawingFollowObject = true;
                } else {
                    isDrawingFollowObject = false;
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
                    if (baseProduct == null || !baseProduct.isConnected()) {
                        SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                        return;
                    }
                    byte[] data = OnboardDataEncoder.encode(OnboardDataEncoder.DataType.OBJECT_TRACKING_START, null);

                    flightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(final DJIError djiError) {
                            if (djiError == null) {
                                SideToast.makeText(MainActivity.this, "正在请求启用目标跟踪", SideToast.LENGTH_SHORT).show();
                            } else {
                                SideToast.makeText(MainActivity.this, "请求启用目标跟踪失败：" + djiError.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                            }
                        }
                    });

                } else {
                    if (baseProduct == null || !baseProduct.isConnected()) {
                        SideToast.makeText(MainActivity.this, "无效操作：飞机未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                        return;
                    }

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
                            new SimpleDialogButton("是", new DialogInterface.OnClickListener() {
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
                            new SimpleDialogButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    goHome();
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
                                isRecording = false;
                                camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "相机发生错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "已停止录像", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
                                                }
                                            });
                                        }
                                    }
                                });
                            } else {
                                isRecording = true;
                                camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "相机发生错误", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SideToast.makeText(MainActivity.this, "已开始录像", SideToast.LENGTH_SHORT, SideToast.TYPE_WARNING).show();
                                                }
                                            });
                                        }
                                    }
                                });
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

        mapPanelCreateLinearLayout.setOnClickListener(newMissionOnClickListener);

        mapPanelStartMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleAlertDialog.show(
                        MainActivity.this,
                        true,
                        "任务确认",
                        "是否立即开始任务",
                        new SimpleDialogButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (flightController == null || !flightController.isConnected()) {
                                    SideToast.makeText(MainActivity.this, "任务执行错误：飞行器未连接", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    return;
                                }

                                if (wayPointList.size() <= 0) {
                                    SideToast.makeText(MainActivity.this, "路径点数量不合法", SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                                    return;
                                }
                                LocationCoordinate3D curLocation = flightController.getState().getAircraftLocation();
                                wayPointList.add(0, new Waypoint(curLocation.getLatitude(), curLocation.getLongitude(), waypointMissionFlightHeight));
                                LatLng latLngBD09 = CoordinationConverter.GPS2BD09(new LatLng(curLocation.getLatitude(), curLocation.getLongitude()));
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
                                                        wayPointList.get(1).coordinate.getLatitude(),
                                                        wayPointList.get(1).coordinate.getLongitude()
                                                )));
                                            }
                                        })
                                        .color(R.color.purple)
                                        .dottedLine(true)
                                );
                                final LocationCoordinate2D homeLocation = flightController.getState().getHomeLocation();
                                final int pointListSize = wayPointList.size();
                                switch (missionFinishMode.get()) {
                                    case 0:
                                        break;
                                    case 2:
                                        wayPointList.add(new Waypoint(homeLocation.getLatitude(), homeLocation.getLongitude(), waypointMissionFlightHeight));
                                    case 1:
                                        latLngBD09 = CoordinationConverter.GPS2BD09(new LatLng(homeLocation.getLatitude(), homeLocation.getLongitude()));
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
                                                                homeLocation.getLatitude(),
                                                                homeLocation.getLongitude()
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
                                        break;
                                }

                              /*  LocationCoordinate3D homeLocation = flightController.getState().getAircraftLocation();
                                wayPointList.add(new Waypoint(homeLocation.getLatitude(), homeLocation.getLongitude(), waypointMissionFlightHeight));

                                final int pointListSize = wayPointList.size();
                                latLngBD09 = CoordinationConverter.GPS2BD09(new LatLng(homeLocation.getLatitude(), homeLocation.getLongitude()));
                                baiduMap.addOverlay(new MarkerOptions()
                                        .position(latLngBD09)
                                        .animateType(MarkerOptions.MarkerAnimateType.grow)
                                        .flat(true)
                                        .anchor(0.5F, 0.5F)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker))
                                        .draggable(false));
                                if (pointListSize >= 2) {
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
                                }*/

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mapPanelStartMissionButton.setVisibility(View.GONE);
                                        mapPanelCancelMissionButton.setVisibility(View.GONE);
                                        mapPanelUndoButton.setVisibility(View.GONE);
                                        mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                                        linearLayoutForMap.setVisibility(View.VISIBLE);
                                        switchPanelImageView.setVisibility(View.VISIBLE);
                                        baiduMap.setOnMapLongClickListener(null);
                                        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                                                MyLocationConfiguration.LocationMode.FOLLOWING,
                                                true,
                                                null));
                                    }
                                });

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

                baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
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
                                    mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                                }
                            });
                            isCompletedByStopping.set(true);
                            isExecutingMission.set(false);

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
                mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
                linearLayoutForMap.setVisibility(View.VISIBLE);
                switchPanelImageView.setVisibility(View.VISIBLE);
            }
        });

        missionConfigurationPanelOKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null));

                isCompletedByStopping.set(false);
                missionFinishMode.set(1);

                clearWaypoint();
                baiduMap.setOnMapLongClickListener(onMapLongClickListener);
                WaypointMissionFinishedAction waypointMissionFinishedAction;
                WaypointMissionFlightPathMode waypointMissionFlightPathMode;
                WaypointMissionGotoWaypointMode waypointMissionGotoWaypointModel;
                WaypointMissionHeadingMode waypointMissionHeadingMode;

                switch (radioGroupMissionFinishAction.getCheckedRadioButtonId()) {
                    case R.id.rb_c:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        missionFinishMode.set(0);
                        break;
                    case R.id.rb_d:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        missionFinishMode.set(1);
                        break;
                    case R.id.rb_e:
                        waypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                        missionFinishMode.set(2);
                        break;
                    default:
                        missionFinishMode.set(1);
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

                waypointMissionFlightHeight = flightHeightSeekbar.getProgress();
                waypointMissionParams.setMissionFinishedAction(waypointMissionFinishedAction);
                waypointMissionParams.setMissionFlightPathMode(waypointMissionFlightPathMode);
                waypointMissionParams.setMissionGotoWaypointMode(waypointMissionGotoWaypointModel);
                waypointMissionParams.setMissionHeadingMode(waypointMissionHeadingMode);
                waypointMissionParams.setAutoFlightSpeed(autoFlightSpeedSeekbar.getProgress() + 2);
                waypointMissionParams.setMaxFlightSpeed(15.0f);
                mapViewPanel.removeView(missionConfigurationPanel);
                mapPanelStartMissionButton.setVisibility(View.VISIBLE);
                mapPanelCancelMissionButton.setVisibility(View.VISIBLE);
                mapPanelUndoButton.setVisibility(View.VISIBLE);


            }
        });

        mapPanelUndoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearWaypoint();
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

    private void clearWaypoint() {
        baiduMap.clear();
        wayPointList.clear();
        System.gc();
    }

    private void checkSocketConnection() {
        new Thread() {
            @Override
            public void run() {
                String buff = null;
                while (true) {
                    if (socket == null || !socket.isConnected()) {
                        try {
                            socket = new Socket("192.168.1.101", 3000);
                            bufferedWriter = new BufferedWriter((new OutputStreamWriter(socket.getOutputStream())));
                            while (true) {
                                if (!socket.isConnected()) throw new Exception();
                                if (buff != null) {
                                    bufferedWriter.write(buff);
                                    bufferedWriter.flush();
                                }
                                buff = logQ.poll(3, TimeUnit.SECONDS);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
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

    private void updateFlightParams(final FlightControllerState flightControllerState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusVerticaLDistanceTextView.setText(String.format(Locale.CHINA, "%.1f", flightControllerState.getAircraftLocation().getAltitude()));
                float velocity = (float) Math.sqrt(Math.pow(flightControllerState.getVelocityX(), 2) + Math.pow(flightControllerState.getVelocityY(), 2));
                statusHorizontalVelocityTextView.setText(String.format(Locale.CHINA, "%.1f", velocity));
                statusVerticalVelocityTextView.setText(String.format(Locale.CHINA, "%.1f", (int) (flightControllerState.getVelocityZ() * 10) == 0 ? 0.0000f : (-1.0) * flightControllerState.getVelocityZ()));
                double distance = LLDistanceConverter.LL2Distance(flightControllerState.getHomeLocation().getLatitude(), flightControllerState.getHomeLocation().getLongitude(), flightControllerState.getAircraftLocation().getLatitude(), flightControllerState.getAircraftLocation().getLongitude());
                statusHorizontalDistanceTextView.setText(String.format(Locale.CHINA, "%.1f", distance));
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
        if (camera != null) {
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
    }

    private void switchMapPanelFocus() {
        if (!isMapPanelFocused) {
            if (isExecutingMission.get()) {
                mapPanelStopMissionButton.setVisibility(View.VISIBLE);
                mapPanelCreateLinearLayout.setVisibility(View.GONE);
            } else {
                mapPanelStopMissionButton.setVisibility(View.GONE);
                mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
            }

            relativeLayoutMain.removeView(cameraShootImageView);
            relativeLayoutMain.removeView(cameraSwitchImageView);
            linearLayoutForMap.removeView(mapViewPanel);
            videoTextureViewFrameLayout.removeView(videoTextureView);
            videoTextureViewFrameLayout.addView(mapViewPanel);
            linearLayoutForMap.addView(videoTextureView);
            landLinearLayout.setVisibility(View.GONE);
            takeOffLinearLayout.setVisibility(View.GONE);
            followLinearLayout.setVisibility(View.GONE);
        } else {
            mapPanelStopMissionButton.setVisibility(View.GONE);
            mapPanelCreateLinearLayout.setVisibility(View.GONE);
            videoTextureViewFrameLayout.removeView(mapViewPanel);
            linearLayoutForMap.removeView(videoTextureView);
            linearLayoutForMap.addView(mapViewPanel);
            videoTextureViewFrameLayout.addView(videoTextureView);
            relativeLayoutMain.addView(cameraShootImageView);
            relativeLayoutMain.addView(cameraSwitchImageView);
            takeOffLinearLayout.setVisibility(View.VISIBLE);
            landLinearLayout.setVisibility(View.VISIBLE);
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

            mission = builder.build();

            DJIError djiErrorFirst = waypointMissionOperator.loadMission(mission);

            if (djiErrorFirst != null) {
                SideToast.makeText(MainActivity.this, djiErrorFirst.toString(), SideToast.LENGTH_SHORT, SideToast.TYPE_ERROR).show();
                mapPanelStopMissionButton.setVisibility(View.GONE);
                linearLayoutForMap.setVisibility(View.VISIBLE);
                switchPanelImageView.setVisibility(View.VISIBLE);
                mapPanelStartMissionButton.setVisibility(View.GONE);
                mapPanelCancelMissionButton.setVisibility(View.GONE);
                mapPanelUndoButton.setVisibility(View.GONE);
                mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);
            } else {
                uploadInfoDialog = new SimpleProgressDialog(MainActivity.this, "正在上传数据…");
                uploadInfoDialog.show();
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

                                                mapPanelStopMissionButton.setVisibility(View.GONE);
                                                linearLayoutForMap.setVisibility(View.VISIBLE);
                                                switchPanelImageView.setVisibility(View.VISIBLE);
                                                mapPanelStartMissionButton.setVisibility(View.GONE);
                                                mapPanelCancelMissionButton.setVisibility(View.GONE);
                                                mapPanelUndoButton.setVisibility(View.GONE);
                                                mapPanelCreateLinearLayout.setVisibility(View.VISIBLE);

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
