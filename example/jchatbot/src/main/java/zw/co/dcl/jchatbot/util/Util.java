package zw.co.dcl.jchatbot.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public class Util {
    static public Map<String, Object> requestHeadersToMap(HttpServletRequest request) {
        if(request == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        var headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            map.put(key, request.getHeader(key));
        }

        return map;
    }
}
