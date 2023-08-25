package `fun`.kaituo.aichanmirai.config

import `fun`.kaituo.aichanmirai.server.SocketPacket
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import `fun`.kaituo.aichanmirai.AiChanMirai.INSTANCE as AiChan
import `fun`.kaituo.aichanmirai.server.SocketServer.INSTANCE as SocketServer

object PlayerDataConfig : AutoSavePluginConfig("UserData") {

    private const val ID_UNDEFINED = "!UNDEFINED!"
    private const val ID_UNLINKED = "!UNLINKED!"

    @ValueDescription("自动清理冗余信息间隔(ms)")
    val cleanInterval by value<Long>(1800000)

    @ValueDescription("玩家信息")
    val playerDataMap: MutableMap<Long, MutableMap<String, String>> by value()

    enum class LinkResult {
        SUCCESS, FAIL_ALREADY_EXIST, FAIL_ALREADY_LINKED
    }

    enum class UnlinkResult {
        SUCCESS, FAIL_NOT_LINKED
    }

    enum class BanResult {
        SUCCESS, FAIL_ALREADY_BANNED, FAIL_NOT_FOUND
    }

    enum class PardonResult {
        SUCCESS, FAIL_NOT_BANNED, FAIL_NOT_FOUND
    }

    fun clean() {
        val iterator = playerDataMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val isLinked = entry.value["isLinked"].toBoolean()
            val isBanned = entry.value["isBanned"].toBoolean()

            if (!isLinked && !isBanned) {
                val id: Long = entry.key
                AiChan.logger.info("Removed empty profile for user $id")
                iterator.remove()
            }
        }
    }

    fun link(userId: Long, mcId: String): LinkResult {
        if (searchMCId(mcId) != -1L) {
            return LinkResult.FAIL_ALREADY_EXIST
        }
        val player = getUserData(userId)
        if (player.isLinked) {
            return LinkResult.FAIL_ALREADY_LINKED
        }
        player.isLinked = true
        player.mcId = mcId
        setUserData(player)
        SocketServer.sendPacket(player.getStatusPacket())
        return LinkResult.SUCCESS
    }

    fun unlink(userId: Long): UnlinkResult {
        val player: PlayerData = getUserData(userId)
        if (!player.isLinked) {
            return UnlinkResult.FAIL_NOT_LINKED
        }

        val unlinkPacket = SocketPacket(SocketPacket.PacketType.PLAYER_NOT_FOUND)
        unlinkPacket.set(0, player.mcId)
        SocketServer.sendPacket(unlinkPacket)

        player.isLinked = false
        player.mcId = ID_UNLINKED
        setUserData(player)
        return UnlinkResult.SUCCESS
    }

    fun ban(userId: Long): BanResult {
        val player = getUserData(userId)
        if (player.isBanned) {
            return BanResult.FAIL_ALREADY_BANNED
        }
        player.isBanned = true
        setUserData(player)

        SocketServer.sendPacket(player.getStatusPacket())
        return BanResult.SUCCESS
    }

    fun pardon(userId: Long): PardonResult {
        val player = getUserData(userId)
        if (!player.isBanned) {
            return PardonResult.FAIL_NOT_BANNED
        }
        player.isBanned = false
        setUserData(player)

        SocketServer.sendPacket(player.getStatusPacket())
        return PardonResult.SUCCESS
    }

    fun banId(mcId: String): BanResult {
        val userId = searchMCId(mcId)
        if (userId == -1L) {
            return BanResult.FAIL_NOT_FOUND
        }
        return ban(userId)
    }

    fun pardonId(mcId: String): PardonResult {
        val userId = searchMCId(mcId)
        if (userId == -1L) {
            return PardonResult.FAIL_NOT_FOUND
        }
        return pardon(userId)
    }

    private fun initUser(userId: Long) {
        playerDataMap.putIfAbsent(userId, mutableMapOf())
        val player: MutableMap<String, String>? = playerDataMap[userId]
        player?.putIfAbsent("isLinked", "false")
        player?.putIfAbsent("MCID", ID_UNDEFINED)
        player?.putIfAbsent("isBanned", "false")
    }

    fun searchMCId(id: String): Long {
        if (id == ID_UNDEFINED || id == ID_UNLINKED) {
            // user does not exist
            return -1
        }
        playerDataMap.forEach { (key, value) ->
            if (value["MCID"] == id) {
                return key
            }
        }
        return -1
    }

    fun getUserData(userId: Long): PlayerData {
        if (!playerDataMap.containsKey(userId)) {
            initUser(userId)
        }
        val player: MutableMap<String, String>? = playerDataMap[userId]
        if (player != null) {
            val mcId = player["MCID"] ?: ID_UNDEFINED
            return PlayerData(
                userId,
                player["isLinked"].toBoolean(),
                mcId,
                player["isBanned"].toBoolean()
            )
        }
        return PlayerData(userId, false, "ERROR", false)
    }

    private fun setUserData(userData: PlayerData) {
        if (!playerDataMap.containsKey(userData.userId)) {
            initUser(userData.userId)
        }
        val player: MutableMap<String, String>? = playerDataMap[userData.userId]
        if (player != null) {
            player["isLinked"] = userData.isLinked.toString()
            player["MCID"] = userData.mcId
            player["isBanned"] = userData.isBanned.toString()
        }
    }
}
