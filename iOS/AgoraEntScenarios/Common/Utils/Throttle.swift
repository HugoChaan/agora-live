//
//  Throttle.swift
//  AgoraScene_iOS
//
// Created by Zhu Jichao on October 10, 2022
//

import Combine
import Foundation

/// struct throttling successive works with provided options.
public enum Throttler {
    typealias WorkIdentifier = String

    typealias Work = () -> Void
    @available(iOS 13.0, *)
    typealias Subject = PassthroughSubject<Work, Never>?
    @available(iOS 13.0, *)
    typealias Bag = Set<AnyCancellable>

    @available(iOS 13.0, *)
    private static var subjects: [WorkIdentifier: Subject] = [:]
    @available(iOS 13.0, *)
    private static var bags: [WorkIdentifier: Bag] = [:]

    /// - Note: Pay special attention to the identifier parameter. the default identifier is \("Thread.callStackSymbols") to make api trailing closure for one liner for the sake of brevity. However, it is highly recommend that a developer should provide explicit identifier for their work to debounce. Also, please note that the default queue is global queue, it may cause thread explosion issue if not explicitly specified , so use at your own risk.
    ///
    /// - Parameters:
    ///   - identifier: the identifier to group works to throttle. Throttler must have equivalent identifier to each work in a group to throttle.
    ///   - queue: a queue to run a work on. dispatch global queue will be chosen by default if not specified.
    ///   - delay: delay for throttle. time unit is second. given default is 1.0 sec.
    ///   - shouldRunImmediately: a boolean type where true will run the first work immediately regardless.
    ///   - shouldRunLatest: A Boolean value that indicates whether to publish the most recent element. If `false`, the publisher emits the first element received during the interval.
    ///   - work: a work to run
    /// - Returns: Void
    @available(iOS 13.0, *)
    public static func throttle(identifier: String = "\(Thread.callStackSymbols)",
                                queue: DispatchQueue? = nil,
                                delay: DispatchQueue.SchedulerTimeType.Stride = .seconds(1),
                                shouldRunImmediately: Bool = true,
                                shouldRunLatest: Bool = true,
                                work: @escaping () -> Void)
    {
        let isFirstRun = subjects[identifier] == nil ? true : false

        if shouldRunImmediately && isFirstRun {
            work()
        }

        if let _ = subjects[identifier] {
            subjects[identifier]?!.send(work)
        } else {
            subjects[identifier] = PassthroughSubject<Work, Never>()
            bags[identifier] = Bag()

            let q = queue ?? .global()

            subjects[identifier]?!
                .throttle(for: delay, scheduler: q, latest: shouldRunLatest)
                .sink(receiveValue: {
                    $0()
                })
                .store(in: &bags[identifier]!)
        }
    }
}
