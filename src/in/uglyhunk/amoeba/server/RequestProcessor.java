/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
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

            String rawRequest = Configuration.getCharset().decode(readBuffer).toString();
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

    private void parseRequest(RequestBean requestBean, String rawRequest) throws Exception {
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
                            String queryString = temp[1];
                            requestBean.setQueryString(queryString);
                            
                            // split query string using "&" as seperator
                            String[] paramValueMaps = queryString.split(Configuration.getParamSeperator());
                            for(String paramValueMap : paramValueMaps){
                                String group[] = paramValueMap.split("=");
                                String param = URLDecoder.decode(group[0], "UTF-8");
                                String value = URLDecoder.decode(group[1], "UTF-8");
                                requestBean.insertIntoQueryStringMap(param, value);
                            }
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
                        // for multipart/form-data, content-type header
                        // will appear multiple times. In that case,
                        // consider only the first content-type
                        if(requestBean.getContentType() != null)
                            requestBean.setContentType(tokens[1]);
                    }
                    break;

                case CONTENT_LENGTH:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setContentLength(tokens[1].trim());
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
                        requestBean.setConnection(tokens[1].trim());
                    }
                    break;

                case COOKIE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setCookie(tokens[1].trim());
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
                        requestBean.setReferer(tokens[1].trim());
                    }
                    break;
                    
                case RANGE:
                    // Range: bytes=0-
                    // Range: bytes=200-700
                    // Range: bytes=200-700, 2-
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        requestBean.setRange(tokens[1].trim());
                    }
                    break;
                    
                case CONTENT_DISPOSITION:
                    // Content-Disposition: form-data; name="submitBtn"
                    // Content-Disposition: form-data; name="myfile"; filename="d=1[1].js"
                    tokens = line.split(":");
                    if(tokens.length == 2){
                        // "myfile"; filename="d=1[1].js" (or) "myfile"
                        String paramName = tokens[1].trim().split("name=")[1];
                        if(paramName.contains(";")){
                           requestBean.insertIntoMultipartBodyMap(paramName.split(";")[0], null);
                        } else {
                            requestBean.insertIntoMultipartBodyMap(paramName, null);
                        }
                    }
                    break;
                    
                default:
                    // If the post data is "application/x-www-form-urlencoded" encoded
                    String contentType = requestBean.getContentType();
                    
                    if(contentType != null && contentType.contains(Configuration.getFormEcoding())){
                        if(line.length() == requestBean.getContentLength()){
                            requestBean.setBody(line);
                        } else {
                            break;
                        }

                        String[] paramValueMaps = line.split(Configuration.getParamSeperator());
                        for(String paramValueMap : paramValueMaps){
                            String group[] = paramValueMap.split("=");
                            String param = URLDecoder.decode(group[0], Configuration.getCharsetName());
                            String value = URLDecoder.decode(group[1], Configuration.getCharsetName());
                            requestBean.insertIntoPostBodyMap(param, value);
                        }
                        break;
                    }

                    // If the post data is "multipart/form-data" encoded
                    if(contentType != null && contentType.contains(Configuration.getMultipartFormEncoding())){
                        String boundary = contentType.split("boundary=")[0];
                        if(!line.equalsIgnoreCase(boundary)){
                            
                        }
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
  
    private static LinkedBlockingQueue<RequestBean> requestQueue = RuntimeData.getRequestQueue();
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap = RuntimeData.getResponseMap();
}
