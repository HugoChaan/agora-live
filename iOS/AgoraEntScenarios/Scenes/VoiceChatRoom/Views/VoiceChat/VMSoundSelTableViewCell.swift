//
//  VMSoundSelTableViewCell.swift
//  AgoraScene_iOS
//
//  Created by CP on 2022/9/9.
//

import UIKit

public enum SOUND_TYPE {
    case chat
    case karaoke
    case game
    case anchor
    case none
}

class VMSoundSelTableViewCell: UITableViewCell {
    private var bgView: UIView = .init()
    private var screenWidth: CGFloat = UIScreen.main.bounds.size.width - 40
    private var typeLabel: UILabel = .init()
    private var iconView: UIImageView = .init()
    private var detailLabel: UILabel = .init()
    private var selView: UIImageView = .init()

    private var typeStr: String = ""
    private var detailStr: String = ""

    private var images = [["wangyi", "momo", "pipi", "yinyu"], ["wangyi", "jiamian", "yinyu", "paipaivoice", "wanba", "qingtian", "skr", "soul"], ["yalla-ludo", "jiamian"], ["qingmang", "cowLive", "yuwan", "weibo"]]
    private var iconImgs: [String]?
    var clickBlock: (() -> Void)?

    private var cellType: SOUND_TYPE = .chat
    private var cellHeight: CGFloat = 0

    public var isSel: Bool = false {
        didSet {
            if isSel {
                bgView.layer.borderWidth = 1
                bgView.layer.borderColor = UIColor(red: 0, green: 159 / 255.0, blue: 1, alpha: 1).cgColor
                selView.isHidden = false
                iconView.image = UIImage.sceneImage(name: "icons／Stock／listen", bundleName: "VoiceChatRoomResource")
            } else {
                bgView.layer.borderWidth = 1
                bgView.layer.borderColor = UIColor.lightGray.cgColor
                selView.isHidden = true
                iconView.image = UIImage.sceneImage(name: "icons／Stock／change", bundleName: "VoiceChatRoomResource")
            }
        }
    }

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        layoutUI()
    }

    init(style: UITableViewCell.CellStyle, reuseIdentifier: String?, cellType: SOUND_TYPE, cellHeight: CGFloat) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        self.cellHeight = cellHeight
        setCellType(with: cellType)
        layoutUI()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func layoutUI() {
        contentView.backgroundColor = .white
        selectionStyle = .none

        bgView.backgroundColor = .white
        bgView.layer.cornerRadius = 16
        bgView.layer.borderWidth = 1
        bgView.layer.borderColor = UIColor(red: 0, green: 159 / 255.0, blue: 1, alpha: 1).cgColor
        setShadow(view: bgView, sColor: .lightGray, offset: CGSize(width: 0, height: 0), opacity: 0.9, radius: 3)
        contentView.addSubview(bgView)

        typeLabel.text = typeStr
        typeLabel.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        typeLabel.textColor = UIColor(red: 0, green: 159 / 255.0, blue: 1, alpha: 1)
        bgView.addSubview(typeLabel)

        iconView.image = UIImage.sceneImage(name: "icons／Stock／listen", bundleName: "VoiceChatRoomResource")
//        let tap = UITapGestureRecognizer(target: self, action: #selector(click))
//        iconView.addGestureRecognizer(tap)
//        iconView.isUserInteractionEnabled = true
        bgView.addSubview(iconView)

        detailLabel.text = detailStr
        detailLabel.textAlignment = .left
        detailLabel.numberOfLines = 0
        detailLabel.textColor = UIColor(hex: 0x3C4267, alpha: 1)
        detailLabel.font = UIFont.systemFont(ofSize: 13)
        detailLabel.lineBreakMode = .byCharWrapping
        bgView.addSubview(detailLabel)

        selView.image = UIImage.sceneImage(name: "dan-check", bundleName: "VoiceChatRoomResource")
        addSubview(selView)

        guard let iconImgs = iconImgs else {
            return
        }
        var basetag = 0
        switch cellType {
        case .chat:
            basetag = 10
        case .karaoke:
            basetag = 20
        case .game:
            basetag = 30
        case .anchor:
            basetag = 40
        case .none:
            break
        }
        for (index, value) in iconImgs.enumerated() {
            let imgView = UIImageView()
            imgView.image = UIImage.voice_image(value)
            imgView.tag = basetag + index
            addSubview(imgView)
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        bgView.frame = CGRect(x: 20, y: 0, width: bounds.size.width - 40, height: bounds.size.height - 20)
        typeLabel.frame = CGRect(x: 20, y: 15, width: 200, height: 17)
        iconView.frame = CGRect(x: screenWidth - 30, y: 15, width: 20, height: 20)
        detailLabel.frame = CGRect(x: 20, y: 40, width: bounds.size.width - 80, height: cellHeight)
        selView.frame = CGRect(x: screenWidth - 10, y: bounds.size.height - 50, width: 30, height: 30)
    }

    func setShadow(view: UIView, sColor: UIColor, offset: CGSize,
                   opacity: Float, radius: CGFloat)
    {
        //Set Shadow Color
        view.layer.shadowColor = sColor.cgColor
        //Set Transparency
        view.layer.shadowOpacity = opacity
        //Set shadow radius
        view.layer.shadowRadius = radius
        //Set Shadow Offset
        view.layer.shadowOffset = offset
    }

//
//    @objc func click(){
//        guard let clickBlock = clickBlock else {
//            return
//        }
//        clickBlock()
//    }

    private func setCellType(with type: SOUND_TYPE) {
        cellType = type
        if type == .chat {
            typeStr = "voice_social_chat".voice_localized
            detailStr = "voice_chatroom_social_chat_introduce".voice_localized
            iconImgs = images[0]
        } else if type == .karaoke {
            typeStr = "voice_karaoke".voice_localized
            detailStr = "voice_chatroom_karaoke_introduce".voice_localized
            iconImgs = images[1]
        } else if type == .game {
            typeStr = "voice_gaming_buddy".voice_localized
            detailStr = "voice_chatroom_gaming_buddy_introduce".voice_localized
            iconImgs = images[2]
        } else if type == .anchor {
            typeStr = "voice_professional_podcaster".voice_localized
            detailStr = "voice_chatroom_professional_broadcaster_introduce".voice_localized
            iconImgs = images[3]
        }
    }
}
