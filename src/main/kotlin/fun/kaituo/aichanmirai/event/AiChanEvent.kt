package `fun`.kaituo.aichanmirai.event

import `fun`.kaituo.aichanmirai.AiChanMiraiMessageHandlers
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent

object AiChanEvent : SimpleListenerHost() {
    @EventHandler
    suspend fun GroupMessageEvent.onUpdateServerMessageThresholdTimer() {
        AiChanMiraiMessageHandlers.updateServerMessageThresholdTimer(this)
    }

    @EventHandler
    suspend fun GroupMessageEvent.onResponse() {
        AiChanMiraiMessageHandlers.response(this)
    }

    @EventHandler
    suspend fun GroupMessageEvent.onGreet() {
        AiChanMiraiMessageHandlers.greet(this)
    }

    @EventHandler
    suspend fun MemberJoinEvent.onWelcomeNewMember() {
        AiChanMiraiMessageHandlers.welcomeNewMember(this)
    }
}
