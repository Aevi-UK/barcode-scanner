package com.aevi.barcode.scanner;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.view.Surface;
import android.view.WindowManager;
import com.aevi.barcode.scanner.emulator.CameraEmulator;
import com.aevi.barcode.scanner.emulator.ImageReaderEmulator;
import com.aevi.barcode.scanner.emulator.SurfaceTextureEmulator;
import io.reactivex.observers.TestObserver;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class TestCameraFrameObservable extends BaseTest {

    private final WindowManager windowManager = Mockito.mock(WindowManager.class);
    private final CameraEmulator cameraEmulator = new CameraEmulator(CameraEmulator.CAMERA_LIST_SAMPLE);
    private final SurfaceTextureEmulator surfaceEmulator = new SurfaceTextureEmulator();
    private final ImageReaderEmulator imageReaderEmulator = new ImageReaderEmulator();
    private final List<Surface> surfaces = Arrays.asList(surfaceEmulator.getSurface(), imageReaderEmulator.getImageReader().getSurface());

    @Test
    public void doCaptureFrame() {
        TestObserver<Image> observer = setupObserver();

        CameraDevice cameraDevice = cameraEmulator.onOpened(CameraEmulator.CAMERA_LIST_SAMPLE[0]);
        surfaceEmulator.onSurfaceTextureAvailable();
        CameraCaptureSession captureSession = cameraEmulator.onConfigured(cameraDevice, surfaces);
        Image image = imageReaderEmulator.onImageAvailable();

        observer.dispose();
        observer.assertValue(image);
        InOrder inOrder = Mockito.inOrder(image, captureSession, cameraDevice, surfaceEmulator.getSurface(), imageReaderEmulator.getImageReader());
        inOrder.verify(image).close();
        inOrder.verify(captureSession).close();
        inOrder.verify(cameraDevice).close();
        inOrder.verify(surfaceEmulator.getSurface()).release();
        inOrder.verify(imageReaderEmulator.getImageReader()).close();
    }

    @Test
    public void doHandleCameraCaptureException() {
        TestObserver<Image> observer = setupObserver();
        CameraDevice cameraDevice = cameraEmulator.onOpened(CameraEmulator.CAMERA_LIST_SAMPLE[0]);
        surfaceEmulator.onSurfaceTextureAvailable();
        cameraEmulator.onConfigureFailed(cameraDevice, surfaces);

        observer.assertError(Exception.class);
        Mockito.verify(surfaceEmulator.getSurface()).release();
        Mockito.verify(cameraDevice).close();
    }

    private TestObserver<Image> setupObserver() {
        return Camera2Preview.createObservable(SurfaceTextureObservable.create(surfaceEmulator.getTextureView(), mainHandler), 0,
                cameraEmulator.getCameraManager(), surfaceTexture -> surfaceEmulator.getSurface(), (width, height, maxImages) -> imageReaderEmulator.getImageReader(),
                null, (a, b) -> {
                }).test();
    }
}
