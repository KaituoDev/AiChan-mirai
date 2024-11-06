package `fun`.kaituo.aichanmirai

object Utils {

    fun removeMinecraftColor(message: String): String {
        return message.replace("[&§]([0-9a-fk-or])".toRegex(), "")
    }
}
