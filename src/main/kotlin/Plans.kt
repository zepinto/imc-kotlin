package pt.lsts.imc.kotlin

import pt.lsts.imc.*
import pt.lsts.imc.net.IMCProtocol
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane

fun plan(id: String, init: Plan.() -> Unit): PlanSpecification {
    val p = Plan(id)
    p.init()
    return p.imc()
}

data class Z(var value: Number, var units: Units) {
    enum class Units {
        DEPTH,
        ALTITUDE,
        HEIGHT,
        NONE
    }

    fun units() = units.toString()
}

data class Speed(var value: Number, var units: Units) {
    enum class Units {
        METERS_PS,
        PERCENTAGE,
        RPM
    }

    fun units() = units.toString()
}

class Plan(val id: String) {

    val MPS = Speed.Units.METERS_PS
    val Percent = Speed.Units.PERCENTAGE
    val RPM = Speed.Units.RPM

    val Depth = Z.Units.DEPTH
    val Altitude = Z.Units.ALTITUDE
    val Height = Z.Units.HEIGHT
    val None = Z.Units.NONE

    var mans = emptyArray<PlanManeuver>()
    var loc: Geo = KnownGeos.APDL
    var speed = Speed(1.0, Speed.Units.METERS_PS)
    var z = Z(0.0, Z.Units.DEPTH)
    var count = 1

    fun <T : Maneuver> maneuver(id: String, m: Class<T>): T {
        val pman = msg(PlanManeuver::class.java) {
            maneuverId = id
            data = msg(m) {
                setValue("lat", loc.lat.asRadians())
                setValue("lon", loc.lon.asRadians())
                setValue("speed", speed.value)
                setValue("speed_units", speed.units)
                setValue("z", z.value)
                setValue("z_units", z.units())
            }
        }
        mans += pman
        return pman.data as T
    }

    fun goto(id: String = "${count++}", loc: Geo = this.loc, speed: Speed = this.speed, z: Z = this.z) = maneuver(id, Goto::class.java)

    fun skeeping(id: String = "${count++}", loc: Geo = this.loc, speed: Speed = this.speed, z: Z = this.z, radius: Number = 20.0, duration: Number = 0): StationKeeping {
        val man = maneuver(id, StationKeeping::class.java)
        man.duration = duration.toInt()
        man.radius = radius.toDouble()
        return man
    }

    fun loiter(id: String = "${count++}", loc: Geo = this.loc, speed: Speed = this.speed, z: Z = this.z, radius: Number = 20.0, duration: Number = 600): Loiter {
        val loiter = maneuver(id, Loiter::class.java)
        loiter.duration = duration.toInt()
        loiter.type = Loiter.TYPE.CIRCULAR
        loiter.radius = radius.toDouble()
        return loiter
    }

    fun yoyo(id: String = "${count++}", loc: Geo = this.loc, speed: Speed = this.speed, z: Z = this.z, max_depth: Number = 20.0, min_depth: Number = 2.0): YoYo {
        val yoyo = maneuver(id, YoYo::class.java)
        yoyo.amplitude = (max_depth.toDouble() - min_depth.toDouble())
        yoyo.z = (max_depth.toDouble() + min_depth.toDouble()) / 2.0
        return yoyo
    }

    fun popup(id: String = "${count++}", loc: Geo = this.loc, speed: Speed = this.speed, z: Z = this.z, duration: Number = 180, currPos: Boolean = true): PopUp {
        val popup = maneuver(id, PopUp::class.java)
        popup.duration = duration.toInt()
        popup.flags = if (currPos) PopUp.FLG_CURR_POS else 0
        return popup
    }

    fun move(northing: Number, easting: Number) {
        loc = loc.translatedBy(northing, easting)
    }

    fun locate(latitude: Number, longitude: Number) {
        loc = Geo(latitude.deg(), longitude.deg())
    }

    fun locate(latitude: Angle, longitude: Angle) {
        loc = Geo(latitude, longitude)
    }

    fun locate(loc: Geo) {
        this.loc = loc
    }

    fun imc(): PlanSpecification {
        return msg(PlanSpecification::class.java) {
            planId = id
            startManId = mans[0].maneuverId
            setManeuvers(mans.asList())
            setTransitions(transitions().asList())
        }
    }

    private fun transitions(): Array<PlanTransition> {
        var trans = emptyArray<PlanTransition>()
        var previous: PlanManeuver? = null

        for (m in mans) {
            if (previous != null)
                trans += msg(PlanTransition::class.java) {
                    sourceMan = previous?.maneuverId
                    destMan = m.maneuverId
                    conditions = "maneuverIsDone"
                }
            previous = m
        }

        return trans
    }
}

class send(val msg: IMCMessage) {

    val imc = IMCProtocol()

    infix fun to (vehicle: String) {
        imc.connect(vehicle)
        val vehicle = imc.waitFor(vehicle, 60000)

        if (vehicle != null && imc.sendMessage(vehicle, msg))
            println ("${msg.abbrev} commanded to $vehicle")
        else
            println ("Error communicating with $vehicle")
    }
}

fun main(args: Array<String>) {

    // Plan builder
    var plan = plan("KotlinPlan") {

        val home = Geo(41.185242, -8.704803)

        // set current plan location
        locate(home)

        // set speed units to use
        speed.units = MPS
        speed.value = 1.2

        // set z reference to use
        z.value = 5
        z.units = Altitude

        // add a goto at current location
        goto()

        // change current location by moving north 100 meters
        move(100, 0)

        // change z value (still using altitude)
        z.value = 4

        // add loiter using default radius but using 4 minutes duration
        loiter(duration = 240)

        // afterwards go back home at surface
        z.value = 0
        z.units = Depth
        skeeping(loc = home)
    }

    plan.description = "This is a PlanSpecification generated by a Kotlin builder"

    send(plan) to "lauv-xplore-1"
}

