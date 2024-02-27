package com.outsystems.experts.samsungpay

import android.os.Build
import android.os.Bundle
import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject


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
            return true
        }

        if (action == CONNECT_DEVICE) {
            return true
        }

        if (action == READ_DATA) {
            return true
        }

        return false
    }

}