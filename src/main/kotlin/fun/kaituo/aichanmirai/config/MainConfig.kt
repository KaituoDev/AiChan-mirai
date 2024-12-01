package `fun`.kaituo.aichanmirai.config

import com.macasaet.fernet.Key
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import `fun`.kaituo.aichanmirai.AiChanMirai as AiChan

object MainConfig : AutoSavePluginConfig("MainConfig") {

    @ValueDescription("配置自动保存时间")
    val autoSaveInterval by value<Long>(600000)

    @ValueDescription("指令反馈间隔时间")
    val commandReplyInterval by value<Long>(500)

    @ValueDescription("消息间隔时间")
    val messageInterval by value<Long>(500)

    @ValueDescription("高频推送间隔时间，仅作用于服务器消息")
    val serverMessageIntervalFast by value<Long>(10000)

    @ValueDescription("低频推送间隔时间，仅作用于服务器消息")
    val serverMessageIntervalSlow by value<Long>(1800000)

    @ValueDescription("高频推送时间阈值，如果在此时间范围内没有其他用户发送消息，则机器人停止高频推送")
    val serverMessageThreshold by value<Long>(60000)

    @ValueDescription("单次发送消息的最大行数")
    val serverMessageMaxLines by value<Int>(20)

    @ValueDescription("机器人的QQ号")
    val senderId by value<Long>(123456)

    @ValueDescription("启用通讯的群")
    val messagingGroup by value<Long>(123456)

    @ValueDescription("群聊向服务器同步聊天时的前缀")
    val groupChatPrefix by value<String>("§7[§b群§7]§r")

    @ValueDescription("启用欢迎和应答的群")
    val responseGroups: MutableList<Long> by value()

    @ValueDescription("绑定的 IP 地址")
    val bindAddress by value<String>("0.0.0.0")

    @ValueDescription("绑定的端口")
    val bindPort by value<Int>(30300)

    @ValueDescription("客户端认证的密码")
    var token by value<String>()

    fun genKey() {
        val keyString = Key.generateKey().serialise()
        if (token.isEmpty()) {
            token = keyString
        }
        AiChan.saveAllPluginConfig()
    }
}
