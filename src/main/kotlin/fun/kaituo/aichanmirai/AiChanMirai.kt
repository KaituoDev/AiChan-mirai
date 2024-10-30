package `fun`.kaituo.aichanmirai

import com.macasaet.fernet.StringValidator
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import `fun`.kaituo.aichanmirai.server.SocketServer
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.data.PluginConfig
import net.mamoe.mirai.console.plugin.jvm.JavaPluginScheduler
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.registerTo
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.HashMap

/**
 * 使用 Java 请把
 * `/src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.JavaPluginMain` <br></br>
 * 也就是当前主类全类名
 *
 *
 * 使用 Java 可以把 kotlin 源集删除且不会对项目有影响
 *
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id 和版本等
 *
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 IDE 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */
object AiChanMirai : KotlinPlugin(JvmPluginDescription.loadFromResource()) {
    private val groupMessages: MutableMap<Long, String> = HashMap()
    private val groupMessageQueue: Queue<Map.Entry<Long, String>> = LinkedList()
    private val commandReplyQueue: Queue<Map.Entry<CommandSender, String>> = LinkedList()

    val scheduler: JavaPluginScheduler = JavaPluginScheduler(this.coroutineContext)
    private val activeTasks: MutableList<Future<*>> = ArrayList()

    val validator = object : StringValidator {}

    private val commands: List<Command> by services()
    private val configs: List<PluginConfig> by services()
    private val listeners: List<ListenerHost> by services()

    fun replyCommand(sender: CommandSender, content: String) {
        commandReplyQueue.add(AbstractMap.SimpleEntry(sender, content))
        logger.info("Queued command reply: ${content.replace("\n", "\\n")}")
    }

    // Queue one group message, concatenate if multiple messages are queued for the same group
    fun queueGroupMessage(groupId: Long, content: String) {
        if (groupId in groupMessages) {
            groupMessages[groupId] += "\n" + content
        } else {
            groupMessages[groupId] = content
        }
        logger.info("Queued message for group $groupId: ${content.replace("\n", "\\n")}")
    }

    // Queue all group messages
    fun queueGroupMessages() {
        groupMessages.forEach { entry: Map.Entry<Long, String> ->
            groupMessageQueue.add(AbstractMap.SimpleEntry(entry.key, entry.value))
        }
        groupMessages.clear()
    }
    private fun sendGroupMessage(groupId: Long, content: String) {
        try {
            val bot = Bot.getInstance(MainConfig.senderId)
            if (!bot.isOnline) {
                logger.warning("Bot ${bot.id} 已离线，消息发送失败！")
                return
            }

            val group = bot.getGroup(groupId)
            if (group == null) {
                logger.warning("QQ 群 $groupId 获取失败，消息发送失败！")
                return
            }

            launch { group.sendMessage(content) }

            logger.info("Sent message for group $groupId: $content")
        } catch (e: NoSuchElementException) {
            logger.warning("Bot ${MainConfig.senderId} 不存在，消息发送失败！", e)
        }
    }

    fun saveAllPluginConfig() {
        for (config in configs) config.save()
    }

    fun reloadAllPluginConfig() {
        for (config in configs) config.reload()
    }

    fun cancelTasks() {
        activeTasks.run {
            forEach { it.cancel(false) }
            clear()
        }
    }

    fun registerTasks() {
        activeTasks.addAll(
            listOf(
                (MainConfig.autoSaveInterval) to this::saveAllPluginConfig,
                (MainConfig.messageInterval) to {
                    queueGroupMessages()
                    if (groupMessageQueue.isNotEmpty()) {
                        val entry = groupMessageQueue.peek()
                        sendGroupMessage(entry.key, entry.value)
                        groupMessageQueue.poll()
                    }
                },
                (MainConfig.messageInterval) to {
                    if (commandReplyQueue.isNotEmpty()) {
                        val entry = commandReplyQueue.peek()
                        val commandSender = entry.key
                        val content = entry.value
                        launch { commandSender.sendMessage(content) }
                        commandReplyQueue.poll()
                    }
                },
                (ResponseConfig.greetCoolDown) to AiChanMiraiTimers.INSTANCE::deductGreetCoolDown,
                (PlayerDataConfig.cleanInterval) to PlayerDataConfig::clean
            ).map { (interval, action) -> scheduler.repeating(interval, action) }
        )
    }

    override fun onEnable() {
        reloadAllPluginConfig()
        for (command in commands) command.register(true)
        registerTasks()
        for (listener in listeners) (listener as SimpleListenerHost).registerTo(
            GlobalEventChannel
                .filterIsInstance<GroupEvent>()
                .filter { it.bot.id == MainConfig.senderId && it.group.id in MainConfig.responseGroups }
                .parentScope(this)
        )

        logger.info("小爱-mirai 已启用")

        val mainConfig = MainConfig
        if (mainConfig.token.isEmpty()) {
            mainConfig.genKey()
        }

        SocketServer.INSTANCE.server.start()
    }

    override fun onDisable() {
        saveAllPluginConfig()
        logger.info("小爱-mirai 已停止")
    }
}
