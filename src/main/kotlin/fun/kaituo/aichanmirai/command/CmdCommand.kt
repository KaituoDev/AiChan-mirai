package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket
import `fun`.kaituo.aichanmirai.server.SocketServer
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object CmdCommand : SimpleCommand(AiChanMirai.INSTANCE,
    "cmd", "c",
    description = "向MC服务器发送指令"
){

    @Handler
    suspend fun CommandSender.cmd(trigger: String, vararg cmd : String) {
        if (this !is MemberCommandSender) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this,ResponseConfig.groupOnlyMessage)
            return
        }
        if (this.group.id != MainConfig.messagingGroup) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this,ResponseConfig.groupOnlyMessage)
            return
        }
        val packet = SocketPacket(SocketPacket.PacketType.SERVER_COMMAND)
        val command = cmd
            .joinToString(" ")
        packet[0] = trigger
        packet[1] = command
        SocketServer.INSTANCE.sendPacket(packet)
    }

}