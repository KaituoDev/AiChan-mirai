package `fun`.kaituo.aichanmirai

object AiChanMiraiTimers {

    // Hardcoded timer update interval
    const val MESSAGE_TIMERS_UPDATE_INTERVAL = 50L

    var greetCounter = 0
        private set

    var commandReplyCoolDownTimer = 0L
    var messageCoolDownTimer = 0L

    var serverMessageCoolDownTimerFast = 0L
    var serverMessageCoolDownTimerSlow = 0L
    var serverMessageThresholdTimer = 0L

    fun updateTimers() {
        val delta = MESSAGE_TIMERS_UPDATE_INTERVAL
        commandReplyCoolDownTimer =
            if (commandReplyCoolDownTimer > delta) commandReplyCoolDownTimer - delta else 0
        messageCoolDownTimer =
            if (messageCoolDownTimer > delta) messageCoolDownTimer - delta else 0
        serverMessageCoolDownTimerFast =
            if (serverMessageCoolDownTimerFast > delta) serverMessageCoolDownTimerFast - delta else 0
        serverMessageCoolDownTimerSlow =
            if (serverMessageCoolDownTimerSlow > delta) serverMessageCoolDownTimerSlow - delta else 0
        serverMessageThresholdTimer =
            if (serverMessageThresholdTimer > delta) serverMessageThresholdTimer - delta else 0
    }

    fun deductGreetCounter() {
        greetCounter -= 1
        if (greetCounter < 0) {
            greetCounter = 0
        }
    }

    fun addGreetCounter() {
        greetCounter += 1
        if (greetCounter > 2) {
            greetCounter = 2
        }
    }

    private val responseInCoolDown: MutableList<String> = ArrayList()

    fun checkResponseAvailability(key: String): Boolean {
        return !responseInCoolDown.contains(key)
    }

    fun setResponseAvailable(key: String) {
        responseInCoolDown.removeIf { anObject: String? -> key == anObject }
    }

    fun setResponseUnavailable(key: String) {
        if (checkResponseAvailability(key)) {
            responseInCoolDown.add(key)
        }
    }
}
