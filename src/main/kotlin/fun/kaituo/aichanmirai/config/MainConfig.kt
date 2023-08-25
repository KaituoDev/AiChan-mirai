package `fun`.kaituo.aichanmirai.config

import com.macasaet.fernet.Key
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import `fun`.kaituo.aichanmirai.AiChanMirai.INSTANCE as AiChan

object MainConfig : AutoSavePluginConfig("MainConfig") {

    @ValueDescription("配置自动保存时间")
    val autoSaveInterval by value<Long>(600000)

    @ValueDescription("发言间隔时间")
    val messageInterval by value<Long>(500)

    @ValueDescription("机器人的QQ号")
    val senderId by value<Long>(123456)

    @ValueDescription("启用通讯的群")
    val messagingGroup by value<Long>(123456)

    @ValueDescription("启用欢迎和应答的群")
    val responseGroups: MutableList<Long> by value()

    @ValueDescription("绑定的 IP 地址")
    val bindAddress by value<String>("0.0.0.0")

    @ValueDescription("绑定的端口")
    val bindPort by value<Int>(30300)

    @ValueDescription("客户端认证的密码")
    val token: MutableList<String> by value()

    fun genKey() {
        val keyString = Key.generateKey().serialise()
        if (token.isEmpty()) {
            token.add(0, keyString)
        } else {
            token[0] = keyString
        }
        AiChan.saveAllPluginConfig()
    }
}
