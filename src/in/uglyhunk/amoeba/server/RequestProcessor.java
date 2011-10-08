/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;


/**
 *
 * @author uglyhunk
 */
public class RequestProcessor implements Runnable {

    public RequestProcessor() {}

    public void run() {
        SelectionKey key = null;
        ResponseBean responseBean = null;
        RequestBean requestBean = null;
        try {
            requestBean = requestQueue.take();
            byte[] readBufferArray = requestBean.getRawRequestBytes();
            key = requestBean.getSelectionKey();
            
            ByteBuffer readBuffer = ByteBuffer.allocate(readBufferArray.length);
            readBuffer.put(readBufferArray);
            readBuffer.flip();

            String rawRequest = charset.decode(readBuffer).toString();
            parseRequest(requestBean, rawRequest);
                                    
            responseBean = new ResponseCreator(requestBean).process();
            responseMap.put(key, responseBean);
        
        } catch(InterruptedException ie){
            // This occurs when requestBean is being taken from the requestQueue.
            // If this request is not removed, application gets stuck with more
            // requests. In case of error, this has to be taken off the queue
//            Configuration.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(ie), ie);
//            reqBean = requestQueue.take();
//            key = reqBean.getSelectionKey();
//            throw InterruptedException;
        } catch(Exception e){
            Configuration.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(e), e);
            responseBean = new ResponseCreator(requestBean).processError();
            responseMap.put(key, responseBean);
        } finally {
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
    }

    private void parseRequest(RequestBean requestBean, String rawRequest) throws ParseException {
        requestBean.setRawRequest(rawRequest);
        String requestLines[] = rawRequest.split(Utilities.getHTTPEOL());
        
        for(String line: requestLines) {
            String tokens[] = null;
            
            switch(matchRequestHeader(line)) {
                case GET:
                case POST:
                    tokens = line.split(" ");
                    if(tokens.length == 3) {
                        // http method
                        requestBean.setMethod(tokens[0]);
                        
                        // extract query string if exists
                        if(tokens[1].contains("?")){
                            String temp[] = tokens[1].split("\\?");
                            
                            //requested resource
                            requestBean.setResource(temp[0]);

                            // query string
                            requestBean.setQueryString(temp[1]);
                        } else {
                            requestBean.setResource(tokens[1]);
                        }
                        
                        // http version
                        requestBean.setHttpVersion(tokens[2]);
                    }
                    break;

                case ACCEPT:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setAccept(tokens[1]);
                    }
                    break;

                case ACCEPT_ENCODING:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setAcceptEncoding(tokens[1]);
                    }
                    break;

                case ACCEPT_LANGUAGE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setAcceptLanguage(tokens[1]);
                    }
                    break;

                case ACCEPT_CHARSET:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setAcceptLanguage(tokens[1]);
                    }
                    break;
                    
                case USER_AGENT:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setUserAgent(tokens[1]);
                    }
                    break;

                case CONTENT_TYPE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setContentType(tokens[1]);
                    }
                    break;

                case CONTENT_LENGTH:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setContentLength(tokens[1]);
                    }
                    break;

                case CACHE_CONTROL:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setCacheControl(tokens[1]);
                    }
                    break;

                case CONNECTION:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setConnection(tokens[1]);
                    }
                    break;

                case COOKIE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setCookie(tokens[1]);
                    }
                    break;
                 
                case IF_MODIFIED_SINCE:
                    tokens = line.split(":");
                    if(tokens.length == 2){
                        String ifModifiedSince = tokens[1].trim();
                        long lastModifiedTime = Configuration.getSimpleDateFormat().parse(ifModifiedSince).getTime();
                        requestBean.setIfModifiedSince(lastModifiedTime);
                    }
                    break;

                case IF_NONE_MATCH:
                    tokens = line.split(":");
                    if(tokens.length == 2){
                        // ETag in request header is enclosed in double quotes
                        requestBean.setETag(tokens[1].trim().split("\"")[1]);
                    }
                    break;
                    
                case HOST:
                    tokens = line.split(":");
                    if(tokens.length >= 2) {
                        requestBean.setHost(tokens[1].trim());
                    }
                    break;

                case REFERER:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setReferer(tokens[1]);
                    }
                    break;
                    
                case RANGE:
                    // Range: bytes 0-
                    // Range: bytes 200-700
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setRange(tokens[1]);
                    }
                    break;

                default:
                    if(line.length() == requestBean.getContentLength()){
                        requestBean.setBody(line);
                    }
            }
        }
    }

    private RequestHeadersEnum matchRequestHeader(String requestLine){
        for(RequestHeadersEnum tokenEnum : RequestHeadersEnum.values()) {
            if(requestLine.contains(tokenEnum.getRequestHeaderName())){
                return tokenEnum;
            }
         }
         return RequestHeadersEnum.NONE;
    }
  
    private static Charset charset = Utilities.getCharset();
    private static LinkedBlockingQueue<RequestBean> requestQueue = RuntimeData.getRequestQueue();
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap = RuntimeData.getResponseMap();
}
