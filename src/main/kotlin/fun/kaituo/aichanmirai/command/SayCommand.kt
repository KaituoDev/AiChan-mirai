package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.nameCardOrNick
import `fun`.kaituo.aichanmirai.AiChanMirai.INSTANCE as AiChan
import `fun`.kaituo.aichanmirai.server.SocketServer.INSTANCE as SocketServer

object SayCommand : SimpleCommand(
    AiChan,
    "say",
    "s",
    description = "向MC服务器发送消息"
) {

    @Handler
    suspend fun CommandSender.say(trigger: String, vararg msg: String) {
        if (this !is MemberCommandSender || this.group.id != MainConfig.messagingGroup) {
            AiChan.queueCommandReplyMessage(this, ResponseConfig.groupOnlyMessage)
            return
        }

        val player = PlayerDataConfig.getUserData(this.user.id)
        val nick = this.user.nameCardOrNick

        when {
            player.isBanned -> AiChan.queueCommandReplyMessage(this, "$nick，你已被封禁！")
            !player.isLinked -> AiChan.queueCommandReplyMessage(this, "$nick，你还未链接 MCID！")
            else -> {
                val mcId = player.mcId
                val message = msg
                    .joinToString(" ")
                    .replace("&", "§")
                val packet = SocketPacket(SocketPacket.PacketType.SERVER_TEXT).apply {
                    this[0] = trigger
                    this[1] = "$mcId: $message"
                }
                SocketServer.sendPacket(packet)
            }
        }
    }
}
