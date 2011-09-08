/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

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
    public String getStatusCode() {
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
    public String getStatusLine() {
        return statusLine;
    }

    /**
     * @param statusLine the statusLine to set
     */
    public void setStatusLine(String statusLine) {
        this.statusLine = statusLine;
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
    public String getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
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
     * @return the contentEncoding
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * @param contentEncoding the contentEncoding to set
     */
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     * @return the body
     */
    public ByteBuffer getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(ByteBuffer body) {
        this.body = body;
    }

     /**
     * @return the socketChannel
     */
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * @param socketChannel the socketChannel to set
     */
    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

      /**
     * @return the absoluteResource
     */
    public String getAbsoluteResource() {
        return absoluteResource;
    }

    /**
     * @param absoluteResource the absoluteResource to set
     */
    public void setAbsoluteResource(String absoluteResource) {
        this.absoluteResource = absoluteResource;
    }
    
   /**
     * @return the selectionKey
     */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /**
     * @param selectionKey the selectionKey to set
     */
    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }
    
    private String statusCode;
    private String statusLine;
    private String contentType;
    private String contentLength;
    private String server;
    private String connection;
    private String contentEncoding;
    private ByteBuffer body;
    private SocketChannel socketChannel;
    private String absoluteResource;
    private SelectionKey selectionKey;
}
