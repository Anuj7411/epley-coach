package health.epley.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.abs

/**
 * Converts a phone orientation into the two angles the Epley manoeuvre is actually specified in.
 *
 * ## The mounting transform
 *
 * The phone is not the head. It is held against the cheek or slipped into a headband at whatever
 * arbitrary orientation the user managed. So the first thing we do is capture a reference: the
 * user sits upright, facing forward, holds still, and we record the phone's orientation as
 * [MountCalibration]. Every angle afterwards is expressed relative to that reference.
 *
 * This has three useful consequences:
 *  - display rotation and device natural orientation become irrelevant
 *  - absolute heading becomes irrelevant, so we never need the magnetometer
 *  - a seated 45° head turn, which is pure yaw and therefore invisible to gravity, becomes a
 *    bounded gyro-integration problem over the ~60 seconds from calibration to supine, rather than
 *    an unbounded heading problem
 *
 * ## What each angle means clinically
 *
 * **Neck extension** is the angle of the head's forward axis below horizontal. The Epley's first
 * position calls for roughly 20–30° of head-hanging extension. Because it is measured against
 * gravity, this angle does not drift: the accelerometer supplies a permanent absolute reference
 * and sensor fusion bounds the error.
 *
 * **Head rotation** is how far the head is turned toward one ear, measured as the twist about the
 * body's long axis. The manoeuvre calls for 45°.
 */
object HeadAngles {

    /**
     * Compute both angles for a phone orientation, given the mounting reference.
     *
     * @param phone current phone orientation in the world frame
     * @param mount the reference captured while sitting upright and facing forward
     */
    fun compute(phone: Quaternion, mount: MountCalibration): HeadPose {
        // Rotation of the head relative to where it was at calibration.
        val relative = (mount.reference.inverse() * phone).normalized()

        // Where the head's forward axis now points, in the world frame.
        val forwardWorld = phone.rotate(mount.forwardInPhoneFrame).normalized()

        // Angle below horizontal. asin is single-valued over the full range and has no
        // discontinuity at vertical, which is exactly why we are not using Euler pitch.
        val extensionRadians = asin((forwardWorld dot Vector3.DOWN).coerceIn(-1.0, 1.0))

        // Head turn is the twist about the body's cranio-caudal axis. At calibration that axis
        // points up; we track it through the same relative rotation so it stays meaningful once
        // the person is lying down.
        val longAxis = mount.longAxisInPhoneFrame
        val decomposition = relative.swingTwist(longAxis)
        val twist = decomposition.twist
        val twistAxisSign = if ((Vector3(twist.x, twist.y, twist.z) dot longAxis) < 0) -1.0 else 1.0
        val rotationRadians = twistAxisSign * twist.angleRadians

        // Swing-twist becomes ill-conditioned as the swing approaches half a turn: the component
        // of rotation about the twist axis shrinks toward nothing, so a tiny change in the input
        // swings the reported twist wildly. Rather than emit a confident wrong number we report
        // the swing and let the caller decide.
        val swingDegrees = Math.toDegrees(decomposition.swing.angleRadians)

        return HeadPose(
            neckExtensionDegrees = Math.toDegrees(extensionRadians),
            headRotationDegrees = Math.toDegrees(normalizeSigned(rotationRadians)),
            swingDegrees = swingDegrees,
        )
    }

    /** Wrap an angle to (-pi, pi] so a 350° turn reads as -10°, not 350°. */
    internal fun normalizeSigned(radians: Double): Double {
        var a = radians
        while (a > Math.PI) a -= 2 * Math.PI
        while (a <= -Math.PI) a += 2 * Math.PI
        return a
    }
}

/**
 * The phone-to-head relationship, captured once while the user sits upright and still.
 *
 * [forwardInPhoneFrame] and [longAxisInPhoneFrame] are the head's anatomical axes expressed in the
 * phone's own coordinate frame. Because the phone is held flat against the side of the face, the
 * head's forward direction is approximately the phone's negative X axis and the head's long axis
 * approximately the phone's Y axis — but we do not assume that. We derive them from the calibration
 * orientation so that any mounting works.
 */
data class MountCalibration(
    val reference: Quaternion,
    val forwardInPhoneFrame: Vector3,
    val longAxisInPhoneFrame: Vector3,
    /**
     * How much of the forward hint survived being flattened into the horizontal plane, 0 to 1.
     *
     * Near 1 the phone was held the way the instructions asked and the forward direction is well
     * determined. Near 0 the hint was pointing almost straight up or down — a phone lying flat on
     * a bed, say — and the forward axis is whatever numerical noise happened to be left over. This
     * is the calibration equivalent of a division by something close to zero, and [isUsable] is
     * how a caller refuses it instead of building an entire session on it.
     */
    val hintHorizontality: Double = 1.0,
    /**
     * How close the screen is to vertical, 0 (flat) to 1 (upright).
     *
     * A separate question from [hintHorizontality], and the one that catches the mistake people
     * actually make. A phone lying face-up on a desk has a perfectly well determined forward axis
     * — its long edge points somewhere horizontal — so the forward check passes it happily. It is
     * still not on anyone's head. Every mount we support holds the screen roughly on edge: facing
     * out from the cheek, out from a headband, or back at the user in the hand. A screen pointing
     * at the ceiling means the phone is on a table, not a person.
     */
    val screenUprightness: Double = 1.0,
) {

    /**
     * Whether this calibration describes a phone actually being held the way the mode asks.
     *
     * Both conditions, because they fail independently: a phone can be upright with its face
     * pointing straight up (forward undetermined) or perfectly forward-facing while lying flat
     * (not on a head).
     */
    val isUsable: Boolean
        get() = hintHorizontality >= MIN_HINT_HORIZONTALITY &&
            screenUprightness >= MIN_SCREEN_UPRIGHTNESS

    companion object {
        /**
         * Below this the forward axis is not determined and the calibration must be rejected.
         *
         * 0.5 is a face pointing at least 30 degrees away from vertical. Someone sitting upright
         * and following the instruction scores close to 1.0, so this only fires when the phone is
         * held nearly flat — face-up on a bed, or tipped fully back — at which point asking for
         * the calibration again costs three seconds and saves the whole session.
         */
        const val MIN_HINT_HORIZONTALITY = 0.5

        /**
         * Below this the screen is too close to flat for the phone to be on a head or in a hand.
         *
         * 0.5 allows the screen to lean 60 degrees off vertical before the calibration is
         * refused, which is far more slop than any of the three holds needs and still rejects a
         * phone resting on a desk or a bed outright.
         */
        const val MIN_SCREEN_UPRIGHTNESS = 0.5

        /** Derive the mount for a given way of holding the phone. */
        fun fromUprightSample(phone: Quaternion, mode: MountMode): MountCalibration =
            fromUprightSample(phone, mode.phoneForwardHint)

        /**
         * Derive the mount from a single upright, forward-facing sample.
         *
         * At calibration we know two things about the world frame: the head's long axis points up,
         * and the head's forward axis is horizontal. Rotating those world-frame facts back through
         * the phone's orientation gives the same axes in the phone's frame, which is what we need
         * to keep tracking them once the person lies down.
         *
         * The forward direction is recovered from the phone's own forward-ish axis projected into
         * the horizontal plane. If the phone happens to be held exactly face-up the projection is
         * degenerate, and we fall back to a fixed axis rather than returning NaN — the caller
         * should reject that calibration and ask the user to try again.
         */
        fun fromUprightSample(
            phone: Quaternion,
            phoneForwardHint: Vector3 = Vector3(-1.0, 0.0, 0.0),
        ): MountCalibration {
            val inverse = phone.inverse()

            // Head long axis points up in the world at calibration time.
            val longAxis = inverse.rotate(Vector3.UP).normalized()

            // Take the hinted phone axis, flatten it into the horizontal plane, and bring it back.
            val hintWorld = phone.rotate(phoneForwardHint).normalized()
            val horizontal = Vector3(hintWorld.x, hintWorld.y, 0.0)

            // How much of the hint was actually horizontal. The hint is a unit vector, so this
            // falls straight out as a 0..1 quality score rather than needing a separate estimate.
            val horizontality = horizontal.length

            val forwardWorld = if (horizontality > 1e-3) {
                horizontal.normalized()
            } else {
                Vector3(1.0, 0.0, 0.0)
            }
            val forward = inverse.rotate(forwardWorld).normalized()

            // Where the screen is pointing. Its horizontal component is 1 when the screen is on
            // edge and 0 when it faces the ceiling or the floor.
            val screenNormalWorld = phone.rotate(Vector3(0.0, 0.0, 1.0)).normalized()
            val uprightness = Vector3(screenNormalWorld.x, screenNormalWorld.y, 0.0).length

            return MountCalibration(
                reference = phone.normalized(),
                forwardInPhoneFrame = forward,
                longAxisInPhoneFrame = longAxis,
                hintHorizontality = horizontality,
                screenUprightness = uprightness,
            )
        }
    }
}

/**
 * The head's current position, in the terms the manoeuvre is written in.
 *
 * Positive [neckExtensionDegrees] means the face is tilted back and down, which is the direction
 * the Epley's head-hanging position calls for. Positive [headRotationDegrees] is a turn to one
 * side; the sign convention is fixed by the calibration, not by anatomy, so the manoeuvre logic
 * decides which sign corresponds to the affected ear.
 */
data class HeadPose(
    val neckExtensionDegrees: Double,
    val headRotationDegrees: Double,
    /**
     * How far the head has swung away from the axis the rotation is measured about.
     *
     * Not a clinical quantity — it is the conditioning of the measurement. Near half a turn the
     * rotation reading stops meaning anything, and [isRotationReliable] says so.
     */
    val swingDegrees: Double = 0.0,
) {

    /**
     * Whether [headRotationDegrees] can be trusted for this sample.
     *
     * The Epley asks for 45 and 90 degrees of head turn with the neck extended 20 to 30 degrees,
     * so a well-performed manoeuvre never approaches the degenerate region. A recording that does
     * is either a torture test or a phone that has come off the head — and in both cases the
     * honest answer is to refuse the number, not to print it.
     */
    val isRotationReliable: Boolean
        get() = swingDegrees < MAX_RELIABLE_SWING_DEGREES
    /** How far this pose is from a target, as the larger of the two angular errors. */
    fun errorAgainst(target: TargetPose): Double = maxOf(
        abs(neckExtensionDegrees - target.neckExtensionDegrees),
        abs(headRotationDegrees - target.headRotationDegrees),
    )

    fun isWithin(target: TargetPose, toleranceDegrees: Double): Boolean =
        errorAgainst(target) <= toleranceDegrees
}

/**
 * Swing beyond this and the twist decomposition is too ill-conditioned to report.
 *
 * 150 degrees leaves generous headroom over the roughly 110 degrees of combined swing a correctly
 * performed Epley produces, while still catching the flip that broke the first hardware recording.
 */
const val MAX_RELIABLE_SWING_DEGREES = 150.0

/** A position the manoeuvre asks the head to reach and hold. */
data class TargetPose(
    val neckExtensionDegrees: Double,
    val headRotationDegrees: Double,
)
