package com.aevi.barcode.scanner.emulator;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SurfaceTextureEmulator {

    private final TextureView textureView = Mockito.mock(TextureView.class);
    private final SurfaceTexture surfaceTexture = Mockito.mock(SurfaceTexture.class);
    private final Surface surface = Mockito.mock(Surface.class);
    private final ArgumentCaptor<TextureView.SurfaceTextureListener> captor = ArgumentCaptor.forClass(TextureView.SurfaceTextureListener.class);

    public TextureView getTextureView() {
        return textureView;
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public Surface getSurface() {
        return surface;
    }

    public void setAvailability(boolean value) {
        Mockito.doReturn(value).when(textureView).isAvailable();
        Mockito.doReturn(surfaceTexture).when(textureView).getSurfaceTexture();
    }

    public void onSurfaceTextureAvailable() {
        Mockito.verify(textureView).setSurfaceTextureListener(captor.capture());
        captor.getValue().onSurfaceTextureAvailable(surfaceTexture, 100, 100);
    }

    public void onNullSurfaceTextureAvailable() {
        Mockito.verify(textureView).setSurfaceTextureListener(captor.capture());
        captor.getValue().onSurfaceTextureAvailable(null, 100, 100);
    }

    public void onSurfaceTextureDestroyed() {
        captor.getValue().onSurfaceTextureDestroyed(surfaceTexture);
    }
}
