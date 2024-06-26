//
//  SoundCardEffectCell.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/10/12.
//

import UIKit

class VRSoundCardEffectCell: UITableViewCell {

    var titleLabel: UILabel!
    var detailLabel: UILabel!
    var imgView: UIImageView!
    var backImgView: UIImageView!
    var checkImgView: UIImageView!
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        layoutUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public func setIsSelected(_ enable: Bool) {
        backImgView.layer.borderColor = UIColor(hexString: "#0A7AFF")?.cgColor
        backImgView.layer.borderWidth = enable ? 2 : 0
        checkImgView.isHidden = enable ? false : true
    }
    
    private func layoutUI() {
        
        backImgView = UIImageView()
        backImgView.image = UIImage.sceneImage(name: "effect_bg")
        self.contentView.addSubview(backImgView)
        backImgView.layer.cornerRadius = 5
        backImgView.layer.masksToBounds = true
        
        titleLabel = UILabel()
        titleLabel.text = "voice_sexy_oba".voice_localized
        titleLabel.font = UIFont.systemFont(ofSize: 13)
        backImgView.addSubview(titleLabel)
        
        detailLabel = UILabel()
        detailLabel.text = "voice_pleasing_magnetism".voice_localized
        detailLabel.font = UIFont.systemFont(ofSize: 13)
        detailLabel.textColor = .lightGray
        backImgView.addSubview(detailLabel)
        
        imgView = UIImageView()
        backImgView.addSubview(imgView)
        
        checkImgView = UIImageView()
        checkImgView.image = UIImage.sceneImage(name: "radio-on")
        backImgView.addSubview(checkImgView)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        backImgView.frame = CGRect(x: 10, y: 5, width: self.bounds.width - 20, height: 60)
        imgView.frame = CGRect(x: 20, y: 10, width: 40, height: 40)
        imgView.layer.cornerRadius = 20
        imgView.layer.masksToBounds = true
        titleLabel.frame = CGRect(x: 90, y: 10, width: self.width - 90 - 62, height: 20)
        detailLabel.frame = CGRect(x: 90, y: 35, width: self.width - 90 - 62, height: 20)
        checkImgView.frame = CGRect(x:self.bounds.width - 62, y: 19, width: 22, height: 22)
    }

}
