package fun.kaituo.aichanmirai;

import fun.kaituo.aichanmirai.config.MainConfig;
import fun.kaituo.aichanmirai.config.ResponseConfig;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;

import java.util.HashMap;
import java.util.Map;

public class AiChanMiraiMessageHandlers {

    public static final AiChanMiraiMessageHandlers INSTANCE = new AiChanMiraiMessageHandlers();


    public void greet(GroupMessageEvent e) {
        if (e.getBot().getId() != MainConfig.INSTANCE.getSenderId()) {
            return;
        }
        if (!MainConfig.INSTANCE.getResponseGroups().contains(e.getGroup().getId())) {
            return;
        }
        if (!e.getMessage().contentToString().equals("小爱")) {
            return;
        }
        if (AiChanMiraiTimers.INSTANCE.getGreetCounter() == 0) {
            AiChanMirai.INSTANCE.queueGroupMessage(e.getGroup().getId(), ResponseConfig.INSTANCE.getFirstGreet());
            AiChanMiraiTimers.INSTANCE.addGreetCoolDown();
        } else if (AiChanMiraiTimers.INSTANCE.getGreetCounter() == 1) {
            AiChanMirai.INSTANCE.queueGroupMessage(e.getGroup().getId(), ResponseConfig.INSTANCE.getSecondGreet());
            AiChanMiraiTimers.INSTANCE.addGreetCoolDown();
        }
    }

    public void response(GroupMessageEvent e) {
        if (e.getBot().getId() != MainConfig.INSTANCE.getSenderId()) {
            return;
        }
        if (!MainConfig.INSTANCE.getResponseGroups().contains(e.getGroup().getId())) {
            return;
        }
        Map<String, String> exactCopy = new HashMap<>(ResponseConfig.INSTANCE.getExactMatchResponses());
        for (Map.Entry<String, String> entry : exactCopy.entrySet()) {
            if (e.getMessage().contentToString().equals(entry.getKey())) {
                if (AiChanMiraiTimers.INSTANCE.checkResponseAvailability(entry.getKey())) {
                    AiChanMirai.INSTANCE.queueGroupMessage(e.getGroup().getId(), entry.getValue());
                    AiChanMiraiTimers.INSTANCE.setResponseUnavailable(entry.getKey());
                    AiChanMirai.INSTANCE.getScheduler().delayed(
                            ResponseConfig.INSTANCE.getResponseCoolDown(),
                            () -> AiChanMiraiTimers.INSTANCE.setResponseAvailable(entry.getKey())
                    );
                } else {
                    AiChanMirai.INSTANCE.getLogger().info(String.format("精确关键词 %s 冷却中", entry.getKey()));
                }
                return;
            }
        }

        Map<String, String> containCopy = new HashMap<>(ResponseConfig.INSTANCE.getContainMatchResponses());
        for (Map.Entry<String, String> entry : containCopy.entrySet()) {
            if (e.getMessage().contentToString().contains(entry.getKey())) {
                if (AiChanMiraiTimers.INSTANCE.checkResponseAvailability(entry.getKey())) {
                    AiChanMirai.INSTANCE.queueGroupMessage(e.getGroup().getId(), entry.getValue());
                    AiChanMiraiTimers.INSTANCE.setResponseUnavailable(entry.getKey());
                    AiChanMirai.INSTANCE.getScheduler().delayed(
                            ResponseConfig.INSTANCE.getResponseCoolDown(),
                            () -> AiChanMiraiTimers.INSTANCE.setResponseAvailable(entry.getKey())
                    );
                } else {
                    AiChanMirai.INSTANCE.getLogger().info(String.format("包含关键词 %s 冷却中", entry.getKey()));
                }
            }
        }
    }

    public void welcomeNewMember(MemberJoinEvent e) {
        if (e.getBot().getId() != MainConfig.INSTANCE.getSenderId()) {
            return;
        }
        if (!MainConfig.INSTANCE.getResponseGroups().contains(e.getGroup().getId())) {
            return;
        }
        AiChanMirai.INSTANCE.queueGroupMessage(
                e.getGroup().getId(),
                ResponseConfig.INSTANCE.getWelcomeMessage().replace("%nick%", e.getMember().getNick())
        );
    }
}
