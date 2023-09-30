package `fun`.kaituo.aichanmirai.server

import com.google.gson.JsonParser
import com.google.gson.annotations.Expose

// Remember to update Packet on the other end
class SocketPacket(@field:Expose val packetType: PacketType) {
    enum class PacketType {
        HEARTBEAT,
        GROUP_TEXT,
        SERVER_TEXT,
        PLAYER_LOOKUP,
        PLAYER_STATUS,
        PLAYER_NOT_FOUND,
        LIST_REQUEST,
        SERVER_COMMAND
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
