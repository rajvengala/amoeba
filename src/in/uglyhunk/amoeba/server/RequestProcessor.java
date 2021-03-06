/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.configuration.KernelProps;
import in.uglyhunk.amoeba.configuration.ResourceProps;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
            key = requestBean.getSelectionKey();
            
            PartialRequest partialRequest = partialRequestMap.get(key);
            partialRequestMap.remove(key);
            
            rawRequestBytes = partialRequest.getRequestBytes();
            ByteBuffer readBuffer = ByteBuffer.allocate(rawRequestBytes.length);
            readBuffer.put(rawRequestBytes);
            readBuffer.flip();
            
            String rawRequest = KernelProps.getCharset().decode(readBuffer).toString();
            parseRequest(rawRequest);
                
            responseCreator = new ResponseCreator(requestBean);
            responseBean = responseCreator.process();
            responseMap.put(key, responseBean);
            
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
        
        } catch(CancelledKeyException cke){
            // discard this request and clean up other resources
            // holding on this key
            KernelProps.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(cke), cke);
            exceptionedSelectionKeyList.add(key);
        } catch(FileNotFoundException fnfe){
            KernelProps.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(fnfe), fnfe);
            if(responseCreator == null){
                responseCreator = new ResponseCreator(requestBean);
            }
            int responseCode = 404;
            try{
                responseBean = responseCreator.processError(responseCode);
            } catch(IOException ioe){}
            responseMap.put(key, responseBean);
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
        } catch(Exception e){
            KernelProps.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(e), e);
            if(responseCreator == null){
                responseCreator = new ResponseCreator(requestBean);
            }
            int responseCode = 500;
            try{
                responseBean = responseCreator.processError(responseCode);
            } catch(IOException ioe){}
            responseMap.put(key, responseBean);
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
    }

    private void parseRequest(String rawRequest) throws IOException, ParseException {
        requestBean.setRawRequest(rawRequest);
        String requestLines[] = rawRequest.split(Utilities.getHttpEOL());
               
        // includes empty-end-of-headers-newline
        headersLength = 0; 
        int eolLength = Utilities.getHttpEOL().length();
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
            String body = rawRequest.substring(headersLength);
            processRequestBody(body);
        }
    }

    private void processRequestHeaders(String line) throws IOException, ParseException{
        String tokens[] = null;
        
        String requestHeaderLineTag = "";
        if(line.contains(":"))
            // results in request header first word eg: HOST, COOKIE etc
            requestHeaderLineTag = line.split(":")[0];
        else
            // results in HTTP method name eg: GET, POST etc
            requestHeaderLineTag = line.split(" ")[0];
        
        switch(requestHeaderLineTag) {
            case "GET":
            case "POST":
                tokens = line.split(" ");
                if(tokens.length == 3) {
                    // http method
                    requestBean.setMethod(tokens[0]);

                    // extract query string if exists
                    if(tokens[1].contains("?")){
                        String temp[] = tokens[1].split("\\?");

                        //requested resource
                        requestBean.setRelativeResourcePath(temp[0]);

                        // query string
                        String queryString = temp[1];
                        requestBean.setQueryString(queryString);

                        // split query string using "&" as seperator
                        String[] paramValueMaps = queryString.split(KernelProps.getParamSeperator());
                        for(String paramValueMap : paramValueMaps){
                            String group[] = paramValueMap.split("=");
                            String param = URLDecoder.decode(group[0], KernelProps.getCharsetName());
                            String value = URLDecoder.decode(group[1], KernelProps.getCharsetName());
                            requestBean.insertIntoQueryStringMap(param, value);
                        }
                    } else {
                        requestBean.setRelativeResourcePath(tokens[1]);
                    }

                    // http version
                    requestBean.setHttpVersion(tokens[2]);
                }
                break;

            case "Accept":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setAccept(tokens[1]);
                }
                break;

            case "Accept-Encoding":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setAcceptEncoding(tokens[1]);
                }
                break;

            case "Accept-Language":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setAcceptLanguage(tokens[1]);
                }
                break;

            case "Accept-Charset":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setAcceptLanguage(tokens[1]);
                }
                break;

            case "User-Agent":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setUserAgent(tokens[1]);
                }
                break;

            case "Content-Type":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setContentType(tokens[1]);
                    // for multipart/form-data, capture boundary marker
                    if(tokens[1].contains("boundary")){
                        boundary = tokens[1].split("boundary=")[1];
                    }
                }
                break;

            case "Content-Length":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setContentLength(tokens[1].trim());
                }
                break;

            case "Cache-Control":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setCacheControl(tokens[1]);
                }
                break;

            case "Connection":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setConnection(tokens[1].trim());
                }
                break;

            case "Cookie":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setCookie(tokens[1].trim());
                }
                break;

            case "IF-MODIFIED-SINCE":
                tokens = line.split(":");
                if(tokens.length == 2){
                    String ifModifiedSince = tokens[1].trim();
                    long lastModifiedTime = KernelProps.getSimpleDateFormat().parse(ifModifiedSince).getTime();
                    requestBean.setIfModifiedSince(lastModifiedTime);
                }
                break;

            case "IF-NONE-MATCH":
                tokens = line.split(":");
                if(tokens.length == 2){
                    // ETag in request header is enclosed in double quotes
                    requestBean.setETag(tokens[1].trim().split("\"")[1]);
                }
                break;

            case "Host":
                tokens = line.split(":");
                if(tokens.length >= 2) {
                    requestBean.setHost(tokens[1].trim());
                }
                break;

            case "Referer":
                tokens = line.split(":");
                if(tokens.length == 2) {
                    requestBean.setReferer(tokens[1].trim());
                }
                break;

            case "Range":
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
    
    private void processRequestBody(String body) throws IOException{
        String contentType = requestBean.getContentType();

        // ******** If the post data is "application/x-www-form-urlencoded" encoded *********
        if(contentType != null && contentType.contains(KernelProps.getFormEcoding())){
            requestBean.setBody(body);
            // paramSeperator is &, by default
            String[] paramValueMaps = body.split(KernelProps.getParamSeperator());
            for(String paramValueMap : paramValueMaps){
                String group[] = paramValueMap.split("=");
                String param = URLDecoder.decode(group[0], KernelProps.getCharsetName());
                String value = URLDecoder.decode(group[1], KernelProps.getCharsetName());
                requestBean.insertIntoPostBodyMap(param, value);
            }
            return;
        } 
        
        // ******** If the post data is "multipart/form-data" encoded ***********
        if(contentType != null && contentType.contains(KernelProps.getMultipartFormEncoding())){
            requestBean.setBody(body);
            boolean isFile = false;
            boolean isBinaryFile = false;
            String multipartParamName = null;
            boolean headerMode = false;
            boolean binFileRead = false;
            FileOutputStream fos = null;
            FileChannel fc = null;
            int bodyIndex = 0;
                 
            for(String line : body.split(Utilities.getHttpEOL())){
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
                    binFileRead = false;
                    isBinaryFile = false;
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
                    String tokens[] = line.split("Content-Type:");
                    if(tokens.length == 2){
                        String mimeType = tokens[1].trim();
                        isBinaryFile = ResourceProps.isContentBinary(mimeType);
                    } else {
                        // mime type not specified, default to non-binary
                        isBinaryFile = false;
                    }
                } else if(line.contains("Content-Transfer-Encoding") && headerMode){
                    ;
                } else if(line.length() == 0 && headerMode){
                    headerMode = false;
                } else {
                    // save content to file
                    if(isFile){
                        ByteBuffer buffer = null;
                        // no existing file, create a new one
                        if(fc == null){
                            String filename = new Random().nextLong() + ".tmp";
                            File tmpFile = new File(conf.getTmpFolder() + File.separator + filename);
                            requestBean.insertIntoMultipartBodyMap(multipartParamName, tmpFile.toString());
                            fos = new FileOutputStream(tmpFile);
                            fc = fos.getChannel();
                            if(!isBinaryFile)
                                buffer = KernelProps.getCharset().encode(line);
                        } else {
                            // more non-binary content with line breaks
                            if(!isBinaryFile)
                                buffer = KernelProps.getCharset().encode(Utilities.getHttpEOL().concat(line));
                        }
                        
                        if(isBinaryFile && !binFileRead){
                            // binary file
                            binFileRead = true;
                            int binFileIndex = headersLength + bodyIndex;
                            ArrayList<Byte> binFileBytesList = new ArrayList<>();
                            while(true){
                                byte b = rawRequestBytes[binFileIndex];
                                if(b == '\r' && 
                                   rawRequestBytes[binFileIndex + 1] == '\n' && 
                                   rawRequestBytes[binFileIndex + 2] == '-' && 
                                   rawRequestBytes[binFileIndex + 3] == '-'){
                                    break; 
                                }
                                binFileBytesList.add(b);
                                binFileIndex++;
                            }

                            // create a byte array from bytearray list
                            byte[] binFileBytes = new byte[binFileBytesList.size()];
                            int index = 0;
                            for(byte b : binFileBytesList){
                                binFileBytes[index] = b;
                                index++;
                            }

                            buffer = ByteBuffer.allocate(binFileBytesList.size());
                            buffer.put(binFileBytes);
                            buffer.flip();
                        }
                        if(buffer != null)
                            fc.write(buffer);
                    } else {
                        // not a file, read the value of the form field
                        requestBean.insertIntoMultipartBodyMap(multipartParamName, line);
                    }
                }
                
                 // track the length of the body
                bodyIndex += line.length() + Utilities.getHttpEOL().length();
            }
            return;
        }
        
        // ******* request body in another format **********
        requestBean.setBody(body);
    }

    private String boundary;
    private boolean isBody;
    private int headersLength;
    private RequestBean requestBean;
    private ResponseBean responseBean;
    private ResponseCreator responseCreator;
    byte[] rawRequestBytes;
    private static KernelProps conf = KernelProps.getInstance();
    private static LinkedBlockingQueue<RequestBean> requestQueue 
                                                    = RuntimeData.getRequestQueue();
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap 
                                                    = RuntimeData.getResponseMap();
    private static ConcurrentHashMap<SelectionKey, PartialRequest> partialRequestMap
                                                    = RuntimeData.getPartialRequestMap();
    private static CopyOnWriteArrayList<SelectionKey> exceptionedSelectionKeyList
                                                    = RuntimeData.getExceptionedSelectionKeyList();
}
