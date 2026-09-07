package health.epley.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuaternionTest {

    private fun deg(d: Double) = Math.toRadians(d)

    @Test
    fun `identity leaves a vector unchanged`() {
        val v = Vector3(1.0, 2.0, 3.0)
        val r = Quaternion.IDENTITY.rotate(v)
        assertEquals(v.x, r.x, 1e-12)
        assertEquals(v.y, r.y, 1e-12)
        assertEquals(v.z, r.z, 1e-12)
    }

    @Test
    fun `ninety degrees about z maps x onto y`() {
        val q = Quaternion.fromAxisAngle(Vector3(0.0, 0.0, 1.0), deg(90.0))
        val r = q.rotate(Vector3(1.0, 0.0, 0.0))
        assertEquals(0.0, r.x, 1e-9)
        assertEquals(1.0, r.y, 1e-9)
        assertEquals(0.0, r.z, 1e-9)
    }

    @Test
    fun `rotation composes in the right order`() {
        val a = Quaternion.fromAxisAngle(Vector3(0.0, 0.0, 1.0), deg(90.0))
        val b = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(90.0))
        val v = Vector3(0.0, 0.0, 1.0)
        // Applying b then a must equal applying the product (a*b).
        val stepwise = a.rotate(b.rotate(v))
        val combined = (a * b).rotate(v)
        assertEquals(stepwise.x, combined.x, 1e-9)
        assertEquals(stepwise.y, combined.y, 1e-9)
        assertEquals(stepwise.z, combined.z, 1e-9)
    }

    @Test
    fun `inverse undoes the rotation`() {
        val q = Quaternion.fromAxisAngle(Vector3(1.0, 2.0, 3.0), deg(57.0))
        val v = Vector3(0.3, -0.7, 0.2)
        val round = q.inverse().rotate(q.rotate(v))
        assertEquals(v.x, round.x, 1e-9)
        assertEquals(v.y, round.y, 1e-9)
        assertEquals(v.z, round.z, 1e-9)
    }

    @Test
    fun `angle recovers what was put in`() {
        for (d in listOf(0.0, 1.0, 30.0, 90.0, 135.0, 179.0)) {
            val q = Quaternion.fromAxisAngle(Vector3(0.0, 1.0, 0.0), deg(d))
            assertEquals(d, Math.toDegrees(q.angleRadians), 1e-6)
        }
    }

    @Test
    fun `a quaternion and its negation are the same rotation`() {
        // The classic bug: forgetting this makes two identical orientations look 360 apart.
        val q = Quaternion.fromAxisAngle(Vector3(0.0, 0.0, 1.0), deg(120.0))
        val negated = Quaternion(-q.w, -q.x, -q.y, -q.z)
        assertEquals(0.0, Math.toDegrees(q.angularDistanceTo(negated)), 1e-6)
    }

    @Test
    fun `angular distance is symmetric and zero against itself`() {
        val a = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(40.0))
        val b = Quaternion.fromAxisAngle(Vector3(0.0, 1.0, 0.0), deg(25.0))
        assertEquals(0.0, a.angularDistanceTo(a), 1e-12)
        assertEquals(a.angularDistanceTo(b), b.angularDistanceTo(a), 1e-12)
    }

    // ---- swing-twist: the decomposition the manoeuvre depends on ----

    @Test
    fun `pure twist about an axis is fully recovered`() {
        val axis = Vector3(0.0, 0.0, 1.0)
        val q = Quaternion.fromAxisAngle(axis, deg(45.0))
        val st = q.swingTwist(axis)
        assertEquals(45.0, Math.toDegrees(st.twist.angleRadians), 1e-6)
        assertEquals(0.0, Math.toDegrees(st.swing.angleRadians), 1e-6)
    }

    @Test
    fun `pure swing perpendicular to the axis yields no twist`() {
        val axis = Vector3(0.0, 0.0, 1.0)
        val q = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(60.0))
        val st = q.swingTwist(axis)
        assertEquals(0.0, Math.toDegrees(st.twist.angleRadians), 1e-6)
        assertEquals(60.0, Math.toDegrees(st.swing.angleRadians), 1e-6)
    }

    @Test
    fun `swing and twist recompose into the original rotation`() {
        val axis = Vector3(0.0, 1.0, 0.0)
        val q = (Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(35.0)) *
            Quaternion.fromAxisAngle(axis, deg(50.0))).normalized()
        val st = q.swingTwist(axis)
        val recomposed = st.swing * st.twist
        assertEquals(0.0, Math.toDegrees(q.angularDistanceTo(recomposed)), 1e-6)
    }

    @Test
    fun `twist is isolated from a simultaneous swing`() {
        // Turn the head 45 degrees AND tip it back 30. The twist must still read 45.
        val longAxis = Vector3(0.0, 1.0, 0.0)
        val turn = Quaternion.fromAxisAngle(longAxis, deg(45.0))
        val tip = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(30.0))
        val st = (tip * turn).swingTwist(longAxis)
        assertEquals(45.0, Math.toDegrees(st.twist.angleRadians), 1e-6)
    }

    @Test
    fun `degenerate decomposition returns identity rather than NaN`() {
        // Rotation exactly perpendicular to the twist axis: twist is undefined.
        val axis = Vector3(0.0, 0.0, 1.0)
        val q = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(180.0))
        val st = q.swingTwist(axis)
        assertTrue(!st.twist.w.isNaN() && !st.twist.x.isNaN())
        assertTrue(!st.swing.w.isNaN())
    }

    // ---- Android interop ----

    @Test
    fun `reconstructs the scalar part when Android omits it`() {
        val full = Quaternion.fromAxisAngle(Vector3(0.0, 1.0, 0.0), deg(72.0))
        val threeComponent = floatArrayOf(full.x.toFloat(), full.y.toFloat(), full.z.toFloat())
        val rebuilt = Quaternion.fromRotationVector(threeComponent)
        assertTrue(
            Math.toDegrees(full.angularDistanceTo(rebuilt)) < 1e-3,
            "rebuilt ${Math.toDegrees(rebuilt.angleRadians)} vs ${Math.toDegrees(full.angleRadians)}",
        )
    }

    @Test
    fun `accepts the four component form Android usually sends`() {
        val full = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), deg(15.0))
        val four = floatArrayOf(
            full.x.toFloat(), full.y.toFloat(), full.z.toFloat(), full.w.toFloat(),
        )
        val rebuilt = Quaternion.fromRotationVector(four)
        assertTrue(Math.toDegrees(full.angularDistanceTo(rebuilt)) < 1e-3)
    }

    @Test
    fun `always returns a unit quaternion`() {
        val q = Quaternion.fromRotationVector(floatArrayOf(0.3f, 0.4f, 0.5f))
        assertEquals(1.0, q.norm, 1e-9)
    }
}
