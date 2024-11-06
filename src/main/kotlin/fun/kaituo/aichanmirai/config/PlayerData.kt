package `fun`.kaituo.aichanmirai.config

import `fun`.kaituo.aichanmirai.server.SocketPacket

class PlayerData(var userId: Long, var isLinked: Boolean, var mcId: String, var isBanned: Boolean) {
    fun clone(originalData: PlayerData): PlayerData {
        return PlayerData(originalData.userId, originalData.isLinked, originalData.mcId, originalData.isBanned)
    }

    fun getStatusPacket(): SocketPacket {
        return SocketPacket(SocketPacket.PacketType.PLAYER_LOOKUP_RESULT_TO_SERVER).apply {
            this[0] = userId.toString()
            this[1] = isLinked.toString()
            this[2] = mcId
            this[3] = isBanned.toString()
        }
    }
}
