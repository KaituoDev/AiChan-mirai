package fun.kaituo.aichanmirai;

import fun.kaituo.aichanmirai.config.MainConfig;
import fun.kaituo.aichanmirai.config.ResponseConfig;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;

import java.util.Map;

public class AiChanMiraiMessageHandlers {

    public static final AiChanMiraiMessageHandlers INSTANCE = new AiChanMiraiMessageHandlers();


    public void greet(GroupMessageEvent e) {
        Bot bot = e.getBot();
        long senderId = MainConfig.INSTANCE.getSenderId();
        long groupId = e.getGroup().getId();
        String messageContent = e.getMessage().contentToString();
        int greetCounter = AiChanMiraiTimers.INSTANCE.getGreetCounter();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(groupId)) {
            return;
        }

        if (!messageContent.equals("小爱")) {
            return;
        }

        if (greetCounter > 1) {
            return;
        }

        AiChanMirai.INSTANCE.queueGroupMessage(
                groupId,
                greetCounter == 0 ? ResponseConfig.INSTANCE.getFirstGreet() : ResponseConfig.INSTANCE.getSecondGreet()
        );
        AiChanMiraiTimers.INSTANCE.addGreetCoolDown();
    }

    public void response(GroupMessageEvent e) {
        Bot bot = e.getBot();
        long senderId = MainConfig.INSTANCE.getSenderId();
        long groupId = e.getGroup().getId();
        String messageContent = e.getMessage().contentToString();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(groupId)) {
            return;
        }

        handleMatchingResponse(messageContent, groupId, ResponseConfig.INSTANCE.getExactMatchResponses());
        handleMatchingResponse(messageContent, groupId, ResponseConfig.INSTANCE.getContainMatchResponses());
    }

    private void handleMatchingResponse(String messageContent, long groupId, Map<String, String> responses) {
        String mode = responses == ResponseConfig.INSTANCE.getExactMatchResponses() ? "精确" : "包含";
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            boolean isExactMatch = messageContent.equals(key) && mode.equals("精确");
            boolean isContainsMatch = messageContent.contains(key) && mode.equals("包含");

            if (isExactMatch || isContainsMatch) {
                if (AiChanMiraiTimers.INSTANCE.checkResponseAvailability(key)) {
                    AiChanMirai.INSTANCE.queueGroupMessage(groupId, value);
                    AiChanMiraiTimers.INSTANCE.setResponseUnavailable(key);
                    AiChanMirai.INSTANCE.getScheduler().delayed(
                            ResponseConfig.INSTANCE.getResponseCoolDown(),
                            () -> AiChanMiraiTimers.INSTANCE.setResponseAvailable(key)
                    );
                } else {
                    AiChanMirai.INSTANCE.logger.info(String.format(
                            "%s关键词 %s 冷却中",
                            mode,
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
        long groupId = e.getGroup().getId();

        if (bot.getId() != senderId || !MainConfig.INSTANCE.getResponseGroups().contains(groupId)) {
            return;
        }

        String welcomeMessage = ResponseConfig.INSTANCE.getWelcomeMessage()
                .replace("%nick%", e.getMember().getNick());
        AiChanMirai.INSTANCE.queueGroupMessage(groupId, welcomeMessage);
    }
}
