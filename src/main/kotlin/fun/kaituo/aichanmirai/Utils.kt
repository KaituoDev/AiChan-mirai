package fun.kaituo.aichanmirai;

public class Utils {
    @SuppressWarnings("unused")
    public static String fixMinecraftColor(String message) {
        return message.replaceAll("&([0-9a-fk-or])","§$1" );
    }

    @SuppressWarnings("unused")
    public static String removeMinecraftColor(String message) {
        return message.replaceAll("&([0-9a-fk-or])","");
    }
}
