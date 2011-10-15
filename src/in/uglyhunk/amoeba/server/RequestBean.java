/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.nio.channels.SelectionKey;
import java.util.HashMap;

/**
 *
 * @author uglyhunk
 */
public class RequestBean {

    public RequestBean() {}

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the resource
     */
    public String getRelativeResourcePath() {
        return relativeResourcePath;
    }

    /**
     * @param resource the resource to set
     */
    void setRelativeResourcePath(String relativeResourcePath) {
        this.relativeResourcePath = relativeResourcePath;
    }

    /**
     * @return the resource
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @param resource the resource to set
     */
    void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * @return the httpVersion
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * @param httpVersion the httpVersion to set
     */
    void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * @return the accept
     */
    public String getAccept() {
        return accept;
    }

    /**
     * @param accept the accept to set
     */
    void setAccept(String accept) {
        this.accept = accept;
    }

    /**
     * @return the acceptLanguage
     */
    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    /**
     * @param acceptLanguage the acceptLanguage to set
     */
    void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @param userAgent the userAgent to set
     */
    void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return the acceptEncoding
     */
    public String getAcceptEncoding() {
        return acceptEncoding;
    }

    /**
     * @param acceptEncoding the acceptEncoding to set
     */
    void setAcceptEncoding(String acceptEncoding) {
        this.acceptEncoding = acceptEncoding;
    }

    /**
     * @return the acceptEncoding
     */
    public String getAcceptCharset() {
        return acceptCharset;
    }

    /**
     * @param acceptEncoding the acceptEncoding to set
     */
    void setAcceptCharset(String acceptCharset) {
        this.acceptCharset = acceptCharset;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the connection
     */
    public String getConnection() {
        return connection;
    }

    /**
     * @param connection the connection to set
     */
    void setConnection(String connection) {
        this.connection = connection;
    }

    /**
     * @return the referer
     */
    public String getReferer() {
        return referer;
    }

    /**
     * @param referer the referer to set
     */
    void setReferer(String referer) {
        this.referer = referer;
    }

    /**
     * @return the cookie
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * @param cookie the cookie to set
     */
    void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the contentLength
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    void setContentLength(String contentLength) {
        this.contentLength = Integer.parseInt(contentLength);
    }

    /**
     * @return the cacheControl
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * @param cacheControl the cacheControl to set
     */
    void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    /**
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    void setBody(String body) {
        this.body = body;
    }


    /**
     * @return the rawRequest
     */
    public String getRawRequest() {
        return rawRequest;
    }

    
    /**
     * @param rawRequest the rawRequest to set
     */
    void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }

    /**
     * @return the rawRequest
     */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }


    /**
     * @param rawRequest the rawRequest to set
     */
    void setSelectionKey(SelectionKey key) {
        selectionKey = key;
    }
    
    /**
     * @return the ifModifiedSince
     */
    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * @param ifModifiedSince the ifModifiedSince to set
     */
    void setIfModifiedSince(long ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    /**
     * @return the eTag
     */
    public String getETag() {
        return eTag;
    }

    /**
     * @param eTag the eTag to set
     */
    void setETag(String eTag) {
        this.eTag = eTag;
    }
  
    /**
     * @return the range
     */
    public String getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    void setRange(String range) {
        this.range = range;
    }
    
    void insertIntoQueryStringMap(String param, String value){
        queryStringMap.put(param, value);
    }
    
    public HashMap<String, String> getQueryStringMap(){
        return queryStringMap;
    }
    
    void insertIntoPostBodyMap(String param, String value){
        postBodyMap.put(param, value);
    }
    
    public HashMap<String, String> getPostBodyMap(){
        return postBodyMap;
    }
    
    void insertIntoMultipartBodyMap(String param, String value){
        multiPartBodyMap.put(param, value);
    }
    
    public HashMap<java.lang.String, java.lang.String> getMultiPartBodyMap() {
        return multiPartBodyMap;
    }
    
    public String getParamValue(String param){
        // search in query string map
        if(queryStringMap.containsKey(param)){
            return queryStringMap.get(param);
        }
        
        // search in post body map
        if(postBodyMap.containsKey(param)){
            return postBodyMap.get(param);
        }
        
        // search in multipart post body map
        if(multiPartBodyMap.containsKey(param)){
            return multiPartBodyMap.get(param);
        }
        return null;
    }
    
    
    /**
     * @return the rawRequestBytes
     */
    public byte[] getRawRequestBytes() {
        return rawRequestBytes;
    }

    /**
     * @param rawRequestBytes the rawRequestBytes to set
     */
    public void setRawRequestBytes(byte[] rawRequestBytes) {
        this.rawRequestBytes = rawRequestBytes;
    }

    private String method;
    private String relativeResourcePath;
    private String queryString;
    private String httpVersion;
    private String accept;
    private String range;
    private String acceptLanguage;
    private String acceptEncoding;
    private String acceptCharset;
    private String userAgent;
    private String host;
    private String connection;
    private String referer;
    private String cookie;
    private String contentType; // used in post request
    private int contentLength; // used in post request
    private String cacheControl;
    private String body;
    private String rawRequest;
    private byte[] rawRequestBytes;
    private SelectionKey selectionKey;
    private long ifModifiedSince;
    private String eTag;
    private HashMap<String, String> queryStringMap = new HashMap<String, String>();
    private HashMap<String, String> postBodyMap = new HashMap<String, String>();
    private HashMap<String, String> multiPartBodyMap = new HashMap<String, String>();

}
