package `fun`.kaituo.aichanmirai.command

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object AiChanCommand : CompositeCommand(
    AiChanMirai.INSTANCE,
    "ai",
    description = "小爱主命令",
) {
    @SubCommand
    @Description("重载小爱配置和数据")
    suspend fun CommandSender.reload() {
        AiChanMirai.INSTANCE.cancelTasks()
        AiChanMirai.INSTANCE.reloadAllPluginConfig()
        AiChanMirai.INSTANCE.registerTasks()
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "已重新加载配置和数据")
    }

    @SubCommand
    @Description("保存小爱配置和数据")
    suspend fun CommandSender.save() {
        AiChanMirai.INSTANCE.saveAllPluginConfig()
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "已保存配置和数据")
    }

    @SubCommand
    @Description("设定精确触发关键词")
    suspend fun CommandSender.set(key: String, vararg reply: String) {
        val replyString = reply.joinToString(" ")
        ResponseConfig.exactMatchResponses[key] = replyString
        AiChanMirai.INSTANCE.queueCommandReplyMessage(
            this,
            "成功设置关键词 $key 触发回答 $replyString"
        )
    }

    @SubCommand
    @Description("删除精确触发关键词")
    suspend fun CommandSender.rm(key: String) {
        ResponseConfig.exactMatchResponses.remove(key)
        AiChanMirai.INSTANCE.queueCommandReplyMessage(
            this,
            "成功删除关键词 $key (如果存在)"
        )
    }

    @SubCommand
    @Description("设定包含触发关键词")
    suspend fun CommandSender.setc(key: String, vararg reply: String) {
        val replyString = reply.joinToString(" ")
        ResponseConfig.containMatchResponses[key] = replyString
        AiChanMirai.INSTANCE.queueCommandReplyMessage(
            this,
            "成功设置(包含)关键词 $key 触发回答 $replyString"
        )
    }

    @SubCommand
    @Description("删除包含触发关键词")
    suspend fun CommandSender.rmc(key: String) {
        ResponseConfig.containMatchResponses.remove(key)
        AiChanMirai.INSTANCE.queueCommandReplyMessage(
            this,
            "成功删除(包含)关键词 $key (如果存在)"
        )
    }

    @SubCommand
    @Description("列出所有关键词")
    suspend fun CommandSender.list() {
        val exactMatchKeywords = ResponseConfig.exactMatchResponses.keys.joinToString(" ")
        val containMatchKeywords = ResponseConfig.containMatchResponses.keys.joinToString(" ")
        val message = listOf(
            "精确匹配关键词有:",
            exactMatchKeywords,
            "包含匹配关键词有:",
            containMatchKeywords
        ).joinToString("\n")
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, message)
    }

    @SubCommand
    @Description("刷新 Socket 通信 Token")
    suspend fun CommandSender.genkey() {
        MainConfig.genKey()
        AiChanMirai.INSTANCE.queueCommandReplyMessage(this, "成功刷新 Token，请前往配置文件复制")
    }
}
