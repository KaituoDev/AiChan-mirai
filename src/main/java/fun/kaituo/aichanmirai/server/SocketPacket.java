package fun.kaituo.aichanmirai.server;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Remember to update Packet on the other end
public class SocketPacket {
    public static final String DELIMITER = "|DELIMITER|";

    //public static final int MAX_DATA_SIZE = 8;

    public enum PacketType{
        HEARTBEAT, GROUP_TEXT, SERVER_TEXT, PLAYER_LOOKUP, PLAYER_STATUS, PLAYER_NOT_FOUND, LIST_REQUEST, SERVER_COMMAND,
    }

    public SocketPacket(PacketType packetType){
        this.packetType = packetType;
    }

    public static SocketPacket parsePacket(String string) {
        JSONObject packetObject = (JSONObject) JSON.parse(string);
        JSONArray contentArray = packetObject.getJSONArray("content");
        SocketPacket result = new SocketPacket(PacketType.valueOf(
                packetObject.getString("packetType")));
        if (contentArray != null) {
            for (int i = 0; i < contentArray.size(); i += 1) {
                result.set(i, contentArray.getString(i));
            }
        }
        return result;
    }

    /*
    public String toJSONString() {
        JSONObject packetObject = new JSONObject();
        packetObject.put("type", packetType.name());
        JSONArray contentArray = packetObject.putArray("content");
        for (int i = 0; i < content.size(); i += 1) {
            contentArray.set(i, content.get(i));
        }
        return JSON.toJSONString(packetObject);
    }

     */

    public void set(int index, String data){
        content.add(index, data);
    }

    public String get(int index) {
        return content.get(index);
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public List<String> getContent() {
        return content;
    }

    @JSONField
    private final PacketType packetType;
    @JSONField
    private final List<String> content = new ArrayList<>();
}