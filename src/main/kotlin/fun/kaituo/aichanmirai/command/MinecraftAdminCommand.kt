package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.PlayerData
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig.BanResult
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick

object MinecraftAdminCommand : CompositeCommand(
    AiChanMirai.INSTANCE,
    "mcop",
    description = "AiMC 管理员命令"
) {

    @SubCommand
    @Description("为他人链接 QQ 号和 MCID")
    suspend fun CommandSender.link(user: User, mcId: String) {
        val result: PlayerDataConfig.LinkResult = PlayerDataConfig.link(user.id, mcId)
        val nick: String = user.nameCardOrNick
        val id: Long = user.id

        val message = when (result) {
            PlayerDataConfig.LinkResult.SUCCESS -> "已成功为用户 $nick($id) 链接 ID 至 $mcId"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_LINKED -> "用户 $nick($id) 已经链接过了！"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_EXIST -> "这个 ID 已被其他用户链接了！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("为他人解绑 QQ 号和 MCID")
    suspend fun CommandSender.unlink(user: User) {
        val result: PlayerDataConfig.UnlinkResult = PlayerDataConfig.unlink(user.id)
        val nick: String = user.nick
        val id: Long = user.id

        val message = when (result) {
            PlayerDataConfig.UnlinkResult.SUCCESS -> "已成功为用户 $nick($id) 解绑 ID"
            PlayerDataConfig.UnlinkResult.FAIL_NOT_LINKED -> "用户 $nick($id) 还未链接 ID！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("封禁用户(QQ 号)")
    suspend fun CommandSender.ban(user: User) {
        val result: PlayerDataConfig.BanResult = PlayerDataConfig.ban(user.id)
        val nick: String = user.nick
        val id: Long = user.id

        val message = when (result) {
            BanResult.SUCCESS -> "已封禁用户 $nick($id)"
            BanResult.FAIL_ALREADY_BANNED -> "用户 $nick($id) 已处于封禁状态！"
            BanResult.FAIL_NOT_FOUND -> "发生了未知错误，请联系管理员检查后台！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("解封用户(QQ号)")
    suspend fun CommandSender.pardon(user: User) {
        val result: PlayerDataConfig.PardonResult = PlayerDataConfig.pardon(user.id)
        val nick: String = user.nick
        val id: Long = user.id

        val message = when (result) {
            PlayerDataConfig.PardonResult.SUCCESS -> "已解封用户 $nick($id)"
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> "用户 $nick($id) 未被封禁！"
            PlayerDataConfig.PardonResult.FAIL_NOT_FOUND -> "未找到用户 $id，请联系管理员检查后台！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("封禁用户(MCID)")
    suspend fun CommandSender.banid(mcId: String) {
        val result: PlayerDataConfig.BanResult = PlayerDataConfig.banId(mcId)
        val id: Long = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            BanResult.FAIL_NOT_FOUND -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "不存在 MCID 为 $mcId 的用户！")
                return
            }

            BanResult.SUCCESS -> "已封禁用户 ($id)"
            BanResult.FAIL_ALREADY_BANNED -> "用户 ($id) 已处于封禁状态！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("解封用户(MCID)")
    suspend fun CommandSender.pardonid(mcId: String) {
        val result: PlayerDataConfig.PardonResult = PlayerDataConfig.pardonId(mcId)
        val id: Long = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            PlayerDataConfig.PardonResult.FAIL_NOT_FOUND -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "不存在 MCID 为 $mcId 的用户！")
                return
            }

            PlayerDataConfig.PardonResult.SUCCESS -> "已解封用户 ($id)"
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> "用户 ($id) 未被封禁！"
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("查询用户状态(QQ 号)")
    suspend fun CommandSender.find(user: User) {
        val player: PlayerData = PlayerDataConfig.getUserData(user.id)
        AiChanMirai.INSTANCE.queueCommandReplyMessage(
            this,
            queryPlayerStatus(user.id, player.mcId)
        )
    }

    @SubCommand
    @Description("查询用户状态(MCID)")
    suspend fun CommandSender.findid(mcId: String) {
        val message = when (val id = PlayerDataConfig.searchMCId(mcId)) {
            -1L -> "不存在 MCID 为 $mcId 的用户！"
            else -> queryPlayerStatus(id, mcId)
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }
}

fun queryPlayerStatus(id: Long, mcId: String): String {
    val player: PlayerData = PlayerDataConfig.getUserData(id)
    val isLinked: Boolean = player.isLinked
    val message = buildList {
        add("用户 $id:")
        add(if (player.isBanned) "已封禁" else "未封禁")
        add(if (isLinked) "链接的 MCID: $mcId" else "未链接")
    }
    return message.joinToString("\n")
}
