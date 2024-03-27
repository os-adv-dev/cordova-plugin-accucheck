import Foundation

private let TAG = "AccuCheckPlugin"
private let START_SCAN = "startScan"
private let CONNECT_DEVICE = "connectDevice"
private let READ_DATA = "readData"

@objc(AccuCheckPlugin)
class AccuCheckPlugin: CDVPlugin {
    
    override func pluginInitialize() {
        super.pluginInitialize()
        // Initialize the BlueCandy SDK
        print("---- ✅ ---- pluginInitialize ---- ✅ ----")
    }
    
    @objc(startScan:)
    func startScan(_ command: CDVInvokedUrlCommand) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Scan OK")
        print("---- ✅ ---- startScan ---- ✅ ----")
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(connectDevice:)
    func connectDevice(_ command: CDVInvokedUrlCommand) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Connect Device OK")
        print("---- ✅ ---- connectDevice ---- ✅ ----")
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(readData:)
    func readData(_ command: CDVInvokedUrlCommand) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Read Data OK")
        print("---- ✅ ---- readData ---- ✅ ----")
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
}