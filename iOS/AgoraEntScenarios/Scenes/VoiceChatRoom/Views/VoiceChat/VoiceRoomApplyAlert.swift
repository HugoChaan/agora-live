//
//  VoiceRoomApplyAlert.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on September 9, 2022
//

import UIKit
import ZSwiftBaseLib

public enum VoiceRoomApplyAlertPosition: Int {
    case center, bottom
}

public class VoiceRoomApplyAlert: UIView {
    /// 30 is cancel,other is confirm
    @objc public var actionEvents: ((Int) -> Void)?

    lazy var header: VoiceRoomAlertContainer = .init(frame: CGRect(x: 0, y: 0, width: self.frame.width, height: 60))

    lazy var content: UILabel = .init(frame: CGRect(x: 20, y: 60, width: self.frame.width - 40, height: 20)).font(.systemFont(ofSize: 16, weight: .semibold)).textAlignment(.center).textColor(.darkText)

    lazy var cancel: UIButton = .init(type: .custom).frame(CGRect(x: 28, y: self.content.frame.maxY + 35, width: (self.frame.width - 78) / 2.0, height: 40)).cornerRadius(20).backgroundColor(UIColor(0xEFF4FF)).textColor(UIColor(0x756E98), .normal).title("voice_cancel".voice_localized, .normal).font(.systemFont(ofSize: 16, weight: .semibold)).tag(30).addTargetFor(self, action: #selector(buttonAction(_:)), for: .touchUpInside)

    lazy var confirm: UIButton = {
        UIButton(type: .custom).frame(CGRect(x: self.cancel.frame.maxX + 22, y: self.content.frame.maxY + 35, width: (self.frame.width - 78) / 2.0, height: 40)).cornerRadius(20).textColor(.white, .normal).title("voice_confirm".voice_localized, .normal).font(.systemFont(ofSize: 16, weight: .semibold)).setGradient([UIColor(red: 0.13, green: 0.61, blue: 1, alpha: 1), UIColor(red: 0.2, green: 0.37, blue: 1, alpha: 1)], [CGPoint(x: 0, y: 0), CGPoint(x: 0, y: 1)]).tag(31).addTargetFor(self, action: #selector(buttonAction(_:)), for: .touchUpInside)
    }()

    lazy var confirmContainer: UIView = .init(frame: self.confirm.frame).backgroundColor(.white)

    private var position: VoiceRoomApplyAlertPosition = .bottom

    override public init(frame: CGRect) {
        super.init(frame: frame)
    }

    public convenience init(frame: CGRect, content: String, cancel tips: String, confirm text: String, position: VoiceRoomApplyAlertPosition) {
        self.init(frame: frame)
        self.position = position
        if position == .bottom {
            addSubViews([header, self.content, cancel, confirmContainer, confirm])
        } else {
            addSubViews([self.content, cancel, confirmContainer, confirm])
        }
        self.content.text(content.voice_localized)
        cancel.setTitle(tips.voice_localized, for: .normal)
        confirm.setTitle(text.voice_localized, for: .normal)
        confirmContainer.layer.cornerRadius = 20
        confirmContainer.layer.shadowRadius = 8
        confirmContainer.layer.shadowOffset = CGSize(width: 0, height: 4)
        confirmContainer.layer.shadowColor = UIColor(red: 0, green: 0.55, blue: 0.98, alpha: 0.2).cgColor
        confirmContainer.layer.shadowOpacity = 0.8
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc func buttonAction(_ sender: UIButton) {
        if actionEvents != nil {
            actionEvents!(sender.tag)
        }
    }
}

public class VoiceRoomCancelAlert: UIView {
    @objc public var actionEvents: ((Int) -> Void)?

    lazy var header: VoiceRoomAlertContainer = .init(frame: CGRect(x: 0, y: 0, width: ScreenWidth, height: 60))

    lazy var cancel: UIButton = .init(type: .custom).frame(CGRect(x: 28, y: self.header.frame.maxY + 9, width: self.frame.width - 56, height: 40)).cornerRadius(20).backgroundColor(UIColor(0xEFF4FF)).textColor(UIColor(0x756E98), .normal).title("voice_cancel_request".voice_localized, .normal).font(.systemFont(ofSize: 16, weight: .semibold)).tag(30).addTargetFor(self, action: #selector(buttonAction(_:)), for: .touchUpInside)

    override public init(frame: CGRect) {
        super.init(frame: frame)
        addSubViews([header, cancel])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc func buttonAction(_ sender: UIButton) {
        if actionEvents != nil {
            actionEvents!(sender.tag)
        }
    }
}
