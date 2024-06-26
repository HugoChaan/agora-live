//
//  CommerceMusicModel.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/18.
//

import Foundation

class CommerceMusicConfigData {
    enum DataType {
        case resource // Music resources
        case beauty  // Bel Canto
        case mixture // reverberation
    }
    
    let title: String
    let dataArray: [CommerceMusicEffectCell.CellData]
    let type: DataType
    var selectedIndex: Int? {
        didSet {
            switch type {
            case .resource:
                if (oldValue == selectedIndex) {
                    selectedIndex = nil
                }
                for (index, item) in dataArray.enumerated() {
                    item.isSelected = index == selectedIndex
                }
            case .beauty, .mixture:
                for (index, item) in dataArray.enumerated() {
                    item.isSelected = index == selectedIndex
                }
            }
        }
    }
    
    init(title: String, dataArray: [CommerceMusicEffectCell.CellData], type: DataType, selectedIndex: Int? = nil) {
        self.title = title
        self.dataArray = dataArray
        self.type = type
        self.selectedIndex = selectedIndex
    }
}

