package `fun`.kaituo.aichanmirai.server

import com.google.gson.Gson
import com.macasaet.fernet.Key
import com.macasaet.fernet.Token
import `fun`.kaituo.aichanmirai.config.MainConfig
import org.xsocket.connection.IConnection
import org.xsocket.connection.INonBlockingConnection
import org.xsocket.connection.IServer
import org.xsocket.connection.Server
import java.io.IOException
import java.net.InetAddress
import `fun`.kaituo.aichanmirai.AiChanMirai as AiChan

class SocketServer {

    fun sendPacket(packet: SocketPacket?) {
        try {
            val data = Gson().toJson(packet)
            val key = Key(MainConfig.token)
            val token = Token.generate(key, data)
            val encryptedData = token.serialise()
            val openConnections: MutableList<INonBlockingConnection> = ArrayList()

            synchronized(ServerHandler.INSTANCE.connections) {
                ServerHandler.INSTANCE.connections.forEach {
                    if (it.isOpen) {
                        openConnections.add(it)
                    }
                }
            }

            openConnections.forEach {
                try {
                    it.write(encryptedData + SocketPacket.DELIMITER)
                } catch (e: IOException) {
                    AiChan.logger.warning("发送消息失败", e)
                }
            }
        } catch (e: Exception) {
            AiChan.logger.warning("信息加密失败，请检查 Key 是否合法！", e)
        }
    }

    val server: IServer

    init {
        try {
            val address = InetAddress.getByName(MainConfig.bindAddress)
            this.server = Server(address, MainConfig.bindPort, ServerHandler.INSTANCE).apply {
                flushmode = IConnection.FlushMode.ASYNC
            }
            AiChan.logger.info("服务器 ${server.localAddress}:${MainConfig.bindPort} 已初始化")
        } catch (e: Exception) {
            AiChan.logger.warning("服务器初始化失败", e)
            throw RuntimeException("服务器初始化失败", e)
        }
    }

    companion object {
        val INSTANCE = SocketServer()
    }
}
