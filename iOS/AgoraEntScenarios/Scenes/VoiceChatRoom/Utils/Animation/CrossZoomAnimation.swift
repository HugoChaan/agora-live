//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 30, 2022
//

import Foundation

public class CrossZoomAnimation: PresentationAnimation {
    private var scale: CGFloat

    public init(scale: CGFloat, options: AnimationOptions = .normal(duration: 0.3), origin: PresentationOrigin? = nil) {
        self.scale = scale
        super.init(options: options, origin: origin)
    }

    override public func beforeAnimation(animationContext: AnimationContext) {
        animationContext.animatingView?.frame = animationContext.finalFrame
        let translate = calculateTranslate(animationContext: animationContext)
        animationContext.animatingView?.transform = animationContext.isPresenting ? CGAffineTransform(translationX: translate.x, y: translate.y).scaledBy(x: scale, y: scale) : .identity
    }

    override public func performAnimation(animationContext: AnimationContext) {
        let translate = calculateTranslate(animationContext: animationContext)
        animationContext.animatingView?.transform = animationContext.isPresenting ? .identity : CGAffineTransform(translationX: translate.x, y: translate.y).scaledBy(x: scale, y: scale)
    }

    private func calculateTranslate(animationContext: AnimationContext) -> CGPoint {
        let finalFrame = animationContext.finalFrame
        let initialFrame = transformInitialFrame(containerFrame: animationContext.containerView.frame, finalFrame: finalFrame)
        let translate = CGPoint(x: initialFrame.minX - finalFrame.minX, y: initialFrame.minY - finalFrame.minY)
        return translate
    }
}
