//
//  FUBeautyManager.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/1/12.
//

import UIKit

class FUBeautyManager: NSObject {
    
    public lazy var render = FUBeautyRender()
    
    private static var _sharedManager: FUBeautyManager?
    static var shareManager: FUBeautyManager {
        get {
            if let sharedManager = _sharedManager { return sharedManager }
            let sharedManager = FUBeautyManager()
            _sharedManager = sharedManager
            return sharedManager
        }
        set {
            _sharedManager = nil
        }
    }
    
    func setBeauty(path: String?, key: String?, value: CGFloat) {
        if path == "init" {
            render.setBeautyPreset()
        } else {
            render.setBeautyWithPath(path ?? "", key: key ?? "", value: Float(value))
        }
    }
    
    func setStyle(path: String?, key: String?, value: CGFloat, isCombined: Bool) {
        render.setStyleWithPath(path ?? "", key: key ?? "", value: Float(value), isCombined: isCombined)
    }
    func setAnimoji(path: String?) {
        render.setAnimojiWithPath(path ?? "")
    }
    
    func setFilter(path: String?, value: CGFloat) { }
    
    func setSticker(path: String?) {
        render.setStickerWithPath(path ?? "")
    }
    
    func reset(datas: [BeautyModel]) {
        render.reset()
        ShowBeautyFaceVC.beautyData.enumerated().forEach({
            $0.element.isSelected = $0.offset == 0
        })
    }
    
    func resetStyle(datas: [BeautyModel]) {
        render.resetStyle()
        ShowBeautyFaceVC.styleData.enumerated().forEach({
            $0.element.isSelected = $0.offset == 0
        })
    }
    
    func resetFilter(datas: [BeautyModel]) {
        
    }
    
    func resetSticker(datas: [BeautyModel]) {
        render.resetSticker()
        ShowBeautyFaceVC.stickerData.enumerated().forEach({
            $0.element.isSelected = $0.offset == 0
        })
    }
    
    func destroy() {
        FUBeautyManager._sharedManager = nil
        reset(datas: ShowBeautyFaceVC.beautyData)
        resetStyle(datas: ShowBeautyFaceVC.styleData)
        resetSticker(datas: ShowBeautyFaceVC.stickerData)
        ShowBeautyFaceVC.backgroundData.forEach({
            $0.isSelected = $0.path == nil
        })
    }
}
