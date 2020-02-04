package com.aevi.barcode.scanner;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

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
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;

public class CameraFrameObservable {

    public interface ImageReaderFactory {

        ImageReader create(int width, int height, int maxImages);
    }

    public static Observable<Image> create(CameraManager cameraManager, Observable<CameraDevice> cameraObservable,
                                           Observable<Surface> surfaceTextureObservable, ImageReaderFactory imageReaderFactory, Scheduler scheduler) {
        return cameraObservable.zipWith(surfaceTextureObservable, (cameraDevice, surface) -> Tuple.of(cameraDevice, surface))
                .concatMap(params -> {
                    CameraDevice camera = params.t1;
                    Surface surface = params.t2;
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(camera.getId());
                    Size imageReaderSize = findOptimalSize(characteristics, 1080, 720, 0d);
                    final ImageReader imageReader = imageReaderFactory.create(imageReaderSize.getWidth(), imageReaderSize.getHeight(), 3);
                    return CaptureSessionObservable.create(camera, Arrays.asList(surface, imageReader.getSurface()))
                            .concatMap((Function<CameraCaptureSession, ObservableSource<ImageReader>>) cameraCaptureSession -> {
                                        CaptureRequest.Builder captureRequestBuilder =
                                                cameraCaptureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, findOptimalAutoExposureRange(characteristics));
                                        captureRequestBuilder.addTarget(surface);
                                        captureRequestBuilder.addTarget(imageReader.getSurface());
                                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                                        return Observable.just(imageReader);
                                    }
                            );
                }).flatMap((Function<ImageReader, ObservableSource<Image>>) imageReader -> ImageObservable.create(imageReader, 500, scheduler));
    }

    private static Size findOptimalSize(CameraCharacteristics characteristics, int width, int height, double ratioDelta)
            throws CameraAccessException {
        StreamConfigurationMap configuration = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = configuration.getOutputSizes(SurfaceTexture.class);
        Size optimal = sizes[0];
        for (Size size : sizes) {
            if (size.getWidth() >= width && size.getHeight() >= height) {
                // Choose the smallest size which matches our criteria
                if (optimal.getWidth() * optimal.getHeight() > size.getWidth() * size.getHeight()) {
                    optimal = size;
                }
                // Fallback to the one with the more pixels
            } else if (optimal.getWidth() * optimal.getHeight() < size.getWidth() * size.getHeight()) {
                optimal = size;
            }
        }
        return optimal;
    }

    private static Range<Integer> findOptimalAutoExposureRange(CameraCharacteristics characteristics) {
        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Range<Integer> optimal = ranges[0];
        for (Range<Integer> range : ranges) {
            // We just lookup for the one with the lower value here since we don't really care about fps drops for a preview
            if (range.getLower() < optimal.getLower()) {
                optimal = range;
            }
        }
        return optimal;
    }
}
