package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket
import `fun`.kaituo.aichanmirai.server.SocketServer
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.contact.nameCardOrNick

object MinecraftUserCommand : CompositeCommand(
    AiChanMirai.INSTANCE,
    "mc",
    description = "AiMC主命令"
) {
    @SubCommand
    @Description("为自己链接 QQ 号和 MCID")
    suspend fun CommandSender.link(mcId: String) {
        if (this !is UserCommandSender) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, ResponseConfig.userOnlyMessage)
        }

        val userSender = this as UserCommandSender
        val result: PlayerDataConfig.LinkResult = PlayerDataConfig.link(userSender.user.id, mcId)
        val nick: String = userSender.user.nameCardOrNick

        val message = when (result) {
            PlayerDataConfig.LinkResult.SUCCESS -> "$nick，你已为自己的 QQ 号链接至 ID：$mcId"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_LINKED -> "$nick，你已经链接过了！请联系管理员解绑！"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_EXIST -> "$nick，该 ID 已被其他用户链接了！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand("list", "l")
    @Description("列出在线玩家")
    suspend fun CommandSender.list() {
        if (this !is MemberCommandSender || this.group.id != MainConfig.messagingGroup) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this, ResponseConfig.groupOnlyMessage)
            return
        }

        val packet = SocketPacket(SocketPacket.PacketType.LIST_REQUEST)
        SocketServer.INSTANCE.sendPacket(packet)
    }
}
