package health.epley.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * How the phone is being held.
 *
 * ## Why this is a choice and not an assumption
 *
 * Every published sensor-guided repositioning study straps an IMU to the head with dedicated
 * hardware. We cannot ship hardware, and we cannot assume a headband: most people trying this at
 * 3am with the room spinning own no such thing, and neither does a reviewer opening the app for
 * three minutes. So the mount is a first-class choice, each option states honestly what it costs
 * in accuracy, and the tolerance band widens to match. Pretending a hand-held phone is as rigid as
 * a strapped one would be the dishonest option; refusing to support it would be the useless one.
 *
 * ## Why the cheek hold is the default
 *
 * The cheekbone is bone-backed and flat. A palm pressing a phone against it couples to the skull
 * almost as well as a strap, and the Epley is performed lying down with both hands free. It is the
 * only mount that requires nothing at all and still tracks the head.
 *
 * ## Why [IN_HAND] exists
 *
 * It is not a therapeutic mode. The phone stands in for the head: tilt it and the whole guided
 * sequence runs. That is how someone learns the four positions before trying them on their own
 * neck, and it is the only way a reviewer can see the app work without lying on a bed.
 *
 * @property toleranceDegrees how far from a target pose still counts as being in position
 * @property phoneForwardHint the phone-frame axis pointing roughly where the face points, used to
 *   resolve the mounting transform at calibration
 * @property tracksTheHead false for [IN_HAND], where the reading describes the phone and says
 *   nothing about the user's head — the one distinction that must never be blurred in the UI
 */
/**
 * What to say out loud when asking someone to get into the mount position.
 *
 * Separate from [MountMode.instruction], which is written to be read. This is written to be heard
 * with the eyes shut and the phone already moving toward a cheek, so it is shorter and it names
 * the action first.
 */
val MountMode.spokenSetupPrompt: String
    get() = when (this) {
        MountMode.CHEEK -> "Put the phone flat against your cheekbone now, screen facing out, " +
            "top of the phone toward the top of your head. Then sit up straight and hold still."
        MountMode.HEADBAND -> "Put the phone in the headband now, screen facing out. Then sit up " +
            "straight and hold still."
        MountMode.IN_HAND -> "Hold the phone upright in front of you, screen toward your face, " +
            "and keep it still."
    }

enum class MountMode(
    val displayName: String,
    val instruction: String,
    val toleranceDegrees: Double,
    val phoneForwardHint: Vector3,
    val tracksTheHead: Boolean,
) {

    /** Phone held flat against the cheekbone by one hand. No equipment. */
    CHEEK(
        displayName = "Against your cheek",
        instruction = "Hold the phone flat against your cheekbone, screen facing out, top of the " +
            "phone toward the top of your head.",
        // Wider than HEADBAND because a hand can creep over a sixty second hold. Still several
        // times tighter than the 40-51 degree error people make following a video with no
        // feedback, which is the accuracy that actually has to be beaten for this to be worth
        // anything.
        toleranceDegrees = 7.0,
        phoneForwardHint = Vector3(-1.0, 0.0, 0.0),
        tracksTheHead = true,
    ),

    /** Phone tucked into a headband, cap, or anything elastic. Best accuracy. */
    HEADBAND(
        displayName = "In a headband or cap",
        instruction = "Tuck the phone into a headband, a cap, or a folded scarf against the side " +
            "of your head, screen facing out.",
        toleranceDegrees = 5.0,
        phoneForwardHint = Vector3(-1.0, 0.0, 0.0),
        tracksTheHead = true,
    ),

    /** Phone held in front of you, standing in for your head. Practice and demonstration. */
    IN_HAND(
        displayName = "In your hand (practice)",
        instruction = "Hold the phone upright in front of you, screen toward your face. The phone " +
            "stands in for your head — tip it back and turn it to follow each step.",
        // Generous: nobody is being treated here, and a practice run that is hard to complete
        // teaches nothing.
        toleranceDegrees = 10.0,
        phoneForwardHint = Vector3(0.0, 0.0, 1.0),
        tracksTheHead = false,
    );

    companion object {
        /** What a first-time user gets before they have chosen anything. */
        val DEFAULT = CHEEK
    }
}

/**
 * Watches for the phone moving relative to the head rather than with it.
 *
 * A single orientation sensor cannot separate "the head turned" from "the phone slid" — both are
 * the same rotation, and no amount of filtering recovers the difference. What it can separate is
 * speed. A slipping phone is a discontinuity; a neck is not. Voluntary head rotation peaks around
 * 300 deg/s in a violent turn and stays under roughly 60 deg/s during a guided manoeuvre, so a
 * spike past [JOLT_DEG_PER_SEC] is a hand losing grip, a phone dropping, or the sensor glitching.
 * None of those leave the calibration describing the head any more.
 *
 * Once jolted the monitor latches. Staying quiet after a jolt would mean continuing to report
 * angles against a reference that has stopped being true, which is the one failure mode that
 * produces confidently wrong numbers instead of obviously wrong ones.
 */
class MountMonitor {

    private var jolted = false
    private var joltCount = 0
    private var worstRate = 0.0

    /** True once a jolt has been seen, until [clear] is called by a recalibration. */
    val isJolted: Boolean get() = jolted

    /** How many jolts this session. Worth logging: it explains a run that looks wrong later. */
    val jolts: Int get() = joltCount

    /** Fastest rotation seen, for the record. */
    val peakRateDegPerSec: Double get() = worstRate

    /** Feed every sample. Returns true if this sample was itself a jolt. */
    fun onSample(rateDegPerSec: Double): Boolean {
        val rate = abs(rateDegPerSec)
        worstRate = max(worstRate, rate)
        if (rate > JOLT_DEG_PER_SEC) {
            jolted = true
            joltCount++
            return true
        }
        return false
    }

    /** Called after the user recalibrates, which is the only thing that fixes a slipped mount. */
    fun clear() {
        jolted = false
    }

    companion object {
        /**
         * Rotation rate above which the mount is assumed to have moved, not the head.
         *
         * 250 deg/s sits a factor of four above the roughly 60 deg/s of a deliberately performed
         * manoeuvre, and below the several hundred a slipping or dropped phone reaches at once.
         */
        const val JOLT_DEG_PER_SEC = 250.0
    }
}

/**
 * Measures how far the mount has drifted, by checking it against a pose whose answer we know.
 *
 * The Epley returns the patient to sitting upright at the end of every cycle. Upright and
 * forward-facing is exactly the pose the calibration was captured in, so at that moment the true
 * reading is zero on both angles. Whatever the app actually reads is the accumulated error — gyro
 * drift, a hand that crept, a headband that shifted — and it is measured rather than assumed.
 *
 * This is the honesty mechanism for the whole mount question. It turns "we hope a hand-held phone
 * is rigid enough" into a number the user and the log can both see, once per cycle, for free.
 */
class MountResidual {

    private val checks = mutableListOf<UprightCheck>()

    /**
     * Record a return-to-upright and report the error found there.
     *
     * @param pose what the app currently believes, at a moment when the truth is (0, 0)
     */
    fun record(pose: HeadPose): UprightCheck {
        val check = UprightCheck(
            index = checks.size + 1,
            neckExtensionDegrees = pose.neckExtensionDegrees,
            headRotationDegrees = pose.headRotationDegrees,
        )
        checks += check
        return check
    }

    val count: Int get() = checks.size
    val last: UprightCheck? get() = checks.lastOrNull()
    val worstDegrees: Double get() = checks.maxOfOrNull { it.residualDegrees } ?: 0.0

    /**
     * Root mean square of the residuals.
     *
     * Reported instead of the mean because errors come in both signs, and a mean would let a
     * left-leaning cycle cancel a right-leaning one into a flattering zero.
     */
    val rmsDegrees: Double
        get() = if (checks.isEmpty()) {
            0.0
        } else {
            sqrt(checks.sumOf { it.residualDegrees * it.residualDegrees } / checks.size)
        }

    fun reset() = checks.clear()
}

/** One return-to-upright measurement: what the app read when the answer was known to be zero. */
data class UprightCheck(
    val index: Int,
    val neckExtensionDegrees: Double,
    val headRotationDegrees: Double,
) {

    /** The error, taken as the worse of the two angles. */
    val residualDegrees: Double get() = max(abs(neckExtensionDegrees), abs(headRotationDegrees))

    /**
     * Whether the mount is still good enough for the tolerance this mode promises.
     *
     * A residual as large as the tolerance band means a pose could read as in-position while
     * sitting a whole band outside it, at which point the guidance has stopped guiding. Half the
     * band is where we ask for a recalibration instead of carrying on.
     */
    fun isAcceptableFor(mode: MountMode): Boolean = residualDegrees <= mode.toleranceDegrees / 2.0
}
