/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.util.Random;
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
        try {
            requestBean = requestQueue.take();
            byte[] readBufferArray = requestBean.getRawRequestBytes();
            key = requestBean.getSelectionKey();
            
            ByteBuffer readBuffer = ByteBuffer.allocate(readBufferArray.length);
            readBuffer.put(readBufferArray);
            readBuffer.flip();

            String rawRequest = Configuration.getCharset().decode(readBuffer).toString();
            parseRequest(requestBean, rawRequest.trim());
                                    
            responseBean = new ResponseCreator(requestBean).process();
            responseMap.put(key, responseBean);
        
        } catch(InterruptedException ie){
            // This occurs when requestBean is being taken from the requestQueue.
            // If this request is not removed, application gets stuck with more
            // requests. In case of error, this has to be taken off the queue
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
        String requestLines[] = rawRequest.split(Utilities.getEOL());
               
        int headersLength = 0;
        int eolLength = Utilities.getEOL().length();
        for(String line: requestLines) {
            if(!isBody){
                processRequestHeaders(line);
                headersLength += line.length() + eolLength;
            } else {
                break;
            }
        }
        // process body only if content length is > 0
        // get requests, hence, will not be processed for body
        int contentLength = requestBean.getContentLength();
        if(contentLength > 0){
            String body = rawRequest.substring(headersLength, headersLength + contentLength - 2);
            processRequestBody(body);
        }
    }

    private void processRequestHeaders(String line) throws Exception{
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
                    requestBean.setContentType(tokens[1]);
                    // for multipart/form-data, capture boundary marker
                    if(tokens[1].contains("boundary")){
                        boundary = tokens[1].split("boundary=")[1];
                    }
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

            default:
                if(line.length() == 0)
                    isBody = true;
        }
    }
    
    private void processRequestBody(String body) throws Exception{
        String contentType = requestBean.getContentType();

        // ******** If the post data is "application/x-www-form-urlencoded" encoded *********
        if(contentType != null && contentType.contains(Configuration.getFormEcoding())){
            requestBean.setBody(body);
            String[] paramValueMaps = body.split(Configuration.getParamSeperator());
            for(String paramValueMap : paramValueMaps){
                String group[] = paramValueMap.split("=");
                String param = URLDecoder.decode(group[0], Configuration.getCharsetName());
                String value = URLDecoder.decode(group[1], Configuration.getCharsetName());
                requestBean.insertIntoPostBodyMap(param, value);
            }
            return;
        } 
        
        // ******** If the post data is "multipart/form-data" encoded ***********
        if(contentType != null && contentType.contains(Configuration.getMultipartFormEncoding())){
            requestBean.setBody(body);
            boolean isFile = false;
            String multipartParamName = null;
            boolean headerMode = false;
            FileOutputStream fos = null;
            FileChannel fc = null;
            
            for(String line : body.split(Utilities.getEOL())){
                if(line.equals("--" + boundary) || line.equals("--" + boundary + "--")){
                    // close file channel
                    if(isFile) {
                        isFile = false;
                        fos.flush();
                        fos.close();
                        fc.close();
                        fc = null;
                    }
                    headerMode = true;
                } else if(line.contains("Content-Disposition:") && headerMode){
                    // Content-Disposition: form-data; name="submitBtn"
                    // Content-Disposition: form-data; name="myfile"; filename="temp.ext"
                    String tokens[] = line.split("Disposition:");
                    if(tokens.length == 2){
                        // paranName = myfile"; filename="temp.ext" (or) myfile"
                        String paramName = tokens[1].split("name=\"")[1];
                        if(paramName.contains("\";")){
                            multipartParamName = paramName.split("\";")[0];
                        } else {
                            multipartParamName = paramName.split("\"")[0];
                        }

                        // if the multipart section is file, flag it here
                        if(tokens[1].contains("filename=")){
                            isFile = true;
                        }
                    }
                } else if(line.contains("Content-Type") && headerMode){
                    headerMode = false;
                } else if(line.length() > 0){
                    // save content to file
                    if(isFile){
                        
                        ByteBuffer buffer = Configuration.getCharset().encode(line.concat(Utilities.getEOL()));
                        //buffer.flip();
                        // open mmapped file and write each line
                        if(fc == null){
                            String filename = new Random().nextLong() + ".tmp";
                            File tmpFile = new File(conf.getTmpFolder() + File.separator + filename);
                            requestBean.insertIntoMultipartBodyMap(multipartParamName, tmpFile.toString());
                            fos = new FileOutputStream(tmpFile);
                            fc = fos.getChannel();
                        }
                        fc.write(buffer);
                    } else {
                        requestBean.insertIntoMultipartBodyMap(multipartParamName, line);
                    }
                }
            }
            return;
        }
        
        // ******* request body in another format **********
        requestBean.setBody(body);
    }
    
    
    private RequestHeadersEnum matchRequestHeader(String requestLine){
        for(RequestHeadersEnum tokenEnum : RequestHeadersEnum.values()) {
            if(requestLine.contains(tokenEnum.getRequestHeaderName())){
                return tokenEnum;
            }
         }
         return RequestHeadersEnum.NONE;
    }
  
    
    private String boundary;
    private boolean isBody;
    private RequestBean requestBean;
    private ResponseBean responseBean;
    private static Configuration conf = Configuration.getInstance();
    private static LinkedBlockingQueue<RequestBean> requestQueue = RuntimeData.getRequestQueue();
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap = RuntimeData.getResponseMap();
    
}
