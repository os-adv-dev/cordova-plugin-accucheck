var exec = require('cordova/exec');

exports.startScan = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'startScan');
};

exports.connectDevice = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'connectDevice');
};

exports.readData = function (success, error) {
    exec(success, error, 'AccuCheckPlugin', 'readData');
};