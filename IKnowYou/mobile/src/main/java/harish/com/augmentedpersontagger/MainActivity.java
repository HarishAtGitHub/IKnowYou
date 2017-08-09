package harish.com.augmentedpersontagger;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import harish.com.augmentedgpstagger.R;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    private static final String CAMERA_ID = "0";
    private static final int MAIN_ACTIVITY_SPEECH_RECOGNITON_CODE = 100;
    private CameraDevice cameraDevice;
    private SurfaceView realityView;
    private TextView infoBox;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest captureRequest;
    private CameraManager manager;
    private CameraDevice.StateCallback cameraDeviceStateCallBack;
    private final Handler handler = new Handler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Intent intent = new Intent("android.media.action.IMAGE_CAPTURE"); // for audio
        // MediaStore.ACTION_VIDEO_CAPTURE for video capture
        //startActivity(intent);
        realityView = (SurfaceView) findViewById(R.id.realityviewer);
        realityView.getHolder().addCallback(new RealityViewTracker());

        // get camera manager
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraDeviceStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                System.out.print("%%%%%%%%%%%%%%%%%Camera opened%%%%%%%%%%%%%%%%%%%%");
                cameraDevice = camera;
                handler.sendEmptyMessage(1);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                System.out.print("%%%%%%%%%%%%%%%%%Camera disconnected%%%%%%%%%%%%%%%%%%%%");

            }

            @Override
            public void onError(CameraDevice camera, int error) {
                System.out.print("%%%%%%%%%%%%%%%%%Camera error%%%%%%%%%%%%%%%%%%%%");
            }
        };

        startSpeechReception();


    }

    private void startSpeechReception() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        //        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, MAIN_ACTIVITY_SPEECH_RECOGNITON_CODE);
        } catch (ActivityNotFoundException a) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MAIN_ACTIVITY_SPEECH_RECOGNITON_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
                    System.out.println(result.get(0));
                }
                startSpeechReception();
                break;
            }

        }
    }

    private void startPreview() {
        System.out.println("preview");
        createCameraPreviewSession();
        
    }

    private void createCameraPreviewSession() {
        final Surface surface = realityView.getHolder().getSurface();
        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            System.out.println("########### capture session configured #########");
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            cameraCaptureSession = session;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                                // Flash is automatically enabled when necessary.
                                //setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.

                                previewRequestBuilder.addTarget(surface);
                                captureRequest = previewRequestBuilder.build();
                                cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            System.out.println("########### capture configuration failed #########");
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // as camera device only accepts a certain type of size, you must set a valid size for each surface
        // so before constructing the surface you need to find the size that it needs
        // so get camera and get its size and then use it
        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                System.out.println("camera permission not granted by user yet so now we request to user for permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);
            }

            manager.openCamera(
                    CAMERA_ID,
                    cameraDeviceStateCallBack,
                    null
            );


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            if(cameraCaptureSession != null) {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
                cameraCaptureSession = null;

            }
        } catch (final CameraAccessException e) {

        } finally {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
                cameraCaptureSession = null;
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                // if both surface is created and camera device is opened
                // - ready to set up preview and other things
                startPreview();
                break;
        }

        return true;
    }
}
