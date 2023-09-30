package `fun`.kaituo.aichanmirai

import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.ResponseConfig
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent

object AiChanMiraiMessageHandlers {
    fun greet(e: GroupMessageEvent) {
        val bot = e.bot
        val senderId = MainConfig.senderId
        val groupId = e.group.id
        val messageContent = e.message.contentToString()
        val greetCounter = AiChanMiraiTimers.INSTANCE.greetCounter

        val notInResponseGroup = !MainConfig.responseGroups.contains(groupId)
        val notSameSender = bot.id != senderId
        val notPing = messageContent != "小爱"
        val shouldShutUp = greetCounter > 1

        if (notSameSender || notInResponseGroup || notPing || shouldShutUp) {
            return
        }

        AiChanMirai.sendGroup(
            groupId,
            if (greetCounter == 0) ResponseConfig.firstGreet else ResponseConfig.secondGreet
        )
        AiChanMiraiTimers.INSTANCE.addGreetCoolDown()
    }

    fun response(e: GroupMessageEvent) {
        val bot = e.bot
        val senderId = MainConfig.senderId
        val groupId = e.group.id
        val messageContent = e.message.contentToString()

        val notSameSender = bot.id != senderId
        val notInResponseGroup = !MainConfig.responseGroups.contains(groupId)
        if (notSameSender || notInResponseGroup) {
            return
        }

        handleMatchingResponse(messageContent, groupId, ResponseConfig.exactMatchResponses)
        handleMatchingResponse(messageContent, groupId, ResponseConfig.containMatchResponses)
    }

    private fun handleMatchingResponse(messageContent: String, groupId: Long, responses: Map<String, String>) {
        val mode = if (responses === ResponseConfig.exactMatchResponses) "精确" else "包含"
        for ((key, value) in responses) {
            val isExactMatch = messageContent == key && mode == "精确"
            val isContainsMatch = messageContent.contains(key) && mode == "包含"
            if (isExactMatch || isContainsMatch) {
                if (AiChanMiraiTimers.INSTANCE.checkResponseAvailability(key)) {
                    AiChanMirai.sendGroup(groupId, value)
                    AiChanMiraiTimers.INSTANCE.setResponseUnavailable(key)
                    AiChanMirai.scheduler.delayed(
                        ResponseConfig.responseCoolDown
                    ) { AiChanMiraiTimers.INSTANCE.setResponseAvailable(key) }
                } else {
                    AiChanMirai.logger.info("${mode}关键词 $key 冷却中")
                }
                return
            }
        }
    }

    fun welcomeNewMember(e: MemberJoinEvent) {
        val bot = e.bot
        val senderId = MainConfig.senderId
        val groupId = e.group.id

        val notInResponseGroup = !MainConfig.responseGroups.contains(groupId)
        val notSameSender = bot.id != senderId
        if (notSameSender || notInResponseGroup) {
            return
        }

        val welcomeMessage = ResponseConfig.welcomeMessage
            .replace("%nick%", e.member.nick)
        AiChanMirai.sendGroup(groupId, welcomeMessage)
    }
}
