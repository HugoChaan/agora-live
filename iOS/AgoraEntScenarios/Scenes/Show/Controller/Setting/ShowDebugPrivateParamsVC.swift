//
//  ShowDebugPrivateParamsVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2023/5/25.
//

import UIKit

class ShowDebugPrivateParamsVC: UIViewController {
    
    var settingManager: ShowAgoraKitManager?
    // Customize navigation bar
    private let naviBar = ShowNavigationBar()
    
    private let textView = UITextView()

    override func viewDidLoad() {
        super.viewDidLoad()
        configCustomNaviBar()
        setupUI()
    }
    
    private func setupUI(){
        view.backgroundColor = .white
        view.addSubview(textView)
        textView.snp.makeConstraints { make in
            make.top.equalTo(100)
            make.left.equalTo(20)
            make.right.equalTo(-20)
            make.bottom.equalTo(-100)
        }
        textView.font = UIFont.systemFont(ofSize: 15)
        textView.keyboardType = .asciiCapable
        textView.becomeFirstResponder()
//        textView.text = settingManager?.privateParams
    }
    
    private func configCustomNaviBar(){
        // Title
        naviBar.title = "Set up private ginseng"
        naviBar.backgroundColor = .white
        view.addSubview(naviBar)
        naviBar.setLeftButtonTarget(self, action: #selector(didClickCanelButton))
        let saveButtonItem = ShowBarButtonItem(title: "show_advanced_setting_private_params_save".show_localized, target: self, action: #selector(didClickSaveButton))
        naviBar.rightItems = [saveButtonItem]
    }
    
    @objc private func didClickSaveButton() {
        let text = textView.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard text.count > 0, let agoraKit = settingManager?.agoraKit else {
            ToastView.show(text: "Can't be empty")
            return
        }
//        settingManager?.privateParams = text
        let ret = agoraKit.setParameters(text)
        if ret != 0 {
            ToastView.show(text: "error: \(ret)")
            return
        }
        self.dismiss(animated: true)
    }
    
    @objc private func didClickCanelButton(){
        self.dismiss(animated: true)
    }
    
    
    

}
