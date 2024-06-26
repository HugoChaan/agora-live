//
//  CommerceUserJoinView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/25.
//

import UIKit

class CommerceUserJoinView: UIView {
    private lazy var nickNameLabel: UILabel = {
        let label = UILabel()
        label.text = "Paul"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 13, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var joinLabel: UILabel = {
        let label = UILabel()
        label.text = "joined 👋"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 13)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private var queue: [String] = []
    private var isShowing: Bool = false
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func joinHandler(nickName: String) {
        queue.append(nickName)
        showNextAnimation()
    }
    
    private func showNextAnimation() {
        if isShowing { return }
        guard let nickName = queue.first else { return }
        queue.removeFirst()
        showAnimation(nickName: nickName)
    }
    
    private func showAnimation(nickName: String) {
        self.isShowing = true
        nickNameLabel.text = nickName
        UIView.animate(withDuration: 0.25, delay: 0, options: [.curveEaseOut], animations: {
            self.frame.origin.x = 16
        })
        UIView.animate(withDuration: 0.25, delay: 2.0, options: [.curveEaseOut], animations: {
            self.frame.origin.x = -(self.frame.width + 16)
        }, completion: { [weak self] _ in
            self?.isShowing = false
            self?.showNextAnimation()
        })
    }
    
    private func setupUI() {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = UIColor(hex: "#7B52F2", alpha: 0.8)
        layer.cornerRadius = 12
        layer.masksToBounds = true
        heightAnchor.constraint(equalToConstant: 24).isActive = true
        
        addSubview(nickNameLabel)
        nickNameLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 8).isActive = true
        nickNameLabel.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        addSubview(joinLabel)
        joinLabel.leadingAnchor.constraint(equalTo: nickNameLabel.trailingAnchor, constant: 3).isActive = true
        joinLabel.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        joinLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -8).isActive = true
    }
}
