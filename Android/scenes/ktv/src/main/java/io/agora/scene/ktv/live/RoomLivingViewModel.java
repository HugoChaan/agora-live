package io.agora.scene.ktv.live;

import static io.agora.rtc2.Constants.ROOM_ACOUSTICS_KTV;
import static io.agora.rtc2.RtcConnection.CONNECTION_STATE_TYPE.CONNECTION_STATE_CONNECTED;
import static io.agora.rtc2.RtcConnection.CONNECTION_STATE_TYPE.getValue;
import static io.agora.rtc2.video.ContentInspectConfig.CONTENT_INSPECT_TYPE_MODERATION;
import static io.agora.scene.ktv.ktvapi.KTVApiKt.createKTVApi;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.agora.musiccontentcenter.Music;
import io.agora.musiccontentcenter.MusicChartInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.ContentInspectConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.scene.base.AudioModeration;
import io.agora.scene.base.BuildConfig;
import io.agora.scene.base.component.AgoraApplication;
import io.agora.scene.base.manager.UserManager;
import io.agora.scene.ktv.KTVLogger;
import io.agora.scene.ktv.R;
import io.agora.scene.ktv.debugSettings.KTVDebugSettingBean;
import io.agora.scene.ktv.debugSettings.KTVDebugSettingsDialog;
import io.agora.scene.ktv.ktvapi.AudioTrackMode;
import io.agora.scene.ktv.ktvapi.IKTVApiEventHandler;
import io.agora.scene.ktv.ktvapi.ILrcView;
import io.agora.scene.ktv.ktvapi.IMusicLoadStateListener;
import io.agora.scene.ktv.ktvapi.ISwitchRoleStateListener;
import io.agora.scene.ktv.ktvapi.KTVApi;
import io.agora.scene.ktv.ktvapi.KTVApiConfig;
import io.agora.scene.ktv.ktvapi.KTVApiImpl;
import io.agora.scene.ktv.ktvapi.KTVLoadMusicConfiguration;
import io.agora.scene.ktv.ktvapi.KTVLoadMusicFailReason;
import io.agora.scene.ktv.ktvapi.KTVLoadMusicMode;
import io.agora.scene.ktv.ktvapi.KTVMusicType;
import io.agora.scene.ktv.ktvapi.KTVSingRole;
import io.agora.scene.ktv.ktvapi.KTVType;
import io.agora.scene.ktv.ktvapi.MusicLoadStatus;
import io.agora.scene.ktv.ktvapi.SwitchRoleFailReason;
import io.agora.scene.ktv.live.bean.MusicSettingBean;
import io.agora.scene.ktv.live.bean.NetWorkEvent;
import io.agora.scene.ktv.live.fragmentdialog.MusicSettingCallback;
import io.agora.scene.ktv.service.VolumeModel;
import io.agora.scene.ktv.live.bean.SoundCardSettingBean;
import io.agora.scene.ktv.service.ChooseSongInputModel;
import io.agora.scene.ktv.service.JoinRoomOutputModel;
import io.agora.scene.ktv.service.KTVServiceProtocol;
import io.agora.scene.ktv.service.MakeSongTopInputModel;
import io.agora.scene.ktv.service.OnSeatInputModel;
import io.agora.scene.ktv.service.OutSeatInputModel;
import io.agora.scene.ktv.service.RemoveSongInputModel;
import io.agora.scene.ktv.service.RoomSeatModel;
import io.agora.scene.ktv.service.RoomSelSongModel;
import io.agora.scene.ktv.service.ScoringAlgoControlModel;
import io.agora.scene.ktv.service.ScoringAverageModel;
import io.agora.scene.ktv.widget.lrcView.LrcControlView;
import io.agora.scene.widget.toast.CustomToast;

/**
 * The type Room living view model.
 */
public class RoomLivingViewModel extends ViewModel {

    private final String TAG = "KTV_Scene_LOG";
    private Handler mainHandler;

    private void runOnMainThread(Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        if (Thread.currentThread() == mainHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    // 默认音效
    private final int DEFAULT_AUDIO_EFFECT = ROOM_ACOUSTICS_KTV;
    private final KTVServiceProtocol ktvServiceProtocol = KTVServiceProtocol.Companion.getImplInstance();
    private KTVApi ktvApiProtocol;

    // loading dialog
    private final MutableLiveData<Boolean> _loadingDialogVisible = new MutableLiveData<>(false);
    /**
     * The Loading dialog visible.
     */
    final LiveData<Boolean> loadingDialogVisible = _loadingDialogVisible;

    /**
     * 房间信息
     */
    final MutableLiveData<JoinRoomOutputModel> roomInfoLiveData;
    /**
     * The Room delete live data.
     */
    final MutableLiveData<Boolean> roomDeleteLiveData = new MutableLiveData<>();
    /**
     * The Room time up live data.
     */
    final MutableLiveData<Boolean> roomTimeUpLiveData = new MutableLiveData<>();
    /**
     * The Room user count live data.
     */
    final MutableLiveData<Integer> roomUserCountLiveData = new MutableLiveData<>(0);

    /**
     * 麦位信息
     */
    boolean isOnSeat = false;
    /**
     * The Seat list live data.
     */
    final MutableLiveData<List<RoomSeatModel>> seatListLiveData = new MutableLiveData<>(new ArrayList<>());
    /**
     * The Seat local live data.
     */
    final MutableLiveData<RoomSeatModel> seatLocalLiveData = new MutableLiveData<>();

    /**
     * The Volume live data.
     */
    final MutableLiveData<VolumeModel> volumeLiveData = new MutableLiveData<>();

    /**
     * 歌词信息
     */
    final MutableLiveData<List<RoomSelSongModel>> songsOrderedLiveData = new MutableLiveData<>();
    /**
     * The Song playing live data.
     */
    final MutableLiveData<RoomSelSongModel> songPlayingLiveData = new MutableLiveData<>();

    /**
     * The type Line score.
     */
    class LineScore {
        /**
         * The Score.
         */
        int score;
        /**
         * The Index.
         */
        int index;
        /**
         * The Cumulative score.
         */
        int cumulativeScore;
        /**
         * The Total.
         */
        int total;
    }

    /**
     * The Main singer score live data.
     */
    final MutableLiveData<LineScore> mainSingerScoreLiveData = new MutableLiveData<>();

    /**
     * Player/RTC信息
     */
    int streamId = 0;

    /**
     * The enum Player music status.
     */
    enum PlayerMusicStatus {
        /**
         * On prepare player music status.
         */
        ON_PREPARE,
        /**
         * On playing player music status.
         */
        ON_PLAYING,
        /**
         * On pause player music status.
         */
        ON_PAUSE,
        /**
         * On stop player music status.
         */
        ON_STOP,
        /**
         * On lrc reset player music status.
         */
        ON_LRC_RESET,
        /**
         * On changing start player music status.
         */
        ON_CHANGING_START,
        /**
         * On changing end player music status.
         */
        ON_CHANGING_END
    }

    /**
     * The Player music status live data.
     */
    final MutableLiveData<PlayerMusicStatus> playerMusicStatusLiveData = new MutableLiveData<>();

    /**
     * The Load music progress live data.
     */
// 加载音乐进度
    final MutableLiveData<Integer> loadMusicProgressLiveData = new MutableLiveData<>();

    /**
     * The enum Join chorus status.
     */
    enum JoinChorusStatus {
        /**
         * On idle join chorus status.
         */
        ON_IDLE,
        /**
         * On join chorus join chorus status.
         */
        ON_JOIN_CHORUS,
        /**
         * On join failed join chorus status.
         */
        ON_JOIN_FAILED,
        /**
         * On leave chorus join chorus status.
         */
        ON_LEAVE_CHORUS,
    }

    /**
     * The Joinchorus status live data.
     */
    final MutableLiveData<JoinChorusStatus> joinchorusStatusLiveData = new MutableLiveData<>();
    /**
     * The No lrc live data.
     */
    final MutableLiveData<Boolean> noLrcLiveData = new MutableLiveData<>();

    /**
     * The Player music open duration live data.
     */
    final MutableLiveData<Long> playerMusicOpenDurationLiveData = new MutableLiveData<>();
    /**
     * The Player music play complete live data.
     */
    final MutableLiveData<ScoringAverageModel> playerMusicPlayCompleteLiveData = new MutableLiveData<>();
    /**
     * The Network status live data.
     */
    final MutableLiveData<NetWorkEvent> networkStatusLiveData = new MutableLiveData<>();

    /**
     * The Scoring algo control live data.
     */
    final MutableLiveData<ScoringAlgoControlModel> scoringAlgoControlLiveData = new MutableLiveData<>();

    /**
     * The Scoring algo live data.
     */
// 打分难度
    final MutableLiveData<Integer> scoringAlgoLiveData = new MutableLiveData<>();

    /**
     * Rtc引擎
     */
    private RtcEngineEx mRtcEngine;

    /**
     * 主版本的音频设置
     */
    private final ChannelMediaOptions mainChannelMediaOption = new ChannelMediaOptions();

    /**
     * 播放器配置
     */
    MusicSettingBean mSetting;

    /**
     * 是否开启后台播放
     */
    KTVDebugSettingBean mDebugSetting;

    /**
     * 是否开启后台播放
     */
    private final boolean isBackPlay = false;

    /**
     * 是否开启耳返
     */
    private boolean isOpnEar = false;

    /**
     * 合唱人数
     */
    public int chorusNum = 0;

    /**
     * The M sound card setting bean.
     */
    SoundCardSettingBean mSoundCardSettingBean;

    /**
     * Instantiates a new Room living view model.
     *
     * @param roomInfo the room info
     */
    public RoomLivingViewModel(JoinRoomOutputModel roomInfo) {
        this.roomInfoLiveData = new MutableLiveData<>(roomInfo);
    }

    /**
     * Is room owner boolean.
     *
     * @return the boolean
     */
    public boolean isRoomOwner() {
        return roomInfoLiveData.getValue().getCreatorNo().equals(UserManager.getInstance().getUser().id.toString());
    }

    /**
     * Is playing boolean.
     *
     * @return the boolean
     */
    public boolean isPlaying(){
        return playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PLAYING;
    }

    /**
     * Init.
     */
    public void init() {
        initSettings();
        initRTCPlayer();
        initRoom();
        initSeats();
        initSongs();
        initReConnectEvent();
    }

    /**
     * Release boolean.
     *
     * @return the boolean
     */
    public boolean release() {
        KTVLogger.d(TAG, "release called");
        streamId = 0;
        if (mRtcEngine != null) {
            ktvApiProtocol.release();
            mSoundCardSettingBean.enable(false, true, () -> null);
        }

        if (mRtcEngine != null) {
            mRtcEngine.enableInEarMonitoring(false, Constants.EAR_MONITORING_FILTER_NONE);
            mRtcEngine.leaveChannel();
            RtcEngineEx.destroy();
            mRtcEngine = null;
            return true;
        }
        return false;
    }

    /**
     * Gets sdk build num.
     *
     * @return the sdk build num
     */
    public String getSDKBuildNum() {
        return RtcEngineEx.getSdkVersion();
    }

    // ======================= 断网重连相关 =======================

    /**
     * Init re connect event.
     */
    public void initReConnectEvent() {
        ktvServiceProtocol.subscribeReConnectEvent(() -> {
            reFetchUserNum();
            reFetchSeatStatus();
            reFetchSongStatus();
            return null;
        });
    }

    private void reFetchUserNum() {
        KTVLogger.d(TAG, "reFetchUserNum: call");
        ktvServiceProtocol.getAllUserList(num -> {
            roomUserCountLiveData.postValue(num);
            return null;
        }, null);
    }

    private void reFetchSeatStatus() {
        KTVLogger.d(TAG, "reFetchSeatStatus: call");
        ktvServiceProtocol.getSeatStatusList((e, data) -> {
            if (e == null && data != null) {
                KTVLogger.d(TAG, "getSeatStatusList: return" + data);
                seatListLiveData.setValue(data);
            }
            return null;
        });
    }

    private void reFetchSongStatus() {
        KTVLogger.d(TAG, "reFetchSongStatus: call");
        onSongChanged();
    }

    // ======================= 房间相关 =======================

    /**
     * Init room.
     */
    public void initRoom() {
        JoinRoomOutputModel _roomInfo = roomInfoLiveData.getValue();
        if (_roomInfo == null) {
            throw new RuntimeException("The roomInfo must be not null before initSeats method calling!");
        }

        roomUserCountLiveData.postValue(_roomInfo.getRoomPeopleNum());

        ktvServiceProtocol.subscribeRoomStatus((ktvSubscribe, vlRoomListModel) -> {
            if (ktvSubscribe == KTVServiceProtocol.KTVSubscribe.KTVSubscribeDeleted) {
                KTVLogger.d(TAG, "subscribeRoomStatus KTVSubscribeDeleted");
                roomDeleteLiveData.postValue(true);
            } else if (ktvSubscribe == KTVServiceProtocol.KTVSubscribe.KTVSubscribeUpdated) {
                // 当房间内状态发生改变时触发
                KTVLogger.d(TAG, "subscribeRoomStatus KTVSubscribeUpdated");
                if (!vlRoomListModel.getBgOption().equals(_roomInfo.getBgOption())) {
                    roomInfoLiveData.postValue(new JoinRoomOutputModel(
                            _roomInfo.getRoomName(),
                            _roomInfo.getRoomNo(),
                            _roomInfo.getCreatorNo(),
                            _roomInfo.getCreatorAvatar(),
                            vlRoomListModel.getBgOption(),
                            _roomInfo.getSeatsArray(),
                            _roomInfo.getRoomPeopleNum(),
                            _roomInfo.getAgoraRTMToken(),
                            _roomInfo.getAgoraRTCToken(),
                            _roomInfo.getAgoraChorusToken(),
                            _roomInfo.getCreatedAt()
                    ));
                }
            }
            return null;
        });

        ktvServiceProtocol.subscribeUserListCount(count -> {
            roomUserCountLiveData.postValue(count);
            return null;
        });

        ktvServiceProtocol.subscribeRoomTimeUp(() -> {
            roomTimeUpLiveData.postValue(true);
            return null;
        });
    }

    /**
     * 退出房间
     */
    public void exitRoom() {
        KTVLogger.d(TAG, "RoomLivingViewModel.exitRoom() called");
        ktvServiceProtocol.leaveRoom(e -> {
            if (e == null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.exitRoom() success");
                roomDeleteLiveData.postValue(false);
                roomTimeUpLiveData.postValue(false);
            } else {
                // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.exitRoom() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
            return null;
        });
    }

    // ======================= 麦位相关 =======================

    /**
     * Init seats.
     */
    public void initSeats() {
        JoinRoomOutputModel _roomInfo = roomInfoLiveData.getValue();
        if (_roomInfo == null) {
            throw new RuntimeException("The roomInfo must be not null before initSeats method calling!");
        }
        List<RoomSeatModel> seatsArray = _roomInfo.getSeatsArray();
        seatListLiveData.postValue(seatsArray);

        if (seatsArray != null) {
            for (RoomSeatModel roomSeatModel : seatsArray) {
                if (roomSeatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                    seatLocalLiveData.setValue(roomSeatModel);
                    isOnSeat = true;
                    if (mRtcEngine != null) {
                        mainChannelMediaOption.publishCameraTrack = roomSeatModel.isVideoMuted() == RoomSeatModel.Companion.getMUTED_VALUE_FALSE();
                        mainChannelMediaOption.publishMicrophoneTrack = true;
                        mainChannelMediaOption.enableAudioRecordingOrPlayout = true;
                        mainChannelMediaOption.autoSubscribeVideo = true;
                        mainChannelMediaOption.autoSubscribeAudio = true;
                        mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                        mRtcEngine.updateChannelMediaOptions(mainChannelMediaOption);

                        updateVolumeStatus(roomSeatModel.isAudioMuted() == RoomSeatModel.Companion.getMUTED_VALUE_FALSE());
                    }
                    break;
                }
            }
        }
        if (seatLocalLiveData.getValue() == null) {
            seatLocalLiveData.setValue(null);
        }

        ktvServiceProtocol.subscribeSeatList((ktvSubscribe, roomSeatModel) -> {
            if (ktvSubscribe == KTVServiceProtocol.KTVSubscribe.KTVSubscribeCreated) {
                KTVLogger.d(TAG, "subscribeRoomStatus KTVSubscribeCreated");
                List<RoomSeatModel> oValue = seatListLiveData.getValue();
                if (oValue == null) {
                    return null;
                }
                List<RoomSeatModel> value = new ArrayList<>(oValue);
                value.add(roomSeatModel);
                seatListLiveData.postValue(value);

                if (roomSeatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                    seatLocalLiveData.setValue(roomSeatModel);
                    updateVolumeStatus(roomSeatModel.isAudioMuted() == RoomSeatModel.Companion.getMUTED_VALUE_FALSE());
                }

            } else if (ktvSubscribe == KTVServiceProtocol.KTVSubscribe.KTVSubscribeUpdated) {
                KTVLogger.d(TAG, "subscribeRoomStatus KTVSubscribeUpdated");
                List<RoomSeatModel> oValue = seatListLiveData.getValue();
                if (oValue == null) {
                    return null;
                }
                List<RoomSeatModel> value = new ArrayList<>(oValue);
                int index = -1;
                for (int i = 0; i < value.size(); i++) {
                    if (value.get(i).getSeatIndex() == roomSeatModel.getSeatIndex()) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    value.remove(index);
                    value.add(index, roomSeatModel);
                    seatListLiveData.postValue(value);

                    if (roomSeatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                        seatLocalLiveData.setValue(roomSeatModel);
                        updateVolumeStatus(roomSeatModel.isAudioMuted() == RoomSeatModel.Companion.getMUTED_VALUE_FALSE());
                    }
                }

            } else if (ktvSubscribe == KTVServiceProtocol.KTVSubscribe.KTVSubscribeDeleted) {
                KTVLogger.d(TAG, "subscribeRoomStatus KTVSubscribeDeleted");
                List<RoomSeatModel> oValue = seatListLiveData.getValue();
                if (oValue == null) {
                    return null;
                }
                List<RoomSeatModel> value = new ArrayList<>(oValue);
                Iterator<RoomSeatModel> iterator = value.iterator();
                while (iterator.hasNext()) {
                    RoomSeatModel next = iterator.next();
                    if (next.getUserNo().equals(roomSeatModel.getUserNo())) {
                        iterator.remove();
                    }
                }
                seatListLiveData.postValue(value);

                if (roomSeatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                    seatLocalLiveData.postValue(null);
                }


                if (roomSeatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                    isOnSeat = false;
                    if (mRtcEngine != null) {
                        mainChannelMediaOption.publishCameraTrack = false;
                        mainChannelMediaOption.publishMicrophoneTrack = false;
                        mainChannelMediaOption.enableAudioRecordingOrPlayout = true;
                        mainChannelMediaOption.autoSubscribeVideo = true;
                        mainChannelMediaOption.autoSubscribeAudio = true;
                        mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
                        mRtcEngine.updateChannelMediaOptions(mainChannelMediaOption);
                    }
                    updateVolumeStatus(false);

                    RoomSelSongModel songPlayingData = songPlayingLiveData.getValue();
                    if (songPlayingData == null) {
                        return null;
                    } else if (roomSeatModel.getChorusSongCode().equals(songPlayingData.getSongNo() + songPlayingData.getCreateAt())) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null);
                        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Solo singer join chorus mode.
     *
     * @param isJoin the is join
     */
    public void soloSingerJoinChorusMode(boolean isJoin) {
        if (songPlayingLiveData.getValue() == null || seatListLiveData.getValue() == null) return;
        if (songPlayingLiveData.getValue().getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
            if (isJoin) {
                // 有人加入合唱
                ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null);
            } else {
                // 最后一人退出合唱
                ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null);
            }
        }
    }

    /**
     * 上麦
     *
     * @param onSeatIndex the on seat index
     */
    public void haveSeat(int onSeatIndex) {
        KTVLogger.d(TAG, "RoomLivingViewModel.haveSeat() called: " + onSeatIndex);
        ktvServiceProtocol.onSeat(new OnSeatInputModel(onSeatIndex), e -> {
            if (e == null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.haveSeat() success");
                isOnSeat = true;
                if (mRtcEngine != null) {
                    mainChannelMediaOption.publishCameraTrack = false;
                    mainChannelMediaOption.publishMicrophoneTrack = true;
                    mainChannelMediaOption.enableAudioRecordingOrPlayout = true;
                    mainChannelMediaOption.autoSubscribeVideo = true;
                    mainChannelMediaOption.autoSubscribeAudio = true;
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                    mRtcEngine.updateChannelMediaOptions(mainChannelMediaOption);
                }
                toggleMic(false);
            } else {
                // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.haveSeat() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
            return null;
        });
    }

    /**
     * 离开麦位
     *
     * @param seatModel the seat model
     */
    public void leaveSeat(RoomSeatModel seatModel) {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveSeat() called");
        ktvServiceProtocol.outSeat(
                new OutSeatInputModel(
                        seatModel.getUserNo(),
                        seatModel.getRtcUid(),
                        seatModel.getName(),
                        seatModel.getHeadUrl(),
                        seatModel.getSeatIndex()
                ),
                e -> {
                    if (e == null) {
                        // success
                        KTVLogger.d(TAG, "RoomLivingViewModel.leaveSeat() success");
                        if (seatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                            isOnSeat = false;
                            if (seatModel.isAudioMuted() == RoomSeatModel.Companion.getMUTED_VALUE_TRUE()) {
                                if (mRtcEngine != null) {
                                    mainChannelMediaOption.publishCameraTrack = false;
                                    mainChannelMediaOption.publishMicrophoneTrack = false;
                                    mainChannelMediaOption.enableAudioRecordingOrPlayout = true;
                                    mainChannelMediaOption.autoSubscribeVideo = true;
                                    mainChannelMediaOption.autoSubscribeAudio = true;
                                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
                                    mRtcEngine.updateChannelMediaOptions(mainChannelMediaOption);
                                }
                                updateVolumeStatus(false);
                            }
                        }

                        if (songPlayingLiveData.getValue() != null) {
                            boolean isJoinChorus = seatModel.getChorusSongCode().equals(songPlayingLiveData.getValue().getSongNo() + songPlayingLiveData.getValue().getCreateAt());
                            if (isJoinChorus && seatModel.getUserNo().equals(UserManager.getInstance().getUser().id.toString())) {
                                leaveChorus();
                            }
                        }
                    } else {
                        // failure
                        KTVLogger.e(TAG, "RoomLivingViewModel.leaveSeat() failed: " + e.getMessage());
                        if (e.getMessage() != null) {
                            CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    }
                    return null;
                });
    }

    /**
     * 开关摄像头
     */
    boolean isCameraOpened = false;

    /**
     * Toggle self video.
     *
     * @param isOpen the is open
     */
    public void toggleSelfVideo(boolean isOpen) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfVideo() called：" + isOpen);
        ktvServiceProtocol.updateSeatVideoMuteStatus(!isOpen, e -> {
            if (e == null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfVideo() success");
                isCameraOpened = isOpen;
                mRtcEngine.enableLocalVideo(isOpen);
                ChannelMediaOptions channelMediaOption = new ChannelMediaOptions();
                channelMediaOption.publishCameraTrack = isOpen;
                mRtcEngine.updateChannelMediaOptions(channelMediaOption);
            } else {
                // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.toggleSelfVideo() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
            return null;
        });
    }

    /**
     * 静音
     *
     * @param isUnMute the is un mute
     */
    public void toggleMic(boolean isUnMute) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleMic() called：" + isUnMute);
        updateVolumeStatus(isUnMute);
        ktvServiceProtocol.updateSeatAudioMuteStatus(!isUnMute, e -> {
            if (e == null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.toggleMic() success");
            } else {
                // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.toggleMic() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
            return null;
        });
    }

    private void updateVolumeStatus(boolean isUnMute) {
        ktvApiProtocol.muteMic(!isUnMute);
        if (!isUnMute && mSetting.getMEarBackEnable()) {
            if (mRtcEngine != null) {
                mRtcEngine.enableInEarMonitoring(false, Constants.EAR_MONITORING_FILTER_NONE);
            }
        } else if (isUnMute && mSetting.getMEarBackEnable()) {
            if (mRtcEngine != null) {
                mRtcEngine.enableInEarMonitoring(true, Constants.EAR_MONITORING_FILTER_NONE);
            }
        }

        if (isUnMute) {
            KTVLogger.d(TAG, "unmute! setMicVolume: " + micOldVolume);
            if (mRtcEngine != null) {
                mRtcEngine.adjustRecordingSignalVolume(micOldVolume);
            }
        }
    }


    // ======================= 歌曲相关 =======================

    /**
     * Init songs.
     */
    public void initSongs() {
        ktvServiceProtocol.subscribeChooseSong((ktvSubscribe, songModel) -> {
            // 歌曲信息发生变化时，重新获取歌曲列表动作
            KTVLogger.d(TAG, "subscribeChooseSong updateSongs");
            onSongChanged();
            return null;
        });

        // 获取初始歌曲列表
        onSongChanged();
    }

    private void onSongChanged() {
        ktvServiceProtocol.getChoosedSongsList((e, data) -> {
            if (e == null && data != null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() success");
                songsOrderedLiveData.postValue(data);

                if (data.size() > 0) {
                    RoomSelSongModel value = songPlayingLiveData.getValue();
                    RoomSelSongModel songPlaying = data.get(0);

                    if (value == null) {
                        // 无已点歌曲， 直接将列表第一个设置为当前播放歌曲
                        KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() chosen song list is empty");
                        songPlayingLiveData.postValue(songPlaying);
                    } else if (!value.getSongNo().equals(songPlaying.getSongNo())) {
                        // 当前有已点歌曲, 且更新歌曲和之前歌曲非同一首
                        KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() single or first chorus");
                        songPlayingLiveData.postValue(songPlaying);
                    }
                } else {
                    KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() return is emptyList");
                    songPlayingLiveData.postValue(null);
                }

            } else {
                // failed
                if (e != null) {
                    KTVLogger.e(TAG, "RoomLivingViewModel.getSongChosenList() failed: " + e.getMessage());
                    if (e.getMessage() != null) {
                        CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Gets song chosen list.
     */
    public void getSongChosenList() {
        ktvServiceProtocol.getChoosedSongsList((e, data) -> {
            if (e == null && data != null) {
                // success
                KTVLogger.d(TAG, "RoomLivingViewModel.getSongChosenList() success");
                songsOrderedLiveData.postValue(data);
            } else {
                // failed
                if (e != null) {
                    KTVLogger.e(TAG, "RoomLivingViewModel.getSongChosenList() failed: " + e.getMessage());
                    if (e.getMessage() != null) {
                        CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                    }
                }
            }
            return null;
        });
    }

    /**
     * 获取歌曲类型
     *
     * @return map key: 类型名称，value: 类型值
     */
    public LiveData<LinkedHashMap<Integer, String>> getSongTypes() {
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongTypes() called");
        MutableLiveData<LinkedHashMap<Integer, String>> liveData = new MutableLiveData<>();

        ktvApiProtocol.fetchMusicCharts((id, status, list) -> {
            KTVLogger.d(TAG, "RoomLivingViewModel.getSongTypes() return");
            LinkedHashMap<Integer, String> types = new LinkedHashMap<>();
            // 重新排序 ----> 按照（嗨唱推荐、抖音热歌、热门新歌、KTV必唱）这个顺序进行怕苦
            for (int i = 0; i < 4; i++) {
                for (MusicChartInfo musicChartInfo : list) {
                    if ((i == 0 && musicChartInfo.type == 3) || // 嗨唱推荐
                            // 抖音热歌
                            (i == 1 && musicChartInfo.type == 4) ||
                            // 热门新歌
                            (i == 2 && musicChartInfo.type == 2) ||
                            // KTV必唱
                            (i == 3 && musicChartInfo.type == 6)) {
                        types.put(musicChartInfo.type, musicChartInfo.name);
                    }
                }
            }
            // 将剩余的插到尾部
            for (MusicChartInfo musicChartInfo : list) {
                if (!types.containsKey(musicChartInfo.type)) {
                    types.put(musicChartInfo.type, musicChartInfo.name);
                }
            }
            // 因为榜单基本是固化的，防止拉取列表失败，直接写入配置
            if (list.length == 0) {
                types.put(3, "嗨唱推荐");
                types.put(4, "抖音热歌");
                types.put(2, "新歌榜");
                types.put(6, "KTV必唱");
                types.put(0, "项目热歌榜单");
                types.put(1, "声网热歌榜");
                types.put(5, "古风热歌");
            }
            liveData.postValue(types);
            return null;
        });
        return liveData;
    }

    /**
     * 获取歌曲列表
     *
     * @param type the type
     * @param page the page
     * @return the song list
     */
    public LiveData<List<RoomSelSongModel>> getSongList(int type, int page) {
        // 从RTC中获取歌曲列表
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() called, type:" + type + " page:" + page);
        MutableLiveData<List<RoomSelSongModel>> liveData = new MutableLiveData<>();
        String jsonOption = "{\"pitchType\":1,\"needLyric\":true}";
        ktvApiProtocol.searchMusicByMusicChartId(type, page, 30, jsonOption,
                (id, status, p, size, total, list) -> {
                    KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() return");
                    List<Music> musicList = new ArrayList<>(Arrays.asList(list));
                    List<RoomSelSongModel> songs = new ArrayList<>();

                    // 需要再调一个接口获取当前已点的歌单来补充列表信息 >_<
                    ktvServiceProtocol.getChoosedSongsList((e, songsChosen) -> {
                        if (e == null && songsChosen != null) {
                            // success
                            for (Music music : musicList) {
                                RoomSelSongModel songItem = null;
                                for (RoomSelSongModel roomSelSongModel : songsChosen) {
                                    if (roomSelSongModel.getSongNo().equals(String.valueOf(music.songCode))) {
                                        songItem = roomSelSongModel;
                                        break;
                                    }
                                }

                                if (songItem == null) {
                                    songItem = new RoomSelSongModel(
                                            music.name,
                                            String.valueOf(music.songCode),
                                            music.singer,
                                            music.poster,
                                            "",
                                            "",
                                            0,
                                            0,
                                            0,
                                            0
                                    );
                                }
                                songs.add(songItem);
                            }
                            liveData.postValue(songs);
                        } else {
                            if (e != null && e.getMessage() != null) {
                                CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        }
                        return null;
                    });
                    return null;
                });

        return liveData;
    }

    /**
     * 搜索歌曲
     *
     * @param condition the condition
     * @return the live data
     */
    public LiveData<List<RoomSelSongModel>> searchSong(String condition) {
        // 从RTC中搜索歌曲
        KTVLogger.d(TAG, "RoomLivingViewModel.searchSong() called, condition:" + condition);
        MutableLiveData<List<RoomSelSongModel>> liveData = new MutableLiveData<>();

        // 过滤没有歌词的歌曲
        String jsonOption = "{\"pitchType\":1,\"needLyric\":true}";
        ktvApiProtocol.searchMusicByKeyword(condition, 0, 50, jsonOption,
                (id, status, p, size, total, list) -> {
                    List<Music> musicList = new ArrayList<>(Arrays.asList(list));
                    List<RoomSelSongModel> songs = new ArrayList<>();

                    // 需要再调一个接口获取当前已点的歌单来补充列表信息 >_<
                    ktvServiceProtocol.getChoosedSongsList((e, songsChosen) -> {
                        if (e == null && songsChosen != null) {
                            // success
                            for (Music music : musicList) {
                                RoomSelSongModel songItem = null;
                                for (RoomSelSongModel roomSelSongModel : songsChosen) {
                                    if (roomSelSongModel.getSongNo().equals(String.valueOf(music.songCode))) {
                                        songItem = roomSelSongModel;
                                        break;
                                    }
                                }

                                if (songItem == null) {
                                    songItem = new RoomSelSongModel(
                                            music.name,
                                            String.valueOf(music.songCode),
                                            music.singer,
                                            music.poster,
                                            "",
                                            "",
                                            0,
                                            0,
                                            0,
                                            0
                                    );
                                }

                                songs.add(songItem);
                            }
                            liveData.postValue(songs);
                        } else {
                            if (e != null && e.getMessage() != null) {
                                CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        }
                        return null;
                    });
                    return null;
                });

        return liveData;
    }

    /**
     * 点歌
     *
     * @param songModel the song model
     * @param isChorus  the is chorus
     * @return the live data
     */
    public LiveData<Boolean> chooseSong(RoomSelSongModel songModel, boolean isChorus) {
        KTVLogger.d(TAG, "RoomLivingViewModel.chooseSong() called, name:" + songModel.getName() + " isChorus:" + isChorus);
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        ktvServiceProtocol.chooseSong(
                new ChooseSongInputModel(
                        songModel.getSongName(),
                        songModel.getSongNo(),
                        songModel.getSinger(),
                        songModel.getImageUrl()),
                e -> {
                    if (e == null) {
                        // success
                        KTVLogger.d(TAG, "RoomLivingViewModel.chooseSong() success");
                        liveData.postValue(true);
                    } else {
                        // failure
                        KTVLogger.e(TAG, "RoomLivingViewModel.chooseSong() failed: " + e.getMessage());
                        if (e.getMessage() != null) {
                            CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                        }
                        liveData.postValue(false);
                    }
                    return null;
                }
        );
        return liveData;
    }

    /**
     * 删歌
     *
     * @param songModel the song model
     */
    public void deleteSong(RoomSelSongModel songModel) {
        KTVLogger.d(TAG, "RoomLivingViewModel.deleteSong() called, name:" + songModel.getName());
        ktvServiceProtocol.removeSong(false,
                new RemoveSongInputModel(songModel.getSongNo()),
                e -> {
                    if (e == null) {
                        // success: do nothing for subscriber dealing with the event already
                        KTVLogger.d(TAG, "RoomLivingViewModel.deleteSong() success");
                    } else {
                        // failure
                        KTVLogger.e(TAG, "RoomLivingViewModel.deleteSong() failed: " + e.getMessage());
                        if (e.getMessage() != null) {
                            CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    }
                    return null;
                }
        );
    }

    /**
     * 置顶歌曲
     *
     * @param songModel the song model
     */
    public void topUpSong(RoomSelSongModel songModel) {
        KTVLogger.d(TAG, "RoomLivingViewModel.topUpSong() called, name:" + songModel.getName());
        ktvServiceProtocol.makeSongTop(new MakeSongTopInputModel(
                songModel.getSongNo()
        ), e -> {
            if (e == null) {
                // success: do nothing for subscriber dealing with the event already
                KTVLogger.d(TAG, "RoomLivingViewModel.topUpSong() success");
            } else {
                // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.topUpSong() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
            return null;
        });
    }

    /**
     * 点击加入合唱
     */
    public void joinChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus() called");
        if (mRtcEngine.getConnectionState() != getValue(CONNECTION_STATE_CONNECTED)) {
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
            return;
        }

        RoomSelSongModel musicModel = songPlayingLiveData.getValue();
        if (musicModel == null) {
            KTVLogger.e(TAG, "RoomLivingViewModel.joinChorus() failed, no song playing now");
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
            return;
        }
        if (!isOnSeat) {
            // 不在麦上， 自动上麦
            ktvServiceProtocol.autoOnSeat(err -> {
                if (err == null) {
                    isOnSeat = true;
                    //自动开麦
                    mainChannelMediaOption.publishMicrophoneTrack = true;
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                    mRtcEngine.updateChannelMediaOptions(mainChannelMediaOption);
                    innerJoinChorus(musicModel.getSongNo());
                } else {
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
                }
                return null;
            });
        } else {
            // 在麦上，直接加入合唱
            innerJoinChorus(musicModel.getSongNo());
        }
    }

    /**
     * 加入合唱
     * @param songCode
     */
    private void innerJoinChorus(String songCode) {
        ktvApiProtocol.loadMusic(Long.parseLong(songCode), new KTVLoadMusicConfiguration(songCode,
                        Integer.parseInt(songPlayingLiveData.getValue().getUserNo()), KTVLoadMusicMode.LOAD_MUSIC_ONLY, false),
                new IMusicLoadStateListener() {
            @Override
            public void onMusicLoadProgress(long songCode, int percent, @NonNull MusicLoadStatus status, @Nullable String msg, @Nullable String lyricUrl) {
                KTVLogger.d(TAG, "onMusicLoadProgress, songCode: " + songCode + " percent: " + percent + " lyricUrl: " + lyricUrl);
                loadMusicProgressLiveData.postValue(percent);
            }

            @Override
            public void onMusicLoadFail(long songCode, @NonNull KTVLoadMusicFailReason reason) {
                joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
            }

            @Override
            public void onMusicLoadSuccess(long songCode, @NonNull String lyricUrl) {
                ktvApiProtocol.switchSingerRole(KTVSingRole.CoSinger, new ISwitchRoleStateListener() {
                    @Override
                    public void onSwitchRoleFail(@NonNull SwitchRoleFailReason reason) {
                        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
                    }

                    @Override
                    public void onSwitchRoleSuccess() {
                        if (isOnSeat) {
                            // 成为合唱成功
                            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_CHORUS);

                            // 麦位UI 同步
                            ktvServiceProtocol.joinChorus(songPlayingLiveData.getValue(), e -> {
                                if (e == null) {
                                    // success
                                    KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus() success");

                                } else {
                                    // failure
                                    KTVLogger.e(TAG, "RoomLivingViewModel.joinChorus() failed: " + e.getMessage());
                                    if (e.getMessage() != null) {
                                        CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                                    }
                                }
                                return null;
                            });

                        } else {
                            ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null);
                            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED);
                        }
                    }
                });
            }
        });
    }

    /**
     * 退出合唱
     */
    public void leaveChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus() called");
        if (isOnSeat) {
            ktvServiceProtocol.leaveChorus(e -> {
                if (e == null) {
                    // success
                    KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus() called");
                    ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null);
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS);
                } else {
                    // failure
                    KTVLogger.e(TAG, "RoomLivingViewModel.leaveChorus() failed: " + e.getMessage());
                    if (e.getMessage() != null) {
                        CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                    }
                }
                return null;
            });
        } else {
            ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null);
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS);
        }
    }

    /**
     * 开始切歌
     */
    public void changeMusic() {
        KTVLogger.d(TAG, "RoomLivingViewModel.changeMusic() called");
        RoomSelSongModel musicModel = songPlayingLiveData.getValue();
        if (musicModel == null) {
            KTVLogger.e(TAG, "RoomLivingViewModel.changeMusic() failed, no song is playing now!");
            return;
        }

        //ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, "", null);

        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_CHANGING_START);
        ktvServiceProtocol.removeSong(true, new RemoveSongInputModel(
                musicModel.getSongNo()
        ), e -> {
            if (e == null) {
                // success do nothing for dealing in song subscriber
                KTVLogger.d(TAG, "RoomLivingViewModel.changeMusic() success");
                playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_CHANGING_END);
            } else {
                // failed
                KTVLogger.e(TAG, "RoomLivingViewModel.changeMusic() failed: " + e.getMessage());
                if (e.getMessage() != null) {
                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                }
                playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_CHANGING_END);
            }
            return null;
        });
    }

    /**
     * 设置歌词view
     *
     * @param view the view
     */
    public void setLrcView(ILrcView view) {
        ktvApiProtocol.setLrcView(view);
        if (mSetting != null) {
            ktvApiProtocol.enableProfessionalStreamerMode(mSetting.getMProfessionalModeEnable());
        }
    }

    // ======================= Player/RTC/MPK相关 =======================
    // ------------------ 初始化音乐播放设置面版 ------------------
    private void initSettings() {
        // debug 设置
        mDebugSetting = new KTVDebugSettingBean(new KTVDebugSettingsDialog.Callback() {
            @Override
            public void onAudioDumpEnable(boolean enable) {
                if (enable) {
                    mRtcEngine.setParameters("{\"rtc.debug.enable\": true}");
                    mRtcEngine.setParameters("{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\",\"max_size_bytes\":\"120000000\",\"uuid\":\"123456789\",\"duration\":\"1200000\"}}");
                } else {
                    mRtcEngine.setParameters("{\"rtc.debug.enable\": false}");
                }
            }

            @Override
            public void onScoringControl(int level, int offset) {
                scoringAlgoControlLiveData.postValue(new ScoringAlgoControlModel(level, offset));
            }

            @Override
            public void onSetParameters(String parameters) {
                mRtcEngine.setParameters(parameters);
            }
        });

        // 音乐设置
        mSetting = new MusicSettingBean(new MusicSettingCallback() {
            @Override
            public void onEarChanged(boolean earBackEnable) {
                KTVLogger.d(TAG, "onEarChanged: " + earBackEnable);
                if (seatLocalLiveData.getValue() == null) return;
                int isMuted = seatLocalLiveData.getValue().isAudioMuted();
                if (isMuted == 1) {
                    isOpnEar = earBackEnable;
                    return;
                }
                if (mRtcEngine != null) {
                    mRtcEngine.enableInEarMonitoring(earBackEnable, Constants.EAR_MONITORING_FILTER_NONE);
                }
            }

            @Override
            public void onEarBackVolumeChanged(int volume) {
                KTVLogger.d(TAG, "onEarBackVolumeChanged: " + volume);
                mRtcEngine.setInEarMonitoringVolume(volume);
            }

            @Override
            public void onEarBackModeChanged(int mode) {
                KTVLogger.d(TAG, "onEarBackModeChanged: " + mode);
                if (mode == 1) {
                    // OpenSL
                    mRtcEngine.setParameters("{\"che.audio.opensl.mode\": 0}");
                } else if (mode == 2) {
                    // Oboe
                    mRtcEngine.setParameters("{\"che.audio.oboe.enable\": true}");
                }
            }

            @Override
            public void onMicVolChanged(int vol) {
                setMicVolume(vol);
            }

            @Override
            public void onAccVolChanged(int vol) {
                setMusicVolume(vol);
            }

            @Override
            public void onRemoteVolChanged(int volume) {
                KTVApi.Companion.setRemoteVolume(volume);
                mRtcEngine.adjustPlaybackSignalVolume(volume);
            }

            @Override
            public void onAudioEffectChanged(int audioEffect) {
                KTVLogger.d(TAG, "onAudioEffectChanged: " + audioEffect);
                mRtcEngine.setAudioEffectPreset(audioEffect);
            }

            // TODO: 2024/1/17 打分难度
            @Override
            public void onScoringDifficultyChanged(int difficulty) {
                KTVLogger.d(TAG, "onScoringDifficultyChanged: " + difficulty);
                scoringAlgoLiveData.postValue(difficulty);
            }

            @Override
            public void onProfessionalModeChanged(boolean enable) {
                KTVLogger.d(TAG, "onProfessionalModeChanged: " + enable);
                ktvApiProtocol.enableProfessionalStreamerMode(enable);
            }

            @Override
            public void onMultiPathChanged(boolean enable) {
                KTVLogger.d(TAG, "onMultiPathChanged: " + enable);
                ktvApiProtocol.enableMulitpathing(enable);
            }

            @Override
            public void onAECLevelChanged(int level) {
                KTVLogger.d(TAG, "onAECLevelChanged: " + level);
                // aiaec关闭的情况下音质选项才能生效
                if (level == 0) {
                    mRtcEngine.setParameters("{\"che.audio.aec.split_srate_for_48k\": 16000}");
                } else if (level == 1) {
                    mRtcEngine.setParameters("{\"che.audio.aec.split_srate_for_48k\": 24000}");
                } else if (level == 2) {
                    mRtcEngine.setParameters("{\"che.audio.aec.split_srate_for_48k\": 48000}");
                }
            }

            @Override
            public void onLowLatencyModeChanged(boolean enable) {
                KTVLogger.d(TAG, "onLowLatencyModeChanged: " + enable);
                if (enable) {
                    mRtcEngine.setParameters("{\"che.audio.ains_mode\": -1}");
                } else {
                    mRtcEngine.setParameters("{\"che.audio.ains_mode\": 0}");
                }
            }


            @Override
            public void onAINSModeChanged(int mode) {
                KTVLogger.d(TAG, "onAINSModeChanged: " + mode);
                if (mode == 0) {
                    // 关闭
                    mRtcEngine.setParameters("{\"che.audio.ains_mode\": 0}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerBound\": 80}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerMask\": 50}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.statisticalbound\": 5}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.finallowermask\": 30}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}");
                } else if (mode == 1) {
                    // 中
                    mRtcEngine.setParameters("{\"che.audio.ains_mode\": 2}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerBound\": 80}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerMask\": 50}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.statisticalbound\": 5}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.finallowermask\": 30}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}");
                } else if (mode == 2) {
                    // 高
                    mRtcEngine.setParameters("{\"che.audio.ains_mode\": 2}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerBound\": 10}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.lowerMask\": 10}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.statisticalbound\": 0}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.finallowermask\": 8}");
                    mRtcEngine.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}");
                }
            }

            @Override
            public void onAIAECChanged(boolean enable) {
                KTVLogger.d(TAG, "onAIAECChanged: " + enable);
                if (enable) {
                    mRtcEngine.setParameters("{\"che.audio.aiaec.working_mode\": 1}");
                } else {
                    mRtcEngine.setParameters("{\"che.audio.aiaec.working_mode\": 0}");
                }
            }

            @Override
            public void onAIAECStrengthSelect(int strength) {
                KTVLogger.d(TAG, "onAIAECStrengthSelect: " + strength);
                mRtcEngine.setParameters("{\"che.audio.aiaec.postprocessing_strategy\":" + strength + "}");
            }
        });

        mSoundCardSettingBean = new SoundCardSettingBean((presetValue, gainValue, gender, effect) -> {
            mRtcEngine.setParameters("{\"che.audio.virtual_soundcard\":{\"preset\":" + presetValue +
                    ",\"gain\":" + gainValue +
                    ",\"gender\":" + gender +
                    ",\"effect\":" + effect + "}}");
            return null;
        });
    }

    private void initRTCPlayer() {
        if (TextUtils.isEmpty(BuildConfig.AGORA_APP_ID)) {
            throw new NullPointerException("please check \"strings_config.xml\"");
        }
        if (mRtcEngine != null) return;

        // ------------------ 初始化RTC ------------------
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = AgoraApplication.the();
        config.mAppId = BuildConfig.AGORA_APP_ID;
        config.mEventHandler = new IRtcEngineEventHandler() {
            @Override
            public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
                // 网络状态回调, 本地user uid = 0
                if (uid == 0) {
                    networkStatusLiveData.postValue(new NetWorkEvent(txQuality, rxQuality));
                }
            }

            @Override
            public void onContentInspectResult(int result) {
                super.onContentInspectResult(result);
                if (result > 1) {
                    CustomToast.show(R.string.ktv_content, Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onStreamMessage(int uid, int streamId, byte[] data) {
                JSONObject jsonMsg;
                try {
                    String strMsg = new String(data);
                    jsonMsg = new JSONObject(strMsg);
                    if (jsonMsg.getString("cmd").equals("singleLineScore")) {
                        int score = jsonMsg.getInt("score");
                        int index = jsonMsg.getInt("index");
                        int cumulativeScore = jsonMsg.getInt("cumulativeScore");
                        int total = jsonMsg.getInt("total");

                        LineScore lineScore = new LineScore();
                        lineScore.score = score;
                        lineScore.index = index;
                        lineScore.cumulativeScore = cumulativeScore;
                        lineScore.total = total;
                        mainSingerScoreLiveData.postValue(lineScore);
                    } else if (jsonMsg.getString("cmd").equals("SingingScore")) {
                        float score = (float) jsonMsg.getDouble("score");
                        playerMusicPlayCompleteLiveData.postValue(new ScoringAverageModel(false, (int) score));
                    }
                } catch (JSONException exp) {
                    KTVLogger.e(TAG, "onStreamMessage:" + exp);
                }
            }

            @Override
            public void onAudioRouteChanged(int routing) { // 0\2\5 earPhone
                super.onAudioRouteChanged(routing);
                KTVLogger.d(TAG, "onAudioRouteChanged, routing:" + routing);
                if (mSetting == null) return;
                if (routing == 0 || routing == 2 || routing == 5 || routing == 6) {
                    mSetting.setMHasEarPhone(true);
                } else {
                    if (songPlayingLiveData.getValue() != null && mSetting.getMEarBackEnable()) {
                        CustomToast.show(R.string.ktv_earphone_close_tip, Toast.LENGTH_SHORT);
                        mSetting.setMEarBackEnable(false);
                    }
                    mSetting.setMHasEarPhone(false);
                }
            }

            @Override
            public void onLocalAudioStats(LocalAudioStats stats) {
                super.onLocalAudioStats(stats);
                if (mSetting == null) return;
                mSetting.setMEarBackDelay(stats.earMonitorDelay);
            }

            @Override
            public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
                super.onAudioVolumeIndication(speakers, totalVolume);
                for (AudioVolumeInfo speaker : speakers) {
                    volumeLiveData.postValue(new VolumeModel(speaker.uid, speaker.volume));
                }
            }
        };
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        config.mAudioScenario = Constants.AUDIO_SCENARIO_GAME_STREAMING;
        config.addExtension("agora_ai_echo_cancellation_extension");
        config.addExtension("agora_ai_noise_suppression_extension");
        try {
            mRtcEngine = (RtcEngineEx) RtcEngine.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            KTVLogger.e(TAG, "RtcEngine.create() called error: " + e);
        }
        mRtcEngine.loadExtensionProvider("agora_drm_loader");

        mRtcEngine.setParameters("{\"che.audio.ains_mode\": -1}");
        mRtcEngine.setParameters("{\"che.audio.input_sample_rate\" : 48000}");

        // ------------------ 场景化api初始化 ------------------
        KTVApi.Companion.setDebugMode(AgoraApplication.the().isDebugModeOpen());
        if (AgoraApplication.the().isDebugModeOpen()) {
            KTVApi.Companion.setMccDomain("api-test.agora.io");
        }
        ktvApiProtocol = createKTVApi(new KTVApiConfig(
                BuildConfig.AGORA_APP_ID,
                roomInfoLiveData.getValue().getAgoraRTMToken(),
                mRtcEngine,
                roomInfoLiveData.getValue().getRoomNo(),
                UserManager.getInstance().getUser().id.intValue(),
                roomInfoLiveData.getValue().getRoomNo() + "_ex",
                roomInfoLiveData.getValue().getAgoraChorusToken(), 10,
                KTVType.Normal,
                KTVMusicType.SONG_CODE)
        );

        ktvApiProtocol.addEventHandler(new IKTVApiEventHandler() {
               @Override
               public void onMusicPlayerStateChanged(@NonNull io.agora.mediaplayer.Constants.MediaPlayerState state, io.agora.mediaplayer.Constants.MediaPlayerError error, boolean isLocal) {
                   KTVLogger.d(TAG, "onMusicPlayerStateChanged, state:" + state + " error:" + error + " isLocal:" + isLocal);
                   switch (state) {
                       case PLAYER_STATE_OPEN_COMPLETED:
                           playerMusicOpenDurationLiveData.postValue(ktvApiProtocol.getMediaPlayer().getDuration());
                           break;
                       case PLAYER_STATE_PLAYING:
                           playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING);
                           runOnMainThread(() -> {
                               if (mSetting != null) {
                                   // 若身份是主唱和伴唱，在演唱时，人声音量、伴泰音量保持原先设置，远端音量自动切为30
                                   mSetting.setMRemoteVolume(MusicSettingBean.DEFAULT_REMOTE_SINGER_VOL);
                                   //主唱/合唱 开始唱歌: 默认关闭 aiaec
                                   mSetting.setMAIAECEnable(false);
                               }
                           });
                           break;
                       case PLAYER_STATE_PAUSED:
                           playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PAUSE);
                           // 若身份是主唱和伴唱，演唱暂停/切歌，人声音量、伴奏音量保持原先设置，远端音量自动转为100
                           runOnMainThread(() -> {
                               if (mSetting != null) mSetting.setMRemoteVolume(MusicSettingBean.DEFAULT_REMOTE_VOL);
                           });
                           break;
                       case PLAYER_STATE_STOPPED:
                           playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_STOP);

                           runOnMainThread(() -> {
                               if (mSetting != null) {
                                   // 若身份是主唱和伴唱，演唱暂停/切歌，人声音量、伴奏音量保持原先设置，远端音量自动转为100
                                   mSetting.setMRemoteVolume(MusicSettingBean.DEFAULT_REMOTE_VOL);
                                   // 主唱/合唱 歌曲结束/退出合唱: 默认开启 aiaec, 强度为1
                                   mSetting.setMAIAECEnable(true);
                                   mSetting.setMAIAECStrength(MusicSettingBean.DEFAULT_AIAEC_STRENGTH);
                               }
                           });
                           break;
                       case PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED:
                           if (isLocal) {
                               playerMusicPlayCompleteLiveData.postValue(new ScoringAverageModel(true, 0));
                               playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_LRC_RESET);
                           }
                           break;
                       default:
                   }
               }
           }
        );

        if (isRoomOwner()) {
            ktvApiProtocol.muteMic(false);
            isOnSeat = true;
        }

        // ------------------ 加入频道 ------------------
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.enableVideo();
        mRtcEngine.enableLocalVideo(false);
        mRtcEngine.enableAudio();
        mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_GAME_STREAMING);
        mRtcEngine.enableAudioVolumeIndication(50, 10, true);
        mRtcEngine.setClientRole(isOnSeat ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE);
        mRtcEngine.setAudioEffectPreset(ROOM_ACOUSTICS_KTV);
        int ret = mRtcEngine.joinChannel(
                roomInfoLiveData.getValue().getAgoraRTCToken(),
                roomInfoLiveData.getValue().getRoomNo(),
                null,
                UserManager.getInstance().getUser().id.intValue()
        );
        if (ret != Constants.ERR_OK) {
            KTVLogger.e(TAG, "joinRTC() called error: " + ret);
        }

        // ------------------ 开启鉴黄服务 ------------------
        ContentInspectConfig contentInspectConfig = new ContentInspectConfig();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sceneName", "ktv");
            jsonObject.put("id", UserManager.getInstance().getUser().id.toString());
            jsonObject.put("userNo", UserManager.getInstance().getUser().userNo);
            contentInspectConfig.extraInfo = jsonObject.toString();
            ContentInspectConfig.ContentInspectModule module = new ContentInspectConfig.ContentInspectModule();
            module.interval = 30;
            module.type = CONTENT_INSPECT_TYPE_MODERATION;
            contentInspectConfig.modules = new ContentInspectConfig.ContentInspectModule[]{module};
            contentInspectConfig.moduleCount = 1;
            mRtcEngine.enableContentInspect(true, contentInspectConfig);
        } catch (JSONException e) {
            KTVLogger.e(TAG, e.toString());
        }

        // ------------------ 开启语音鉴定服务 ------------------
        AudioModeration.INSTANCE.moderationAudio(
                roomInfoLiveData.getValue().getRoomNo(),
                UserManager.getInstance().getUser().id,
                AudioModeration.AgoraChannelType.rtc,
                "ktv",
                null,
                null
        );

        // 外部使用的StreamId
        if (streamId == 0) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = false;
            streamId = mRtcEngine.createDataStream(cfg);
        }
    }

    // ======================= settings =======================
    // ------------------ 音量调整 ------------------
    private int micOldVolume = 100;

    private void setMusicVolume(int v) {
        ktvApiProtocol.getMediaPlayer().adjustPlayoutVolume(v);
        ktvApiProtocol.getMediaPlayer().adjustPublishSignalVolume(v);
    }

    private void setMicVolume(int v) {
        RoomSeatModel value = seatLocalLiveData.getValue();
        int isMuted = value == null ? RoomSeatModel.Companion.getMUTED_VALUE_TRUE() : value.isAudioMuted();
        if (isMuted == RoomSeatModel.Companion.getMUTED_VALUE_TRUE()) {
            KTVLogger.d(TAG, "muted! setMicVolume: " + v);
            micOldVolume = v;
            return;
        }
        KTVLogger.d(TAG, "unmute! setMicVolume: " + v);
        if (mRtcEngine != null) {
            mRtcEngine.adjustRecordingSignalVolume(v);
        }
    }

    /**
     * Music toggle original.
     *
     * @param audioTrack the audio track
     */
// ------------------ 原唱/伴奏 ------------------
    public void musicToggleOriginal(LrcControlView.AudioTrack audioTrack) {
        KTVLogger.d("musicToggleOriginal called, ", "aim: " + audioTrack);
        ktvApiProtocol.switchAudioTrack(getAudioTrackMode(audioTrack));
    }

    private AudioTrackMode getAudioTrackMode(LrcControlView.AudioTrack audioTrack) {
        if (audioTrack == LrcControlView.AudioTrack.Acc) {
            return AudioTrackMode.BAN_ZOU;
        } else if (audioTrack == LrcControlView.AudioTrack.DaoChang) {
            return AudioTrackMode.DAO_CHANG;
        } else {
            return AudioTrackMode.YUAN_CHANG;
        }
    }

    /**
     * Music toggle start.
     */
// ------------------ 暂停/播放 ------------------
    public void musicToggleStart() {
        if (playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PLAYING) {
            ktvApiProtocol.pauseSing();
        } else if (playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PAUSE) {
            ktvApiProtocol.resumeSing();
        }
    }

    /**
     * Render local camera video.
     *
     * @param surfaceView the surface view
     */
// ------------------ 本地视频渲染 ------------------
    public void renderLocalCameraVideo(SurfaceView surfaceView) {
        if (mRtcEngine == null) return;
        mRtcEngine.startPreview();
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0));
    }

    /**
     * Render remote camera video.
     *
     * @param surfaceView the surface view
     * @param uid         the uid
     */
// ------------------ 远端视频渲染 ------------------
    public void renderRemoteCameraVideo(SurfaceView surfaceView, int uid) {
        if (mRtcEngine == null) return;
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid));
    }

    /**
     * Reset music status.
     */
// ------------------ 重置歌曲状态(歌曲切换时) ------------------
    public void resetMusicStatus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.resetMusicStatus() called");
        chorusNum = 0;
        retryTimes = 0;
        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_IDLE);
        ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null);
    }

    // ------------------ 歌曲开始播放 ------------------
    private int retryTimes = 0;

    /**
     * Music start play.
     *
     * @param music the music
     */
    public void musicStartPlay(@NonNull RoomSelSongModel music) {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStartPlay() called");
        if (music.getUserNo() == null) return;
        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PREPARE);

        boolean isOwnSong = Objects.equals(music.getUserNo(), UserManager.getInstance().getUser().id.toString());
        long songCode = Long.parseLong(music.getSongNo());
        int mainSingerUid = Integer.parseInt(music.getUserNo());
        if (isOwnSong) {
            // 主唱加载歌曲
            loadMusic(new KTVLoadMusicConfiguration(music.getSongNo(), mainSingerUid,
                    KTVLoadMusicMode.LOAD_MUSIC_AND_LRC,false), songCode, true);
        } else {
            if (seatLocalLiveData.getValue() != null && seatLocalLiveData.getValue().getChorusSongCode().equals(music.getSongNo() + music.getCreateAt())) {
                // 合唱者
                loadMusic(new KTVLoadMusicConfiguration(music.getSongNo(), mainSingerUid,
                        KTVLoadMusicMode.LOAD_LRC_ONLY,false), songCode, false);
                // 加入合唱
                innerJoinChorus(music.getSongNo());
            } else {
                // 观众
                loadMusic(new KTVLoadMusicConfiguration(music.getSongNo(), mainSingerUid,
                        KTVLoadMusicMode.LOAD_LRC_ONLY,false), songCode, false);
            }
        }

        // 标记歌曲为播放中
        ktvServiceProtocol.makeSongDidPlay(music, e -> {
            if (e != null && e.getMessage() != null) {
                // failure
                CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
            }
            return null;
        });
    }

    private void loadMusic(KTVLoadMusicConfiguration config, Long songCode, Boolean isOwnSong) {
        ktvApiProtocol.loadMusic(songCode, config, new IMusicLoadStateListener() {
            @Override
            public void onMusicLoadProgress(long songCode, int percent, @NonNull MusicLoadStatus status, @Nullable String msg, @Nullable String lyricUrl) {
                KTVLogger.d(TAG, "onMusicLoadProgress, songCode: " + songCode + " percent: " + percent + " lyricUrl: " + lyricUrl);
                loadMusicProgressLiveData.postValue(percent);
            }

            @Override
            public void onMusicLoadSuccess(long songCode, @NonNull String lyricUrl) {
                // 当前已被切歌
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG);
                    return;
                }

                if (isOwnSong) {
                    // 需要判断此时是否有合唱者，如果有需要切换成LeaderSinger身份
                    if (chorusNum == 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null);
                    } else if (chorusNum > 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null);
                    }
                    ktvApiProtocol.startSing(songCode, 0);
                }

                // 重置settings
                retryTimes = 0;
                playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING);
            }

            @Override
            public void onMusicLoadFail(long songCode, @NonNull KTVLoadMusicFailReason reason) {
                // 当前已被切歌
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG);
                    return;
                }

                KTVLogger.e(TAG, "onMusicLoadFail， reason: " + reason);
                if (reason == KTVLoadMusicFailReason.NO_LYRIC_URL) {
                    // 未获取到歌词 正常播放
                    retryTimes = 0;
                    playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING);
                    noLrcLiveData.postValue(true);
                } else if (reason == KTVLoadMusicFailReason.MUSIC_PRELOAD_FAIL) {
                    // 歌曲加载失败 ，重试3次
                    CustomToast.show(R.string.ktv_load_failed, Toast.LENGTH_LONG);
                    retryTimes = retryTimes + 1;
                    if (retryTimes < 3) {
                        loadMusic(config, songCode, isOwnSong);
                    } else {
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING);
                        CustomToast.show(R.string.ktv_try, Toast.LENGTH_LONG);
                    }
                } else if (reason == KTVLoadMusicFailReason.CANCELED) {
                    // 当前已被切歌
                    CustomToast.show(R.string.ktv_load_failed_another_song, Toast.LENGTH_LONG);
                }
            }
        });
    }

    /**
     * Re get lrc url.
     */
// ------------------ 重新获取歌词url ------------------
    public void reGetLrcUrl() {
        if (songPlayingLiveData.getValue() == null) return;
        boolean isOwnSong = Objects.equals(songPlayingLiveData.getValue().getUserNo(), UserManager.getInstance().getUser().id.toString());
        loadMusic(new KTVLoadMusicConfiguration(songPlayingLiveData.getValue().getSongNo(),
                Integer.parseInt(songPlayingLiveData.getValue().getUserNo()), KTVLoadMusicMode.LOAD_LRC_ONLY,false),
                Long.parseLong(songPlayingLiveData.getValue().getSongNo()), isOwnSong);
    }

    /**
     * Music seek.
     *
     * @param time the time
     */
// ------------------ 歌曲seek ------------------
    public void musicSeek(long time) {
        ktvApiProtocol.seekSing(time);
    }

    /**
     * Gets song duration.
     *
     * @return the song duration
     */
    public Long getSongDuration() {
        return ktvApiProtocol.getMediaPlayer().getDuration();
    }

    /**
     * Music stop.
     */
// ------------------ 歌曲结束播放 ------------------
    public void musicStop() {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStop() called");
        // 列表中无歌曲， 还原状态
        resetMusicStatus();
    }

    /**
     * On start.
     */
    public void onStart() {
        if (isBackPlay) {
            ktvApiProtocol.getMediaPlayer().mute(false);
        }
    }

    /**
     * On stop.
     */
    public void onStop() {
        if (isBackPlay) {
            ktvApiProtocol.getMediaPlayer().mute(true);
        }
    }

    /**
     * Sync single line score.
     *
     * @param score           the score
     * @param cumulativeScore the cumulative score
     * @param index           the index
     * @param total           the total
     */
// ------------------ 歌词组件相关 ------------------
    public void syncSingleLineScore(int score, int cumulativeScore, int index, int total) {
        if (mRtcEngine == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "singleLineScore");
        msg.put("score", score);
        msg.put("index", index);
        msg.put("cumulativeScore", cumulativeScore);
        msg.put("total", total);
        JSONObject jsonMsg = new JSONObject(msg);
        int ret = mRtcEngine.sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            KTVLogger.e(TAG, "syncSingleLineScore() sendStreamMessage called returned: " + ret);
        }
    }

    /**
     * Sync singing average score.
     *
     * @param score the score
     */
    public void syncSingingAverageScore(double score) {
        if (mRtcEngine == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "SingingScore");
        msg.put("score", score);
        JSONObject jsonMsg = new JSONObject(msg);
        int ret = mRtcEngine.sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            KTVLogger.e(TAG, "syncSingingAverageScore() sendStreamMessage called returned: " + ret);
        }
    }
}