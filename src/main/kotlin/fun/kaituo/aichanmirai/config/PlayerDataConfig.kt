package `fun`.kaituo.aichanmirai.config

import `fun`.kaituo.aichanmirai.server.SocketPacket
import kotlinx.serialization.Serializable
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
    val playerDataMap: MutableMap<Long, Player> by value()

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
        playerDataMap.entries.removeIf { (id, player) ->
            val shouldRemove = !player.isLinked && !player.isBanned

            if (shouldRemove) {
                AiChan.logger.info("Removed empty profile for user $id")
            }
            shouldRemove
        }
    }

    fun link(userId: Long, mcId: String): LinkResult {
        val existingUserId = searchMCId(mcId)
        val player = getUserData(userId)

        return when {
            (existingUserId != -1L) -> {
                if (player.isLinked) {
                    LinkResult.FAIL_ALREADY_LINKED
                } else {
                    LinkResult.FAIL_ALREADY_EXIST
                }
            }
            else -> {
                player.apply {
                    isLinked = true
                    this.mcId = mcId
                }
                setUserData(player)
                SocketServer.sendPacket(player.getStatusPacket())
                LinkResult.SUCCESS
            }
        }
    }

    fun unlink(userId: Long): UnlinkResult {
        val player: PlayerData = getUserData(userId)

        return when {
            !player.isLinked -> UnlinkResult.FAIL_NOT_LINKED
            else -> {
                val unlinkPacket = SocketPacket(SocketPacket.PacketType.PLAYER_NOT_FOUND)
                unlinkPacket.set(0, player.mcId)
                SocketServer.sendPacket(unlinkPacket)

                player.apply {
                    isLinked = false
                    mcId = ID_UNLINKED
                }
                setUserData(player)
                UnlinkResult.SUCCESS
            }
        }
    }

    fun ban(userId: Long): BanResult {
        val player = getUserData(userId)

        return when {
            player.isBanned -> BanResult.FAIL_ALREADY_BANNED
            else -> {
                player.apply { isBanned = true }
                setUserData(player)

                SocketServer.sendPacket(player.getStatusPacket())
                return BanResult.SUCCESS
            }
        }
    }

    fun pardon(userId: Long): PardonResult {
        val player = getUserData(userId)

        return when {
            !player.isBanned -> PardonResult.FAIL_NOT_BANNED
            else -> {
                player.apply { isBanned = false }
                setUserData(player)

                SocketServer.sendPacket(player.getStatusPacket())
                return PardonResult.SUCCESS
            }
        }
    }

    fun banId(mcId: String): BanResult {
        val userId = searchMCId(mcId)
        return when {
            (userId == -1L) -> BanResult.FAIL_NOT_FOUND
            else -> ban(userId)
        }
    }

    fun pardonId(mcId: String): PardonResult {
        val userId = searchMCId(mcId)
        return when {
            (userId == -1L) -> PardonResult.FAIL_NOT_FOUND
            else -> pardon(userId)
        }
    }

    private fun initUser(userId: Long) {
        playerDataMap.putIfAbsent(userId, Player(false, ID_UNDEFINED, false))
    }

    fun searchMCId(id: String): Long {
        return id.takeIf { it !in listOf(ID_UNDEFINED, ID_UNLINKED) }
            ?.let { searchId ->
                playerDataMap.entries.firstNotNullOfOrNull { (key, value) ->
                    if (value.MCID == searchId) key else null
                }
            } ?: -1
    }

    fun getUserData(userId: Long): PlayerData {
        val player: Player = playerDataMap.getOrPut(userId) {
            initUser(userId)
            Player(false, ID_UNDEFINED, false)
        }
        println(player.toString())

        val mcId = player.MCID
        val isLinked = player.isLinked

        return PlayerData(
            userId,
            isLinked,
            mcId,
            player.isBanned
        )
    }

    fun setUserData(userData: PlayerData) {
        val player: Player = playerDataMap.getOrPut(userData.userId) {
            initUser(userData.userId)
            Player(false, ID_UNDEFINED, false)
        }

        player.apply {
            isLinked = userData.isLinked
            MCID = userData.mcId
            isBanned = userData.isBanned
        }
    }
}

@Serializable
data class Player(var isLinked: Boolean, var MCID: String, var isBanned: Boolean)
