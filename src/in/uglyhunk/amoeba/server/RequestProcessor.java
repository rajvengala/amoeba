/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


/**
 *
 * @author uglyhunk
 */
public class RequestProcessor implements Runnable {

    public RequestProcessor() {}

    public void run() {
        try {
            RequestBean reqBean = requestQueue.take();
            byte[] readBufferArray = reqBean.getRawRequestBytes();
            long timestamp = reqBean.getTimestamp();
            SelectionKey key = reqBean.getSelectionKey();
            
            ByteBuffer readBuffer = ByteBuffer.allocate(readBufferArray.length);
            readBuffer.put(readBufferArray);
            readBuffer.flip();

            String rawRequest = charset.decode(readBuffer).toString();
            parseRequest(reqBean, rawRequest);
                                    
            ResponseBean response = new ResponseCreator(reqBean).process();
            responseMap.put(timestamp, response);
                 
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
            
        } catch (UnsupportedEncodingException use){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(use), use);
        } catch(IOException ioe){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
        } catch(InterruptedException ie){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ie), ie);
        } catch(ParseException pe){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(pe), pe);
        } catch(NoSuchAlgorithmException nsae){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(nsae), nsae);
        } catch(Exception e){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(e), e);
        }
    }

    private void parseRequest(RequestBean reqBean, String rawRequest) throws ParseException {
        reqBean.setRawRequest(rawRequest);
        String requestLines[] = rawRequest.split(Utilities.getHTTPEOL());
        
        for(String line: requestLines) {
            String tokens[] = null;
            
            switch(matchRequestHeader(line)) {
                case GET:
                case POST:
                    tokens = line.split(" ");
                    if(tokens.length == 3) {
                        // http method
                        reqBean.setMethod(tokens[0]);
                        
                        // extract query string if exists
                        if(tokens[1].contains("?")){
                            String temp[] = tokens[1].split("\\?");
                            
                            //requested resource
                            reqBean.setResource(temp[0]);

                            // query string
                            reqBean.setQueryString(temp[1]);
                        } else {
                            reqBean.setResource(tokens[1]);
                        }
                        
                        // http version
                        reqBean.setHttpVersion(tokens[2]);
                    }
                    break;

                case ACCEPT:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setAccept(tokens[1]);
                    }
                    break;

                case ACCEPT_ENCODING:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setAcceptEncoding(tokens[1]);
                    }
                    break;

                case ACCEPT_LANGUAGE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setAcceptLanguage(tokens[1]);
                    }
                    break;

                case ACCEPT_CHARSET:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setAcceptLanguage(tokens[1]);
                    }
                    break;
                    
                case USER_AGENT:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setUserAgent(tokens[1]);
                    }
                    break;

                case CONTENT_TYPE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setContentType(tokens[1]);
                    }
                    break;

                case CONTENT_LENGTH:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setContentLength(tokens[1]);
                    }
                    break;

                case CACHE_CONTROL:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setCacheControl(tokens[1]);
                    }
                    break;

                case CONNECTION:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setConnection(tokens[1]);
                    }
                    break;

                case COOKIE:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setCookie(tokens[1]);
                    }
                    break;
                 
                case IF_MODIFIED_SINCE:
                    tokens = line.split(":");
                    if(tokens.length == 2){
                        String ifModifiedSince = tokens[1].trim();
                        long lastModifiedTime = Configuration.getSimpleDateFormat().parse(ifModifiedSince).getTime();
                        reqBean.setIfModifiedSince(lastModifiedTime);
                    }
                    break;

                case IF_NONE_MATCH:
                    tokens = line.split(":");
                    if(tokens.length == 2){
                        // ETag in request header is enclosed in double quotes
                        reqBean.setETag(tokens[1].trim().split("\"")[1]);
                    }
                    break;
                    
                case HOST:
                    tokens = line.split(":");
                    if(tokens.length >= 2) {
                        reqBean.setHost(tokens[1].trim());
                    }
                    break;

                case REFERER:
                    tokens = line.split(":");
                    if(tokens.length == 2) {
                        reqBean.setReferer(tokens[1]);
                    }
                    break;

                default:
                    if(line.length() == reqBean.getContentLength()){
                        reqBean.setBody(line);
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
    private static ArrayBlockingQueue<RequestBean> requestQueue = RuntimeData.getRequestQueue();
    private static ConcurrentHashMap<Long, ResponseBean> responseMap = RuntimeData.getResponseMap();
}
