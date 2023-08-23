package `fun`.kaituo.aichanmirai.config

import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.server.SocketPacket
import `fun`.kaituo.aichanmirai.server.SocketServer
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object PlayerDataConfig : AutoSavePluginConfig("UserData"){

    const val ID_UNDEFINED: String = "!UNDEFINED!"
    const val ID_UNLINKED: String = "!UNLINKED!"


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

    enum class PardonResult{
        SUCCESS, FAIL_NOT_BANNED, FAIL_NOT_FOUND
    }

    fun clean() {
        val iterator = playerDataMap.iterator()
        while (iterator.hasNext()) {
            // If player is not linked and is not banned
            if (!iterator.next().value["isLinked"].toBoolean() &&
                !iterator.next().value["isBanned"].toBoolean()) {
                val id : Long = iterator.next().key
                AiChanMirai.INSTANCE.logger.info("Removed empty profile for user $id")
                iterator.remove()
            }
        }
    }

    fun link(userId : Long, mcId : String) : LinkResult {
        if (searchMCId(mcId) != -1L) {
            return LinkResult.FAIL_ALREADY_EXIST
        }
        val pData : PlayerData = getUserData(userId)
        if (pData.isLinked) {
            return LinkResult.FAIL_ALREADY_LINKED
        }
        pData.isLinked = true
        pData.mcId = mcId
        setUserData(pData)
        SocketServer.INSTANCE.sendPacket(pData.getStatusPacket())
        return LinkResult.SUCCESS
    }

    fun unlink(userId : Long) : UnlinkResult {
        val pData : PlayerData = getUserData(userId)
        if (!pData.isLinked) {
            return UnlinkResult.FAIL_NOT_LINKED
        }

        val unlinkPacket = SocketPacket(SocketPacket.PacketType.PLAYER_NOT_FOUND)
        unlinkPacket.set(0, pData.mcId)
        SocketServer.INSTANCE.sendPacket(unlinkPacket)

        pData.isLinked = false
        pData.mcId = ID_UNLINKED
        setUserData(pData)
        return UnlinkResult.SUCCESS
    }

    fun ban(userId : Long) : BanResult {
        val pData : PlayerData = getUserData(userId)
        if (pData.isBanned) {
            return BanResult.FAIL_ALREADY_BANNED
        }
        pData.isBanned = true
        setUserData(pData)

        SocketServer.INSTANCE.sendPacket(pData.getStatusPacket())
        return BanResult.SUCCESS
    }

    fun pardon(userId : Long) :PardonResult{
        val pData : PlayerData = getUserData(userId)
        if (!pData.isBanned) {
            return PardonResult.FAIL_NOT_BANNED
        }
        pData.isBanned = false
        setUserData(pData)

        SocketServer.INSTANCE.sendPacket(pData.getStatusPacket())
        return PardonResult.SUCCESS
    }

    fun banId (mcId: String) : BanResult{
        val userId : Long = searchMCId(mcId)
        if (userId == -1L) {
            return BanResult.FAIL_NOT_FOUND
        }
        return ban(userId)
    }


    fun pardonId (mcId: String) : PardonResult{
        val userId : Long = searchMCId(mcId)
        if (userId == -1L) {
            return PardonResult.FAIL_NOT_FOUND
        }
        return pardon(userId)
    }

    private fun initUser(userId : Long) {
        playerDataMap.putIfAbsent(userId, mutableMapOf())
        val pData : MutableMap<String, String>? = playerDataMap[userId]
        pData?.putIfAbsent("isLinked", "false")
        pData?.putIfAbsent("MCID", ID_UNDEFINED)
        pData?.putIfAbsent("isBanned", "false")
    }

    // -1 means user does not exist
    fun searchMCId(id : String) : Long {
        if (id == ID_UNDEFINED || id == ID_UNLINKED) {
            return -1
        }
        for ((key, value) in playerDataMap.toMap()) {
            if (value["MCID"]?.equals(id) == true) {
                return key
            }
        }
        return -1
    }
    fun getUserData(userId : Long) : PlayerData {
        if (!playerDataMap.containsKey(userId)) {
            initUser(userId)
        }
        val pData : MutableMap<String, String>? = playerDataMap[userId]
        if (pData != null) {
            var mcId: String? = pData["MCID"]
            if (mcId == null) {
                mcId = ID_UNDEFINED
            }
            return PlayerData(userId, pData["isLinked"].toBoolean(),
                mcId, pData["isBanned"].toBoolean())
        }
        return PlayerData(userId, false, "ERROR", false)
    }
    fun setUserData(userData : PlayerData) {
        if (!playerDataMap.containsKey(userData.userId)) {
            initUser(userData.userId)
        }
        val pData : MutableMap<String, String>? = playerDataMap[userData.userId]
        if (pData != null) {
            pData["isLinked"] = userData.isLinked.toString()
            pData["MCID"]  = userData.mcId
            pData["isBanned"] = userData.isBanned.toString()
        }
    }

}