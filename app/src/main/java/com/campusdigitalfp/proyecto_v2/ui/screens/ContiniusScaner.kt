package com.campusdigitalfp.proyecto_v2.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback

@Composable
fun ContinuousScanner(
    modifier: Modifier = Modifier,
    onScan: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->

            val barcodeView = DecoratedBarcodeView(context)

            val callback = object : BarcodeCallback {
                override fun barcodeResult(result: com.journeyapps.barcodescanner.BarcodeResult?) {

                    result?.text?.let { code ->
                        onScan(code)
                    }
                }

                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
            }

            barcodeView.decodeContinuous(callback)

            barcodeView.resume()

            barcodeView
        }
    )
}