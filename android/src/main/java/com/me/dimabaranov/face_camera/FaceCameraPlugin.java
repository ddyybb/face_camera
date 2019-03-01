package com.me.dimabaranov.face_camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

/**
 * FaceCameraPlugin
 */
public class FaceCameraPlugin implements MethodCallHandler {

    private final Registrar flutterRegistrar;
    private TextureRegistry textureRegistry;
    private TextureRegistry.SurfaceTextureEntry surfaceTextureEntry;
    private Surface surface;
    Random random;
    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;

    // CAMERA START
    private static final int CAMERA_REQUEST_ID = 513469796;
    private static final String TAG = "CameraPlugin";
    private static CameraManager cameraManager;
    private Camera camera;
    // CAMERA END

    private class DrawThread extends Thread {

        MethodChannel.Result result;
        Boolean runFlag = true;

        DrawThread(MethodChannel.Result result) {
            this.result = result;
        }

        @Override
        public void run() {
            while (runFlag) {
                synchronized (surface) {
                    Canvas canvas = surface.lockCanvas(null);
                    canvas.drawColor(Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                    surface.unlockCanvasAndPost(canvas);
                    result.success(null);
                }
            }
        }
    }

    private DrawThread drawThread;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

        String method = methodCall.method;

        switch (method) {

            case "init":
                textureRegistry = flutterRegistrar.textures();
                surfaceTextureEntry = textureRegistry.createSurfaceTexture();
                SurfaceTexture tex = surfaceTextureEntry.surfaceTexture();
                tex.setDefaultBufferSize(640, 480);
                surface = new Surface(tex);

                Map<String, Long> reply = new HashMap<>();
                long textureId = surfaceTextureEntry.id();

                if (camera != null) {
                    camera.close();
                }
                camera = new Camera();
                this.flutterRegistrar.activity()
                        .getApplication()
                        .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks);

                reply.put("textureId", textureId);
                result.success(reply);
                break;

            case "render":
//                drawThread = new DrawThread(result);
//                drawThread.run();
                Canvas canvas = surface.lockCanvas(null);
                //canvas.drawColor(Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
//                Bitmap picture = BitmapFactory.decodeResource(flutterRegistrar.activity().getResources(), R.drawable.robot);
                Paint p = new Paint();
                p.setColor(Color.BLUE);
                //p.setStrokeWidth(99.000003f);
                p.setStrokeWidth(10);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawPoint(320 - 5, 220 - 5, p);

                //canvas.drawBitmap(picture, new Rect(0,0,300,300), new Rect(0,0, 640,480), null );
//                synchronized (surface) {

                //canvas.drawPoint(50, 50, p);
                surface.unlockCanvasAndPost(canvas);
//                }

                result.success(null);
                break;

            default:
                break;
        }
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter.io/SurfaceTest");
        channel.setMethodCallHandler(new FaceCameraPlugin(registrar));
        cameraManager = (CameraManager) registrar.activity().getSystemService(Context.CAMERA_SERVICE);
    }

    private FaceCameraPlugin(Registrar registrar) {
        registrar.addRequestPermissionsResultListener(new CameraRequestPermissionsListener());
        this.flutterRegistrar = registrar;
        random = new Random();

        this.activityLifecycleCallbacks =
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        boolean wasRequestingPermission = requestingPermission;
                        if (requestingPermission) {
                            requestingPermission = false;
                        }
                        if (activity != FaceCameraPlugin.this.flutterRegistrar.activity()) {
                            return;
                        }

                        if (camera != null && !wasRequestingPermission) {
                            camera.openCamera(null);
                        }
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        if (activity == FaceCameraPlugin.this.flutterRegistrar.activity()) {

                            if (camera != null) {
                                camera.close();
                            }
                        }
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        if (activity == FaceCameraPlugin.this.flutterRegistrar.activity()) {
                            if (camera != null) {
                                camera.close();
                            }
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                };
    }

    // CAMERA

    private class Camera {

        private CameraDevice cameraDevice;
        private CameraCaptureSession cameraCaptureSession;
        private CaptureRequest.Builder captureRequestBuilder;
        private ImageReader imageStreamReader;

        Camera() {
            FirebaseVisionFaceDetectorOptions realTimeOpts =
                    new FirebaseVisionFaceDetectorOptions.Builder()
                            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                            .build();

            detector = FirebaseVision.getInstance().getVisionFaceDetector(realTimeOpts);

            metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(640)   // 480x360 is typically sufficient for
                    .setHeight(480)  // image recognition
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
                    .build();

            try {
                String[] cameraNames = cameraManager.getCameraIdList();
                for (String cameraName : cameraNames) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraName);
                    int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing == CameraMetadata.LENS_FACING_FRONT) {
                        openCameraWithPermissions(cameraName);
                    }
                }

            } catch (CameraAccessException e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
        }

        private void closeCaptureSession() {
            if (cameraCaptureSession != null) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
        }

        private void close() {
            closeCaptureSession();

            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }

            if (imageStreamReader != null) {
                imageStreamReader.close();
                imageStreamReader = null;
            }
        }

        SurfaceTexture surfaceTexture;

        private void startPreviewWithImageStream() throws CameraAccessException {
            closeCaptureSession();

            surfaceTexture = new SurfaceTexture(10);
            surfaceTexture.setDefaultBufferSize(640, 480);

            captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            List<Surface> surfaces = new ArrayList<>();

            final Surface previewSurface = new Surface(surfaceTexture);
            surfaces.add(previewSurface);
            captureRequestBuilder.addTarget(previewSurface);

            surfaces.add(imageStreamReader.getSurface());
            captureRequestBuilder.addTarget(imageStreamReader.getSurface());

            cameraDevice.createCaptureSession(
                    surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                //sendErrorEvent("The camera was closed during configuration.");
                                return;
                            }
                            try {
                                cameraCaptureSession = session;
                                captureRequestBuilder.set(
                                        CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                //sendErrorEvent(e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            //sendErrorEvent("Failed to configure the camera for streaming images.");
                        }
                    },
                    null);


            imageStreamReader.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(final ImageReader reader) {
                            Image img = reader.acquireLatestImage();

                            if (img == null) return;

                            final byte[] dataYUV = imageToByteBuffer(img);

                            img.close();

                            Bitmap bitmap = YUV_420_888_toRGBIntrinsics(flutterRegistrar.activity(), 640, 480, dataYUV);

                            Matrix matrix = new Matrix();
                            matrix.postRotate(-90);
                            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, 640, 480,
                                    matrix, true);

                            bitmap.recycle();

                            Paint p = new Paint();
                            p.setColor(Color.GREEN);
                            p.setStrokeWidth(2);
                            p.setStyle(Paint.Style.STROKE);

                            Canvas canvas = surface.lockCanvas(null);

                            canvas.drawBitmap(rotated, 0, 0, null);

                            if (faceRect != null) {
                                canvas.drawRect(faceRect, p);
                            }

                            if (upperLipBottomContour != null) {
                                for (FirebaseVisionPoint point : upperLipBottomContour) {
                                    canvas.drawPoint(
                                            point.getX().intValue(),
                                            point.getY().intValue(),
                                            p
                                    );
                                }
                            }

                            if (lowerLipTopContour != null) {
                                for (FirebaseVisionPoint point : lowerLipTopContour) {
                                    canvas.drawPoint(
                                            point.getX().intValue(),
                                            point.getY().intValue(),
                                            p
                                    );
                                }
                            }

                            surface.unlockCanvasAndPost(canvas);

                            // facedetection

                            if (faceDetectionResult != null && !faceDetectionResult.isComplete()) {
                                rotated.recycle();
                                return;
                            }

                            FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(dataYUV, metadata);

                            faceDetectionResult =
                                    detector.detectInImage(image)
                                            .addOnSuccessListener(
                                                    new OnSuccessListener<List<FirebaseVisionFace>>() {
                                                        @Override
                                                        public void onSuccess(List<FirebaseVisionFace> faces) {
                                                            // Task completed successfully
                                                            // ...
                                                            Log.d(TAG, "face detection has successed!");

                                                            for (FirebaseVisionFace face : faces) {
                                                                faceRect = face.getBoundingBox();

                                                                upperLipBottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
                                                                lowerLipTopContour = face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints();
                                                            }
                                                        }
                                                    })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            // Task failed with an exception
                                                            // ...
                                                            Log.d(TAG, "face detection has failed");
                                                        }
                                                    });

                            //rotated.recycle();
                        }
                    },
                    null);
        }

        private Task<List<FirebaseVisionFace>> faceDetectionResult;
        private Rect faceRect;
        private List<FirebaseVisionPoint> upperLipBottomContour;
        private List<FirebaseVisionPoint> lowerLipTopContour;
        private FirebaseVisionFaceDetector detector;
        private FirebaseVisionImageMetadata metadata;

        private String cameraName;

        @SuppressLint("MissingPermission")
        private void openCamera(String name) {

            if (name != null) {
                cameraName = name;
            }

            if (!hasCameraPermission()) {
                //if (result != null) result.error("cameraPermission", "Camera permission not granted", null);
            } else {
                try {
                    imageStreamReader =
                            ImageReader.newInstance(
                                    640, 480, ImageFormat.YUV_420_888, 5);

                    cameraManager.openCamera(cameraName, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice cameraDevice) {
                            Camera.this.cameraDevice = cameraDevice;
                            try {
                                //startPreview();
                                startPreviewWithImageStream();
                            } catch (CameraAccessException e) {
//                                if (result != null)
//                                    result.error("CameraAccess", e.getMessage(), null);
                                cameraDevice.close();
                                Camera.this.cameraDevice = null;
                                return;
                            }

//                            if (result != null) {
//                                Map<String, Object> reply = new HashMap<>();
//                                reply.put("textureId", textureEntry.id());
//                                reply.put("previewWidth", previewSize.getWidth());
//                                reply.put("previewHeight", previewSize.getHeight());
//                                result.success(reply);
//                            }
                        }

                        @Override
                        public void onClosed(CameraDevice camera) {
//                            if (eventSink != null) {
//                                Map<String, String> event = new HashMap<>();
//                                event.put("eventType", "cameraClosing");
//                                eventSink.success(event);
//                            }
                            super.onClosed(camera);
                        }

                        @Override
                        public void onDisconnected(CameraDevice cameraDevice) {
                            cameraDevice.close();
                            Camera.this.cameraDevice = null;
//                            sendErrorEvent("The camera was disconnected.");
                        }

                        @Override
                        public void onError(CameraDevice cameraDevice, int errorCode) {
                            cameraDevice.close();
                            Camera.this.cameraDevice = null;
                            String errorDescription;
                            switch (errorCode) {
                                case ERROR_CAMERA_IN_USE:
                                    errorDescription = "The camera device is in use already.";
                                    break;
                                case ERROR_MAX_CAMERAS_IN_USE:
                                    errorDescription = "Max cameras in use";
                                    break;
                                case ERROR_CAMERA_DISABLED:
                                    errorDescription =
                                            "The camera device could not be opened due to a device policy.";
                                    break;
                                case ERROR_CAMERA_DEVICE:
                                    errorDescription = "The camera device has encountered a fatal error";
                                    break;
                                case ERROR_CAMERA_SERVICE:
                                    errorDescription = "The camera service has encountered a fatal error.";
                                    break;
                                default:
                                    errorDescription = "Unknown camera error";
                            }
//                            sendErrorEvent(errorDescription);
                        }
                    }, null);
                } catch (CameraAccessException e) {
                    //if (result != null) result.error("cameraAccess", e.getMessage(), null);
                }
            }
        }

        private void openCameraWithPermissions(final String name) {
            try {
                if (cameraPermissionContinuation != null) {
                    //result.error("cameraPermission", "Camera permission request ongoing", null);
                    return;
                }

                cameraPermissionContinuation =
                        new Runnable() {
                            @Override
                            public void run() {
                                cameraPermissionContinuation = null;
                                if (!hasCameraPermission()) {
                                    //                            result.error(
                                    //                                    "cameraPermission",
                                    //                                    "MediaRecorderCamera permission not granted",
                                    //                                    null
                                    //                            );
                                    return;
                                }
                                openCamera(name);
                            }
                        };

                requestingPermission = false;

                if (hasCameraPermission()) {
                    cameraPermissionContinuation.run();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestingPermission = true;
                        flutterRegistrar
                                .activity()
                                .requestPermissions(
                                        new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                                        CAMERA_REQUEST_ID);
                    }
                }
//            } catch (CameraAccessException e) {
                //result.error("CameraAccess", e.getMessage(), null);
            } catch (IllegalArgumentException e) {
                //result.error("IllegalArgumentException", e.getMessage(), null);
            }
        }
    }

    // CAMERA PERMISSION
    private boolean requestingPermission;

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || flutterRegistrar.activity().checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private Runnable cameraPermissionContinuation;

    private class CameraRequestPermissionsListener
            implements PluginRegistry.RequestPermissionsResultListener {
        @Override
        public boolean onRequestPermissionsResult(int id, String[] permissions, int[] grantResults) {
            if (id == CAMERA_REQUEST_ID) {
                cameraPermissionContinuation.run();
                return true;
            }
            return false;
        }
    }

    // CAMERA IMAGE CONVERT

    private byte[] convertYUV420ToNV21_ALL_PLANES(Image imgYUV420) {

        assert (imgYUV420.getFormat() == ImageFormat.YUV_420_888);
        Log.d(TAG, "image: " + imgYUV420.getWidth() + "x" + imgYUV420.getHeight() + " " + imgYUV420.getFormat());
        Log.d(TAG, "planes: " + imgYUV420.getPlanes().length);
        for (int nplane = 0; nplane < imgYUV420.getPlanes().length; nplane++) {
            Log.d(TAG, "plane[" + nplane + "]: length " + imgYUV420.getPlanes()[nplane].getBuffer().remaining() + ", strides: " + imgYUV420.getPlanes()[nplane].getPixelStride() + " " + imgYUV420.getPlanes()[nplane].getRowStride());
        }

        byte[] rez = new byte[imgYUV420.getWidth() * imgYUV420.getHeight() * 3 / 2];
        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer1 = imgYUV420.getPlanes()[1].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();

        int n = 0;
        assert (imgYUV420.getPlanes()[0].getPixelStride() == 1);
        for (int row = 0; row < imgYUV420.getHeight(); row++) {
            for (int col = 0; col < imgYUV420.getWidth(); col++) {
                rez[n++] = buffer0.get();
            }
        }
        assert (imgYUV420.getPlanes()[2].getPixelStride() == imgYUV420.getPlanes()[1].getPixelStride());
        int stride = imgYUV420.getPlanes()[1].getPixelStride();
        for (int row = 0; row < imgYUV420.getHeight(); row += 2) {
            for (int col = 0; col < imgYUV420.getWidth(); col += 2) {
                rez[n++] = buffer1.get();
                rez[n++] = buffer2.get();
                for (int skip = 1; skip < stride; skip++) {
                    if (buffer1.remaining() > 0) {
                        buffer1.get();
                    }
                    if (buffer2.remaining() > 0) {
                        buffer2.get();
                    }
                }
            }
        }

        Log.w(TAG, "total: " + rez.length);
        return rez;
    }

    public static Bitmap YUV_420_888_toRGBIntrinsics(Context context, int width, int height, byte[] yuv) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuv.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);


        Bitmap bmpOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        in.copyFromUnchecked(yuv);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        out.copyTo(bmpOut);
        return bmpOut;
    }

//    RenderScript rs;
//    ScriptIntrinsicYuvToRGB yuvToRgb;
//    Allocation yuvAllocation;
//    Type.Builder yuvType;
//    Allocation rgbaAllocation;
//    Type.Builder rgbaType;
//    Bitmap bmpout;

//    public Bitmap convertYuvImageToBitmap(Context context, YuvImage yuvImage) {
//
//        int w = yuvImage.getWidth();
//        int h = yuvImage.getHeight();
//
//        if (rs == null) {
//            // once
//            rs = RenderScript.create(context);
//            yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
//        }
//
//        if (yuvAllocation == null || yuvAllocation.getBytesSize() < yuvImage.getYuvData().length) {
//            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuvImage.getYuvData().length);
//            yuvAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
//            Log.w(TAG, "allocate in " + yuvAllocation.getBytesSize() + " " + w + "x" + h);
//        }
//
//        if (rgbaAllocation == null ||
//                rgbaAllocation.getBytesSize() < rgbaAllocation.getElement().getBytesSize()*w*h) {
//            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
//            rgbaAllocation = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
//            Log.w(TAG, "allocate out " + rgbaAllocation.getBytesSize() + " " + w + "x" + h);
//        }
//
//        yuvAllocation.copyFrom(yuvImage.getYuvData());
//
//        yuvToRgb.setInput(yuvAllocation);
//        yuvToRgb.forEach(rgbaAllocation);
//
//        if (bmpout == null) {
//            bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//        }
//        rgbaAllocation.copyTo(bmpout);
//        return bmpout;
//    }

    private byte[] imageToByteBuffer(final Image image) {
        final Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();

        final Image.Plane[] planes = image.getPlanes();
        final byte[] rowData = new byte[planes[0].getRowStride()];
        final int bufferSize = image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset = 0;
        int outputStride = 0;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else if (planeIndex == 2) {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer = planes[planeIndex].getBuffer();
            final int rowStride = planes[planeIndex].getRowStride();
            final int pixelStride = planes[planeIndex].getPixelStride();

            final int shift = (planeIndex == 0) ? 0 : 1;
            final int widthShifted = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

            for (int row = 0; row < heightShifted; row++) {
                final int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        byte[] arr = new byte[output.remaining()];
        output.get(arr);

        return arr;
    }
}
