package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.PlayerData
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket
import `fun`.kaituo.aichanmirai.server.SocketServer
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.nameCardOrNick

object SayCommand : SimpleCommand(
    AiChanMirai.INSTANCE,
    "say",
    "s",
    description = "向MC服务器发送消息"
) {

    @Handler
    suspend fun CommandSender.say(trigger: String, vararg msg: String) {
        if (this !is MemberCommandSender) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, ResponseConfig.groupOnlyMessage)
            return
        }
        if (this.group.id != MainConfig.messagingGroup) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, ResponseConfig.groupOnlyMessage)
            return
        }
        val player: PlayerData = PlayerDataConfig.getUserData(this.user.id)
        val nick: String = this.user.nameCardOrNick
        if (player.isBanned) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "$nick，你已被封禁！")
        } else if (!player.isLinked) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "$nick，你还未链接 MCID！")
        } else {
            val packet = SocketPacket(SocketPacket.PacketType.SERVER_TEXT)
            val mcId = player.mcId
            val message = msg
                .joinToString(" ")
                .replace("&", "§")
            packet[0] = trigger
            packet[1] = "$mcId: $message"
            SocketServer.INSTANCE.sendPacket(packet)
        }
    }
}
