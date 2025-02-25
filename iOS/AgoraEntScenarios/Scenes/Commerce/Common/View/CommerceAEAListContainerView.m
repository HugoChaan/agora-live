//
//  AEAListContainerView.m
//  AgoraEditAvatar
//
//  Created by FanPengpeng on 2022/9/21.
//

#import "CommerceAEAListContainerView.h"

@interface CommerceAEAListContainerView ()

@property (strong, nonatomic) NSMutableDictionary *childVCDic;
@property (weak, nonatomic) UIViewController *currentVC;

@end

@implementation CommerceAEAListContainerView

- (NSArray<UIViewController *> * __nullable)allLoadedViewControllers {
    return self.childVCDic.allValues;
}

- (UIViewController *)viewControllerAtIndex:(NSInteger) index {
    UIViewController *vc = [self.childVCDic objectForKey:@(index)];
    return vc;
}

- (void)setSelectedIndex:(NSInteger)index {
    if (self.childVCDic == nil) {
        self.childVCDic = [NSMutableDictionary dictionary];
    }
    
    UIViewController *vc = [self viewControllerAtIndex:index];
    if (!vc) {
        if ([self.dataSource respondsToSelector:@selector(listContainerView:viewControllerForIndex:)]) {
            vc = [self.dataSource listContainerView:self viewControllerForIndex:index];
        }
        NSAssert(vc, @"listContainerView:viewControllerForIndex:In the method index = %d the controller has returned. nil",index);
        [self.childVCDic setObject:vc forKey:@(index)];
    }
    
    // If VC is the current controller that does not take any action and returns directly
    if (_currentVC == vc) {
        return;
    }
    [self addChildVC:vc];
    if (_currentVC != nil) {
        [self removeChildVC:_currentVC];
    }
    _currentVC = vc;
}

- (void)addChildVC:(UIViewController *)vc {
    if ([self.dataSource isKindOfClass:[UIViewController class]]) {
        UIViewController *containerVC = (UIViewController *)self.dataSource;
        [containerVC addChildViewController:vc];
        [self addSubview:vc.view];
        vc.view.frame = self.bounds;
        [containerVC didMoveToParentViewController:vc];
    }else{
        [self addSubview:vc.view];
        vc.view.frame = self.bounds;
    }
}

- (void)removeChildVC:(UIViewController *)vc {
    [vc willMoveToParentViewController:nil];
    [vc.view removeFromSuperview];
    [vc removeFromParentViewController];
}


@end
