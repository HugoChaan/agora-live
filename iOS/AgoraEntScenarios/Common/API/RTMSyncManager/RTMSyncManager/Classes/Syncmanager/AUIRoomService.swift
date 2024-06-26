//
//  AUIRoomService.swift
//  RTMSyncManager
//
//  Created by wushengtao on 2024/5/8.
//

import Foundation

private let kPayloadOwnerId = "room_payload_owner_id"

let RoomServiceTag = "AUIRoomService"
public class AUIRoomService: NSObject {
    private var expirationPolicy: RoomExpirationPolicy
    private var roomManager: AUIRoomManagerImpl
    private var syncmanager: AUISyncManager
    private var roomInfoMap: [String: AUIRoomInfo] = [:]
    
    public required init(expirationPolicy: RoomExpirationPolicy, roomManager: AUIRoomManagerImpl, syncmanager: AUISyncManager) {
        self.expirationPolicy = expirationPolicy
        self.roomManager = roomManager
        self.syncmanager = syncmanager
        super.init()
    }
    
    public func getRoomList(lastCreateTime: Int64,
                            pageSize: Int,
                            cleanClosure: ((AUIRoomInfo) ->(Bool))? = nil,
                            completion: @escaping (NSError?, Int64, [AUIRoomInfo]?)->()) {
        //房间列表会返回最新的服务端时间ts
        roomManager.getRoomInfoList(lastCreateTime: lastCreateTime, pageSize: pageSize) {[weak self] err, ts, roomList in
            guard let self = self else {return}
            if let err = err {
                completion(err, ts, nil)
                return
            }
            
            var list: [AUIRoomInfo] = []
            roomList?.forEach({ roomInfo in
                var needCleanRoom: Bool = false
                //遍历每个房间信息，查询是否已经过期
                if self.expirationPolicy.expirationTime > 0, ts - roomInfo.createTime >= self.expirationPolicy.expirationTime + 60 * 1000 {
                    aui_info("remove expired room[\(roomInfo.roomId)]", tag: RoomServiceTag)
                    needCleanRoom = true
                } else if cleanClosure?(roomInfo) ?? false {
                    aui_info("external decision to delete room[\(roomInfo.roomId)]", tag: RoomServiceTag)
                    needCleanRoom = true
                }
                
                if needCleanRoom {
                    let scene = self.syncmanager.createScene(channelName: roomInfo.roomId, roomExpiration: self.expirationPolicy)
                    scene.delete()
                    self.roomManager.destroyRoom(roomId: roomInfo.roomId) { _ in
                    }
                    self.roomInfoMap[roomInfo.roomId] = nil
                    return
                }
                
                list.append(roomInfo)
            })
            completion(nil, ts, list)
        }
    }
    
    //TODO: AUIRoomInfo替换成协议IAUIRoomInfo？服务端会创建房间id，这里是否roomManager创建后往外抛roomId
    public func createRoom(room: AUIRoomInfo, completion: @escaping ((NSError?, AUIRoomInfo?)->())) {
        let scene = self.syncmanager.createScene(channelName: room.roomId, roomExpiration: self.expirationPolicy)
        roomManager.createRoom(room: room) {[weak self] err, roomInfo in
            guard let self = self else { return }
            if let err = err {
                completion(err, nil)
                return
            }
            guard let roomInfo = roomInfo else {
                assert(false)
                return
            }
            
            self.roomInfoMap[roomInfo.roomId] = roomInfo
            //传入服务端设置的创建房间的时间戳createTime
            scene.create(createTime: roomInfo.createTime, payload: [kPayloadOwnerId: room.owner?.userId ?? ""]) {[weak self] err in
                if let err = err {
                    //失败需要清理脏房间信息
                    self?.createRoomRevert(roomId: room.roomId)
                    completion(err, nil)
                    return
                }
                
                scene.enter { payload, err in
                    if let err = err {
                        //失败需要清理脏房间信息
                        self?.createRoomRevert(roomId: room.roomId)
                        completion(err, nil)
                        return
                    }
                    completion(nil, roomInfo)
                }
            }
        }
    }
    
    public func enterRoom(roomId: String, completion: @escaping ((NSError?)->())) {
        let scene = syncmanager.createScene(channelName: roomId, roomExpiration: self.expirationPolicy)
        scene.enter {[weak self] payload, err in
            let ownerId = payload?[kPayloadOwnerId] as? String ?? ""
            let room = AUIRoomInfo()
            room.roomId = roomId
            let owner = AUIUserInfo()
            owner.userId = ownerId
            room.owner = owner
            self?.roomInfoMap[room.roomId] = room
            if let err = err {
                self?.enterRoomRevert(roomId: room.roomId)
                completion(err)
                return
            }
            completion(nil)
        }
    }
    
    public func leaveRoom(roomId: String) {
        let scene = syncmanager.createScene(channelName: roomId)
        let isOwner = roomInfoMap[roomId]?.owner?.userId == AUIRoomContext.shared.currentUserInfo.userId
        if isOwner {
            roomManager.destroyRoom(roomId: roomId) { _ in
            }
            scene.delete()
        } else {
            scene.leave()
        }
        roomInfoMap[roomId] = nil
    }
    
    public func leaveRoom(room: AUIRoomInfo) {
        roomInfoMap[room.roomId] = room
        leaveRoom(roomId: room.roomId)
    }
    
    public func isRoomOwner(roomId: String) -> Bool {
        return AUIRoomContext.shared.isRoomOwner(channelName: roomId)
    }
}

//MARK: private
extension AUIRoomService {
    private func createRoomRevert(roomId: String) {
        aui_info("createRoomRevert[\(roomId)]", tag: RoomServiceTag)
        leaveRoom(roomId: roomId)
    }
    
    private func enterRoomRevert(roomId: String) {
        aui_info("enterRoomRevert[\(roomId)]", tag: RoomServiceTag)
        leaveRoom(roomId: roomId)
    }
}

