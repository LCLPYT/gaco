package work.lclpnet.gaco.asset.util;

import java.net.*;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileUtil {

    private FileUtil() {}

    public static String encodeURIComponent(String s) {
        return URLEncoder.encode(s, UTF_8).replaceAll("\\+", "%20");
    }
}
