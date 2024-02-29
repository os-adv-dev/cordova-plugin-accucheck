var exec = require('cordova/exec');

exports.getDevicesSupported = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'getDevicesSupported');
};

exports.startScan = function (success, error, args) {
    exec(success, error, 'AccuCheckPlugin', 'startScan',[args]);
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