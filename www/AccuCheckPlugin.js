var exec = require('cordova/exec');

exports.getDevicesSupported = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'getDevicesSupported');
};

exports.startScan = function (success, error, args) {
    exec(success, error, 'AccuCheckPlugin', 'startScan',[args]);
};

exports.scanDeviceListener = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'scanDeviceListener');
};

exports.deviceInfoListener = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'deviceInfoListener');
};

exports.disconnectDevice = function (success, error, address) {
    exec(success, error, 'AccuCheckPlugin', 'disconnectDevice', address);
};

exports.disconnectAllDevices = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'disconnectAllDevices');
};

exports.glucoseMeasurementsListener = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'glucoseMeasurementsListener');
};

exports.stopScan = function (success, error, args) {
    exec(success, error, 'AccuCheckPlugin', 'stopScan', [args]);
};

exports.connectDevice = function (success, error, args) {
    exec(success, error, 'AccuCheckPlugin', 'connectDevice', [args]);
};

exports.readData = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'readData');
};