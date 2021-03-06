package kr.ac.inu.deepect.arnavigation;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.PixelCopy;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.skt.Tmap.TMapPoint;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import kr.ac.inu.deepect.R;
import kr.ac.inu.deepect.arnavigation.navigation.ConnectServer;
import kr.ac.inu.deepect.arnavigation.navigation.GpsManager;
import kr.ac.inu.deepect.arnavigation.navigation.MainActivity;
import kr.ac.inu.deepect.arnavigation.rendering.LocationNode;
import kr.ac.inu.deepect.arnavigation.rendering.LocationNodeRender;
import kr.ac.inu.deepect.arnavigation.sensor.DeviceLocation;
import kr.ac.inu.deepect.arnavigation.utils.ARLocationPermissionHelper;

public class ARActivity extends AppCompatActivity {
    private boolean installRequested;
    private boolean[] hasFinishedLoading = {false, false};

    private RelativeLayout container;

    // private ArFragment fragment;

    private ArSceneView arSceneView;

    // Our ARCore-Location scene
    private LocationScene locationScene;

    // 3D Renderable
    private ModelRenderable targetRenderable;

    private static final String TAG = "LocationActivity";


    // kotlin?????? ????????? ???????????? ?????? ?????? ???????????? ?????? static?????? ??????.
    private static TMapPoint destination;

    public static void setDestination(@NotNull TMapPoint dest) {
        destination = dest;
    }

    // ?????? ????????? ???, ????????? description??? class??? ???????????? ??????.
    private static class LatLonDesc {
        private double latitude;
        private double longitude;
        private String description;

        LatLonDesc(double latitude, double longitude, String description) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.description = description;
        }

        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public String getDescription() {
            return description;
        }
    }

    private static List<LatLonDesc> middleNodes = null;
    private static List<String> descriptions = null;

    private int descIndex = 0;

    // ?????? ???????????? ????????? middle node?????? ?????? ??????.
    public static void clearMiddleNodes() {
        if (middleNodes != null) {
            middleNodes = null;
        }
        middleNodes = new ArrayList<LatLonDesc>();
    }

    public static void setMiddleNodes(@NotNull double lat, double lon, String desc) {
        // ?????? description??? ????????? ????????? ?????????, ?????? ????????? middle node??? ?????? ???????????? ??????.
        if (desc != "") {
            LatLonDesc node = new LatLonDesc(lat, lon, desc);
            middleNodes.add(node);
        }
    }

    public void clearDescriptions() {
        if (descriptions != null) {
            descriptions = null;
        }
        descriptions = new ArrayList<String>();
    }

    // ???????????? ????????? ???????????? ????????? ??????
    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {
        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void takePhoto() {
        Toast.makeText(getBaseContext(), "?????? ?????? ????????????. ????????? ??????????????????.", Toast.LENGTH_LONG).show();
        final String filename = generateFilename();
        ArSceneView view = arSceneView;

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(ARActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                File photoFile = new File(filename);

                Uri photoURI = FileProvider.getUriForFile(ARActivity.this,
                        ARActivity.this.getPackageName() + ".ar.codelab.name.provider",
                        photoFile);

                // ????????? ????????? ?????????, ????????? ????????? ????????? ???????????? ?????? ?????? ???????????????.
                ConnectServer connectServer = new ConnectServer(photoFile,
                        new ConnectServer.EventListener() {
                            public void onSocketResult(String result) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                                builder.setTitle("??????")
                                        .setMessage(result + "???(???) ???????????? ?????? ?????????????????????????")
                                        .setNegativeButton("?????????", null)
                                        .setPositiveButton("???", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(ARActivity.this, MainActivity.class);
                                                intent.putExtra("POI", result);
                                                intent.putExtra("LAT", destination.getLatitude());
                                                intent.putExtra("LON", destination.getLongitude());
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                setResult(Activity.RESULT_OK, intent);
                                                finish();
                                            }
                                        }).show();
                            }

                            public void onSocketFailed() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                                builder.setTitle("??????")
                                        .setMessage("??????")
                                        .setPositiveButton("??????", null)
                                        .show();
                            }
                        });
                connectServer.start();
            } else {
                Toast toast = Toast.makeText(ARActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_main);
        container = (RelativeLayout) findViewById(R.id.ar_container);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Log.d(TAG, "kmyLog, size : " + size);

        // ARCore??? ???????????? AR scene??? ??????????????? arSceneView.
        arSceneView = findViewById(R.id.ar_scene_view);

        // ????????? ??????????????? ???????????? renderable ???????????? ?????????????????? ???.
        // renderable : ARCore??? node??? ???????????? 3D ????????? ??????????????? ?????? ?????????.
        // node??? ?????? ????????? ???????????? ????????? ????????????, ???????????? ???, ????????? ????????? ?????? ?????????.
        ViewRenderable roadsignLayoutRenderables[] = new ViewRenderable[middleNodes.size() - 1];
        clearDescriptions();
        Toast.makeText(this, "????????? ?????? ????????? ?????? ????????? ?????? ??? ????????????.", Toast.LENGTH_LONG).show();

        for (int i = 0; i < middleNodes.size(); i++) {
            String desc = middleNodes.get(i).getDescription();
            desc = desc.replace(" ??? ", "???(???) ");
            descriptions.add(desc);
            // Log.d(TAG, "kmyLog, desc : " + i + ", " + desc);
        }

        // AR ????????? ????????? text?????? textview??? ?????? ??????.
        TextView descView = findViewById(R.id.descView);
        TextView descIndexView = findViewById(R.id.descIndexView);
        TextView correctionView = findViewById(R.id.correctionView);
        TextView accuracyView = findViewById(R.id.accuracyView);
        correctionView.setVisibility(View.INVISIBLE);

        descIndexView.setText(String.valueOf(descIndex + 1));
        descView.setText(descriptions.get(descIndex));
        descView.setTypeface(null, Typeface.BOLD);
        //destinationView.setVisibility(View.INVISIBLE);

        // ?????? ?????? ????????? ?????? ??????.
        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (descIndex == descriptions.size() - 1) {
                    Toast.makeText(ARActivity.this, "????????? ???????????????.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (descIndex == descriptions.size() - 2) {
                    descIndex++;
                    // ????????? ????????? ???????????? ??? ????????? ???????????? ?????????.
                    descIndexView.setBackgroundResource(R.drawable.destination_layout_style);
                    descIndexView.setText("");
                    descView.setText(descriptions.get(descIndex));
                } else {
                    descIndex++;
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                }
            }
        });

        Button btnPrevious = findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (descIndex == 0) {
                    Toast.makeText(ARActivity.this, "????????? ???????????????.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (descIndex == descriptions.size() - 1) {
                    descIndex--;
                    descIndexView.setBackgroundResource(R.drawable.roadsign_layout_desc_style);
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                } else {
                    descIndex--;
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                }
            }
        });

        Button btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setVisibility(View.INVISIBLE);

        // capture ?????? ????????? ?????? ????????? takePhoto ????????? call ????????? ??????????????? ??????.
        btnCapture.setOnClickListener(view -> takePhoto());

        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setVisibility(View.INVISIBLE);

        Button btnCorrection = findViewById(R.id.btnCorrection);
        btnCorrection.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                descView.setVisibility(View.INVISIBLE);
                descIndexView.setVisibility(View.INVISIBLE);
                btnNext.setVisibility(View.INVISIBLE);
                btnPrevious.setVisibility(View.INVISIBLE);
                btnCorrection.setVisibility(View.INVISIBLE);
                correctionView.setVisibility(View.VISIBLE);
                btnReturn.setVisibility(View.VISIBLE);
                btnCapture.setVisibility(View.VISIBLE);
            }
        });

        btnReturn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                descView.setVisibility(View.VISIBLE);
                descIndexView.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
                btnPrevious.setVisibility(View.VISIBLE);
                btnCorrection.setVisibility(View.VISIBLE);
                correctionView.setVisibility(View.INVISIBLE);
                btnReturn.setVisibility(View.INVISIBLE);
                btnCapture.setVisibility(View.INVISIBLE);
            }
        });

//         sceneform??? ?????? build() ???????????? CompleableFuture??? ????????????
//         CompletableFuture<ViewRenderable> exampleLayout = // "????????? ????????? ??????(Task)??????,
//         Task ????????? ?????????????????? ?????? ???????????????, ?????? Task??? ??????????????? ??????(trigger)????????? Task."
//                ViewRenderable.builder()
//                        .setView(this, R.layout.roadsign_layout)
//                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ViewRenderable> roadsignFutures[] = new CompletableFuture[middleNodes.size() - 1];
        for (int i = 0; i < roadsignFutures.length; i++) {
            roadsignFutures[i] = ViewRenderable.builder()
                    .setView(this, R.layout.roadsign_layout)
                    .build();
        }
        CompletableFuture<ModelRenderable> targetFuture = ModelRenderable.builder()
                .setSource(this, R.raw.target)
                .build();

        CompletableFuture.allOf(
                targetFuture)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                targetRenderable = targetFuture.get();
                                hasFinishedLoading[0] = true;
                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });

        CompletableFuture.allOf(
                roadsignFutures)
                .handle(
                        (notUsed, throwable) -> {
                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                for (int i = 0; i < roadsignFutures.length; i++) {
                                    roadsignLayoutRenderables[i] = roadsignFutures[i].get();
                                }
                                hasFinishedLoading[1] = true;
                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(frameTime -> {
                    if (!hasFinishedLoading[0] || !hasFinishedLoading[1]) {
                        return;
                    }
                    if (locationScene == null) {
                        // Adding a simple location marker of a 3D model
                        locationScene = new LocationScene(this, arSceneView);
                        if (locationScene.mLocationMarkers.size() > 0) {
                            locationScene.mLocationMarkersClear();
                        }
                        LocationMarker camera;
                        {
                            Node node = new Node();
                            camera = createLocationMarker(0, 0, node);
                            camera.setAtCameraPosition(true);
                            locationScene.mLocationMarkers.add(camera);
                        }
                        LocationMarker prevLocationMarker = null;

                        for (int i = 0; i < middleNodes.size() - 1; i++) {
                            final int finalI = i;
                            LatLonDesc point = middleNodes.get(i);

                            Node node = getExampleView(roadsignLayoutRenderables[i]);
                            LocationMarker layoutLocationMarker = createLocationMarker(
                                    point.getLatitude(), point.getLongitude(), node);
                            layoutLocationMarker.setCameraNode(camera.node);

                            if (prevLocationMarker != null) {
                                prevLocationMarker.setLookNode(node);
                            }
                            prevLocationMarker = layoutLocationMarker;

                            layoutLocationMarker.setScalingMode(LocationMarker.ScalingMode.SIMPLE_SCALING);
                            layoutLocationMarker.setGradualScalingMaxScale(3.5F);
                            layoutLocationMarker.setGradualScalingMinScale(1.8F);
                            layoutLocationMarker.setOnlyRenderWhenWithin(350);

                            layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                                @Override
                                public void render(LocationNode node) {
                                    ViewRenderable roadsignLayoutRendarable = roadsignLayoutRenderables[finalI];
                                    // Log.d(TAG, "kmyLog, in render nodeId : " + node);
                                    double angle = node.getAngle(layoutLocationMarker.cameraNode, layoutLocationMarker.node, layoutLocationMarker.nodeToLook);
                                    View eView = roadsignLayoutRendarable.getView();
                                    TextView roadsignTextView = eView.findViewById(R.id.textView);
                                    roadsignTextView.setTypeface(null, Typeface.BOLD);
                                    LinearLayout roadsignLayout = eView.findViewById(R.id.roadsignLayout);
                                    String arrow;
                                    int index = finalI + 1;
                                    String indexString = Integer.toString(index);
                                    if (node.getDistance() > 100) {
                                        roadsignLayout.setBackgroundResource(R.drawable.roadsign_layout_style_half);
                                        roadsignTextView.setText(indexString);
                                    } else {
                                        roadsignLayout.setBackgroundResource(R.drawable.roadsign_layout_style);
                                        if (angle >= 30)
                                            arrow = "???";
                                        else if (angle <= -30)
                                            arrow = "???";
                                        else
                                            arrow = "???";
                                        roadsignTextView.setText(indexString + '\n' + arrow);
                                    }
                                    if (DeviceLocation.getIsAccuracyLow()) {
                                        accuracyView.setBackgroundResource(R.drawable.accuracy_style_red);
                                    } else {
                                        accuracyView.setBackgroundResource(R.drawable.accuracy_style_green);
                                    }
                                }
                            });
                            // Adding the marker
                            layoutLocationMarker.lookCamera(true);
                            locationScene.mLocationMarkers.add(layoutLocationMarker);
                        }
                        Node node = new Node();
                        LocationMarker locationMarker = createLocationMarker(destination.getLatitude(), destination.getLongitude(), node);
                        node.setRenderable(targetRenderable);
                        locationMarker.setScalingMode(LocationMarker.ScalingMode.SIMPLE_SCALING);
                        locationMarker.setGradualScalingMaxScale(4F);
                        locationMarker.setGradualScalingMinScale(1.7F);
                        prevLocationMarker.setLookNode(node);
                        prevLocationMarker = locationMarker;
                        LocationScene.mLocationMarkers.add(locationMarker);
                    }

                    Frame frame = arSceneView.getArFrame();
                    if (frame == null) {
                        return;
                    }
                    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }
                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }
                });

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
    }

    private LocationMarker createLocationMarker(double latitude, double longitude, Node node) {
        // memory leak.
        LocationMarker marker = new LocationMarker(latitude, longitude, node);
        marker.setHeight(0);
        return marker;
    }

    private Node getExampleView(ViewRenderable exampleLayoutRenderable) {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Log.d(TAG, "kmyLog : " + base.getRenderable());
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });
        return base;
    }

    /***
     * Example Node of a 3D model
     *
     * @return
     */

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                // installRequested?????? ?????? ????????? ???????????? ?????????.
                // DemoUtils?????? ????????? ????????? ???????????? ???????????? Session??? ?????? ???.
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    Config config = session.getConfig();
                    config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                    session.configure(config);
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            // resume()??? onResume()?????? ??????????????? ??????.
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) { // ???????????? ??? ??? ?????? ????????? ?????? ??????.
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        // pause() ???????????? onPause()?????? ???????????? ??????.
        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
