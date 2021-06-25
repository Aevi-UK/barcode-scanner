# Barcode scanner
Besides what its name says this library is primarily intended to scan QR codes and has been tweaked as such. Regular barcode recognition may be added in the future, the core of this library being based on ZBar. 
The zbar sources have been imported from the most up to date available revision found at: http://zbar.hg.sourceforge.net:8000/hgroot/zbar/zbar (changeset _362:38e78368283d_)

## Build
This library depends on the following Android SDK components to build:
* ndk-bundle
* cmake

In order to be able to download dependencies you will need to generate a [Github personal access token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) with the `read:packages` OAuth scope and add the following properties to your `$HOME/.gradle/gradle.properties` file:
```
github_actor=[your github username]
github_token=[your github token]
```
This will allow Gradle to fetch prebuilt binaries of libraries from the AEVI publication repository. You can then build the project using Gradle:
```./gradlew assembleRelease```

## Usage
You will need to generate a [Github personal access token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) with the `read:packages` OAuth scope and then define the following repository in you main gradle project file:
```
maven {
    name = "github"
    url = uri("https://maven.pkg.github.com/aevi-uk/*")
    credentials {
        username = "[your github username]"
        password = "[your github personal access token]"
    }
}
```

This will allow Gradle to fetch prebuilt binaries of the library from the AEVI publication repository. You can then add the dependency to your relevant module _build.gradle_ file:
```
implementation 'com.aevi.barcode:barcode-scanner:<version>'
```

### Camera preview
To create a camera preview in your activity / fragment, simply use the dedicated `Camera2Preview` view, you can embed it directly in your layout file:
```xml
<com.aevi.barcode.scanner.Camera2Preview
        android:id="@+id/camera_preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```
In order for the preview to start, you will still need to explictly call the `start()` method of `Camera2Preview` and subscribe to the returned `Observable`. This will return a `Disposable` which you will then be able to use in order to stop the preview. For a concrete example, refer to the next section.

### Code scanning
Once you have started the camera preview as described in the previous section, scanning codes is relatively simple. Just call `BarcodeObservable.create()` passing the `Observable` returned by the `Camera2Preview.start()` method and you will get notified whenever a valid code is scanned.

```java
@Override
public void onResume() {
    super.onResume();
    disposable = camera2Preview.start(BarcodeScanner.IMAGE_FORMAT)
                    .compose(new BarcodeScanner())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(content -> Log.d("QrActivity", "Scanned QR code: " + content));
}

@Override
public void onPause() {
    super.onPause();
    disposable.dispose();
}
```

## License
This library is licensed under the [LGPL license](LICENSE)

Copyright (c) 2021 AEVI International GmbH. All rights reserved
