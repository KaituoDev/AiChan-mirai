package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import `fun`.kaituo.aichanmirai.AiChanMirai.INSTANCE as AiChan

object MinecraftAdminCommand : CompositeCommand(
    AiChan,
    "mcop",
    description = "AiMC 管理员命令"
) {

    @SubCommand
    @Description("为他人链接 QQ 号和 MCID")
    suspend fun CommandSender.link(user: User, mcId: String) {
        val result = PlayerDataConfig.link(user.id, mcId)
        val nick = user.nameCardOrNick
        val id = user.id

        val message = when (result) {
            PlayerDataConfig.LinkResult.SUCCESS -> "已成功为用户 $nick($id) 链接 ID 至 $mcId"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_LINKED -> "用户 $nick($id) 已经链接过了！"
            PlayerDataConfig.LinkResult.FAIL_ALREADY_EXIST -> "这个 ID 已被其他用户链接了！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("为他人解绑 QQ 号和 MCID")
    suspend fun CommandSender.unlink(user: User) {
        val result = PlayerDataConfig.unlink(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            PlayerDataConfig.UnlinkResult.SUCCESS -> "已成功为用户 $nick($id) 解绑 ID"
            PlayerDataConfig.UnlinkResult.FAIL_NOT_LINKED -> "用户 $nick($id) 还未链接 ID！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("封禁用户(QQ 号)")
    suspend fun CommandSender.ban(user: User) {
        val result = PlayerDataConfig.ban(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            PlayerDataConfig.BanResult.SUCCESS -> "已封禁用户 $nick($id)"
            PlayerDataConfig.BanResult.FAIL_ALREADY_BANNED -> "用户 $nick($id) 已处于封禁状态！"
            PlayerDataConfig.BanResult.FAIL_NOT_FOUND -> "发生了未知错误，请联系管理员检查后台！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("解封用户(QQ号)")
    suspend fun CommandSender.pardon(user: User) {
        val result = PlayerDataConfig.pardon(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            PlayerDataConfig.PardonResult.SUCCESS -> "已解封用户 $nick($id)"
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> "用户 $nick($id) 未被封禁！"
            PlayerDataConfig.PardonResult.FAIL_NOT_FOUND -> "未找到用户 $id，请联系管理员检查后台！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("封禁用户(MCID)")
    suspend fun CommandSender.banid(mcId: String) {
        val result = PlayerDataConfig.banId(mcId)
        val id = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            PlayerDataConfig.BanResult.FAIL_NOT_FOUND -> {
                AiChan.queueCommandReplyMessage(this, "不存在 MCID 为 $mcId 的用户！")
                return
            }

            PlayerDataConfig.BanResult.SUCCESS -> "已封禁用户 ($id)"
            PlayerDataConfig.BanResult.FAIL_ALREADY_BANNED -> "用户 ($id) 已处于封禁状态！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("解封用户(MCID)")
    suspend fun CommandSender.pardonid(mcId: String) {
        val result = PlayerDataConfig.pardonId(mcId)
        val id = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            PlayerDataConfig.PardonResult.FAIL_NOT_FOUND -> {
                AiChan.queueCommandReplyMessage(this, "不存在 MCID 为 $mcId 的用户！")
                return
            }

            PlayerDataConfig.PardonResult.SUCCESS -> "已解封用户 ($id)"
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> "用户 ($id) 未被封禁！"
        }
        AiChan.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("查询用户状态(QQ 号)")
    suspend fun CommandSender.find(user: User) {
        val player = PlayerDataConfig.getUserData(user.id)
        AiChan.queueCommandReplyMessage(
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
        AiChan.queueCommandReplyMessage(this, message)
    }
}

fun queryPlayerStatus(id: Long, mcId: String): String {
    val player = PlayerDataConfig.getUserData(id)
    val isLinked = player.isLinked
    val message = buildList {
        add("用户 $id:")
        add(if (player.isBanned) "已封禁" else "未封禁")
        add(if (isLinked) "链接的 MCID: $mcId" else "未链接")
    }
    return message.joinToString("\n")
}
