/*
 * Copyright (c) 2019 AEVI International GmbH. All rights reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.aevi.barcode.scanner;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import com.aevi.barcode.scanner.CameraFrameObservable.ImageReaderFactory;
import com.aevi.barcode.scanner.SurfaceTextureObservable.Callback;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class Camera2Preview extends TextureView {

    public interface SurfaceFactory {
        Surface create(SurfaceTexture surfaceTexture);
    }

    public interface OnPreviewReadyListener {
        void onPreviewReady(int sensorOrientation, Size cameraSize);
    }

    private WindowManager windowManager;
    private Runnable transform = null;

    public Camera2Preview(Context context) {
        super(context);
    }

    public Camera2Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2Preview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Camera2Preview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public Observable<Image> start(int imageFormat) {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Observable<Tuple.Tuple2<Callback, SurfaceTexture>> surfaceTextureObservable = SurfaceTextureObservable
                .create(this, new Handler(Looper.getMainLooper()))
                .doOnNext(tuple -> transform(tuple.t1));

        return createObservable(surfaceTextureObservable,
                (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE),
                (surfaceTexture) -> new Surface(surfaceTexture),
                (width, height, maxImages) -> ImageReader.newInstance(width, height, imageFormat, maxImages),
                Schedulers.computation(), (sensorOrientation, cameraSize) -> {
                    transform = () -> transform(sensorOrientation, cameraSize);
                    transform.run();
                });
    }

    public static Observable<Image> createObservable(Observable<Tuple.Tuple2<Callback, SurfaceTexture>> surfaceTextureObservable,
                                                     CameraManager cameraManager, SurfaceFactory surfaceFactory,
                                                     ImageReaderFactory imageReaderFactory, Scheduler scheduler,
                                                     OnPreviewReadyListener onPreviewReadyListener) {

        return CameraFrameObservable.create(CameraObservable.create(cameraManager).zipWith(
                surfaceTextureObservable.filter(event -> Callback.AVAILABLE.equals(event.t1)), (camera, surfaceEvent) -> {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(camera.getId());
                    SurfaceTexture surfaceTexture = surfaceEvent.t2;
                    Size cameraSize = CameraFrameObservable.findOptimalSize(characteristics, 1080, 720, 0d);
                    surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
                    onPreviewReadyListener.onPreviewReady(characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION), cameraSize);
                    return Tuple.of(camera, characteristics, surfaceTexture);
                })
                .flatMap(tuple -> Observable.using(() -> surfaceFactory.create(tuple.t3),
                        surface -> Observable.create(emitter -> emitter.onNext(Tuple.of(tuple.t1, tuple.t2, surface))),
                        surface -> surface.release())), imageReaderFactory, scheduler);
    }

    private void transform(Callback type) {
        if (transform != null && Callback.SIZE_CHANGED.equals(type)) {
            transform.run();
        }
    }

    private void transform(int sensorOrientation, Size cameraSize) {
        SizeF frameSize = sensorOrientation % 180 == 0 ? new SizeF(cameraSize.getWidth(), cameraSize.getHeight())
                : new SizeF(cameraSize.getHeight(), cameraSize.getWidth());

        PointF center = new PointF(getWidth() / 2f, getHeight() / 2f);
        Matrix matrix = new Matrix();

        matrix.preScale(frameSize.getWidth() / getWidth(), frameSize.getHeight() / getHeight(), center.x, center.y);

        int rotation = (windowManager.getDefaultDisplay().getRotation() * -90) % 360;
        matrix.postRotate(rotation, center.x, center.y);

        SizeF rotatedFrameSize = rotation % 180 == 0 ? frameSize : new SizeF(frameSize.getHeight(), frameSize.getWidth());
        float ratio = Math.max(getWidth() / rotatedFrameSize.getWidth(), getHeight() / rotatedFrameSize.getHeight());
        matrix.postScale(ratio, ratio, center.x, center.y);

        setTransform(matrix);
    }
}
