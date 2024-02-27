import Foundation
import Cordova

private let TAG = "AccuCheckPlugin"
private let START_SCAN = "startScan"
private let CONNECT_DEVICE = "connectDevice"
private let READ_DATA = "readData"

@objc(AccuCheckPlugin) class AccuCheckPlugin: CDVPlugin {
    
    override func pluginInitialize() {
        super.pluginInitialize()
        // Initialize the BlueCandy SDK
    }
    
    @objc(execute:)
    func execute(command: CDVInvokedUrlCommand) -> Bool {
        let action = command.methodName
        
        if action == START_SCAN {
            self.startScan(command)
            return true
        }
        
        if action == CONNECT_DEVICE {
            self.connectDevice(command)
            return true
        }
        
        if action == READ_DATA {
            self.readData(command)
            return true
        }
        
        return false
    }
    
    func startScan(_ command: CDVInvokedUrlCommand) {
       
    }
    
    func connectDevice(_ command: CDVInvokedUrlCommand) {
       
    }
    
    func readData(_ command: CDVInvokedUrlCommand) {
       
    }
}
