package `fun`.kaituo.aichanmirai

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Utils {

    fun removeMinecraftColor(message: String): String {
        return message.replace("[&§]([0-9a-fk-or])".toRegex(), "")
    }

    fun getFormattedTime(): String {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val formattedTime = "[${currentTime.format(formatter)}]"
        return formattedTime
    }
}
