package com.amastigote.demo.dji;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.sdk.airlink.DJILBAirLink;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener {
    private static DJIBaseProduct djiBaseProduct;

    /*
        UI components
     */
    private SimpleProgressDialog regProgDialog;
    private SimpleProgressDialog waitForProdProgDialog;
    private TextureView videoTextureView;

    /*
        Video Utils
     */
    private DJICamera.CameraReceivedVideoDataCallback cameraReceivedVideoDataCallback;
    private DJILBAirLink.DJIOnReceivedVideoCallback djiOnReceivedVideoCallback;
    private DJICodecManager djiCodecManager;

//    public static DJIBaseProduct getDjiBaseProduct() {
//        return djiBaseProduct;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoTextureView = (TextureView) findViewById(R.id.texture_view);
        videoTextureView.setSurfaceTextureListener(this);

        regProgDialog = new SimpleProgressDialog(this, "Validating API key");
        waitForProdProgDialog = new SimpleProgressDialog(this, "Waiting for aircraft");

        djiOnReceivedVideoCallback = (videoBuffer, size) -> {
            if (djiCodecManager != null)
                djiCodecManager.sendDataToDecoder(videoBuffer, size);
        };

        cameraReceivedVideoDataCallback = (videoBuffer, size) -> {
            if (djiCodecManager != null)
                djiCodecManager.sendDataToDecoder(videoBuffer, size);
        };

        try {
            if (djiBaseProduct.getModel() != Model.UnknownAircraft)
                djiBaseProduct.getCamera().setDJICameraReceivedVideoDataCallback(cameraReceivedVideoDataCallback);
            else
                djiBaseProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(djiOnReceivedVideoCallback);
        } catch (Exception e) {
            Log.e("--->", "error in set video callback");
        }

        regProgDialog.show();
        DJISDKManager.getInstance().initSDKManager(this, djisdkManagerCallback);
        setContentView(R.layout.activity_main);
    }

    private DJISDKManager.DJISDKManagerCallback djisdkManagerCallback
            = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError djiError) {
            if (djiError.toString().equals(DJISDKError.REGISTRATION_SUCCESS.toString())) {
                regProgDialog.dismiss();
                waitForProdProgDialog.show();
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setTitle("API Key Validation Error")
                        .setMessage("Contact the developers or check your network connection")
                        .setNegativeButton("quit", (dialog, which) -> System.exit(0))
                        .show();
            }
        }

        @Override
        public void onProductChanged(DJIBaseProduct previousProd, DJIBaseProduct presentProd) {
            djiBaseProduct = presentProd;
            if (djiBaseProduct != null && djiBaseProduct.isConnected()) {
//                djiBaseProduct.setDJIBaseProductListener(...);
                waitForProdProgDialog.dismiss();
                new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setTitle("Product Connected")
                        .setMessage("Present product is " + djiBaseProduct.getModel().getDisplayName())
                        .setPositiveButton("ok", null)
                        .show();
            }
        }
    };

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
}
