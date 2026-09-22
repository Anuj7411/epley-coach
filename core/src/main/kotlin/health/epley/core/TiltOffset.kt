package health.epley.core

/**
 * A phone's own tilt error, measured and then subtracted.
 *
 * The guidance never needs this: every clinical angle is relative to a calibration taken on the
 * user's head, so a fixed device offset sits in both the reference and the reading and cancels
 * (see `SensorBiasTest`). Absolute readings are the exception — the accuracy self-check, and any
 * raw tilt shown to a human — and on the test device those were about 9° out.
 *
 * The correction is a single angle rather than a full rotation because the reversal measures a
 * single axis: the one the reading is taken along. That is exactly the axis the correction is
 * applied to.
 */
object TiltOffset {

    /**
     * The offset to store after a reversal check.
     *
     * Readings are already corrected by whatever is stored, so a fresh measurement finds only the
     * remaining error; adding it refines the stored value instead of throwing it away.
     */
    fun from(current: Double, measured: ReversalResult): Double = current + measured.sensorErrorDegrees

    /** Apply the stored offset to a raw reading. */
    fun apply(rawDegrees: Double, offsetDegrees: Double): Double = rawDegrees - offsetDegrees
}
