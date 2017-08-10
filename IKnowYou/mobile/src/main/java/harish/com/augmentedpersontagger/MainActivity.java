package harish.com.augmentedpersontagger;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.*;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.*;
import cz.msebera.android.httpclient.Header;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.entity.StringEntity;
import harish.com.augmentedpersontagger.R;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    // camera related
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
    private LinearLayout info_screen;

    private TextView person_name;
    private TextView careers;
    private TextView major;
    private TextView title;
    private TextView affiliations;
    private TextView locations;
    private TextView eid;

    // audio related
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening;
    private Set<String> person_questions = new HashSet<>(Arrays.asList(
            "hey buddy who is this",
            "hey buddy tell me who this is",
            "hey buddy tell me who is this",
            "hey buddy tell who is this",
            "hey buddy who is this",
            "hey buddy who is this person here"));
    private Set<String> thanks_questions = new HashSet<>(Arrays.asList(
            "hey buddy thanks",
            "hey buddy thanks for the info"));
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        info_screen = (LinearLayout) findViewById(R.id.info_screen);
        person_name = (TextView) findViewById(R.id.person_name);
        careers = (TextView)findViewById(R.id.careers);
        major = (TextView)findViewById(R.id.major);
        title = (TextView)findViewById(R.id.title);
        affiliations = (TextView)findViewById(R.id.affiliations);
        locations = (TextView)findViewById(R.id.locations);
        eid = (TextView)findViewById(R.id.eid);
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
        if(speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            SpeechRecognitionListener listener = new SpeechRecognitionListener();
            speechRecognizer.setRecognitionListener(listener);
        }

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        //speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);

        speechRecognizer.startListening(speechRecognizerIntent);
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
        startSpeechReception();
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

        stopServices();
    }

    private void stopServices() {

        // stop audio service for this app
        if (speechRecognizer != null)
        {
            speechRecognizer.destroy();
            speechRecognizer = null;
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

    protected class SpeechRecognitionListener implements RecognitionListener
    {

        @Override
        public void onReadyForSpeech(Bundle params) {
            System.out.println("************ ready for speech **********");
        }

        @Override
        public void onBeginningOfSpeech() {
            System.out.println("************ beginning speech **********");
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            System.out.println("************ endof speech **********");
            //speechRecognizer.startListening(speechRecognizerIntent);
        }

        @Override
        public void onError(int error) {
            System.out.println("**********=====" + error);
            //speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
            startSpeechReception();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            //System.out.println(matches.get(0));
            if (person_questions.contains(matches.get(0))) {
                System.out.println("matched");
                System.out.println(matches.get(0));
                takePicture();
            } else if (thanks_questions.contains(matches.get(0))) {
                if(info_screen.getVisibility() == View.VISIBLE) {
                    info_screen.setVisibility(View.INVISIBLE);
                }
            }
            speechRecognizer.destroy();
            speechRecognizer = null;
            startSpeechReception();
            //speechRecognizer.startListening(speechRecognizerIntent);
            //speechRecognizer.destroy();
            // startSpeechReception();
        }

        private void takePicture() {
            if(cameraDevice == null) {
                return;
            }

            try {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
                Size[] jpegSizes = null;
                if (characteristics != null) {
                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                }
                int width = 640;
                int height = 480;
                if (jpegSizes != null && 0 < jpegSizes.length) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                }
                ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            Bitmap imageBitMap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            imageBitMap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                            byte[] bytesCompressed = baos.toByteArray();
                            postCapture(bytesCompressed);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                            }
                        }
                    }
                    private void postCapture(byte[] bytes) throws IOException {
                        System.out.println("bytes got");
                        AsyncHttpClient client = new AsyncHttpClient();
                        //client.addHeader("Content-Type", "application/json");
                        client.addHeader("x-api-key", "h4ANNxg5AA5PpYMxu3QZg7t8C9St6KKI9rZPmrQT");
                        RequestParams requestParams = new RequestParams();
                        String imageString = new String(Base64.encodeBase64(bytes), StandardCharsets.UTF_8);
                        requestParams.put("image", imageString);
                        List<NameValuePair> params = new ArrayList<>();
                        JSONObject jpayload = new JSONObject();
                        try {
                            jpayload.put("image", jpayload.toString()) ;
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        String url = "https://h2h2c0e7p9.execute-api.us-west-2.amazonaws.com/beta/imageinfo";

                        client.post(null, url, new StringEntity("{\"image\" :\""  + imageString + "\"}", "UTF-8"),
                                "application/json", new JsonHttpResponseHandler() {
                                    @Override
                                    public void onFailure(int statusCode,
                                                          cz.msebera.android.httpclient.Header[] headers,
                                                          java.lang.String responseString, java.lang.Throwable throwable) {
                                        System.out.println(responseString);

                                    }

                                    @Override
                                    public void onFailure(int statusCode,
                                                          cz.msebera.android.httpclient.Header[] headers,
                                                          java.lang.Throwable throwable,
                                                          org.json.JSONObject errorResponse ) {
                                        System.out.println(errorResponse.toString());
                                    }


                                    @Override
                                    public void onSuccess(int statusCode,
                                                          cz.msebera.android.httpclient.Header[] headers,
                                                          org.json.JSONObject response) {
                                        System.out.println("SUCCESS");
                                        System.out.println(response.toString());
                                        // FIXME: fix in lambdat o handle this case
                                        if (response.toString().indexOf("An error occurred (InvalidParameterException) when calling " +
                                                "the SearchFacesByImage operation: There are no faces in the image. " +
                                                "Should be at least 1.") == -1) {
                                            try {
                                                JSONObject jObject = new JSONObject(response.toString());
                                                JSONObject info = (JSONObject) jObject.getJSONObject("info").getJSONObject("response").getJSONArray("docs").get(0);
                                                String name = (String) info.get("firstName") +
                                                        (String) info.get("middleName") +
                                                        (String) info.get("lastName");
                                                info_screen.setVisibility(View.VISIBLE);
                                                person_name.setText("Name: " + name);
                                                careers.setText("Careers: " + ((JSONArray)info.get("careers")).toString());
                                                major.setText("Major: " + ((JSONArray) info.get("majors")).toString());
                                                title.setText("Title: " + (String)info.get("primaryTitle").toString());
                                                affiliations.setText("Affiliations: " +
                                                        ((JSONArray) info.get("affiliations")).toString());
                                                locations.setText("Location: " +
                                                        ((JSONArray) info.get("locations")).toString());
                                                eid.setText("EID: " + (String) info.get("eid"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    }
                                    @Override
                                    public void onRetry(int retryNo) {
                                        // called when request is retried
                                        System.out.println("retrying");
                                    }

                        });
                        /*
                        client.post(url, requestParams, new JsonHttpResponseHandler() {
                           @Override
                            public void onFailure(int statusCode,
                                                  cz.msebera.android.httpclient.Header[] headers,
                                                  java.lang.String responseString, java.lang.Throwable throwable) {
                                System.out.println(responseString);

                            }

                            @Override
                            public void onFailure(int statusCode,
                                                  cz.msebera.android.httpclient.Header[] headers,
                                                  java.lang.Throwable throwable,
                                                  org.json.JSONObject errorResponse ) {
                                  System.out.println(errorResponse.toString());
                            }


                            @Override
                            public void onSuccess(int statusCode,
                                                  cz.msebera.android.httpclient.Header[] headers,
                                                  org.json.JSONObject response){
                                System.out.println(response.toString());
                            }

                            @Override
                            public void onRetry(int retryNo) {
                                // called when request is retried
                                System.out.println("retrying");
                            }
                        });   */

                    }
                };
                reader.setOnImageAvailableListener(readerListener, null);
                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        startPreview();

                    }
                };
                cameraDevice.createCaptureSession(Arrays.asList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {

                            session.capture(captureBuilder.build(), captureListener, handler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                    }
                }, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            System.out.println("************ event occured **********");
        }
    }
}
