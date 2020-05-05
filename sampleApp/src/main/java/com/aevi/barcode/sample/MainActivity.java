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
package com.aevi.barcode.sample;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.aevi.barcode.scanner.BarcodeScanner;
import com.aevi.barcode.scanner.Camera2Preview;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements Consumer<String> {

    private static final String DIALOG_TAG = "dialog";

    public static class ResultDialog extends DialogFragment {

        private static final String PARAM_CONTENT = "content";

        public static ResultDialog create(String content) {
            Bundle bundle = new Bundle();
            bundle.putString(PARAM_CONTENT, content);
            ResultDialog dialog = new ResultDialog();
            dialog.setArguments(bundle);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String content = getArguments().getString(PARAM_CONTENT, getString(R.string.text_no_content));
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.title_scanned_code)
                    .setMessage(getArguments().getString(PARAM_CONTENT, getString(R.string.text_no_content)))
                    .setPositiveButton(android.R.string.copy, (dialog, which) -> copy(content))
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        private void copy(String value) {
            ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("qrcode", value));
            Toast.makeText(getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static final String CAMERA_INDEX = "camera_index";

    private int cameraIndex = 0;
    private Camera2Preview camera2Preview;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera2Preview = findViewById(R.id.camera_preview);
        findViewById(R.id.camera_switch).setOnClickListener((view) -> switchCamera());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CAMERA_INDEX, cameraIndex);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        cameraIndex = savedInstanceState.getInt(CAMERA_INDEX);

    }

    private void startScanning() {
        disposable = camera2Preview.start(cameraIndex, Camera2Preview.FillMode.ZOOM, 0, BarcodeScanner.IMAGE_FORMAT)
                .compose(new BarcodeScanner())
                .observeOn(AndroidSchedulers.mainThread())
                .retryWhen(throwable -> throwable.delay(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()))
                .subscribe(this);
    }

    private void stopScanning() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void switchCamera() {
        cameraIndex += 1;
        stopScanning();
        startScanning();
    }

    @Override
    public void accept(String barcode) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            ResultDialog.create(barcode).show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }
}
