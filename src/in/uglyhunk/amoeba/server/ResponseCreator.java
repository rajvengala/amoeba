/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

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
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.DatatypeConverter;
import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.dyn.DynamicRequest;
import java.util.HashMap;

/**
 *
 * @author uglyhunk
 */
public class ResponseCreator{

    ResponseCreator(RequestBean request) throws IOException {
        this.request = request;
        this.response = new ResponseBean();
        response.setSocketChannel((SocketChannel)request.getSelectionKey().channel());
        response.setSelectionKey(request.getSelectionKey());
    }

    public ResponseBean process() throws Exception {
            // If virtual host is set to true in njws.conf
            // append host string value from the http request header
            // to the document root and then append the requested
            // resourcePath name to get the absolute path

            // If virtual host is set to false, append "default" and then
            // append resourcePath name to the document root for absolute path
            resourcePath = new StringBuilder();
            
            // resource will be in one of the following forms
            // 1. /
            // 2. /image.gif
            // 3. /CLASSES/getStockQuote
            String resource = request.getResource();
            if(resource.equals("/"))
                resource = "/index.html";
        
            if(resource.contains("."))
                resourceType = resource.split("\\.")[1];
            else
                resourceType = "html";
            
            String documentRoot = conf.getDocumentRoot();
            // if virtual host is enabled
            // context name is hostname
            // else context name is default
            if(conf.isVirtualHost()) 
                contextName = request.getHost();
            else
                contextName = Configuration.getDefaultContext();
            
            // resourcePath will be in one of the follows formats
            // if virtual host is disabled
            // 1. %DOC_ROOT%/default/CLASSES/getStockQuote
            // 2. %DOC_ROOT%/default/CLASSES/image.gif
            
            // if virtual host is enabled
            // 3. %DOC_ROOT%/hostname/CLASSES/getStockQuote
            // 4. %DOC_ROOT%/hostname/CLASSES/image.gif
            resourcePath.append(documentRoot).append(File.separator).append(contextName).append(resource);
            response.setAbsoluteResource(resourcePath.toString());

            // if in maintenance, serve 503 response for all the requests
            if(conf.isMaintenance()){
                respCode = "_503";
                setErrorResponse(respCode, resource);
            } else {
                // non-maintenance mode operation
                prepareResponseBody(resource);
            }
            prepareResponseHeaders();
            return response;
    }

    private void prepareResponseBody(String resource) {
       try {
            ByteBuffer responseBodyByteBuffer = null;
            String dynClassTag = Configuration.getDynamicClassTag();
            
            // ResourcePath format - <DOC_ROOT>/<CONTEXT>/CLASSES/getStockQuote
            // If the resourcePath absolute path contains "CLASSES" string,
            // pass them to user classes in CLASSES sub-directory
            if(resourcePath.toString().contains(dynClassTag)) {
                DynamicRequest dynamicReq = null;
                // Extracts target class name from the resourcePath
                // eg: /var/www/default/CLASSES/getStockQuote => getStockQuote
                String className = resourcePath.toString().split(dynClassTag + "/")[1];
                
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
                        String absoluteContextPath = resourcePath.toString().split("/" + dynClassTag)[0];
                        classLoader = new AmoebaClassLoader(absoluteContextPath);
                        classLoaderMap.put(contextName, classLoader);
                    }

                    // Retrieve full class name from class name
                    // eg: main => in.uglyhunk.amoeba.server.Main
                    HashMap<String, String> classMap = contextMap.get(contextName);
                    String fullClassName = classMap.get(className);

                    dynamicReq = (DynamicRequest) classLoader.loadClass(fullClassName).newInstance();
                    
                    // save the instance in a map for later requests for the same class
                    HashMap<String, DynamicRequest> dynamicInstanceMap = null;
                    if(contextDynamicInstanceMap.contains(contextName)){
                        dynamicInstanceMap = contextDynamicInstanceMap.get(contextName);
                    } else {
                        dynamicInstanceMap = new HashMap<String, DynamicRequest>();
                    }
                    dynamicInstanceMap.put(className, dynamicReq);
                    contextDynamicInstanceMap.put(contextName, dynamicInstanceMap);
                }
                
                // Let the dynamic class process the request
                dynamicReq.process(request, response);
                
                String rawBody = response.getRespBody();
                byte rawBodyBytes[] = rawBody.getBytes(charset);
                responseBodyByteBuffer = ByteBuffer.allocate(rawBodyBytes.length);
                responseBodyByteBuffer.put(rawBodyBytes);
                responseBodyByteBuffer.flip();
                
                respContentLength = responseBodyByteBuffer.limit();
                if(response.getContentType() == null){
                    response.setContentType("text/html; charset=UTF-8");
                }
                if(response.getStatusCode() != null){
                    String statusLine = statusLine("_" + respCode);
                    response.setStatusLine(statusLine);
                } else {
                    respCode = "_200";
                }
                response.setresponseCacheTag("[Dynamic]");
                
            } else {
                // if the request is for a static resourcePath
                // get the resource info from the file system
                File f = new File(resourcePath.toString());
                lastModified = f.lastModified();
                
                // check if the request is conditional,
                // retrieve eTag header if exists in the request
                // hearders for thie resource
                String reqETag = request.getETag();
                eTag = calculateETag();
                if(reqETag != null && reqETag.equals(eTag)){
                    respCode = "_304";
                    respContentLength = 0;
                    response.setBody(null);
                    return;
                }
                    
                // check if the request is conditional
                // retrieve if-modified-since header if exists
                long ifModifiedSince = request.getIfModifiedSince();
                if(ifModifiedSince != 0 && lastModified == ifModifiedSince){
                    respCode = "_304";
                    respContentLength = 0;
                    response.setBody(null);
                    response.setresponseCacheTag("[Conditional]");
                    return;
                }
                                
                // client does not have this resource
                // read from the cache, if exists
                LRUResourceCache lruCache = null;
                try{
                    lruCache = LRUResourceCache.getCache(contextName);
                    lruCache.getCacheLock().lock();
                    
                    if(lruCache.containsKey(eTag)){
                        // resource is found in the cache
                        // read from the cache
                        byte[] cachedResource = lruCache.get(eTag);
                        resourceSize = cachedResource.length;
                        responseBodyByteBuffer = ByteBuffer.allocate(resourceSize);
                        responseBodyByteBuffer.put(cachedResource);
                        responseBodyByteBuffer.flip();
                        respCode = "_200";
                        respContentLength = resourceSize;
                        response.setresponseCacheTag("[Cache]");
                        resourcesReadFromCache++;
                    } else {
                        // client does not have this resource
                        // nor does cache; read from disk
                        FileInputStream fis = new FileInputStream(f);
                        resourceSize = (int)f.length();

                        FileChannel fc = fis.getChannel();
                        responseBodyByteBuffer = ByteBuffer.allocate(resourceSize);
                        fc.read(responseBodyByteBuffer);
                        responseBodyByteBuffer.flip();
                        respCode = "_200";
                        respContentLength = resourceSize;
                        fis.close();
                        fc.close();
                        
                        // save the resource in the cache
                        if(isCacheable(resourceType)){
                            lruCache.put(eTag, responseBodyByteBuffer.array());
                        }
                        response.setresponseCacheTag("[Disk]");
                        resourcesReadFromDisk++;
                    }
                } finally{
                    lruCache.getCacheLock().unlock();
                }
            }               
            
            // if compression is enabled and if the resource is compressable, do so now
            if(conf.getCompression() && isCompressable(resourceType)){
                compress(responseBodyByteBuffer.array());
            } else {
                // either compression is disabled or resource is not compressable
                response.setBody(responseBodyByteBuffer);
            }
        } catch (FileNotFoundException fnfe) {
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(fnfe), fnfe);
            respCode = "_404";
            setErrorResponse(respCode, resource);
            return;
        } catch (Exception ioe) {
            Configuration.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(ioe), ioe);
            respCode = "_500";
            setErrorResponse(respCode, resource);
            return;
        }
        return;
    }

    private void prepareResponseHeaders() throws Exception {
        // if status code is not set by dynamic request handler,
        // set it here
        if(response.getStatusLine() == null){
            String statusLine = statusLine(respCode);
            response.setStatusCode(respCode.split("_")[1]);
            response.setStatusLine(statusLine);
        }
        
        // set last modified value and etag value
        // if resource is cacheable
        if(isCacheable(resourceType)){
            response.setLastModified(lastModified);
            response.setETag(eTag);
        }
        
        // if content type is not set by dynamic request handler,
        // set it here
        if(response.getContentType() == null){
            String contentType = contentType(resourceType);
            response.setContentType(contentType);
        }
        
        if(respContentLength != 0){
            response.setContentLength(respContentLength + "");
            // if compression is enabled and resource
            // is compressable, do it
            if(conf.getCompression() && isCompressable(resourceType)){
                response.setContentEncoding("gzip");
            }
        }
        response.setServer(Configuration.getSeverHeader());
        return;
    }

    private String calculateETag() throws Exception {
        String eTagSource = resourcePath.toString() + lastModified;
        byte[] eTagSouceBytes = eTagSource.getBytes(charset);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(eTagSouceBytes);
        return DatatypeConverter.printBase64Binary(digest);
    }
    
    private String statusLine(String targetStatusCode){
        for(ResponseStatusLineEnum enumStatusCode : ResponseStatusLineEnum.values()) {
            if(targetStatusCode.equals(enumStatusCode.toString()))
                return enumStatusCode.getStatusLine();

        }
        return null;
    }

    private String contentType(String resourceType) {
        for(ResponseContentTypeEnum enumContentType : ResponseContentTypeEnum.values()){
            if(resourceType.equalsIgnoreCase(enumContentType.toString())){
                if(enumContentType.isBinary()) {
                    return enumContentType.getContentType();
                } else {
                    return enumContentType.getContentType() + "; charset=UTF-8";
                }
            }
        }
        return "application/" + resourceType;
    }
    
    private boolean isCompressable(String resourceType){
        for(ResponseContentTypeEnum enumContentType : ResponseContentTypeEnum.values()){
            if(resourceType.equalsIgnoreCase(enumContentType.toString())){
                return enumContentType.isCompressable();
            }
        }
        return true;
    }
    
    private boolean isCacheable(String resourceType){
        for(ResponseContentTypeEnum enumContentType : ResponseContentTypeEnum.values()){
            if(resourceType.equalsIgnoreCase(enumContentType.toString())){
                return enumContentType.isCacheable();
            }
        }
        return true;
    }

    private void setErrorResponse(String respCode, String resource) {
        try{
            File f = new File(conf.getErrorPageFolder() + File.separator + resource);
            if(!f.exists()){
                f = new File(conf.getErrorPageFolder() + File.separator + respCode.split("_")[1] + ".html");
            }

            FileInputStream fis = new FileInputStream(f);
            int fileLength = (int)f.length();

            FileChannel fc = fis.getChannel();
            ByteBuffer responseBodyByteBuffer = ByteBuffer.allocate(fileLength);
            fc.read(responseBodyByteBuffer);
            responseBodyByteBuffer.flip();

            fis.close();
            fc.close();

            if(conf.getCompression()){
                compress(responseBodyByteBuffer.array());
            } else {
                respContentLength = fileLength;
                response.setresponseCacheTag("[Disk]");
                resourcesReadFromDisk++;
                response.setBody(responseBodyByteBuffer);
            }
        } catch(IOException ioe){
            Configuration.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(ioe), ioe);
        }
        return;
    }
    
    private void compress(byte[] content)throws IOException {
        ByteArrayOutputStream zippedStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipos = new GZIPOutputStream(zippedStream);
        gzipos.write(content, 0, content.length);
        gzipos.flush();
        gzipos.close();
        
        respContentLength = zippedStream.size();
        ByteBuffer zippedResponseByteBuffer = ByteBuffer.allocate(respContentLength);
        zippedResponseByteBuffer.put(zippedStream.toByteArray());
        zippedResponseByteBuffer.flip();
        
        response.setBody(zippedResponseByteBuffer);
    }
     
    public static long getResourcesReadFromDisk(){
        return resourcesReadFromDisk;
    }
    
    public static long getResourcesReadFromCache(){
        return resourcesReadFromCache;
    }
    
    private String respCode;
    private int respContentLength;
    private RequestBean request;
    private ResponseBean response;
    private String resourceType;
    private long lastModified;
    private StringBuilder resourcePath;
    private int resourceSize;
    private String eTag;
    private String contextName;
    
    private static Configuration conf = Configuration.getInstance();
    private static ConcurrentHashMap<String, AmoebaClassLoader> classLoaderMap = RuntimeData.getClassLoaderMap();
    private static ConcurrentHashMap<String, HashMap<String, String>> contextMap = RuntimeData.getContextMap();
    private static ConcurrentHashMap<String, HashMap<String, DynamicRequest>> contextDynamicInstanceMap 
                                                                                = RuntimeData.getContextDymaicInstanceMap();
   
    private static final String charset = "UTF-8";
    // multiple threads change the following volatile variables
    private static volatile long resourcesReadFromDisk;
    private static volatile long resourcesReadFromCache;
}
