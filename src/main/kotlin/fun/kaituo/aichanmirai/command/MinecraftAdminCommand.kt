package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig.BanResult
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig.LinkResult
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig.PardonResult
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig.UnlinkResult
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import `fun`.kaituo.aichanmirai.AiChanMirai as AiChan
import `fun`.kaituo.aichanmirai.server.SocketServer.Companion.INSTANCE as SocketServer

object MinecraftAdminCommand : CompositeCommand(
    AiChan,
    "mcop",
    description = "AiMC 管理员命令"
) {

    @SubCommand
    @Description("为他人链接 QQ 号和 MCID")
    suspend fun CommandSender.link(user: User, vararg mcId: String) {
        val result = PlayerDataConfig.link(user.id, mcId[0])
        val nick = user.nameCardOrNick
        val id = user.id

        val message = when (result) {
            LinkResult.SUCCESS -> "已成功为用户 $nick($id) 链接 ID 至 ${mcId[0]}"
            LinkResult.FAIL_ALREADY_EXIST -> "这个 ID 已被其他用户链接了！"
            LinkResult.FAIL_ALREADY_LINKED -> {
                val arg = runCatching { mcId[1] }.getOrDefault("")
                if (arg in listOf("--force", "-f")) {
                    val player = PlayerDataConfig.getUserData(id).apply { this.mcId = mcId[0] }
                    PlayerDataConfig.setUserData(player)
                    SocketServer.sendPacket(player.getStatusPacket())
                    "已强制为用户 $nick($id) 链接 ID 至 ${mcId[0]}！"
                } else {
                    "用户 $nick($id) 已经链接过了(可在命令末尾加上 --force 或 -f 以强制覆盖)！"
                }
            }
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("为他人解绑 QQ 号和 MCID")
    suspend fun CommandSender.unlink(user: User) {
        val result = PlayerDataConfig.unlink(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            UnlinkResult.SUCCESS -> "已成功为用户 $nick($id) 解绑 ID"
            UnlinkResult.FAIL_NOT_LINKED -> "用户 $nick($id) 还未链接 ID！"
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("封禁用户(QQ 号)")
    suspend fun CommandSender.ban(user: User) {
        val result = PlayerDataConfig.ban(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            BanResult.SUCCESS -> "已封禁用户 $nick($id)"
            BanResult.FAIL_ALREADY_BANNED -> "用户 $nick($id) 已处于封禁状态！"
            BanResult.FAIL_NOT_FOUND -> "发生了未知错误，请联系管理员检查后台！"
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("解封用户(QQ号)")
    suspend fun CommandSender.pardon(user: User) {
        val result = PlayerDataConfig.pardon(user.id)
        val nick = user.nick
        val id = user.id

        val message = when (result) {
            PardonResult.SUCCESS -> "已解封用户 $nick($id)"
            PardonResult.FAIL_NOT_BANNED -> "用户 $nick($id) 未被封禁！"
            PardonResult.FAIL_NOT_FOUND -> "未找到用户 $id，请联系管理员检查后台！"
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("封禁用户(MCID)")
    suspend fun CommandSender.banid(mcId: String) {
        val result = PlayerDataConfig.banId(mcId)
        val id = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            BanResult.FAIL_NOT_FOUND -> "不存在 MCID 为 $mcId 的用户！"
            BanResult.SUCCESS -> "已封禁用户 ($id)"
            BanResult.FAIL_ALREADY_BANNED -> "用户 ($id) 已处于封禁状态！"
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("解封用户(MCID)")
    suspend fun CommandSender.pardonid(mcId: String) {
        val result = PlayerDataConfig.pardonId(mcId)
        val id = PlayerDataConfig.searchMCId(mcId)

        val message = when (result) {
            PardonResult.FAIL_NOT_FOUND -> "不存在 MCID 为 $mcId 的用户！"
            PardonResult.SUCCESS -> "已解封用户 ($id)"
            PardonResult.FAIL_NOT_BANNED -> "用户 ($id) 未被封禁！"
        }
        AiChan.queueCommandReply(this, message)
    }

    @SubCommand
    @Description("查询用户状态(QQ 号)")
    suspend fun CommandSender.find(user: User) {
        val player = PlayerDataConfig.getUserData(user.id)
        AiChan.queueCommandReply(
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
        AiChan.queueCommandReply(this, message)
    }
}

fun queryPlayerStatus(id: Long, mcId: String): String {
    val player = PlayerDataConfig.getUserData(id)
    return """
        用户 $id:
        ${if (player.isBanned) "已封禁" else "未封禁"}
        ${if (player.isLinked) "链接的 MCID: $mcId" else "未链接"}
    """.trimIndent()
}
