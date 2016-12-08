import pt.lsts.imc.*
import pt.lsts.imc.test.Geo
import pt.lsts.imc.test.KnownGeos
import pt.lsts.msg
import java.time.Duration
import java.util.*

class Plan(val id: String) {

    var mans = emptyArray<PlanManeuver>()

    var loc: Geo = KnownGeos.APDL.translatedBy(0.0, -200.0)
    var speed = 1.0
    var z = 0.0
    var z_units = "DEPTH"
    var count = 1

    fun <T : Maneuver> maneuver(id: String, m: Class<T>): T {
        val pman = msg(PlanManeuver::class.java) {
            maneuverId = id
            data = msg(m) {
                setValue("lat", loc.lat.asRadians())
                setValue("lon", loc.lon.asRadians())
                setValue("speed", speed)
                setValue("speed_units", "METERS_PS")
                setValue("z", z)
                setValue("z_units", z_units)
            }
        }
        mans += pman;
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
        popup.flags = if (currPos) PopUp.FLG_CURR_POS else 0;
        return popup
    }

    fun imc(): PlanSpecification {
        return msg(PlanSpecification::class.java) {
            planId = id;
            startManId = mans[0].maneuverId
            setManeuvers(mans.asList())
            setTransitions(transitions().asList())
        }
    }

    fun transitions(): Array<PlanTransition> {
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

        return trans;
    }
}

fun main(args: Array<String>) {
    var plan = Plan("my_plan")
    plan.speed = 1.2
    plan.z_units = "ALTITUDE"
    plan.z = 5.0
    plan.goto()
    plan.loc = plan.loc.translatedBy(100.0, 0.0)
    plan.loiter(duration = 3*60)
    println(plan.imc().asXml(false))
}

