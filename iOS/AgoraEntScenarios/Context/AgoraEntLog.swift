//
//  AgoraEntLog.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2023/1/3.
//

import Foundation
import SwiftyBeaver

@objc class AgoraEntLogConfig: NSObject {
    var sceneName: String = ""
    var logFileMaxSize: Int = (1 * 1024 * 1024)
    
    init(sceneName: String, logFileMaxSize: Int = 1 * 1024 * 1024) {
        super.init()
        self.sceneName = sceneName
        self.logFileMaxSize = logFileMaxSize
    }
}

@objc class AgoraEntLog: NSObject {
    static func createLog(config: AgoraEntLogConfig) -> SwiftyBeaver.Type {
        let log = SwiftyBeaver.self
        
        // add log destinations. at least one is needed!
        let console = ConsoleDestination()
         // log to Xcode Console
        let file = FileDestination()  // log to default swiftybeaver.log file
        let logDir = logsDir()
        file.logFileURL = URL(fileURLWithPath: "\(logDir)/agora_ent_\(config.sceneName).log")
        
        // use custom format and set console output to short time, log level & message
        console.format = "[$DMM/dd/yy HH:mm:ss.SSS$d][Agora][$L][\(config.sceneName)][$X]: $M"
        file.format = console.format
        file.logFileMaxSize = config.logFileMaxSize
        file.logFileAmount = 2
        // or use this for JSON output: console.format = "$J"

        // add the destinations to SwiftyBeaver
        #if DEBUG
        log.addDestination(console)
        #endif
        log.addDestination(file)

        return log
    }
    
    @objc static func cacheDir() ->String {
        let dir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.cachesDirectory,
                                                      FileManager.SearchPathDomainMask.userDomainMask, true).first
        return dir ?? ""
    }
    
    @objc static func logsDir() ->String {
        let dir = cacheDir()
        let logDir = "\(dir)/agora_ent_log"
        try? FileManager.default.createDirectory(at: URL(fileURLWithPath: logDir), withIntermediateDirectories: true)
        
        return logDir
    }
}
