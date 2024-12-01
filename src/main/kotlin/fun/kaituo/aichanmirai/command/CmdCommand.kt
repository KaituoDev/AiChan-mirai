package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import `fun`.kaituo.aichanmirai.AiChanMirai as AiChan
import `fun`.kaituo.aichanmirai.server.SocketServer.Companion.INSTANCE as SocketServer

object CmdCommand : SimpleCommand(
    AiChan,
    "cmd",
    "c",
    description = "向 MC 服务器发送并运行指令"
) {

    @Handler
    suspend fun CommandSender.cmd(trigger: String, vararg cmd: String) {
        if (this !is MemberCommandSender || this.group.id != MainConfig.messagingGroup) {
            AiChan.queueCommandReply(this, ResponseConfig.groupOnlyMessage)
            return
        }

        val packet = SocketPacket(SocketPacket.PacketType.COMMAND_TO_SERVER).apply {
            this[0] = trigger
            this[1] = cmd.joinToString(" ")
        }
        SocketServer.sendPacket(packet)
    }
}
