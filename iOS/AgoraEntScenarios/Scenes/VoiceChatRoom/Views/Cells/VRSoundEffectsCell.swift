//
//  VRSoundEffectsCell.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 26, 2022
//

import UIKit
import ZSwiftBaseLib

public class VRSoundEffectsCell: UITableViewCell {
    var entity: VRRoomMenuBarEntity?

    private var images = [["wangyi", "momo", "pipi", "yinyu"], ["wangyi", "jiamian", "yinyu", "paipaivoice", "wanba", "qingtian", "skr", "soul"], ["yalla-ludo", "jiamian"], ["qingmang", "cowLive", "yuwan", "weibo"]]

    lazy var background: UIView = .init(frame: CGRect(x: 20, y: 15, width: ScreenWidth - 40, height: self.frame.height - 15)).backgroundColor(.white).cornerRadius(20)

    lazy var shaodw: UIView = .init(frame: CGRect(x: 32, y: 17, width: ScreenWidth - 64, height: self.frame.height - 15)).backgroundColor(.white)

    lazy var effectName: UILabel = .init(frame: CGRect(x: 20, y: 17.5, width: self.background.frame.width - 40, height: 20)).textColor(UIColor(0x156EF3)).font(.systemFont(ofSize: 16, weight: .semibold))

    lazy var effectDesc: UILabel = .init(frame: CGRect(x: 20, y: self.effectName.frame.maxY + 4, width: self.effectName.frame.width, height: 60)).font(.systemFont(ofSize: 13, weight: .regular)).textColor(UIColor(0x3C4267)).numberOfLines(0)

    lazy var chooseSymbol: UIImageView = .init(frame: CGRect(x: self.background.frame.width - 32, y: self.frame.height - 31, width: 32, height: 31)).image(UIImage.sceneImage(name: "dan-check", bundleName: "VoiceChatRoomResource")!).contentMode(.scaleAspectFit)

    override public init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        contentView.backgroundColor = .clear
        backgroundColor = .clear
        contentView.addSubview(shaodw)
        contentView.addSubview(background)
        shaodw.layer.shadowRadius = 8
        shaodw.layer.shadowOffset = CGSize(width: 0, height: 2)
        shaodw.layer.shadowColor = UIColor(red: 0.04, green: 0.1, blue: 0.16, alpha: 0.12).cgColor
        shaodw.layer.shadowOpacity = 1
        background.addSubViews([effectName, effectDesc, chooseSymbol])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension VRSoundEffectsCell {
    static func items() -> [VRRoomMenuBarEntity] {
        var items = [VRRoomMenuBarEntity]()
        do {
            for dic in [["title": "voice_social_chat".voice_localized, "detail": "voice_chatroom_social_chat_introduce".voice_localized, "selected": true, "index": 0, "soundType": 1], ["title": "voice_karaoke".voice_localized, "detail": "voice_chatroom_karaoke_introduce".voice_localized, "selected": false, "index": 1, "soundType": 2], ["title": "voice_gaming_buddy".voice_localized, "detail": "voice_chatroom_gaming_buddy_introduce".voice_localized, "selected": false, "index": 2, "soundType": 3], ["title": "voice_professional_podcaster".voice_localized, "detail": "voice_chatroom_professional_broadcaster_introduce".voice_localized, "selected": false, "index": 3, "soundType": 4]] {
                let data = try JSONSerialization.data(withJSONObject: dic, options: [])
                let item = try JSONDecoder().decode(VRRoomMenuBarEntity.self, from: data)
                items.append(item)
            }
        } catch {
            assertionFailure("\(error.localizedDescription)")
        }
        return items
    }

    func refresh(item: VRRoomMenuBarEntity) {
        entity = item
        effectName.text = item.title
        effectDesc.text = item.detail
        chooseSymbol.isHidden = !item.selected
        if item.selected {
            background.layerProperties(UIColor(0x009FFF), 1)
        } else {
            background.layerProperties(.clear, 1)
        }
        background.frame = CGRect(x: 20, y: 15, width: contentView.frame.width - 40, height: contentView.frame.height - 15)
        shaodw.frame = CGRect(x: 35, y: 15, width: contentView.frame.width - 70, height: frame.height - 16)
        effectName.frame = CGRect(x: 20, y: 15, width: background.frame.width - 40, height: 22)
        effectDesc.frame = CGRect(x: 20, y: effectName.frame.maxY + 4, width: effectName.frame.width, height: VRSoundEffectsList.heightMap[item.title] ?? 60)
        chooseSymbol.frame = CGRect(x: background.frame.width - 32, y: background.frame.height - 31, width: 32, height: 31)
    }
}

public class VRIconCell: UICollectionViewCell {
    lazy var imageView: UIImageView = .init(frame: CGRect(x: 0, y: 0, width: self.frame.width, height: self.frame.height)).contentMode(.scaleAspectFill)

    override public init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(imageView)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

//    public override func layoutSubviews() {
//        super.layoutSubviews()
//        self.imageView.frame = CGRect(x: 0, y: 0, width: self.frame.width, height: self.frame.height)
//    }
}
