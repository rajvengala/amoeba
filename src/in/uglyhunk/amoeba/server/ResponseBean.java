/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 *
 * @author uglyhunk
 */
public class ResponseBean {


    /**
     * @return the statusCode
     */
    String getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the statusLine
     */
    String getStatusLine() {
        return statusLine;
    }

    /**
     * @param statusLine the statusLine to set
     */
    void setStatusLine(String statusLine) {
        this.statusLine = statusLine;
    }

    /**
     * @return the contentType
     */
    String getContentType() {
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
    String getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return the server
     */
    String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the connection
     */
    String getConnection() {
        return connection;
    }

    /**
     * @param connection the connection to set
     */
    void setConnection(String connection) {
        this.connection = connection;
    }

    /**
     * @return the contentEncoding
     */
    String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * @param contentEncoding the contentEncoding to set
     */
    void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     * @return the body
     */
    ByteBuffer getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    void setBody(ByteBuffer body) {
        this.body = body;
    }
    
    public void setRespBody(String respBody){
        this.respBody = respBody;
    }
    
    String getRespBody(){
        return respBody;
    }

     /**
     * @return the socketChannel
     */
    SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * @param socketChannel the socketChannel to set
     */
    void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

      /**
     * @return the absoluteResource
     */
    String getAbsoluteResource() {
        return absoluteResource;
    }

    /**
     * @param absoluteResource the absoluteResource to set
     */
    void setAbsoluteResource(String absoluteResource) {
        this.absoluteResource = absoluteResource;
    }
    
   /**
     * @return the selectionKey
     */
    SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /**
     * @param selectionKey the selectionKey to set
     */
    void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }
    
    
    /**
     * @return the getLastModified
     */
    long getLastModified() {
        return lastModified;
    }

    /**
     * @param getLastModified the getLastModified to set
     */
    void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the eTag
     */
    String getETag() {
        return eTag;
    }

    /**
     * @param eTag the eTag to set
     */
    void setETag(String eTag) {
        this.eTag = eTag;
    }
    
    void setresponseCacheTag(String tag){
        this.responseCacheTag = tag;
    }
    
    String getresponseCacheTag(){
        return responseCacheTag;
    }
    
     /**
     * @return the contentRange
     */
    String getContentRange() {
        return contentRange;
    }

    /**
     * @param contentRange the contentRange to set
     */
    void setContentRange(String contentRange) {
        this.contentRange = contentRange;
    }

    /**
     * @return the AcceptRanges
     */
    String getAcceptRanges() {
        return AcceptRanges;
    }

    /**
     * @param AcceptRanges the AcceptRanges to set
     */
    void setAcceptRanges(String AcceptRanges) {
        this.AcceptRanges = AcceptRanges;
    }
        
    private String statusCode;
    private String statusLine;
    private String contentType;
    private String contentLength;
    private String contentRange;
    private String AcceptRanges;
    private String server;
    private String connection;
    private String contentEncoding;
    private ByteBuffer body;
    private String respBody;
    private long lastModified;
    private String eTag;
    private SocketChannel socketChannel;
    private String absoluteResource;
    private SelectionKey selectionKey;
    private String responseCacheTag;

   
    
}
