/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;


/**
 *
 * @author uglyhunk
 */
public enum RequestHeadersEnum {
    GET ("GET"), POST ("POST"), 
    ACCEPT ("Accept"), 
    ACCEPT_ENCODING ("Accept-Encoding"),
    ACCEPT_LANGUAGE ("Accept-Language"), 
    ACCEPT_CHARSET ("Accept-Charset"), 
    USER_AGENT ("User-Agent"),
    CONTENT_TYPE ("Content-Type"), 
    CONTENT_LENGTH ("Content-Length"), 
    CACHE_CONTROL ("Cache-Control"),
    RANGE("Range"),
    CONNECTION ("Connection"), 
    COOKIE ("Cookie"), 
    HOST ("Host"), 
    REFERER ("Referer"), 
    IF_MODIFIED_SINCE("If-Modified-Since"),
    IF_NONE_MATCH("If-None-Match"),
    NONE("NONE"),
    CONTENT_DISPOSITION("Content-Disposition");

    RequestHeadersEnum(String requestHeaderName) {
        this.requestHeaderName = requestHeaderName;
    }

    public String getRequestHeaderName(){
        return requestHeaderName;
    }

    private String requestHeaderName;
}
