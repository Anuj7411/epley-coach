package health.epley.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * A unit quaternion representing a 3D rotation.
 *
 * ## Why quaternions and not Euler angles
 *
 * Android exposes head orientation most conveniently through `SensorManager.getOrientation()`,
 * which returns Euler angles with pitch bounded to ±90°. The Epley manoeuvre begins by extending
 * the neck 20–30° *below* horizontal from a supine start, so the head's forward axis passes
 * through and beyond that boundary. At exactly the clinically critical moment the Euler
 * representation produces sign flips, 180° azimuth jumps, and roll discontinuities. Android's own
 * documentation adds that the legacy orientation path "is reliable only when the roll angle is 0",
 * and the Epley rolls the head 90° twice.
 *
 * Quaternions have no such singularity. Every angle this file reports is computed as a
 * well-conditioned scalar that stays continuous through head-hanging.
 */
data class Quaternion(
    val w: Double,
    val x: Double,
    val y: Double,
    val z: Double,
) {

    val norm: Double get() = sqrt(w * w + x * x + y * y + z * z)

    fun normalized(): Quaternion {
        val n = norm
        require(n > 1e-12) { "cannot normalize a zero quaternion" }
        return Quaternion(w / n, x / n, y / n, z / n)
    }

    /** The inverse rotation. For a unit quaternion this is the conjugate. */
    fun inverse(): Quaternion = Quaternion(w, -x, -y, -z)

    operator fun times(other: Quaternion): Quaternion = Quaternion(
        w = w * other.w - x * other.x - y * other.y - z * other.z,
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
    )

    /** Rotate a vector by this quaternion. */
    fun rotate(v: Vector3): Vector3 {
        // v' = v + 2 * r x (r x v + w * v), where r is the vector part. Avoids building a matrix.
        val r = Vector3(x, y, z)
        val t = (r cross v) * 2.0
        return v + t * w + (r cross t)
    }

    /**
     * Angle of rotation in radians, in [0, pi].
     *
     * Uses the vector-part magnitude rather than acos(w) because acos loses precision badly for
     * small angles, which is exactly the regime the stillness detector cares about.
     */
    val angleRadians: Double
        get() {
            val vectorNorm = sqrt(x * x + y * y + z * z)
            return 2.0 * kotlin.math.atan2(vectorNorm, abs(w))
        }

    /**
     * Shortest angular distance to another orientation, in radians.
     *
     * q and -q are the same rotation, so we take the absolute dot product. Forgetting this is the
     * classic quaternion bug: two identical orientations can otherwise report 360° apart.
     */
    fun angularDistanceTo(other: Quaternion): Double {
        val dot = abs(w * other.w + x * other.x + y * other.y + z * other.z).coerceIn(-1.0, 1.0)
        return 2.0 * acos(dot)
    }

    /**
     * Split this rotation into a twist about [axis] and the remaining swing.
     *
     * This is what lets us report "how far is the head turned toward the affected ear" as a single
     * continuous number, independent of how far the neck is extended. Projecting the quaternion's
     * vector part onto the axis isolates the component of rotation around it; whatever is left is
     * the swing.
     *
     * Degenerate case: when the rotation axis is perpendicular to [axis] the twist is undefined,
     * and we return identity rather than a NaN. In practice that means "no measurable rotation
     * about this axis", which is the right answer.
     */
    fun swingTwist(axis: Vector3): SwingTwist {
        val a = axis.normalized()
        val r = Vector3(x, y, z)
        val projection = a * (r dot a)

        val twistNorm = sqrt(w * w + projection.lengthSquared)
        if (twistNorm < 1e-9) {
            return SwingTwist(swing = this, twist = IDENTITY)
        }
        var twist = Quaternion(w, projection.x, projection.y, projection.z).normalized()
        // Keep the twist on the near hemisphere so the reported angle is signed sensibly.
        if (twist.w < 0.0) twist = Quaternion(-twist.w, -twist.x, -twist.y, -twist.z)

        val swing = this * twist.inverse()
        return SwingTwist(swing = swing, twist = twist)
    }

    companion object {
        val IDENTITY = Quaternion(1.0, 0.0, 0.0, 0.0)

        /**
         * Build from Android's `TYPE_GAME_ROTATION_VECTOR` values.
         *
         * Android delivers the vector part first and may omit the scalar, in which case it is
         * recovered from the unit-length constraint. The game rotation vector is preferred over
         * `TYPE_ROTATION_VECTOR` because the latter fuses the magnetometer, and a magnetometer
         * beside a steel bed frame, a laptop and a second phone is not a heading reference.
         */
        fun fromRotationVector(values: FloatArray): Quaternion {
            require(values.size >= 3) { "rotation vector needs at least 3 components" }
            val x = values[0].toDouble()
            val y = values[1].toDouble()
            val z = values[2].toDouble()
            val w = if (values.size >= 4) {
                values[3].toDouble()
            } else {
                val t = 1.0 - (x * x + y * y + z * z)
                if (t > 0.0) sqrt(t) else 0.0
            }
            return Quaternion(w, x, y, z).normalized()
        }

        /** Rotation of [radians] about [axis]. */
        fun fromAxisAngle(axis: Vector3, radians: Double): Quaternion {
            val a = axis.normalized()
            val half = radians / 2.0
            val s = kotlin.math.sin(half)
            return Quaternion(kotlin.math.cos(half), a.x * s, a.y * s, a.z * s)
        }
    }
}

/** The result of separating a rotation into rotation-about-an-axis and everything else. */
data class SwingTwist(val swing: Quaternion, val twist: Quaternion)

/** A 3D vector. Deliberately minimal — only what the angle math needs. */
data class Vector3(val x: Double, val y: Double, val z: Double) {

    val lengthSquared: Double get() = x * x + y * y + z * z
    val length: Double get() = sqrt(lengthSquared)

    fun normalized(): Vector3 {
        val n = length
        require(n > 1e-12) { "cannot normalize a zero vector" }
        return Vector3(x / n, y / n, z / n)
    }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vector3(x * scalar, y * scalar, z * scalar)

    infix fun dot(other: Vector3): Double = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vector3) = Vector3(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x,
    )

    companion object {
        /** World up. Android's world frame has Z pointing away from the ground. */
        val UP = Vector3(0.0, 0.0, 1.0)
        val DOWN = Vector3(0.0, 0.0, -1.0)
    }
}
