package fun.kaituo.aichanmirai.server;

import com.alibaba.fastjson2.JSON;
import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import fun.kaituo.aichanmirai.AiChanMirai;
import fun.kaituo.aichanmirai.config.MainConfig;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class SocketServer {
    public static final SocketServer INSTANCE = new SocketServer();

    public void sendPacket(SocketPacket packet) {

        String data = JSON.toJSONString(packet);
        Key key = new Key(MainConfig.INSTANCE.getToken().get(0));
        Token token = Token.generate(key, data);

        String encryptedData = token.serialise();

        for (INonBlockingConnection c : new ArrayList<>(ServerHandler.INSTANCE.connections)) {
            if (c == null || !c.isOpen())
                continue;
            try {
                c.write(encryptedData + SocketPacket.DELIMITER);
            } catch (IOException e) {
                AiChanMirai.INSTANCE.getLogger().warning("发送消息失败", e);
            }
        }


    }

    public IServer getServer() {
        return server;
    }

    private IServer server;

    private SocketServer() {
        try {
            InetAddress address = InetAddress.getByName(MainConfig.INSTANCE.getBindAddress());
            this.server = new Server(address, MainConfig.INSTANCE.getBindPort(), ServerHandler.INSTANCE);
            this.server.setFlushmode(IConnection.FlushMode.ASYNC);
            AiChanMirai.INSTANCE.getLogger().info(
                    String.format("服务器 %s:%s 已初始化", this.server.getLocalAddress(), MainConfig.INSTANCE.getBindPort())
            );
        } catch (Exception e) {
            AiChanMirai.INSTANCE.getLogger().warning("服务器初始化失败", e);
        }
    }
}
