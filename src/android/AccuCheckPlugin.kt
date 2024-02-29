package com.outsystems.experts.accucheck

import com.google.gson.Gson
import com.mysugr.bluecandy.glucometer.sdk.pairing.DeviceType
import com.mysugr.bluecandy.glucometer.sdk.pairing.GlucometerDeviceFilter
import org.apache.cordova.*
import org.json.JSONArray


private const val TAG = "AccuCheckPlugin"
private const val GET_DEVICES_SUPPORTED = "getDevicesSupported"
private const val START_SCAN = "startScan"
private const val STOP_SCAN = "stopScan"
private const val CONNECT_DEVICE = "connectDevice"
private const val READ_DATA = "readData"

// Errors
private const val ERROR_NO_DEVICES_SUPPORTED = "No devices supported!"

class AccuCheckPlugin : CordovaPlugin() {

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {

        if (action == GET_DEVICES_SUPPORTED) {
            this.getDevicesSupported(callbackContext)
            return true
        }
        if (action == START_SCAN) {
            this.startScan(callbackContext)
            return true
        }
        if (action == STOP_SCAN) {
            this.stopScan(callbackContext)
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

    private fun getDevicesSupported(callbackContext: CallbackContext) {
        val supportedDevices = GlucometerDeviceFilter.supportedGlucometerTypes
        if (supportedDevices.isEmpty()) {
            callbackContext.error(ERROR_NO_DEVICES_SUPPORTED)
        } else {
            sendSuccessResultArray(callbackContext, supportedDevices)
        }
    }

    private fun startScan(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun stopScan(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun connectDevice(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun readData(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun sendSuccessResultArray(callbackContext: CallbackContext, data: List<DeviceType>) {
        val jsonResult = Gson().toJson(data)
        val result = PluginResult(PluginResult.Status.OK, jsonResult)
        callbackContext.sendPluginResult(result)
    }
}