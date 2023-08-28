package fun.kaituo.aichanmirai;

import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Validator;
import fun.kaituo.aichanmirai.command.*;
import fun.kaituo.aichanmirai.config.MainConfig;
import fun.kaituo.aichanmirai.config.PlayerDataConfig;
import fun.kaituo.aichanmirai.config.ResponseConfig;
import fun.kaituo.aichanmirai.server.SocketServer;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.data.AutoSavePluginConfig;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.utils.MiraiLogger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;


/**
 * 使用 Java 请把
 * {@code /src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin}
 * 文件内容改成 {@code org.example.mirai.plugin.JavaPluginMain} <br/>
 * 也就是当前主类全类名
 * <p>
 * 使用 Java 可以把 kotlin 源集删除且不会对项目有影响
 * <p>
 * 在 {@code settings.gradle.kts} 里改构建的插件名称、依赖库和插件版本
 * <p>
 * 在该示例下的 {@link JvmPluginDescription} 修改插件名称，id 和版本等
 * <p>
 * 可以使用 {@code src/test/kotlin/RunMirai.kt} 在 IDE 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

public final class AiChanMirai extends JavaPlugin {
    public static final AiChanMirai INSTANCE = new AiChanMirai();

    private static final List<AutoSavePluginConfig> configList = List.of(
            MainConfig.INSTANCE,
            PlayerDataConfig.INSTANCE,
            ResponseConfig.INSTANCE
    );

    private AiChanMirai() {
        super(
                new JvmPluginDescriptionBuilder("fun.kaituo.aichan-mirai", "0.1.0")
                        .info("Welcome back, my master!")
                        .name("AiChan-mirai")
                        .build()
        );

        this.logger = getLogger();
    }

    public final MiraiLogger logger;

    private final Queue<Map.Entry<Long, String>> groupMessageQueue = new LinkedList<>();
    private final Queue<Map.Entry<CommandSender, String>> commandReplyQueue = new LinkedList<>();

    private final List<Future<Void>> activeTasks = new ArrayList<>();

    public final Validator<String> validator = new StringValidator() {
    };


    public void queueCommandReplyMessage(CommandSender sender, String content) {
        commandReplyQueue.add(new AbstractMap.SimpleEntry<>(sender, content));
    }

    public void queueGroupMessage(Long groupId, String content) {
        groupMessageQueue.add(new AbstractMap.SimpleEntry<>(groupId, content));
    }

    private void sendGroupMessage(Long groupId, String content) {
        try {
            Bot bot = Bot.getInstance(MainConfig.INSTANCE.getSenderId());

            if (!bot.isOnline()) {
                logger.warning(String.format("Bot %s 已离线，消息发送失败！", bot.getId()));
                return;
            }

            Group group = bot.getGroup(groupId);
            if (group == null) {
                logger.warning(String.format("QQ 群 %s 获取失败，消息发送失败！", groupId));
                return;
            }
            group.sendMessage(content);


        } catch (NoSuchElementException e) {
            logger.warning(String.format("Bot %s 不存在，消息发送失败！", MainConfig.INSTANCE.getSenderId()), e);
        }


    }

    public void saveAllPluginConfig() {
        savePluginConfig(MainConfig.INSTANCE);
        savePluginConfig(PlayerDataConfig.INSTANCE);
        savePluginConfig(ResponseConfig.INSTANCE);
    }

    public void reloadAllPluginConfig() {
        reloadPluginConfig(MainConfig.INSTANCE);
        reloadPluginConfig(PlayerDataConfig.INSTANCE);
        reloadPluginConfig(ResponseConfig.INSTANCE);
    }

    private void registerCommands() {
        CommandManager.INSTANCE.registerCommand(AiChanCommand.INSTANCE, true);
        CommandManager.INSTANCE.registerCommand(MinecraftUserCommand.INSTANCE, true);
        CommandManager.INSTANCE.registerCommand(MinecraftAdminCommand.INSTANCE, true);
        CommandManager.INSTANCE.registerCommand(SayCommand.INSTANCE, true);
        CommandManager.INSTANCE.registerCommand(CmdCommand.INSTANCE, true);
    }

    public void cancelTasks() {
        for (Future<Void> future : activeTasks) {
            future.cancel(false);
        }
        activeTasks.clear();
    }

    public void registerTasks() {
        activeTasks.add(getScheduler().repeating(MainConfig.INSTANCE.getAutoSaveInterval(), this::saveAllPluginConfig));
        activeTasks.add(
                getScheduler().repeating(MainConfig.INSTANCE.getMessageInterval(), () -> {
                    if (!groupMessageQueue.isEmpty()) {
                        long groupId = groupMessageQueue.peek().getKey();
                        String content = groupMessageQueue.peek().getValue();
                        sendGroupMessage(groupId, content);
                        groupMessageQueue.poll();
                    }
                }));
        activeTasks.add(getScheduler().repeating(MainConfig.INSTANCE.getMessageInterval(), () -> {
            if (!commandReplyQueue.isEmpty()) {
                commandReplyQueue.peek().getKey().sendMessage(commandReplyQueue.peek().getValue());
                commandReplyQueue.poll();
            }
        }));
        activeTasks.add(getScheduler().repeating(
                ResponseConfig.INSTANCE.getGreetCoolDown(), AiChanMiraiTimers.INSTANCE::deductGreetCoolDown)
        );
        activeTasks.add(getScheduler().repeating(
                PlayerDataConfig.INSTANCE.getCleanInterval(), PlayerDataConfig.INSTANCE::clean)
        );
    }

    private void subscribeEvents() {
        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        eventChannel.subscribeAlways(GroupMessageEvent.class, AiChanMiraiMessageHandlers.INSTANCE::response);
        eventChannel.subscribeAlways(GroupMessageEvent.class, AiChanMiraiMessageHandlers.INSTANCE::greet);
        eventChannel.subscribeAlways(MemberJoinEvent.class, AiChanMiraiMessageHandlers.INSTANCE::welcomeNewMember);
    }

    @Override
    public void onEnable() {
        reloadAllPluginConfig();
        logger.info("小爱-mirai 已启用");

        registerCommands();
        registerTasks();
        subscribeEvents();

        if (MainConfig.INSTANCE.getToken().isEmpty()) {
            MainConfig.INSTANCE.genKey();
        }

        try {
            SocketServer.INSTANCE.getServer().start();
        } catch (IOException e) {
            logger.warning("服务器启动失败", e);
        }
    }

    @Override
    public void onDisable() {
        saveAllPluginConfig();
        logger.info("小爱-mirai 已停止");
    }
}

