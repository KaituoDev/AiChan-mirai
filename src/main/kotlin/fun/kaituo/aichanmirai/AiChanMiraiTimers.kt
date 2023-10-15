package `fun`.kaituo.aichanmirai

class AiChanMiraiTimers {
    var greetCounter = 0
        private set

    fun deductGreetCoolDown() {
        greetCounter -= 1
        if (greetCounter < 0) {
            greetCounter = 0
        }
    }

    fun addGreetCoolDown() {
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

    companion object {
        val INSTANCE = AiChanMiraiTimers()
    }
}
