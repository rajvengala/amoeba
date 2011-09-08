/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

import java.nio.channels.SelectionKey;

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
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(String resource) {
        this.resource = resource;
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
    public void setQueryString(String queryString) {
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
    public void setHttpVersion(String httpVersion) {
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
    public void setAccept(String accept) {
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
    public void setAcceptLanguage(String acceptLanguage) {
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
    public void setUserAgent(String userAgent) {
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
    public void setAcceptEncoding(String acceptEncoding) {
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
    public void setAcceptCharset(String acceptCharset) {
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
    public void setHost(String host) {
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
    public void setConnection(String connection) {
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
    public void setReferer(String referer) {
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
    public void setCookie(String cookie) {
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
    public void setContentType(String contentType) {
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
    public void setContentLength(String contentLength) {
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
    public void setCacheControl(String cacheControl) {
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
    public void setBody(String body) {
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
    public void setRawRequest(String rawRequest) {
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
    public void setSelectionKey(SelectionKey key) {
        selectionKey = key;
    }
    
    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
    private String resource;
    private String queryString;
    private String httpVersion;
    private String accept;
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
    private SelectionKey selectionKey;
    private long timestamp;
    private byte[] rawRequestBytes;

  
}
