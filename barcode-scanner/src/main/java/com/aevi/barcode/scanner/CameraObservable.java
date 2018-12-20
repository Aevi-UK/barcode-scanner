package com.aevi.barcode.scanner;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class CameraObservable extends CameraDevice.StateCallback implements ObservableOnSubscribe<CameraDevice> {

    public static Observable<CameraDevice> create(CameraManager cameraManager) {
        return Observable.create(new CameraObservable(cameraManager));
    }

    private final CameraManager cameraManager;
    private volatile CameraDevice cameraDevice;
    private volatile ObservableEmitter<CameraDevice> observableEmitter;

    private CameraObservable(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void subscribe(ObservableEmitter<CameraDevice> emitter) throws Exception {
        observableEmitter = emitter;
        if (!observableEmitter.isDisposed()) {
            observableEmitter.setCancellable(() -> {
                if (cameraDevice != null) {
                    cameraDevice.close();
                }
            });
            cameraManager.openCamera(cameraManager.getCameraIdList()[0], this, null);

        }
    }

    @Override
    public void onOpened(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
        observableEmitter.onNext(cameraDevice);
        if (observableEmitter != null && observableEmitter.isDisposed()) {
            cameraDevice.close();
        }
    }

    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
        observableEmitter.onComplete();
    }

    @Override
    public void onError(CameraDevice cameraDevice, int i) {
        observableEmitter.tryOnError(new Exception("Unable to open camera, return code: " + i));
        cameraDevice.close();
    }
}
