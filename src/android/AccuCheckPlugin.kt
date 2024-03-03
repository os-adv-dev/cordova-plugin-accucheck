package com.outsystems.experts.accucheck

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.mysugr.bluecandy.api.BluetoothException
import com.mysugr.bluecandy.api.BluetoothOffException
import com.mysugr.bluecandy.glucometer.sdk.InitializationManager
import com.mysugr.bluecandy.glucometer.sdk.pairing.DeviceType
import com.mysugr.bluecandy.glucometer.sdk.pairing.GlucometerDeviceFilter
import com.mysugr.bluecandy.glucometer.sdk.pairing.LeScanningManager
import com.mysugr.bluecandy.glucometer.sdk.pairing.ScanResultItem
import org.apache.cordova.*
import org.json.JSONArray
import org.json.JSONObject


private const val TAG = "AccuCheckPlugin"
private const val GET_DEVICES_SUPPORTED = "getDevicesSupported"
private const val START_SCAN = "startScan"
private const val SCAN_DEVICE_LISTENER = "scanDeviceListener"
private const val STOP_SCAN = "stopScan"
private const val CONNECT_DEVICE = "connectDevice"
private const val READ_DATA = "readData"

// Errors
private const val ERROR_NO_DEVICES_SUPPORTED = "No devices supported!"
private const val TURN_ON_BLUETOOTH_ERROR = "Please turn on bluetooth"

class AccuCheckPlugin : CordovaPlugin() {

    private var genericLeScanner: LeScanningManager? = null
    private lateinit var scanDeviceCallbackContext: CallbackContext
    private lateinit var startScanCallbackContext: CallbackContext
    private lateinit var args: JSONArray
    private val scanResults = mutableListOf<ScanResultItem>()

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)
        if (cordova?.context != null) {
            InitializationManager(cordova.context)
        }

        genericLeScanner = LeScanningManager()
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        this.args = args
        if (action == GET_DEVICES_SUPPORTED) {
            this.getDevicesSupported(callbackContext)
            return true
        }
        if (action == START_SCAN) {
            this.startScanCallbackContext = callbackContext
            if (hasAllPermissions()) {
                this.startScan(args)
            } else {
                requestPermissionsStartScan()
            }
            return true
        }
        if (action == SCAN_DEVICE_LISTENER) {
            this.scanDeviceCallbackContext = callbackContext
            this.scanDeviceListener()
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

    private fun scanDeviceListener() {
        genericLeScanner?.let { scanner ->
            scanner.onScanError = {
                Log.v(TAG, "❌ >>>> onScanError >>> ${it.message} ")
                sendErrorToScanDevice(it.message.toString())
            }

            scanner.onScanStarted = {
                Log.v(TAG, "✅ >>>> onScanStarted >>> ")
                scanStarted()
            }

            scanner.onScanStopped = {
                Log.v(TAG, "✅ >>>> onScanStopped >>> ")
                scanStopped()
            }
            scanner.onDeviceFound = { scanResultItem ->
                val result = JSONObject().apply {
                    put("deviceName", scanResultItem.device.name)
                    put("deviceAddress", scanResultItem.device.address)
                }

                if (!scanResults.contains(scanResultItem)) {
                    scanResults.add(scanResultItem)

                    Log.v(TAG, ">>>> ✅ >>> deviceName : ${scanResultItem.device.name}")
                    Log.v(TAG, ">>>> ✅ >>> deviceName : ${scanResultItem.device.address}")

                    val pluginResult = PluginResult(PluginResult.Status.OK, result)
                    pluginResult.keepCallback = true
                    this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
                }
            }
        }
    }

    private fun startScan(args: JSONArray) {
        try {
            val name = (args.get(0) as JSONObject).get("name") as String
            val id = (args.get(0) as JSONObject).get("id") as String
            val bluetoothId = (args.get(0) as JSONObject).get("bluetoothId") as String
            val deviceType = DeviceType(name = name, id = id, bluetoothId = bluetoothId)

            genericLeScanner?.start(deviceType)

            this.startScanCallbackContext.success()
        } catch (exception: BluetoothOffException) {
            Toast.makeText(cordova.context, TURN_ON_BLUETOOTH_ERROR, Toast.LENGTH_LONG).show()
            this.startScanCallbackContext.error(TURN_ON_BLUETOOTH_ERROR)
        } catch (exception: BluetoothException) {
            Toast.makeText(cordova.context, exception.message, Toast.LENGTH_LONG).show()
            this.startScanCallbackContext.error(exception.message)
        }
    }

    private fun hasAllPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (cordova.hasPermission(permission).not()) {
                return false
            }
        }
        return true
    }

    private fun requestPermissionsStartScan() {
        cordova.requestPermissions(this, REQUEST_PERMISSIONS, REQUIRED_PERMISSIONS)
    }

    override fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                var allPermissionsGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false
                        break
                    }
                }

                if (!allPermissionsGranted) {
                    Toast.makeText(
                        cordova.context,
                        "Give the permissions manually in app settings",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    this.startScan(this.args)
                }
            }
        }
    }

    private fun sendErrorToScanDevice(error: String) {
        val result = JSONObject().apply {
            put("scanMessage", error)
        }

        val pluginResult = PluginResult(PluginResult.Status.ERROR, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun scanStarted() {
        val result = JSONObject().apply {
            put("scanMessage", "Scan Started!")
        }

        val pluginResult = PluginResult(PluginResult.Status.OK, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun scanStopped() {
        val result = JSONObject().apply {
            put("scanMessage", "Scan Stopped!")
        }

        val pluginResult = PluginResult(PluginResult.Status.OK, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun stopScan(callbackContext: CallbackContext) {
        if (genericLeScanner != null) {
            genericLeScanner?.stop()
        }
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

    companion object {
        private const val REQUEST_PERMISSIONS = 1001

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }
}