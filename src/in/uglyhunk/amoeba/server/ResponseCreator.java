/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.configuration.KernelProps;
import in.uglyhunk.amoeba.configuration.ResourceProps;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.DatatypeConverter;
import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.dyn.DynamicRequest;
import java.nio.MappedByteBuffer;
import java.util.HashMap;

/**
 *
 * @author uglyhunk
 */
public class ResponseCreator{

    ResponseCreator(RequestBean request) {
        this.requestBean = request;
        this.responseBean = new ResponseBean();
        responseBean.setSocketChannel((SocketChannel)request.getSelectionKey().channel());
        responseBean.setSelectionKey(request.getSelectionKey());
    }
    
    public ResponseBean processError(int errorCode) throws IOException{
        errorResponse = true;
        responseCode = errorCode;
        resourceType = "html";
        prepareErrorResponseBody(responseCode);
        prepareResponseHeaders();
        return responseBean;
    }

    public ResponseBean process()  throws FileNotFoundException, Exception{
        // If virtual host is set to true in njws.conf
        // append host string value from the http request header
        // to the document root and then append the requested
        // resourcePath name to get the absolute path

        // If virtual host is set to false, append "default" and then
        // append resourcePath name to the document root for absolute path
        absoluteResourcePath = new StringBuilder();

        // resource will be in one of the following forms
        // 1. /
        // 2. /image.gif
        // 3. /classes/getStockQuote
        String relativeResourcePath = requestBean.getRelativeResourcePath();
        if(relativeResourcePath.equals("/"))
            relativeResourcePath = "/index.html";

        if(relativeResourcePath.contains(".")){
            resourceType = relativeResourcePath.split("\\.")[1];
        } else {
            // resouce type is not obvious from the file extension
            // assume the resource type as html and set content type accordingly
            resourceType = "html";
        }

        String documentRoot = conf.getDocumentRoot();
        // if virtual host is enabled
        // context name is hostname
        // else context name is default
        if(conf.isVirtualHost()) 
            contextName = requestBean.getHost();
        else
            contextName = KernelProps.getDefaultContext();

        // resourcePath will be in one of the follows formats
        // If virtual host is disabled
        //  + <DOC_ROOT>/default/CLASSES/getStockQuote
        //  + <DOC_ROOT>/default/image.gif
        // If virtual host is enabled
        //  + <DOC_ROOT>/<hostname_in_request_header>/CLASSES/getStockQuote
        //  + <DOC_ROOT>/<hostname_in_request_header>/image.gif
        absoluteResourcePath.append(documentRoot).append("/").append(contextName).append(relativeResourcePath);
        responseBean.setAbsoluteResource(absoluteResourcePath.toString());

        // if in maintenance, serve 503 response for all the requests
        if(conf.isMaintenance()){
            responseCode = 503;
            prepareErrorResponseBody(responseCode);
        } else {
            // non-maintenance mode operation
            prepareResponseBody(relativeResourcePath);
        }
        prepareResponseHeaders();
        return responseBean;
    }

    private void prepareResponseBody(String relativeResourcePath) throws FileNotFoundException, Exception {
        ByteBuffer responseBodyByteBuffer = null;
        
        // ************* Dynamic request *************
        // ResourcePath format - <DOC_ROOT>/<hostname_in_request_header>/classes/getStockQuote
        // If the resourcePath contains "clases" string,
        // pass them to context classes in "classes" sub-directory
        String dynClassTag = KernelProps.getDynamicClassTag();
        if(absoluteResourcePath.toString().contains(dynClassTag)) {
            DynamicRequest dynamicReq = null;
            // Extracts target class name from the resourcePath
            // eg: /var/www/default/CLASSES/getStockQuote => getStockQuote
            String className = absoluteResourcePath.toString().split(dynClassTag + "/")[1];

            // Check if the instance for this class is already created.
            // If so, use that instance for all requests
            if(contextDynamicInstanceMap.contains(contextName)){
                HashMap<String, DynamicRequest> dynamicInstanceMap = contextDynamicInstanceMap.get(contextName);
                if(dynamicInstanceMap.containsKey(className)){
                    dynamicReq = dynamicInstanceMap.get(className);
                }
            }

            // There is no previous instance of this class
            if(dynamicReq == null){
                // Load it from the filesystem using custom class loader
                // Check if the class loader exists for this context.
                // If exists, use that loader to load the dynamic class.
                // If not, create new loader, save it in a map and load the class file 
                AmoebaClassLoader classLoader = null;
                if(classLoaderMap.contains(className)){
                    // Classloader instance already created for this context. 
                    // Use this to load the dynamic class
                    classLoader = classLoaderMap.get(contextName);
                } else {
                    // No classloader exists for this context.
                    // Create a new class loader and put it in a classloader map
                    String absoluteContextPath = absoluteResourcePath.toString().split("/" + dynClassTag)[0];
                    classLoader = new AmoebaClassLoader(absoluteContextPath);
                    classLoaderMap.put(contextName, classLoader);
                }

                // Retrieve full class name from class name
                // eg: main => in.uglyhunk.amoeba.server.Main
                HashMap<String, String> classMap = contextMap.get(contextName);
                String fullClassName = classMap.get(className);

                if(fullClassName != null)
                    dynamicReq = (DynamicRequest) classLoader.loadClass(fullClassName).newInstance();
                else
                    throw new FileNotFoundException(relativeResourcePath + " is not found");

                // save the instance in a map for later requests for the same class
                HashMap<String, DynamicRequest> dynamicInstanceMap = null;
                if(contextDynamicInstanceMap.contains(contextName)){
                    dynamicInstanceMap = contextDynamicInstanceMap.get(contextName);
                } else {
                    dynamicInstanceMap = new HashMap<>();
                }
                dynamicInstanceMap.put(className, dynamicReq);
                contextDynamicInstanceMap.put(contextName, dynamicInstanceMap);
            }

            // Let the dynamic class process the request
            // user saves the content of the response
            // in responseBean
            dynamicReq.process(requestBean, responseBean);

            // retreive the response body set by the user
            // from the responseBean instance
            String rawBody = responseBean.getRespBody();
            byte rawBodyBytes[] = rawBody.getBytes(KernelProps.getCharset());
            responseBodyByteBuffer = ByteBuffer.allocate(rawBodyBytes.length);
            responseBodyByteBuffer.put(rawBodyBytes);
            responseBodyByteBuffer.flip();

            responseContentLength = responseBodyByteBuffer.limit();
            if(responseBean.getContentType() == null){
                responseBean.setContentType("text/html; charset=UTF-8");
            }

            if(responseBean.getStatusCode() == null){
                responseCode = 200;
            }

            responseBean.setResponseCacheTag("[Dynamic]");

        } else {
            // ************* Static resource request *************
            // if the request is for a static resourcePath
            // read the resource contents from the file system
            File f = new File(absoluteResourcePath.toString());
            lastModified = f.lastModified();

            // ************* Conditional request test *************
            // Check if the request is conditional and retrieve eTag header 
            // if exists in the request hearders for thie resource
            String reqETag = requestBean.getETag();
            eTag = calculateETag();
            if(reqETag != null && reqETag.equals(eTag)){
                responseCode = 304;
                responseContentLength = 0;
                responseBean.setBody(null);
                return;
            }

            // check if the request is conditional and retrieve 
            // if-modified-since header if exists
            long ifModifiedSince = requestBean.getIfModifiedSince();
            if(ifModifiedSince != 0 && lastModified == ifModifiedSince){
                responseCode = 304;
                responseContentLength = 0;
                responseBean.setBody(null);
                responseBean.setResponseCacheTag("[Conditional]");
                return;
            }

            // ************* Server cache *************
            // Browser does not have this resource. Read from the cache, if exists
            LRUResourceCache lruCache = null;
            try{
                lruCache = LRUResourceCache.getCache(contextName);
                lruCache.getCacheLock().lock();

                if(lruCache.containsKey(eTag)){
                    // resource is found in the server cache, read from here
                    byte[] cachedResource = lruCache.get(eTag);
                    resourceSize = cachedResource.length;
                    responseBodyByteBuffer = ByteBuffer.allocate(resourceSize);
                    responseBodyByteBuffer.put(cachedResource);
                    responseBodyByteBuffer.flip();

                    responseCode = 200;
                    responseContentLength = resourceSize;
                    responseBean.setResponseCacheTag("[Cache]");
                    resourcesReadFromCache++;
                } else {
                    // client does not have this resource nor does cache; read from disk
                    FileInputStream fis = new FileInputStream(f);
                    resourceSize = (int)f.length();
                    FileChannel fc = fis.getChannel();

                    // if file size is greater than 1 MB, let a 
                    // seperate thread handle the data serving part
                    if(resourceSize > KernelProps.getLargeFileStartSize()){
                        int fileStartPosition = 0;
                        int fileSize = resourceSize;
                        int fileEndPosition = resourceSize-1;
                        String rangeHeader = requestBean.getRange();
                        responseCode = 200;

                        if(rangeHeader != null){
                            responseCode = 206;
                            // Range: bytes=500-999, -2 (or) Range: bytes 500-999 (or) Range: bytes 500-

                            // 500-999 (or) 500-, (or) 500-900, -2
                            String range = rangeHeader.split("=")[1];

                            // tokens = {"500", "999"} (or) {"500"} (or) {"500", "900, -2"}                               
                            String tokens[] = range.split("-");
                            // 500
                            fileStartPosition = Integer.parseInt(tokens[0]);
                            // {"500"}
                            if(tokens.length == 1){
                                fileEndPosition = resourceSize-1;
                                fileSize = resourceSize - fileStartPosition;
                            } else {
                                // {"500", "900, -2"}
                                if(tokens[1].contains(",")){
                                    fileEndPosition = Integer.parseInt(tokens[1].split(",")[0]);
                                } else {
                                    // {"500", "999"}
                                    fileEndPosition = Integer.parseInt(tokens[1]);
                                }
                                fileSize = fileEndPosition - fileStartPosition + 1;
                            }
                        } 

                        responseBean.setAcceptRanges("bytes");
                        responseContentLength = fileSize;
                        responseBean.setResponseCacheTag("[MMap]");

                        // Content-Range: bytes 500-1000/1200
                        responseBean.setContentRange("bytes " + 
                                                    fileStartPosition + "-" + 
                                                    fileEndPosition + "/" + 
                                                    resourceSize);

                        MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 
                                                                    fileStartPosition, 
                                                                    fileSize);
                        responseBean.setMappedByteBuffer(mappedByteBuffer);
                        RuntimeData.getSelectionKeyLargeFileMap().put(responseBean.getSelectionKey(), Boolean.FALSE);
                    } else {
                        responseBodyByteBuffer = ByteBuffer.allocate(resourceSize);
                        fc.read(responseBodyByteBuffer);
                        responseBodyByteBuffer.flip();
                        responseCode = 200;
                        responseContentLength = resourceSize;
                        fis.close();
                        responseBean.setResponseCacheTag("[Disk]");
                        resourcesReadFromDisk++;

                        // save the resource in the cache
                        //if(ContentTypeEnum.isCacheable(resourceType)){
                        if(ResourceProps.isCacheable(resourceType)){
                            lruCache.put(eTag, responseBodyByteBuffer.array());
                        }
                    }
                    fc.close();     // close file channel
                }
            } finally{
                lruCache.getCacheLock().unlock();
            }
        }               

        // ************* Compression *************
        // if compression is enabled and if the resource is compressable, do so now
        //if(conf.getCompression() && isCompressable(resourceType)){
        if(conf.getCompression() && ResourceProps.isCompressable(resourceType)){
            compress(responseBodyByteBuffer.array());
        } else {
            // either compression is disabled or resource is not compressable
            responseBean.setBody(responseBodyByteBuffer);
        }
        return;
    }

    private void prepareResponseHeaders() {
        // set the status line based on response code;
        //String statusLine = statusLine(responseCode);
        String statusLine = ResponseStatusLine.getStatusLine(responseCode);
        //responseBean.setStatusCode(responseCode.split("_")[1]);
        responseBean.setStatusCode(responseCode + "");
        responseBean.setStatusLine(statusLine);
        
        if(!errorResponse){
            // set last modified value and etag value, if resource is cacheable
            //if(isCacheable(resourceType)){
            if(ResourceProps.isCacheable(resourceType)){
                responseBean.setLastModified(lastModified);
                responseBean.setETag(eTag);
            }
        }
        
        // if content type is not set by dynamic request handler, set it here
        if(responseBean.getContentType() == null){
            //String contentType = contentType(resourceType);
            String contentType = ResourceProps.contentType(resourceType);
            responseBean.setContentType(contentType);
        }
        
        if(responseContentLength != 0){
            responseBean.setContentLength(responseContentLength + "");
            // if compression is enabled and resource is compressable, do it
            if(conf.getCompression() && ResourceProps.isCompressable(resourceType)){
                responseBean.setContentEncoding("gzip");
            }
        }
        responseBean.setServer(KernelProps.getSeverHeader());
        return;
    }

    private String calculateETag() throws Exception {
        String eTagSource = absoluteResourcePath.toString() + lastModified;
        byte[] eTagSouceBytes = eTagSource.getBytes(KernelProps.getCharset());
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(eTagSouceBytes);
        return DatatypeConverter.printBase64Binary(digest);
    }
    
    private void prepareErrorResponseBody(int respCode) throws IOException{
        
        StringBuilder response = new StringBuilder();
        switch(respCode){
            case 404:
                response.append("<b>404 - Page Not Found</b>")
                    .append("<br/><hr/>")
                    .append("Could not find the requested resource - ")
                    .append(requestBean.getRelativeResourcePath());
                break;
        
            case 500:
                response.append("<b>500 - Internal Server Error</b>")
                    .append("<br/><hr/>")
                    .append("Error while processing the request for the resource - ")
                    .append(requestBean.getRelativeResourcePath());
                break;
            
            case 503:
                response.append("<b>503 - Service Unavailable</b>")
                    .append("<br/><hr/>")
                    .append("Server in maintenance mode");
                break;
        }
        
        byte responseBytes[] = response.toString().getBytes(KernelProps.getCharset());
        ByteBuffer responseBodyByteBuffer = ByteBuffer.allocate(responseBytes.length);
        responseBodyByteBuffer.put(responseBytes);
        responseBodyByteBuffer.flip();
        
        responseContentLength = responseBodyByteBuffer.limit();
        responseBean.setResponseCacheTag("[Memory]");
        
        // if compression is enabled and if the resource is compressable, do so now
        //if(conf.getCompression() && isCompressable(resourceType)){
        if(conf.getCompression() && ResourceProps.isCompressable(resourceType)){
            compress(responseBodyByteBuffer.array());
        } else {
            // either compression is disabled or resource is not compressable
            responseBean.setBody(responseBodyByteBuffer);
        }
        return;
    }
    
    private void compress(byte[] content)throws IOException {
        ByteArrayOutputStream zippedStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipos = new GZIPOutputStream(zippedStream)) {
            gzipos.write(content, 0, content.length);
            gzipos.flush();
        }
        
        responseContentLength = zippedStream.size();
        ByteBuffer zippedResponseByteBuffer = ByteBuffer.allocate(responseContentLength);
        zippedResponseByteBuffer.put(zippedStream.toByteArray());
        zippedResponseByteBuffer.flip();
        
        responseBean.setBody(zippedResponseByteBuffer);
    }
     
    public static long getResourcesReadFromDisk(){
        return resourcesReadFromDisk;
    }
    
    public static long getResourcesReadFromCache(){
        return resourcesReadFromCache;
    }
    
    private int responseCode;
    private int responseContentLength;
    private RequestBean requestBean;
    private ResponseBean responseBean;
    private String resourceType;
    private long lastModified;
    private StringBuilder absoluteResourcePath;
    private int resourceSize;
    private String eTag;
    private String contextName;
    private boolean errorResponse;
    
    private static KernelProps conf = KernelProps.getInstance();
    private static ConcurrentHashMap<String, AmoebaClassLoader> classLoaderMap = RuntimeData.getClassLoaderMap();
    private static ConcurrentHashMap<String, HashMap<String, String>> contextMap = RuntimeData.getContextMap();
    private static ConcurrentHashMap<String, HashMap<String, DynamicRequest>> contextDynamicInstanceMap 
                                                                                = RuntimeData.getContextDymaicInstanceMap();
    // multiple threads change the following volatile variables
    private static volatile long resourcesReadFromDisk;
    private static volatile long resourcesReadFromCache;
}
