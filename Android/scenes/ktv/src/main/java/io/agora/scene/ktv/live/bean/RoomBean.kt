package io.agora.scene.ktv.live.bean

/**
 * The enum Player music status.
 */
enum class PlayerMusicStatus {
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
 * The enum Join chorus status.
 */
enum class JoinChorusStatus {
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
    ON_LEAVE_CHORUS
}

/**
 * The type Line score.
 */
class LineScore {
    /**
     * The Score.
     */
    var score = 0

    /**
     * The Index.
     */
    var index = 0

    /**
     * The Cumulative score.
     */
    var cumulativeScore = 0

    /**
     * The Total.
     */
    var total = 0
}

/**
 * Scoring algo control model
 *
 * @property level
 * @property offset
 * @constructor Create empty Scoring algo control model
 */
data class ScoringAlgoControlModel constructor(
    val level: Int,
    val offset: Int
)

/**
 * Scoring average model
 *
 * @property isLocal
 * @property score
 * @constructor Create empty Scoring average model
 */
data class ScoringAverageModel constructor(
    val isLocal: Boolean,
    val score: Int
)

/**
 * Volume model
 *
 * @property uid
 * @property volume
 * @constructor Create empty Volume model
 */
data class VolumeModel constructor(
    val uid: Int,
    val volume: Int
)