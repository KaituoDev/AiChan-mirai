package fun.kaituo.aichanmirai;

import fun.kaituo.aichanmirai.config.MainConfig;
import fun.kaituo.aichanmirai.config.ResponseConfig;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;

import java.util.Map;

public class AiChanMiraiMessageHandlers {

    public static final AiChanMiraiMessageHandlers INSTANCE = new AiChanMiraiMessageHandlers();


    public void greet(GroupMessageEvent e) {
        Bot bot = e.getBot();
        long senderId = MainConfig.INSTANCE.getSenderId();
        Group group = e.getGroup();
        String messageContent = e.getMessage().contentToString();
        int greetCounter = AiChanMiraiTimers.INSTANCE.getGreetCounter();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(group.getId())) {
            return;
        }

        if (!messageContent.equals("小爱")) {
            return;
        }

        switch (greetCounter) {
            case 0 -> AiChanMirai.INSTANCE.queueGroupMessage(
                    e.getGroup().getId(), ResponseConfig.INSTANCE.getFirstGreet()
            );
            case 1 -> AiChanMirai.INSTANCE.queueGroupMessage(
                    e.getGroup().getId(), ResponseConfig.INSTANCE.getSecondGreet()
            );
            default -> {
                return;
            }
        }
        AiChanMiraiTimers.INSTANCE.addGreetCoolDown();
    }

    public void response(GroupMessageEvent e) {
        Bot bot = e.getBot();
        long senderId = MainConfig.INSTANCE.getSenderId();
        Group group = e.getGroup();
        String messageContent = e.getMessage().contentToString();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(group.getId())) {
            return;
        }


        if (e.getBot().getId() != MainConfig.INSTANCE.getSenderId()) {
            return;
        }
        if (!MainConfig.INSTANCE.getResponseGroups().contains(e.getGroup().getId())) {
            return;
        }

        handleMatchingResponse(messageContent, group, ResponseConfig.INSTANCE.getExactMatchResponses());
        handleMatchingResponse(messageContent, group, ResponseConfig.INSTANCE.getContainMatchResponses());
    }

    private void handleMatchingResponse(String messageContent, Group group, Map<String, String> responses) {
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (messageContent.equals(entry.getKey()) || messageContent.contains(entry.getKey())) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (AiChanMiraiTimers.INSTANCE.checkResponseAvailability(key)) {
                    AiChanMirai.INSTANCE.queueGroupMessage(group.getId(), value);
                    AiChanMiraiTimers.INSTANCE.setResponseUnavailable(key);
                    AiChanMirai.INSTANCE.getScheduler().delayed(
                            ResponseConfig.INSTANCE.getResponseCoolDown(),
                            () -> AiChanMiraiTimers.INSTANCE.setResponseAvailable(key)
                    );
                } else {
                    AiChanMirai.INSTANCE.logger.info(String.format(
                            "%s关键词 %s 冷却中",
                            responses == ResponseConfig.INSTANCE.getExactMatchResponses() ? "精确" : "包含",
                            key
                    ));
                }
                return;
            }
        }
    }

    public void welcomeNewMember(MemberJoinEvent e) {
        Bot bot = e.getBot();
        long senderId = MainConfig.INSTANCE.getSenderId();
        Group group = e.getGroup();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(group.getId())) {
            return;
        }

        String welcomeMessage = ResponseConfig.INSTANCE.getWelcomeMessage()
                .replace("%nick%", e.getMember().getNick());
        AiChanMirai.INSTANCE.queueGroupMessage(group.getId(), welcomeMessage);
    }
}
