package pt.lsts

import pt.lsts.imc.*
import pt.lsts.imc.adapter.ImcAdapter
import pt.lsts.imc.net.Consume
import pt.lsts.imc.net.IMCProtocol
import pt.lsts.imc.test.Geo
import pt.lsts.imc.test.KnownGeos
import pt.lsts.neptus.messages.listener.Periodic

fun <T : IMCMessage> msg(msg: Class<T>, builder: T.() -> Unit): T {
    val m = IMCDefinition.getInstance().create(msg)
    m.builder()
    return m;
}


//Plain Old Kotlin Object (POKO) that will handle incoming data
class ImcAgent(name: String, id: Int, port: Int, type: Announce.SYS_TYPE) : ImcAdapter(name, id, port, type) {

    var req_id = 0

    // method called every 3000 milliseconds
    @Periodic(3000)
    fun update() {
        println("3 seconds have passed")
    }

    // method call whenever a message arrives from any system
    @Consume
    fun on(msg: IMCMessage) = println("${msg.abbrev} from ${msg.sourceName}")

    // Message creation and sending
    fun sendPlanControl() {

        val pt1 = KnownGeos.APDL.translatedBy(0.0, 150.0)

        val pc = msg(PlanControl::class.java) {
            requestId = ++req_id
            info = "Go to APDL"
            type = PlanControl.TYPE.REQUEST
            op = PlanControl.OP.START
            flags = PlanControl.FLG_CALIBRATE

            arg = msg(Goto::class.java) {
                lat = pt1.lat.asRadians()
                lon = pt1.lon.asRadians()
                z = 2.0
                zUnits = Goto.Z_UNITS.DEPTH
                speed = 1.0
                speedUnits = Goto.SPEED_UNITS.METERS_PS
            }
        }

        // Send to all connected peers
        dispatch(pc)
    }
}

fun main(args: Array<String>) {
    // create and start the agent
    ImcAgent("Kotlin Agent", 0x4444, 7010, Announce.SYS_TYPE.UAV)
}
