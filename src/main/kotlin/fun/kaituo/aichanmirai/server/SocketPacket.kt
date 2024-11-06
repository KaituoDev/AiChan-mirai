package `fun`.kaituo.aichanmirai.server

import com.google.gson.JsonParser
import com.google.gson.annotations.Expose

// Remember to update Packet on the other end
class SocketPacket(@field:Expose val packetType: PacketType) {
    enum class PacketType {
        HEARTBEAT_TO_BOT,
        SERVER_CHAT_TO_BOT,
        GROUP_CHAT_TO_SERVER,
        PLAYER_LOOKUP_REQUEST_TO_BOT,
        PLAYER_LOOKUP_RESULT_TO_SERVER,
        PLAYER_NOT_FOUND_TO_SERVER,
        LIST_REQUEST_TO_SERVER,
        COMMAND_TO_SERVER,
        SERVER_INFORMATION_TO_BOT
    }

    operator fun set(index: Int, data: String) {
        content.add(index, data)
    }

    operator fun get(index: Int): String {
        return content[index]
    }

    @Expose
    val content: MutableList<String> = ArrayList()

    companion object {
        const val DELIMITER = "|DELIMITER|"
        fun parsePacket(string: String): SocketPacket {
            val packetObject = JsonParser.parseString(string).asJsonObject
            val contentArray = packetObject.getAsJsonArray("content")
            return SocketPacket(PacketType.valueOf(packetObject["packetType"].asString)).apply {
                if (contentArray != null) {
                    for (content in contentArray) {
                        this.content.add(content.asString)
                    }
                }
            }
        }
    }
}
