# CameraX-QRcode

ZXing QR code package library implemented by CameraX

## Feature

- Simple to use
- Auto focus camera, improve decoding speed
- When scanning and decoding is successful, it will automatically stop scanning
- No need to consider camera destruction related actions

## Download

```gradle
implementation 'io.github.clistery:camerax-scanner-qrcode:1.1.2'
```

## USE

- in xml

  ```xml
  <com.yh.cxqr.QRScannerView
      android:id="@+id/scanner"
      android:layout_width="match_parent"
      android:layout_height="match_parent" />
  ```

- start scan

  ```kotlin
  beginScan()
  ```

- end scan

  ```kotlin
  stopScan()
  ```

- register scan result callback

  ```kotlin
  resultCallback = object : QRScannerView.IScanResultCallback {
      override fun onCallback(result: Barcode) {
      }
  }
  ```

- on/off flash

  ```kotlin
  switchFlash()
  ```

- decode ImageProxy

  ```kotlin
  QRCodeParser().decodeImageProxy(imageProxy, success, fail)
  ```

- decode image uri

  ```kotlin
  QRCodeParser().decodeImageUri(context, fileUri, success, fail)
  ```

- decode bitmap

  ```kotlin
  QRCodeParser().decodeBitmap(originBitmap, success, fail)
  ```
