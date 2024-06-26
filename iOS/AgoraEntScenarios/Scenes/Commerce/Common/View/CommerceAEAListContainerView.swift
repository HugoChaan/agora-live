//
//  AEAListContainerView.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/18.
//

import Foundation

protocol CommerceAEAListContainerViewDataSource: AnyObject {
    func listContainerView(_ listContainerView: CommerceAEAListContainerView,
                           viewControllerForIndex index: Int) -> UIViewController?
}

class CommerceAEAListContainerView: UIView {
    
    weak var currentVC: UIViewController?
    weak var dataSource: CommerceAEAListContainerViewDataSource?
    
    private var childVCDic = [Int: UIViewController]()
    
    func allLoadedViewControllers() -> [UIViewController] {
        return Array(childVCDic.values)
    }
    
    func viewController(atIndex index: Int) -> UIViewController? {
        return childVCDic[index]
    }
    
    func setSelectedIndex(_ index: Int) {
        
        var vc = viewController(atIndex: index)
        if vc == nil {
            if let dataSource = dataSource, let viewController = dataSource.listContainerView(self, viewControllerForIndex: index) {
                vc = viewController
            }
            assert(vc != nil, "listContainerView:viewControllerForIndex:method index = \(index) The controller returned nil")
            childVCDic[index] = vc
        }
        
        if currentVC === vc {
            return
        }
        
        addChildVC(vc ?? UIViewController())
        if let currentVC = currentVC {
            removeChildVC(currentVC)
        }
        currentVC = vc
    }
    
    private func addChildVC(_ vc: UIViewController) {
        if let containerVC = dataSource as? UIViewController {
            containerVC.addChild(vc)
            addSubview(vc.view)
            vc.view.frame = bounds
            containerVC.didMove(toParent: vc)
        } else {
            addSubview(vc.view)
            vc.view.frame = bounds
        }
    }
    
    private func removeChildVC(_ vc: UIViewController) {
        vc.willMove(toParent: nil)
        vc.view.removeFromSuperview()
        vc.removeFromParent()
    }
}
