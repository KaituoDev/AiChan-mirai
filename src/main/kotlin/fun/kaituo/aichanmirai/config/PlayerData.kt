package `fun`.kaituo.aichanmirai.config

import `fun`.kaituo.aichanmirai.server.SocketPacket

class PlayerData(var userId: Long, var isLinked: Boolean,var mcId: String,var isBanned: Boolean) {
    fun clone(originalData : PlayerData) : PlayerData{
        return PlayerData(originalData.userId, originalData.isLinked, originalData.mcId, originalData.isBanned)
    }

    fun getStatusPacket() : SocketPacket {
        val packet = SocketPacket(SocketPacket.PacketType.PLAYER_STATUS)
        packet[0] = userId.toString()
        packet[1] = isLinked.toString()
        packet[2] = mcId
        packet[3] = isBanned.toString()
        return packet
    }

}