package fun.kaituo.aichanmirai.server;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import fun.kaituo.aichanmirai.AiChanMirai;
import fun.kaituo.aichanmirai.config.MainConfig;
import fun.kaituo.aichanmirai.config.PlayerDataConfig;
import net.mamoe.mirai.utils.MiraiLogger;
import org.xsocket.connection.*;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

public class ServerHandler implements IDataHandler, IConnectHandler, IIdleTimeoutHandler, IConnectionTimeoutHandler, IDisconnectHandler {
    public static final ServerHandler INSTANCE = new ServerHandler();

    private ServerHandler() {
        logger = AiChanMirai.INSTANCE.logger;
    }

    private final MiraiLogger logger;

    // The boolean value represents whether the connection is authenticated
    public final List<INonBlockingConnection> connections = new ArrayList<>();

    @Override
    public boolean onConnect(INonBlockingConnection nbc) throws BufferUnderflowException {
        nbc.setEncoding("UTF-8");
        String remoteName = nbc.getRemoteAddress().getHostName();
        logger.info(String.format("客户端 %s %s 已连接", nbc.getId(), remoteName));
        connections.add(nbc);
        return true;
    }

    @Override
    public boolean onDisconnect(INonBlockingConnection nbc) throws IOException {
        String remoteName = nbc.getRemoteAddress().getHostName();
        logger.info(String.format("客户端 %s %s 已断开", nbc.getId(), remoteName));
        connections.remove(nbc);
        nbc.close();
        return true;
    }

    @Override
    public boolean onData(INonBlockingConnection nbc) throws IOException, BufferUnderflowException {
        String encryptedData = nbc.readStringByDelimiter(SocketPacket.DELIMITER);

        Token token = Token.fromString(encryptedData);
        Key key = new Key(MainConfig.INSTANCE.getToken().get(0));


        String data = "";
        try {
            data = token.validateAndDecrypt(key, AiChanMirai.INSTANCE.validator);
        } catch (Exception e) {
            connections.remove(nbc);
            logger.warning(String.format("解密失败，断开客户端连接！异常信息：%s", e.getMessage()));
            return true;
        }

        SocketPacket packet = SocketPacket.parsePacket(data);
        switch (packet.getPacketType()) {
            case GROUP_TEXT -> AiChanMirai.INSTANCE.queueGroupMessage(
                    MainConfig.INSTANCE.getMessagingGroup(), packet.get(0)
            );
            case PLAYER_LOOKUP -> {
                String mcId = packet.get(0);
                long id = PlayerDataConfig.INSTANCE.searchMCId(mcId);
                if (id == -1) {
                    SocketPacket notFoundPacket = new SocketPacket(SocketPacket.PacketType.PLAYER_NOT_FOUND);
                    notFoundPacket.set(0, mcId);
                    SocketServer.INSTANCE.sendPacket(notFoundPacket);
                } else {
                    SocketPacket statusPacket = PlayerDataConfig.INSTANCE.getUserData(id).getStatusPacket();
                    SocketServer.INSTANCE.sendPacket(statusPacket);
                }
            }
        }

        return true;
    }

    /**
     * 请求处理超时的处理事件
     */
    @Override
    public boolean onIdleTimeout(INonBlockingConnection nbc) throws IOException {
        String remoteName = nbc.getRemoteAddress().getHostName();
        logger.info(String.format("客户端 %s %s 已断开", nbc.getId(), remoteName));
        connections.remove(nbc);
        nbc.close();
        return true;
    }

    /**
     * 连接超时处理事件
     */
    @Override
    public boolean onConnectionTimeout(INonBlockingConnection nbc) throws IOException {
        String remoteName = nbc.getRemoteAddress().getHostName();
        logger.info(String.format("客户端 %s %s 已断开", nbc.getId(), remoteName));
        connections.remove(nbc);
        nbc.close();
        return true;
    }
}