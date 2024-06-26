//
//  ShowSelectQualityVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import UIKit

private let cellHeight: CGFloat = 48
private let lineSpacing: CGFloat = 15

struct Commerce_Resolution {
    let width: CGFloat
    let height: CGFloat
    let valueStr: String
    let name: String
}

class CommerceSelectQualityVC: UIViewController {
    
    var selectedItem: ((_ item: Commerce_Resolution, _ index: Int)->())?
    var dismissed: (()->())?
    var defalutSelectIndex =  0
    
    private let dataArray  = [
        Commerce_Resolution(width: 1080, height: 1920, valueStr: "1080P", name: "Ultra clear"),
        Commerce_Resolution(width: 720, height: 1280, valueStr: "720P", name: "Ultra"),
        Commerce_Resolution(width: 540, height: 960, valueStr: "540P", name: "HD"),
        Commerce_Resolution(width: 480, height: 854, valueStr: "480P", name: "Standard"),
        Commerce_Resolution(width: 360, height: 640, valueStr: "360P", name: "smooth"),
        Commerce_Resolution(width: 270, height: 480, valueStr: "270P", name: "Low"),
    ]
    
    private lazy var bgView: UIView = {
        let bgView = UIView()
        bgView.backgroundColor = .commerce_dark_cover_bg
        return bgView
    }()
    
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .commerce_main_text
        label.font = .commerce_navi_title
        label.text = "create_select_quality_title".commerce_localized
        return label
    }()
    
    private lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        let marginLeft: CGFloat = 20
        let interSpacing: CGFloat = 15
        let countFowRow: CGFloat = 3
        let cellWidth: CGFloat = (Screen.width - marginLeft * 2  - (countFowRow - 1) * interSpacing) / countFowRow - 2
        layout.minimumInteritemSpacing = interSpacing
        layout.minimumLineSpacing = lineSpacing
        layout.sectionInset = UIEdgeInsets(top: 0, left: marginLeft, bottom: 0, right: marginLeft)
        layout.itemSize = CGSize(width: cellWidth, height: cellHeight)
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        collectionView.register(CommerceSelectQualityCell.self,
                                forCellWithReuseIdentifier: NSStringFromClass(CommerceSelectQualityCell.self))
        collectionView.delegate = self
        collectionView.dataSource = self
        return collectionView
    }()
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        modalPresentationStyle = .overCurrentContext
//        modalTransitionStyle = .crossDissolve
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setUpUI()
        configDefaultSelect()
    }
    
    private func setUpUI(){
        
        view.addSubview(bgView)
        bgView.snp.makeConstraints { make in
            make.left.bottom.right.equalToSuperview()
            make.height.equalTo(240)
        }
        
        bgView.addSubview(titleLabel)
        titleLabel.snp.makeConstraints { make in
            make.left.equalTo(20)
            make.top.equalTo(20)
        }
        
        bgView.addSubview(collectionView)
        collectionView.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            make.top.equalTo(65)
            make.height.equalTo(lineSpacing + 2 * cellHeight)
        }
      
    }
    
    private func configDefaultSelect(){
        CATransaction.begin()
        CATransaction.setCompletionBlock {
            let indexPath = IndexPath(item: self.defalutSelectIndex, section: 0)
            if self.collectionView.numberOfItems(inSection: 0)  > self.defalutSelectIndex {
                self.collectionView.selectItem(at: indexPath, animated: false, scrollPosition: .left)
            }
        }
        collectionView.reloadData()
        CATransaction.commit()
    }
}

extension CommerceSelectQualityVC {
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        bgView.commerce_setRoundingCorners([.topLeft, .topRight], radius: 20)
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        dismiss(animated: true)
        dismissed?()
    }
}

extension CommerceSelectQualityVC: UICollectionViewDelegateFlowLayout, UICollectionViewDataSource {
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return dataArray.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell: CommerceSelectQualityCell = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(CommerceSelectQualityCell.self),
                                                                                 for: indexPath) as! CommerceSelectQualityCell
        let model = dataArray[indexPath.item]
        cell.setValueStr(model.valueStr, name: model.name)
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let item = dataArray[indexPath.item]
        selectedItem?(item, indexPath.item)
        dismiss(animated: true)
        dismissed?()
    }
}

