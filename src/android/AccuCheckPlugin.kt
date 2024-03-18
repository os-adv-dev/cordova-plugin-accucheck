package com.outsystems.experts.accucheck

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.mysugr.bluecandy.api.BluetoothDevice
import com.mysugr.bluecandy.api.BluetoothDeviceInfo
import com.mysugr.bluecandy.api.BluetoothException
import com.mysugr.bluecandy.api.BluetoothOffException
import com.mysugr.bluecandy.api.gatt.dataconverters.DateTime
import com.mysugr.bluecandy.glucometer.sdk.InitializationManager
import com.mysugr.bluecandy.glucometer.sdk.connection.DeviceConnectionController
import com.mysugr.bluecandy.glucometer.sdk.connection.GlucometerController
import com.mysugr.bluecandy.glucometer.sdk.connection.GlucoseConcentrationMeasurement
import com.mysugr.bluecandy.glucometer.sdk.connection.OnGlucometerControllerCreated
import com.mysugr.bluecandy.glucometer.sdk.extensions.asDeviceInfo
import com.mysugr.bluecandy.glucometer.sdk.pairing.DeviceType
import com.mysugr.bluecandy.glucometer.sdk.pairing.GlucometerDeviceFilter
import com.mysugr.bluecandy.glucometer.sdk.pairing.LeScanningManager
import com.mysugr.bluecandy.glucometer.sdk.pairing.ScanResultItem
import com.mysugr.bluecandy.service.glucose.glucosemeassurement.GlucoseMeasurementContext
import com.mysugr.bluecandy.service.glucose.glucosemeassurement.SensorStatus
import com.mysugr.measurement.glucose.GlucoseConcentrationUnit
import com.mysugr.measurement.glucose.MgDLValue
import org.apache.cordova.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AccuCheckPlugin"
private const val GET_DEVICES_SUPPORTED = "getDevicesSupported"
private const val START_SCAN = "startScan"
private const val SCAN_DEVICE_LISTENER = "scanDeviceListener"
private const val DEVICE_INFO_LISTENER = "deviceInfoListener"
private const val GLUCOSE_MEASUREMENTS_LISTENER = "glucoseMeasurementsListener"
private const val STOP_SCAN = "stopScan"
private const val CONNECT_DEVICE = "connectDevice"
private const val STOP_CONNECT_DEVICE = "stopConnectDevice"
private const val READ_DATA = "readData"
private const val DISCONNECT_DEVICES = "disconnectDevice"
private const val DISCONNECT_ALL_DEVICES = "disconnectAllDevices"

// Errors
private const val ERROR_NO_DEVICES_SUPPORTED = "No devices supported!"
private const val TURN_ON_BLUETOOTH_ERROR = "Please turn on bluetooth"
private const val ERROR_TO_CONNECT_INVALID_PARAMETERS =
    "Error to connect to device, invalid parameters"
private const val ERROR_PERMISSIONS = "Give the permissions manually in app settings"
private const val ERROR_TO_CONNECT_NOT_FOUND =
    "This device is not found near you to connect, try again"

data class MeasurementData(
    val glucoseReadingValue: String?,
    val glucoseReadingValueUnit: String?,
    val glucoseSequenceNumber: String?,
    val glucoseReadingDate: String,
    val glucoseSampleLocation : String?,
    val glucoseReadingBoundary: String?,
    val glucoseReadingMeal: String?
)

data class Info(
    val modelNumber: String?,
    val firmwareRevision: String?,
    val serialNumber: String?,
    val manufacturerName: String?,
    val manufacturerId: String?
)

class AccuCheckPlugin : CordovaPlugin(), OnGlucometerControllerCreated {

    private var genericLeScanner: LeScanningManager? = null
    private lateinit var scanDeviceCallbackContext: CallbackContext
    private lateinit var startScanCallbackContext: CallbackContext
    private lateinit var connectDeviceCallback: CallbackContext
    private lateinit var args: JSONArray
    private val scanResults = mutableListOf<ScanResultItem>()

    private var glucometerController: GlucometerController? = null
    private var deviceConnectionController: DeviceConnectionController? = null

    // Listeners to Device Information
    private lateinit var deviceInformationCallback: CallbackContext
    private lateinit var glucoseMeasurementsCallback: CallbackContext

    private var dialog: AlertDialog? = null

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)
        if (cordova?.context != null) {
            InitializationManager(cordova.context)
        }
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
            this.connectDeviceCallback = callbackContext
            this.connectDevice(args)
            return true
        }
        if (action == STOP_CONNECT_DEVICE) {
            this.stopConnectDevice(callbackContext)
            return true
        }
        if (action == DEVICE_INFO_LISTENER) {
            this.deviceInformationCallback = callbackContext
            return true
        }
        if (action == GLUCOSE_MEASUREMENTS_LISTENER) {
            this.glucoseMeasurementsCallback = callbackContext
            return true
        }
        if (action == READ_DATA) {
            this.readData(callbackContext)
            return true
        }

        if (action == DISCONNECT_DEVICES) {
            this.disconnectDevices(callbackContext, args)
            return true
        }

        if (action == DISCONNECT_ALL_DEVICES) {
            this.disconnectAllDevices(callbackContext)
            return true
        }

        return false
    }

    private fun stopConnectDevice(callbackContext: CallbackContext) {
        try {
            deviceConnectionController?.disconnectAll()
            genericLeScanner?.stop()
            deviceConnectionController = null
            glucometerController = null
            callbackContext.success()
        } catch (ex: Exception) {
            callbackContext.error(ex.message)
        }
    }

    private fun disconnectDevices(callbackContext: CallbackContext, args: JSONArray) {
        try {
            val address = args.getString(0)
            if (address != null) {
                deviceConnectionController?.disconnect(address)
                genericLeScanner?.stop()
                deviceConnectionController = null
            }
            callbackContext.success()
        } catch (ex: Exception) {
            callbackContext.error(ex.message)
        }
    }

    private fun disconnectAllDevices(callbackContext: CallbackContext) {
        try {
            deviceConnectionController?.disconnectAll()
            genericLeScanner?.stop()
            deviceConnectionController = null
            callbackContext.success()
        } catch (ex: Exception) {
            callbackContext.error(ex.message)
        }
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
        genericLeScanner = LeScanningManager()
        genericLeScanner?.let { scanner ->
            scanner.onScanError = {
                sendErrorToScanDevice(it.message.toString())
            }

            scanner.onScanStarted = {
                scanStarted()
            }
            scanner.onScanStopped = {
                scanStopped()
            }
            scanner.onDeviceFound = { scanResultItem ->
                val result = JSONObject().apply {
                    put("scanMessage", "Devices found")
                    put("deviceName", scanResultItem.device.name)
                    put("deviceAddress", scanResultItem.device.address)
                }

                if (!scanResults.contains(scanResultItem)) {
                    scanResults.add(scanResultItem)
                    val pluginResult = PluginResult(PluginResult.Status.OK, result)
                    pluginResult.keepCallback = true
                    this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
                }
            }
        }
    }

    private fun createAndShowDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Accu-Check Status")
        builder.setMessage("Loading...")
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        dialog = builder.create()
        dialog?.show()
    }

    private fun dismissDialog() {
        dialog?.dismiss()
    }

    private fun startScan(args: JSONArray) {
        scanDeviceListener()
        try {
            val name = (args.get(0) as JSONObject).get("name") as String
            val id = (args.get(0) as JSONObject).get("id") as String
            val bluetoothId = (args.get(0) as JSONObject).get("bluetoothId") as String
            val deviceType = DeviceType(name = name, id = id, bluetoothId = bluetoothId)

            genericLeScanner?.start(deviceType)

            this.startScanCallbackContext.success()
        } catch (exception: BluetoothOffException) {
            this.startScanCallbackContext.error(TURN_ON_BLUETOOTH_ERROR)
        } catch (exception: BluetoothException) {
            this.startScanCallbackContext.error(exception.message)
        }
    }

    private fun hasAllPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        for (permission in requiredPermissions) {
            if (cordova.hasPermission(permission).not()) {
                return false
            }
        }
        return true
    }

    private fun requestPermissionsStartScan() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        cordova.requestPermissions(this, REQUEST_PERMISSIONS, permissionsToRequest)
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
                    this.startScanCallbackContext.error(ERROR_PERMISSIONS)
                } else {
                    this.startScan(this.args)
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            deviceConnectionController?.disconnectAll()
            genericLeScanner?.stop()
            deviceConnectionController = null
            genericLeScanner = null
        } catch (ex: Exception) {
            Log.v(TAG, ex.message.toString())
        }
        super.onDestroy()
    }

    private fun sendErrorToScanDevice(error: String) {
        val result = JSONObject().apply {
            put("scanMessage", error)
            put("deviceName", "")
            put("deviceAddress", "")
        }

        val pluginResult = PluginResult(PluginResult.Status.ERROR, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun scanStarted() {
        val result = JSONObject().apply {
            put("scanMessage", "Scan Started!")
            put("deviceName", "")
            put("deviceAddress", "")
        }

        val pluginResult = PluginResult(PluginResult.Status.OK, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun scanStopped() {
        val result = JSONObject().apply {
            put("scanMessage", "Scan Stopped!")
            put("deviceName", "")
            put("deviceAddress", "")
        }

        val pluginResult = PluginResult(PluginResult.Status.OK, result)
        pluginResult.keepCallback = true
        this.scanDeviceCallbackContext.sendPluginResult(pluginResult)
    }

    private fun stopScan(callbackContext: CallbackContext) {
        if (genericLeScanner != null) {
            genericLeScanner?.stop()
            deviceConnectionController = null
            scanResults.clear()
        }
        callbackContext.success()
    }

    private fun connectDevice(args: JSONArray) {
        val name = (args.get(0) as JSONObject).get("name") as String?
        val address = (args.get(0) as JSONObject).get("address") as String?
        if (name == null || address == null) {
            this.connectDeviceCallback.error(ERROR_TO_CONNECT_INVALID_PARAMETERS)
        } else {
            val deviceToConnect =
                scanResults.firstOrNull { it.device.address == address && it.device.address == address }
            if (deviceToConnect == null) {
                this.connectDeviceCallback.error(ERROR_TO_CONNECT_NOT_FOUND)
            } else {
                deviceToConnect.pairLe(
                    onPairingFailed = {
                        this.connectDeviceCallback.error(it.message)
                    },
                    onPairingSuccessful = { scanResultItem ->
                        val data = scanResultItem.device.asSerializableDevice()
                        registerDeviceConnectCallback(data)
                    }
                )
            }
        }
    }

    private fun registerDeviceConnectCallback(data: SerializableDevice) {
        deviceConnectionController = DeviceConnectionController(this)
        deviceConnectionController?.connect(data.deviceInfo)
        deviceConnectionController?.let { controller ->
            controller.onError = { errorMessage ->
                this.connectDeviceCallback.error(errorMessage)
            }
        }
    }

    private fun readData(callbackContext: CallbackContext) {
        callbackContext.success()
    }

    private fun <T> sendSuccessResultArray(callbackContext: CallbackContext, data: List<T>) {
        val jsonResult = Gson().toJson(data)
        val result = PluginResult(PluginResult.Status.OK, jsonResult)
        result.keepCallback = true
        callbackContext.sendPluginResult(result)
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1001
    }

    data class SerializableDevice(val name: String, val deviceInfo: BluetoothDeviceInfo) :
        Serializable {
        companion object {
            const val serialVersionUID: Long = 1L
        }
    }

    private fun BluetoothDevice.asSerializableDevice(): SerializableDevice =
        SerializableDevice(name, asDeviceInfo())


    override fun invoke(glucometer: GlucometerController) {
        glucometerController = glucometer
        cordova.activity.runOnUiThread {
            createAndShowDialog(cordova.activity)
        }

        glucometer.onGlucometerReady = {
            readDeviceInformation(glucometer)
            readGlucoseMeasurements(glucometer)
        }
        glucometer.onGlucometerPaused = {
            dismissDialog()
        }
    }

    private fun showPopup(message: String) {
        cordova.activity.runOnUiThread {
            Toast.makeText(cordova.context,  message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readGlucoseMeasurements(glucometer: GlucometerController) {
        glucometer.readGlucoseMeasurements(
            GlucometerController.ImportAction.All,
            onSyncSuccess = { measurementList ->
                if (this::glucoseMeasurementsCallback.isInitialized) {
                    if (measurementList.isEmpty()) {
                        sendSuccessResultArray<MeasurementData>(this.glucoseMeasurementsCallback, listOf())
                    } else {
                        val glucoseMeasurements = createMeasurementFormatted(measurementList)
                        sendSuccessResultArray(this.glucoseMeasurementsCallback, glucoseMeasurements)
                    }
                }
            },
            onSyncFailed = { error ->
                showPopup(error.message ?: "An error occurred during sync")
                val result = JSONObject().apply { put("message", error.message ?: "An error occurred during sync") }
                val pluginResult = PluginResult(PluginResult.Status.ERROR, result)
                pluginResult.keepCallback = true
                if (this::glucoseMeasurementsCallback.isInitialized) {
                    this.glucoseMeasurementsCallback.sendPluginResult(pluginResult)
                }
            }
        )
    }

    private fun readDeviceInformation(glucometerController: GlucometerController) {
        glucometerController.readDeviceInfo(
            { deviceInfo ->
                val data = JSONObject().apply {
                    put("modelNumber",deviceInfo.modelNumber)
                    put("firmwareRevision",deviceInfo.firmwareRevision)
                    put("serialNumber",deviceInfo.serialNumber)
                    put("manufacturerName",deviceInfo.manufacturerName)
                    put("manufacturerId", "${deviceInfo.reversedSystemId?.manufacturerId}")
                }
                Log.v(TAG, "----- readDeviceInfo $deviceInfo")
                val pluginResult = PluginResult(PluginResult.Status.OK, data)
                pluginResult.keepCallback = true
                if (this::deviceInformationCallback.isInitialized) {
                    this.deviceInformationCallback.sendPluginResult(pluginResult)
                }
            },
            { error ->
                val result = JSONObject().apply {
                    put("message", error.message.toString())
                    put("isLoading", false)
                }
                val pluginResult = PluginResult(PluginResult.Status.ERROR, result)
                pluginResult.keepCallback = true

                if (this::deviceInformationCallback.isInitialized) {
                    this.deviceInformationCallback.sendPluginResult(pluginResult)
                }
            }
        )
    }

    private fun createMeasurementFormatted(data: List<GlucoseConcentrationMeasurement>): List<MeasurementData> {
        val list = arrayListOf<MeasurementData>()
        data.forEach { glucose ->
            val measurementInMgDL = glucose.value?.toMgDL()
            val glucoseReadingValue = measurementInMgDL?.formattedToString()
            val glucoseReadingValueUnit = measurementInMgDL?.toGlucoseConcentration()?.unit?.formattedToString()
            val glucoseSequenceNumber = glucose.sequenceNumber
            val glucoseReadingDate = glucose.getMeasurementDate().formattedString()
            val glucoseSampleLocation = glucose.sampleLocation?.name?.getUserFriendlyString() ?: "N/A"
            val glucoseReadingBoundary = glucose.sensorStatusAnnunciation?.let {
                when {
                    it.contains(SensorStatus.SENSOR_RESULT_TOO_HIGH) -> "Too high!⚠️"
                    it.contains(SensorStatus.SENSOR_RESULT_TOO_LOW) -> "Too low!⚠️"
                    else -> ""
                }
            }
            val glucoseReadingMeal = "${glucose.context?.meal?.getIconString()} ${glucose.context?.meal?.name?.getUserFriendlyString()}"
            val model = MeasurementData(
                glucoseReadingValue = glucoseReadingValue,
                glucoseReadingValueUnit = glucoseReadingValueUnit,
                glucoseSequenceNumber = "$glucoseSequenceNumber",
                glucoseReadingDate = glucoseReadingDate,
                glucoseSampleLocation = glucoseSampleLocation,
                glucoseReadingBoundary = glucoseReadingBoundary,
                glucoseReadingMeal = glucoseReadingMeal,
            )
            list.add(model)
        }
        return list
    }

    @SuppressLint("SimpleDateFormat")
    fun Date.formattedString(): String = SimpleDateFormat("dd MMM yyyy HH:mm:ss")
        .format(this) ?: ""

    private fun GlucoseMeasurementContext.Meal.getIconString() = when (this) {
        GlucoseMeasurementContext.Meal.RESERVED -> ""
        GlucoseMeasurementContext.Meal.BEFORE_MEAL -> "⬅️"
        GlucoseMeasurementContext.Meal.AFTER_MEAL -> "➡️"
        GlucoseMeasurementContext.Meal.FASTING -> "🚫"
        GlucoseMeasurementContext.Meal.SNACK -> "🥜"
        GlucoseMeasurementContext.Meal.BEDTIME -> "🛌"
    }

    private fun String.getUserFriendlyString() = split("_").joinToString(" ") {
        it.toLowerCase().capitalize()
    }

    private fun GlucoseConcentrationMeasurement.getMeasurementDate(): Date {
        val calendar = GregorianCalendar.getInstance()
        calendar.time = baseTime.toLocalTimeZoneDate() ?: Calendar.getInstance().time
        val timeOffset = timeOffset?.toInt() ?: return calendar.time
        calendar.add(Calendar.MINUTE, timeOffset)
        return calendar.time
    }

    /**
     * Converts [DateTime] to [Date]. [DateTime] is assumed to be in the local timezone of the phone.
     */
    private fun DateTime.toLocalTimeZoneDate(): Date? {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .parse("$year-$month-$day $hours:$minutes:$seconds")
    }

    @Suppress("MagicNumber")
    fun MgDLValue?.formattedToString(): String {
        if (this == null) return "N/A"
        return when (value) {
            Double.POSITIVE_INFINITY -> "Infinity"
            Double.NEGATIVE_INFINITY -> "-Infinity"
            else -> {
                val roundedValue = BigDecimal.valueOf(value)
                    .setScale(4, BigDecimal.ROUND_HALF_DOWN)
                    .toFloat()
                "$roundedValue"
            }
        }
    }

    private fun GlucoseConcentrationUnit.formattedToString(): String {
        return when (this) {
            GlucoseConcentrationUnit.MG_DL -> "mg/dL"
            GlucoseConcentrationUnit.MMOL_L -> "mmol/L"
        }
    }
}