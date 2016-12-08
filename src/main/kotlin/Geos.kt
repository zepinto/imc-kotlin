package pt.lsts.imc.kotlin

import pt.lsts.util.WGS84Utilities

data class Angle constructor(val rad: Number) {
    fun asDegrees() = Math.toDegrees(rad.toDouble())
    fun asRadians() = rad.toDouble()
    operator fun plus(a: Angle) = Angle(a.asRadians() + asRadians())
    operator fun minus(a: Angle) = Angle(a.asRadians() - asRadians())
    operator fun div(a: Number) = Angle(asRadians() / a.toDouble())
    operator fun times(a: Number) = Angle(asRadians() * a.toDouble())
    operator fun unaryMinus() = Angle(-asRadians())
    override fun toString() = String.format("%.1fº", asDegrees())
}

fun Number.deg() = Angle(Math.toRadians(this.toDouble()))
fun Number.rad() = Angle(this.toDouble())

object KnownGeos {
    val FEUP = Geo(41.1781918.deg(), -8.5954308.deg())
    val APDL = Geo(41.185242.deg(), -8.704803.deg())
}

data class Geo constructor(val lat: Angle, val lon: Angle) {

    infix fun distance(l: Geo): Double {
        val offsets = offsets(l)
        return Math.hypot(offsets.first, offsets.second)
    }

    infix fun angle(l: Geo): Angle {
        val offsets = offsets(l)
        return Math.atan2(offsets.second, offsets.first).rad()
    }

    fun translatedBy(northing: Double, easting: Double): Geo {
        val coords = WGS84Utilities.WGS84displace(lat.asDegrees(), lon.asDegrees(), 0.0, northing, easting, 0.0)
        return Geo(coords[0].deg(), coords[1].deg())
    }

    infix fun offsets(l: Geo): Pair<Double, Double> {
        val offsets = WGS84Utilities.WGS84displacement(
                lat.asDegrees(), lon.asDegrees(), 0.0, l.lat.asDegrees(), l.lon.asDegrees(), 0.0)
        return Pair(offsets[0], offsets[1])
    }

    override fun toString(): String = "Geo(${lat.asDegrees()}, ${lon.asDegrees()})"
}

fun main(args: Array<String>) {
    println(90.deg() == Math.PI.rad() / 2)
    println(Math.PI.rad() / 2)

    val l1 = KnownGeos.FEUP
    val l3 = l1.translatedBy(200.0, 200.0)

    println(KnownGeos.FEUP distance KnownGeos.APDL)
    println(l1 offsets l3)
    println((l1 angle l3).asRadians())
}