//
//  LanguageManager.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on September 1, 2022
//

import Foundation
import ZSwiftBaseLib

public enum LanguageType: String {
    case Chinese = "zh-Hans", Chinese_traditional = "zh-Hant", English = "en", Russian = "ru", German = "de", French = "fr", Japanese = "ja", Korean = "ko", Auto = "auto"
}

public final class LanguageManager: NSObject {
    @UserDefault("AvatarAppLanguage", defaultValue: "") var lanuage

    static func localValue(key: String) -> String {
        LanguageManager.shared.localValue(key)
    }

    static func setLanguage(type: LanguageType) {
        LanguageManager.shared.setLanguage(type)
    }

    static let shared = LanguageManager()

    override private init() {}

    lazy var bundle: Bundle = {
        guard let bundlePath = Bundle.main.path(forResource: "VoiceChatRoomResource", ofType: "bundle"), let bundle = Bundle(path: bundlePath) else {
            assertionFailure("vrcm bundle == nil")
            return Bundle.main
        }
        guard var lang = NSLocale.preferredLanguages.first else { return Bundle.main }
        if lang.contains("zh") {
            lang = "zh-Hans"
        } else {
            lang = "en"
        }
        let path = bundle.path(forResource: lang, ofType: "lproj") ?? ""
        return Bundle(path: path) ?? .main
    }()

    var currentLocal: Locale {
        Locale.current
    }

    private func localValue(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    private func setLanguage(_ type: LanguageType) {
        lanuage = type.rawValue
        //Return the path to the en.lproj file in the project
        let path = bundle.path(forResource: type.rawValue, ofType: "lproj")
        bundle = Bundle(path: path!)!
        if type == .Auto {
            bundle = Bundle.main
        }
    }
}
