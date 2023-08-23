package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.PlayerData
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.User

object MinecraftAdminCommand : CompositeCommand (
    AiChanMirai.INSTANCE, "mcop",
    description = "AiMC管理员命令"
){

    @SubCommand
    @Description("为他人链接QQ号和MCID")
    suspend fun CommandSender.link(user: User, mcId: String) {
        val result : PlayerDataConfig.LinkResult = PlayerDataConfig.link(user.id, mcId)
        val nick : String = user.nick
        val id : Long = user.id
        when (result) {
            PlayerDataConfig.LinkResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已为用户 $nick($id) 成功链接ID $mcId")
            }
            PlayerDataConfig.LinkResult.FAIL_ALREADY_LINKED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $nick($id) 已经链接过了！")
            }
            PlayerDataConfig.LinkResult.FAIL_ALREADY_EXIST -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"这个ID已被其他用户链接了！")
            }
        }
    }

    @SubCommand
    @Description("为他人解绑QQ号和MCID")
    suspend fun CommandSender.unlink(user: User) {
        val result : PlayerDataConfig.UnlinkResult = PlayerDataConfig.unlink(user.id)
        val nick : String = user.nick
        val id : Long = user.id
        when (result) {
            PlayerDataConfig.UnlinkResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已为用户 $nick($id) 成功解绑ID")
            }
            PlayerDataConfig.UnlinkResult.FAIL_NOT_LINKED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $nick($id) 还未链接ID！")
            }
        }
    }

    @SubCommand
    @Description("封禁用户(QQ号)")
    suspend fun CommandSender.ban(user : User) {
        val result: PlayerDataConfig.BanResult = PlayerDataConfig.ban(user.id)
        val nick : String = user.nick
        val id : Long = user.id
        when (result) {
            PlayerDataConfig.BanResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已封禁用户 $nick($id)")
            }
            PlayerDataConfig.BanResult.FAIL_ALREADY_BANNED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $nick($id) 已处于封禁状态！")
            }
            PlayerDataConfig.BanResult.FAIL_NOT_FOUND -> {
                AiChanMirai.INSTANCE.logger.warning("This should not occur!")
            }
        }
    }
    @SubCommand
    @Description("解封用户(QQ号)")
    suspend fun CommandSender.pardon(user : User) {
        val result: PlayerDataConfig.PardonResult = PlayerDataConfig.pardon(user.id)
        val nick : String = user.nick
        val id : Long = user.id
        when (result) {
            PlayerDataConfig.PardonResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已解封用户 $nick($id)")
            }
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $nick($id) 未被封禁！")
            }
            PlayerDataConfig.PardonResult.FAIL_NOT_FOUND -> {
                AiChanMirai.INSTANCE.logger.warning("User $id not found. This should not occur!")
            }
        }
    }

    @SubCommand
    @Description("封禁用户(MCID)")
    suspend fun CommandSender.banid(mcId : String) {
        val result: PlayerDataConfig.BanResult = PlayerDataConfig.banId(mcId)
        if (result == PlayerDataConfig.BanResult.FAIL_NOT_FOUND) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"MCID $mcId 不存在！")
            return
        }
        val id : Long = PlayerDataConfig.searchMCId(mcId)
        when (result) {
            PlayerDataConfig.BanResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已封禁用户 ($id)")
            }
            PlayerDataConfig.BanResult.FAIL_ALREADY_BANNED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 ($id) 已处于封禁状态！")
            }

            else -> {
                AiChanMirai.INSTANCE.logger.warning("Wrong condition. This should not occur!")}
        }
    }

    @SubCommand
    @Description("解封用户(MCID)")
    suspend fun CommandSender.pardonid(mcId : String) {
        val result: PlayerDataConfig.PardonResult = PlayerDataConfig.pardonId(mcId)
        if (result == PlayerDataConfig.PardonResult.FAIL_NOT_FOUND) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"MCID $mcId 不存在！")
            return
        }
        val id : Long = PlayerDataConfig.searchMCId(mcId)
        when (result) {
            PlayerDataConfig.PardonResult.SUCCESS -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"已解封用户 ($id)")
            }
            PlayerDataConfig.PardonResult.FAIL_NOT_BANNED -> {
                AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 ($id) 未被封禁！")
            }

            else -> {
                AiChanMirai.INSTANCE.logger.warning("Wrong condition. This should not occur!")
            }
        }
    }

    @SubCommand
    @Description("查询用户状态(QQ号)")
    suspend fun CommandSender.find(user : User) {
        val pData: PlayerData = PlayerDataConfig.getUserData(user.id)
        val id : Long = user.id
        val isLinked: Boolean = pData.isLinked
        val mcId : String = pData.mcId
        val isBanned: Boolean = pData.isBanned
        val displayedMcId = when(isLinked) {
            true -> "\nMCID: $mcId"
            false -> ""
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $id :\n是否链接: $isLinked\n是否封禁: $isBanned$displayedMcId")
    }

    @SubCommand
    @Description("查询用户状态(MCID)")
    suspend fun CommandSender.findid(mcId : String) {
        val id = PlayerDataConfig.searchMCId(mcId)
        if (id == -1L) {
            AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"MCID $mcId 不存在！")
            return
        }
        val pData: PlayerData = PlayerDataConfig.getUserData(id)
        val isLinked: Boolean = pData.isLinked
        val isBanned: Boolean = pData.isBanned
        val displayedMcId = when(isLinked) {
            true -> "\nMCID: $mcId"
            false -> ""
        }
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this,"用户 $id :\n是否链接: $isLinked\n是否封禁: $isBanned$displayedMcId")
    }

}