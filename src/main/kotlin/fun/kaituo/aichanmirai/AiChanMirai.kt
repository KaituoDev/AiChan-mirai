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
    private val commandReplyQueue: Queue<Map.Entry<CommandSender, String>> = LinkedList()
    private val groupMessageQueue: Queue<Map.Entry<Long, String>> = LinkedList()

    // Messages sent by Minecraft servers, have their own timers
    val serverMessages: MutableList<String> = ArrayList()

    val scheduler: JavaPluginScheduler = JavaPluginScheduler(this.coroutineContext)
    private val activeTasks: MutableList<Future<*>> = ArrayList()

    val validator = object : StringValidator {}

    private val commands: List<Command> by services()
    private val configs: List<PluginConfig> by services()
    private val listeners: List<ListenerHost> by services()

    // Hardcoded message polling interval
    private const val MESSAGE_POLLING_INTERVAL = 200L

    // HardCoded message delay
    private const val MESSAGE_DELAY = 1000L

    fun queueCommandReply(sender: CommandSender, content: String) {
        commandReplyQueue.add(AbstractMap.SimpleEntry(sender, content))
        logger.info("Queued command reply: ${content.replace("\n", "\\n")}")
    }

    fun queueGroupMessage(groupId: Long, content: String) {
        groupMessageQueue.add(AbstractMap.SimpleEntry(groupId, content))
        logger.info("Queued message for group $groupId: ${content.replace("\n", "\\n")}")
    }

    // If cool down is over, poll one group message and send it
    private fun groupMessagePolling() {
        if (AiChanMiraiTimers.messageCoolDownTimer > 0) {
            return
        }
        if (groupMessageQueue.isNotEmpty()) {
            val entry = groupMessageQueue.peek()
            scheduler.delayed(MESSAGE_DELAY) { sendGroupMessage(entry.key, entry.value) }
            groupMessageQueue.poll()
            AiChanMiraiTimers.messageCoolDownTimer = MainConfig.messageInterval
        }
    }

    // If cool down is over, poll one command reply and send it
    private fun commandReplyPolling() {
        if (AiChanMiraiTimers.commandReplyCoolDownTimer > 0) {
            return
        }
        if (commandReplyQueue.isNotEmpty()) {
            val entry = commandReplyQueue.peek()
            val commandSender = entry.key
            val content = entry.value
            scheduler.delayed(MESSAGE_DELAY) { launch { commandSender.sendMessage(content) } }
            commandReplyQueue.poll()
            AiChanMiraiTimers.commandReplyCoolDownTimer = MainConfig.commandReplyInterval
        }
    }

    private fun collectLatestServerMessages(maxLines: Int): String {
        val groupMessageCopy = serverMessages.toList()
        if (groupMessageCopy.size <= maxLines) {
            return groupMessageCopy.joinToString("\n")
        } else {
            val prefix = "(已隐藏更早的" + (groupMessageCopy.size - maxLines) + "条消息)\n"
            return prefix + groupMessageCopy.subList(groupMessageCopy.size - maxLines, groupMessageCopy.size)
                .joinToString("\n")
        }
    }

    private fun serverMessagePolling() {
        if (AiChanMiraiTimers.serverMessageThresholdTimer > 0) {
            if (AiChanMiraiTimers.serverMessageCoolDownTimerFast > 0) {
                // Other users have sent messages within the threshold,
                // but the fast cool down is not over
                return
            }
        } else {
            if (AiChanMiraiTimers.serverMessageCoolDownTimerSlow > 0) {
                // Other users have not sent messages within the threshold,
                // and the slow cool down is not over
                return
            }
        }
        if (serverMessages.isNotEmpty()) {
            val message = collectLatestServerMessages(MainConfig.serverMessageMaxLines)
            serverMessages.clear()
            val groupId = MainConfig.messagingGroup
            scheduler.delayed(MESSAGE_DELAY) { sendGroupMessage(groupId, message) }
            AiChanMiraiTimers.serverMessageCoolDownTimerFast = MainConfig.serverMessageIntervalFast
            AiChanMiraiTimers.serverMessageCoolDownTimerSlow = MainConfig.serverMessageIntervalSlow
        }
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
                AiChanMiraiTimers.MESSAGE_TIMERS_UPDATE_INTERVAL to AiChanMiraiTimers::updateTimers,
                (MainConfig.autoSaveInterval) to this::saveAllPluginConfig,
                MESSAGE_POLLING_INTERVAL to this::commandReplyPolling,
                MESSAGE_POLLING_INTERVAL to this::groupMessagePolling,
                MESSAGE_POLLING_INTERVAL to this::serverMessagePolling,
                (ResponseConfig.greetCoolDown) to AiChanMiraiTimers::deductGreetCounter,
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
