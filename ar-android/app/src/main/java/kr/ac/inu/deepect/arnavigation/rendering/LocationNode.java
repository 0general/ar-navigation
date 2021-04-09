package kr.ac.inu.deepect.arnavigation.rendering;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;

import kr.ac.inu.deepect.arnavigation.LocationMarker;
import kr.ac.inu.deepect.arnavigation.LocationScene;
import kr.ac.inu.deepect.arnavigation.navigation.Vector;
import kr.ac.inu.deepect.arnavigation.utils.ARUtils;

public class LocationNode extends AnchorNode {

    private String TAG = "LocationNode";

    private LocationMarker locationMarker;
    private LocationNodeRender renderEvent;
    private int distance;
    private double distanceInAR;
    private float scaleModifier = 1F;
    private float height = 0F;
    private float gradualScalingMinScale = 0.8F;
    private float gradualScalingMaxScale = 1.4F;
    private double scale;

    private LocationMarker.ScalingMode scalingMode = LocationMarker.ScalingMode.NO_SCALING; // !
    private LocationScene locationScene;

    public LocationNode(Anchor anchor, LocationMarker locationMarker, LocationScene locationScene) {
        super(anchor);
        this.locationMarker = locationMarker;
        this.locationScene = locationScene;
    }

    public double getScale() { return scale; }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getScaleModifier() {
        return scaleModifier;
    }

    public void setScaleModifier(float scaleModifier) {
        this.scaleModifier = scaleModifier;
    }

    public LocationMarker getLocationMarker() {
        return locationMarker;
    }

    public LocationNodeRender getRenderEvent() {
        return renderEvent;
    }

    public void setRenderEvent(LocationNodeRender renderEvent) {
        this.renderEvent = renderEvent;
    }

    public int getDistance() {
        return distance;
    }

    public double getDistanceInAR() {
        return distanceInAR;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setDistanceInAR(double distanceInAR) {
        this.distanceInAR = distanceInAR;
    }

    public LocationMarker.ScalingMode getScalingMode() {
        return scalingMode;
    }

    public void setScalingMode(LocationMarker.ScalingMode scalingMode) {
        this.scalingMode = scalingMode;
    }

    @Override
    // custom node 클래스의 onUpdate() 메소드 재정의
    public void onUpdate(FrameTime frameTime) {

        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.


        for (Node n : getChildren()) {
            if (getScene() == null) {
                return;
            }

            Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
            Vector3 nodePosition = n.getWorldPosition();

            // Compute the difference vector between the camera and anchor
            float dx = cameraPosition.x - nodePosition.x;
            float dy = cameraPosition.y - nodePosition.y;
            float dz = cameraPosition.z - nodePosition.z;

            // Compute the straight-line distance.
            double distanceInAR = Math.sqrt(dx * dx + dy * dy + dz * dz);
            setDistanceInAR(distanceInAR);

            if (locationScene.shouldOffsetOverlapping()) {
                if (locationScene.mArSceneView.getScene().overlapTestAll(n).size() > 0) {
                    setHeight(getHeight() + 1.2F);
                }
            }

            if (locationScene.shouldRemoveOverlapping()) {
                Ray ray = new Ray();
                ray.setOrigin(cameraPosition);

                float xDelta = (float) (distanceInAR * Math.sin(Math.PI / 15)); //12 degrees
                Vector3 cameraLeft = getScene().getCamera().getLeft().normalized();

                Vector3 left = Vector3.add(nodePosition, cameraLeft.scaled(xDelta));
                Vector3 center = nodePosition;
                Vector3 right = Vector3.add(nodePosition, cameraLeft.scaled(-xDelta));

                boolean isOverlapping = isOverlapping(n, ray, left, cameraPosition)
                        || isOverlapping(n, ray, center, cameraPosition)
                        || isOverlapping(n, ray, right, cameraPosition);

                if (isOverlapping) {
                    setEnabled(false);
                } else {
                    setEnabled(true);
                }
            }
        }

        if (!locationScene.minimalRefreshing())
            scaleAndRotate();


        if (renderEvent != null) {
            if (this.isTracking() && this.isActive() && this.isEnabled())
                renderEvent.render(this);
        }
    }

    private boolean isOverlapping(Node n, Ray ray, Vector3 target, Vector3 cameraPosition) {
        Vector3 nodeDirection = Vector3.subtract(target, cameraPosition);
        ray.setDirection(nodeDirection);

        ArrayList<HitTestResult> hitTestResults = locationScene.mArSceneView.getScene().hitTestAll(ray);
        if (hitTestResults.size() > 0) {

            HitTestResult closestHit = null;
            for (HitTestResult hit : hitTestResults) {
                //Get the closest hit on enabled Node
                if (hit.getNode() != null && hit.getNode().isEnabled()) {
                    closestHit = hit;
                    break;
                }
            }

            // if closest hit is not the current node, it is hidden behind another node that is closer
            return closestHit != null && closestHit.getNode() != n;
        }
        return false;
    }

    public void scaleAndRotate() {
        for (Node n : getChildren()) {
            int markerDistance = (int) Math.ceil(
                    ARUtils.distance(
                            locationMarker.latitude,
                            locationScene.deviceLocation.currentBestLocation.getLatitude(),
                            locationMarker.longitude,
                            locationScene.deviceLocation.currentBestLocation.getLongitude(),
                            0,
                            0)
            );
            setDistance(markerDistance);

            // Limit the distance of the Anchor within the scene.
            // Prevents uk.co.appoly.arcorelocation.rendering issues.
            int renderDistance = markerDistance;
            if (renderDistance > locationScene.getDistanceLimit())
                renderDistance = locationScene.getDistanceLimit();

            float scale = 1f;
            final Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
            Vector3 nodeWorldPosition = locationMarker.isAtCameraPosition() ?
                    cameraPosition : n.getWorldPosition();
            Vector3 directionFromCamera = Vector3.subtract(cameraPosition, nodeWorldPosition);

            switch (scalingMode) {
                case FIXED_SIZE_ON_SCREEN:
                    scale = (float) Math.sqrt(
                            directionFromCamera.x * directionFromCamera.x +
                            directionFromCamera.y * directionFromCamera.y +
                            directionFromCamera.z * directionFromCamera.z);
                    break;
                case GRADUAL_TO_MAX_RENDER_DISTANCE:
                    float scaleDifference = gradualScalingMaxScale - gradualScalingMinScale;
                    scale = (gradualScalingMinScale + ((locationScene.getDistanceLimit() - markerDistance) *
                            (scaleDifference / locationScene.getDistanceLimit()))) * renderDistance;
                    break;
                case GRADUAL_FIXED_SIZE:
                    scale = (float) Math.sqrt(directionFromCamera.x * directionFromCamera.x
                            + directionFromCamera.y * directionFromCamera.y +
                            directionFromCamera.z * directionFromCamera.z);
                    float gradualScale = gradualScalingMaxScale - gradualScalingMinScale;
                    gradualScale = gradualScalingMaxScale - (gradualScale / renderDistance * markerDistance);
                    scale *= Math.max(gradualScale, gradualScalingMinScale);
                    break;
                case SIMPLE_SCALING:
                    // markerDistance, renderDistance
                    scale = ((1.0F / markerDistance) * 200);
                    if (scale > gradualScalingMaxScale)
                        scale = gradualScalingMaxScale;

                    else if (scale < gradualScalingMinScale)
                        scale = gradualScalingMinScale;
                    this.scale = scale;
            }

            scale *= scaleModifier;

            //Log.d("LocationScene", "scale " + scale);
            n.setWorldPosition(new Vector3(nodeWorldPosition.x, getHeight(), nodeWorldPosition.z));
            switch (locationMarker.getDirectionMode()) {
                case LOOK_CAMERA:
                    n.setWorldRotation(Quaternion.lookRotation(directionFromCamera, Vector3.up()));
                    break;
                case LOOK_NODE:
                    n.setLocalRotation(Quaternion.lookRotation(
                            Vector3.subtract(locationMarker.nodeToLook.getWorldPosition(),
                                    nodeWorldPosition),
                            Vector3.up()));
                    break;
                default:
                    n.setWorldRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), 0));
            }
            n.setWorldScale(new Vector3(scale, scale, scale));
        }
    }

    public double getAngle(Node now, Node n1, Node n2) {
        Vector3 v1 = Vector3.subtract(now.getWorldPosition(), n1.getWorldPosition());
        Vector3 v2 = Vector3.subtract(now.getWorldPosition(), n2.getWorldPosition());
        Vector3 cross = Vector3.cross(v1, v2);
        if (cross.y < 0)
            return Vector3.angleBetweenVectors(v1, v2);
        else
            return -(Vector3.angleBetweenVectors(v1, v2));
    }

    public float getGradualScalingMinScale() {
        return gradualScalingMinScale;
    }

    public void setGradualScalingMinScale(float gradualScalingMinScale) {
        this.gradualScalingMinScale = gradualScalingMinScale;
    }

    public float getGradualScalingMaxScale() {
        return gradualScalingMaxScale;
    }

    public void setGradualScalingMaxScale(float gradualScalingMaxScale) {
        this.gradualScalingMaxScale = gradualScalingMaxScale;
    }
}