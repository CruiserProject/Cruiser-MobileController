package com.amastigote.demo.dji;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;

import com.amastigote.demo.dji.UIComponentUtil.SimpleAlertDialog;
import com.amastigote.demo.dji.UIComponentUtil.SimpleDialogButton;
import com.amastigote.demo.dji.UIComponentUtil.SimpleProgressDialog;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.sdk.base.DJIBaseProduct;
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

    private DJICodecManager djiCodecManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoTextureView = (TextureView) findViewById(R.id.texture_view);
        videoTextureView.setSurfaceTextureListener(this);

        regProgDialog = new SimpleProgressDialog(this, "Validating API key");
        waitForProdProgDialog = new SimpleProgressDialog(this, "Waiting for aircraft");

        regProgDialog.show();
        DJISDKManager.getInstance().initSDKManager(this, djisdkManagerCallback);
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
                SimpleAlertDialog.show(
                        getBaseContext(),
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
                SimpleAlertDialog.show(
                        getBaseContext(),
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
                            getBaseContext(),
                            false,
                            "Exception",
                            e.toString(),
                            new SimpleDialogButton("quit", (x, y) -> System.exit(0))
                    );
                }
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
