import pt.lsts.imc.*
import pt.lsts.imc.test.Geo
import pt.lsts.imc.test.KnownGeos
import pt.lsts.msg
import java.time.Duration
import java.util.*

class Plan(val id : String) {

    var maneuvers = HashMap<String, Maneuver>()
    var order = HashMap<Int, String>()

    var loc:Geo = KnownGeos.APDL
    var speed = 1.0
    var z = 0.0
    var z_units = "DEPTH"
    var count = 1

    fun <T : Maneuver> maneuver(id: String, m: Class<T>): T {
        val man = msg(m) {
            setValue("lat", loc.lat.asRadians())
            setValue("lon", loc.lon.asRadians())
            setValue("speed", speed)
            setValue("speed_units", "METERS_PS")
            setValue("z", z)
            setValue("z_units", z_units)
        }

        order[order.size] = id
        maneuvers[id] = man

        return man
    }

    fun goto(id: String = "${count++}") = maneuver(id, Goto::class.java)

    fun skeeping(id: String = "${count++}", duration: Duration) : StationKeeping {
        val man = maneuver(id, StationKeeping::class.java)
        man.duration = duration.seconds.toInt()
        return man
    }

    fun loiter(id: String = "${count++}", radius: Double, duration: Duration) : Loiter {
        val loiter = maneuver(id, Loiter::class.java)
        loiter.duration = duration.seconds.toInt()
        loiter.type = Loiter.TYPE.CIRCULAR
        loiter.radius = radius
        return loiter
    }

    fun yoyo(id:String = "${count++}", max_depth: Double, min_depth: Double) : YoYo {
        val yoyo = maneuver(id, YoYo::class.java)
        yoyo.amplitude = (max_depth - min_depth)
        yoyo.z = (max_depth + min_depth) / 2.0
        return yoyo
    }

    fun popup(id:String = "${count++}", duration: Duration, currPos: Boolean) : PopUp {
        val popup = maneuver(id, PopUp::class.java)
        popup.duration = duration.seconds.toInt()
        popup.flags = if (currPos) PopUp.FLG_CURR_POS else 0;
        return popup
    }
}

fun main(args: Array<String>) {
    var plan = Plan("my_plan")
    plan.speed = 1.2
    plan.z_units = "ALTITUDE"
    plan.z = 5.0

    plan.goto()
    plan.loc = plan.loc.translatedBy(100.0, 0.0)
    plan.loiter(radius = 40.0, duration = Duration.ofMinutes(3))
    println(plan.maneuvers)
}

