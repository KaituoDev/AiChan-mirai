package `fun`.kaituo.aichanmirai.server

import com.macasaet.fernet.Key
import com.macasaet.fernet.Token
import `fun`.kaituo.aichanmirai.AiChanMirai
import `fun`.kaituo.aichanmirai.Utils.removeMinecraftColor
import `fun`.kaituo.aichanmirai.config.MainConfig
import `fun`.kaituo.aichanmirai.config.PlayerDataConfig
import `fun`.kaituo.aichanmirai.server.SocketPacket.PacketType
import net.mamoe.mirai.utils.MiraiLogger
import org.xsocket.connection.*
import java.io.IOException
import java.nio.BufferUnderflowException

class ServerHandler private constructor() :
    IDataHandler,
    IConnectHandler,
    IIdleTimeoutHandler,
    IConnectionTimeoutHandler,
    IDisconnectHandler {
    private val logger: MiraiLogger = AiChanMirai.logger

    // The boolean value represents whether the connection is authenticated
    val connections: MutableList<INonBlockingConnection> = ArrayList()

    @Throws(BufferUnderflowException::class)
    override fun onConnect(nbc: INonBlockingConnection): Boolean {
        logger.info("客户端 ${nbc.id} ${nbc.remoteAddress.hostName} 已连接")
        nbc.apply {
            encoding = "UTF-8"
            connections.add(this)
        }
        return true
    }

    @Throws(IOException::class)
    override fun onDisconnect(nbc: INonBlockingConnection): Boolean {
        logger.info("客户端 ${nbc.id} ${nbc.remoteAddress.hostName} 已断开")
        nbc.run {
            connections.remove(this)
            close()
        }
        return true
    }

    @Throws(IOException::class, BufferUnderflowException::class)
    override fun onData(nbc: INonBlockingConnection): Boolean {
        val encryptedData = nbc.readStringByDelimiter(SocketPacket.DELIMITER)
        val token = Token.fromString(encryptedData)
        val key = Key(MainConfig.token)

        val data = runCatching { token.validateAndDecrypt(key, AiChanMirai.validator) }
            .getOrElse {
                connections.remove(nbc)
                logger.warning("解密失败，断开客户端连接！异常信息：${it.message}")
                return true
            }

        logger.info("Received data from client ${nbc.id}: $data")

        val packet = SocketPacket.parsePacket(data)

        when (packet.packetType) {
            PacketType.GROUP_TEXT -> {
                AiChanMirai.queueGroupMessage(MainConfig.messagingGroup, removeMinecraftColor(packet[1]))
                val trigger = packet[0]
                val content = packet[1]
                val serverTextPacket = SocketPacket(PacketType.SERVER_TEXT).apply {
                    this[0] = trigger
                    this[1] = content
                }
                SocketServer.INSTANCE.sendPacket(serverTextPacket)
            }
            PacketType.PLAYER_LOOKUP -> {
                val mcId = packet[0]
                val id = PlayerDataConfig.searchMCId(mcId)
                if (id == -1L) {
                    val notFoundPacket = SocketPacket(PacketType.PLAYER_NOT_FOUND).apply {
                        this[0] = mcId
                    }
                    SocketServer.INSTANCE.sendPacket(notFoundPacket)
                } else {
                    val statusPacket: SocketPacket = PlayerDataConfig.getUserData(id).getStatusPacket()
                    SocketServer.INSTANCE.sendPacket(statusPacket)
                }
            }

            else -> {}
        }
        return true
    }

    /**
     * 请求处理超时的处理事件
     */
    @Throws(IOException::class)
    override fun onIdleTimeout(nbc: INonBlockingConnection): Boolean {
        logger.info("客户端 ${nbc.id} ${nbc.remoteAddress.hostName} 已断开")
        nbc.run {
            connections.remove(this)
            close()
        }
        return true
    }

    /**
     * 连接超时处理事件
     */
    @Throws(IOException::class)
    override fun onConnectionTimeout(nbc: INonBlockingConnection): Boolean {
        logger.info("客户端 ${nbc.id} ${nbc.remoteAddress.hostName} 已断开")
        nbc.run {
            connections.remove(this)
            close()
        }
        return true
    }

    companion object {
        val INSTANCE = ServerHandler()
    }
}
