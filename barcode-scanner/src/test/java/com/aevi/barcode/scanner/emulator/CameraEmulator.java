package com.aevi.barcode.scanner.emulator;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class CameraEmulator {

    public static final String[] CAMERA_LIST_SAMPLE = {
            "backCamera",
            "frontCamera"
    };

    private static Range mockRange(int lower, int upper) {
        Range range = Mockito.mock(Range.class);
        Mockito.doReturn(lower).when(range).getLower();
        Mockito.doReturn(upper).when(range).getUpper();
        return range;
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.setAccessible(true);
        field.set(null, newValue);
    }

    static {
        for (String field : new String[]{"CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES", "SCALER_STREAM_CONFIGURATION_MAP", "SENSOR_ORIENTATION"}) {
            try {
                setFinalStatic(CameraCharacteristics.class.getField(field), Mockito.mock(CameraCharacteristics.Key.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final CameraManager cameraManager = Mockito.mock(CameraManager.class);
    private final CameraCharacteristics characteristics = Mockito.mock(CameraCharacteristics.class);
    private final StreamConfigurationMap streamConfigurationMap = Mockito.mock(StreamConfigurationMap.class);

    private final ArgumentCaptor<CameraDevice.StateCallback> openCameraCaptor = ArgumentCaptor.forClass(CameraDevice.StateCallback.class);
    private final ArgumentCaptor<CameraCaptureSession.StateCallback> createCaptureSessionCaptor =
            ArgumentCaptor.forClass(CameraCaptureSession.StateCallback.class);


    public CameraEmulator() {
    }

    public CameraEmulator(String[] cameraList) {
        try {
            Mockito.doReturn(cameraList).when(cameraManager).getCameraIdList();
            Mockito.doReturn(characteristics).when(cameraManager).getCameraCharacteristics(Mockito.anyString());
            Mockito.doReturn(Integer.valueOf(0)).when(characteristics).get(CameraCharacteristics.SENSOR_ORIENTATION);
            Mockito.doReturn(new Range[]{mockRange(5, 30)}).when(characteristics).get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Mockito.doReturn(streamConfigurationMap).when(characteristics).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Mockito.doReturn(new Size[]{Mockito.mock(Size.class)}).when(streamConfigurationMap).getOutputSizes(Mockito.any(Class.class));
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public CameraDevice onOpened(String cameraId) {
        try {
            Mockito.verify(cameraManager).openCamera(Mockito.eq(cameraId), openCameraCaptor.capture(), Mockito.nullable(Handler.class));
            CameraDevice cameraDevice = Mockito.mock(CameraDevice.class);
            Mockito.doReturn(cameraId).when(cameraDevice).getId();
            Mockito.doReturn(Mockito.mock(CaptureRequest.Builder.class)).when(cameraDevice).createCaptureRequest(Mockito.anyInt());
            openCameraCaptor.getValue().onOpened(cameraDevice);
            return cameraDevice;
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onDisconnected(CameraDevice cameraDevice) {
        openCameraCaptor.getValue().onDisconnected(cameraDevice);
    }

    public void onError(CameraDevice cameraDevice, int errorCode) {
        openCameraCaptor.getValue().onError(cameraDevice, errorCode);
    }

    public CameraCaptureSession onConfigured(CameraDevice cameraDevice, List<Surface> surfaces) {
        try {
            Mockito.verify(cameraDevice)
                    .createCaptureSession(Mockito.eq(surfaces), createCaptureSessionCaptor.capture(), Mockito.nullable(Handler.class));
            CameraCaptureSession cameraCaptureSession = Mockito.mock(CameraCaptureSession.class);
            Mockito.doReturn(cameraDevice).when(cameraCaptureSession).getDevice();
            createCaptureSessionCaptor.getValue().onConfigured(cameraCaptureSession);
            return cameraCaptureSession;
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onClosed(CameraCaptureSession cameraCaptureSession) {
        createCaptureSessionCaptor.getValue().onClosed(cameraCaptureSession);
    }

    public void onConfigureFailed(CameraDevice cameraDevice, List<Surface> surfaces) {
        try {
            Mockito.verify(cameraDevice)
                    .createCaptureSession(Mockito.eq(surfaces), createCaptureSessionCaptor.capture(), Mockito.nullable(Handler.class));
            createCaptureSessionCaptor.getValue().onConfigureFailed(Mockito.mock(CameraCaptureSession.class));
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
