/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.logging.Level;


/**
 *
 * @author uglyhunk
 */
public class RequestProcessor implements Runnable {

    public RequestProcessor() {}

    public void run() {
        try {
            RequestBean reqBean = Main.getRequestQueue().take();
            byte[] readBufferArray = reqBean.getRawRequestBytes();
            long timestamp = reqBean.getTimestamp();
            SelectionKey key = reqBean.getSelectionKey();
            
            ByteBuffer readBuffer = ByteBuffer.allocate(readBufferArray.length);
            readBuffer.put(readBufferArray);
            readBuffer.flip();

            String rawRequest = charset.decode(readBuffer).toString();
            parseRequest(reqBean, rawRequest);
                                    
            ResponseBean response = new ResponseCreator(reqBean).process();
            Main.getResponseMap().put(timestamp, response);
                 
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
            
        } catch (UnsupportedEncodingException use){
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(use), use);
        } catch(IOException ioe){
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
        } catch(InterruptedException ie){
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ie), ie);
        }
    }

    private void parseRequest(RequestBean reqBean, String rawRequest) {
        
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
    
    private static Charset charset = Charset.forName("UTF-8");
}
