package pt.lsts.imc.kotlin

import pt.lsts.imc.*
import pt.lsts.imc.test.Geo
import pt.lsts.imc.test.KnownGeos
import pt.lsts.imc.test.deg
import pt.lsts.msg
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane

fun plan(id: String, init: Plan.() -> Unit): Plan {
    val p = Plan(id)
    p.init()
    return p
}

fun debug(msg: IMCMessage) {
    val frm = JFrame("Contents for ${msg.abbrev}")

    frm.contentPane = JScrollPane(JLabel(IMCUtil.getAsHtml(msg)))
    frm.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frm.setSize(800, 600)
    frm.isVisible = true
}


data class Z(var value: Double, var units: Units) {
    enum class Units {
        DEPTH,
        ALTITUDE,
        HEIGHT,
        NONE
    }

    fun units() = units.toString()
}

data class Speed(var value: Double, var units: Units) {
    enum class Units {
        METERS_PS,
        PERCENTAGE,
        RPM
    }

    fun units() = units.toString()
}

class Plan(val id: String) {

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

    fun goto(id: String = "${count++}") = maneuver(id, Goto::class.java)

    fun skeeping(id: String = "${count++}", radius: Double = 20.0, duration: Int = 0): StationKeeping {
        val man = maneuver(id, StationKeeping::class.java)
        man.duration = duration
        man.radius = radius
        return man
    }

    fun loiter(id: String = "${count++}", radius: Double = 20.0, duration: Int = 600): Loiter {
        val loiter = maneuver(id, Loiter::class.java)
        loiter.duration = duration
        loiter.type = Loiter.TYPE.CIRCULAR
        loiter.radius = radius
        return loiter
    }

    fun yoyo(id: String = "${count++}", max_depth: Double = 20.0, min_depth: Double = 2.0): YoYo {
        val yoyo = maneuver(id, YoYo::class.java)
        yoyo.amplitude = (max_depth - min_depth)
        yoyo.z = (max_depth + min_depth) / 2.0
        return yoyo
    }

    fun popup(id: String = "${count++}", duration: Int = 180, currPos: Boolean = true): PopUp {
        val popup = maneuver(id, PopUp::class.java)
        popup.duration = duration
        popup.flags = if (currPos) PopUp.FLG_CURR_POS else 0
        return popup
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
                    conditions = "maneuverDone"
                }
            previous = m
        }

        return trans
    }
}

fun main(args: Array<String>) {

    // Plan builder
    var p = plan("KotlinPlan") {

        // set current plan location
        loc = Geo(41.185242.deg(), -8.704803.deg())

        // set speed units to use
        speed.units = Speed.Units.METERS_PS
        speed.value = 1.2

        // set z reference to use
        z.value = 5.0
        z.units = Z.Units.ALTITUDE

        // add goto at this location
        goto()

        // change location
        loc = loc.translatedBy(100.0, 0.0)

        // change z value (but not changing reference)
        z.value = 4.0

        // add loiter using default radius but using 4 minutes duration
        loiter(duration = 240)
    }

    // Get the plan as IMC
    val planImc = p.imc()

    debug(planImc)
}

