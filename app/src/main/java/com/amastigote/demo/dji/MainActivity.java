package com.amastigote.demo.dji;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Button;

import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener,
        DJIMissionManager.MissionProgressStatusCallback,
        DJICommonCallbacks.DJICompletionCallback {
    private static DJIBaseProduct djiBaseProduct;

    /*
        UI components
     */

    private Button takeOffButton;
    private Button landButton;
    private SimpleProgressDialog regProgDialog;
    private SimpleProgressDialog waitForProdProgDialog;
    private TextureView videoTextureView;

    private List<DJIMissionStep> djiMissionStepList = new ArrayList<>();
    private DJICodecManager djiCodecManager;
    private DJIMissionManager missionManager;
    private DJISDKManager.DJISDKManagerCallback djisdkManagerCallback
            = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError djiError) {
            if (djiError.toString().equals(DJISDKError.REGISTRATION_SUCCESS.toString())) {
                regProgDialog.dismiss();
                waitForProdProgDialog.show();
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
        public void onProductChanged(DJIBaseProduct previousProd, DJIBaseProduct presentProd) {
            djiBaseProduct = presentProd;
            if (djiBaseProduct != null && djiBaseProduct.isConnected()) {
//                djiBaseProduct.setDJIBaseProductListener(...);
                waitForProdProgDialog.dismiss();

                initMissionManager();

                SimpleAlertDialog.show(
                        MainActivity.this,
                        false,
                        "Product Connected",
                        "Present product is " + djiBaseProduct.getModel().getDisplayName(),
                        new SimpleDialogButton("ok", null)
                );
                try {
                    if (djiBaseProduct.getModel() != Model.UnknownAircraft)
                        djiBaseProduct.getCamera().setDJICameraReceivedVideoDataCallback((videoBuffer, size) -> {
                            if (djiCodecManager != null)
                                djiCodecManager.sendDataToDecoder(videoBuffer, size);
                        });
                    else
                        djiBaseProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback((videoBuffer, size) -> {
                            if (djiCodecManager != null)
                                djiCodecManager.sendDataToDecoder(videoBuffer, size);
                        });
                } catch (Exception e) {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            false,
                            "Exception",
                            e.toString(),
                            new SimpleDialogButton("quit", (x, y) -> System.exit(0))
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

        takeOffButton = (Button) findViewById(R.id.takeoff_btn);
        landButton = (Button) findViewById(R.id.land_btn);

        takeOffButton.setOnClickListener((view -> {
            takeOffButton.setEnabled(false);
            landButton.setEnabled(false);
            djiMissionStepList.add(new DJITakeoffStep(this));
            DJICustomMission customMission = new DJICustomMission(djiMissionStepList);
            DJIMission.DJIMissionProgressHandler progressHandler = ((type, progress) -> {
            });
            missionManager.prepareMission(customMission, progressHandler, djiError -> {
                if (djiError != null) {
                    SimpleAlertDialog.show(
                            MainActivity.this,
                            false,
                            "Mission Preparation Error",
                            djiError.getDescription(),
                            new SimpleDialogButton("ok", null)
                    );
                } else {
                    missionManager.startMissionExecution(djiError2 -> {
                        if (djiError2 != null) {
                            SimpleAlertDialog.show(
                                    MainActivity.this,
                                    false,
                                    "Mission Execution Error",
                                    djiError2.getDescription(),
                                    new SimpleDialogButton("ok", null)
                            );
                        }
                    });
                }
            });


        }));

        landButton.setOnClickListener((view -> {

        }));
        videoTextureView = (TextureView) findViewById(R.id.texture_view);
        videoTextureView.setSurfaceTextureListener(this);

        regProgDialog = new SimpleProgressDialog(this, "Validating API key");
        waitForProdProgDialog = new SimpleProgressDialog(this, "Waiting for aircraft");

        regProgDialog.show();
        DJISDKManager.getInstance().initSDKManager(this, djisdkManagerCallback);
    }

    /*
        initialize missionManager;
    */

    public void initMissionManager() {
        missionManager = djiBaseProduct.getMissionManager();
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

    /*
        implements DJIMissionManager.MissionProgressStatusCallback
     */

    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus djiMissionProgressStatus) {

    }

    @Override
    public void onResult(DJIError djiError) {

    }
}
