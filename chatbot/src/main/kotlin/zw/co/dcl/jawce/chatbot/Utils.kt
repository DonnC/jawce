package zw.co.dcl.jawce.chatbot

import jakarta.servlet.http.HttpServletRequest


fun requestHeadersToMap(request: HttpServletRequest): Map<String, Any> {
    var map = mutableMapOf<String, Any>();
    var headerNames = request.headerNames;

    while (headerNames.hasMoreElements()) {
        val key = headerNames.nextElement();
        map.put(key, request.getHeader(key));
    }

    return map;
}
