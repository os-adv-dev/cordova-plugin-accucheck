package com.outsystems.experts.accucheck

import android.widget.Toast
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray


private const val TAG = "AccuCheckPlugin"
private const val START_SCAN = "startScan"
private const val CONNECT_DEVICE = "connectDevice"
private const val READ_DATA = "readData"

class AccuCheckPlugin : CordovaPlugin() {

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)
       // init the bluecandy SDK
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
   
        if (action == START_SCAN) {
            this.startScan(callbackContext)
            return true
        }

        if (action == CONNECT_DEVICE) {
            this.connectDevice(callbackContext)
            return true
        }

        if (action == READ_DATA) {
            this.readData(callbackContext)
            return true
        }

        return false
    }

    private fun startScan(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun connectDevice(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun readData(callbackContext: CallbackContext) {
        callbackContext.success()
    }
}