package `fun`.kaituo.aichanmirai.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object ResponseConfig : AutoSavePluginConfig("ResponseConfig") {
    @ValueDescription("欢迎消息")
    val welcomeMessage by value<String>("欢迎主人%nick%入群，请阅读群公告并修改群昵称哦！")

    @ValueDescription("非用户执行消息")
    val userOnlyMessage by value<String>("主人，只有QQ用户才能执行该指令哦")

    @ValueDescription("仅通讯群可用消息")
    val groupOnlyMessage by value<String>("主人，这个指令只有在通讯群才能使用哦")

    @ValueDescription("呼唤刷新间隔(ms)")
    val greetCoolDown by value<Long>(10000)

    @ValueDescription("关键词回应间隔(ms)")
    val responseCoolDown by value<Long>(60000)

    @ValueDescription("首次呼唤应答")
    val firstGreet by value<String>("我在，主人")

    @ValueDescription("再次呼唤应答")
    val secondGreet by value<String>("我在，不用再叫我啦(〃'▽'〃)")

    @ValueDescription("精确匹配回应")
    val exactMatchResponses: MutableMap<String, String> by value()

    @ValueDescription("包含匹配回应")
    val containMatchResponses: MutableMap<String, String> by value()
}
