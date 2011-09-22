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

    private void prepareResponseBody(String resource) throws Exception{
       try {
            // if the resourcePath absolute path contains "CLASSES" string,
            // pass them to user classes in CLASSES sub-directory
           
            // resourcePath format - <DOC_ROOT>/<CONTEXT>/CLASSES/getStockQuote
            if(resourcePath.toString().contains(Configuration.getDynamicClassTag())) {
                // check if the class loader exists for this context
                // if exists, check the class is already loaded
                // if loaded, create a new instance of the class
                // if the loaded does not exist, create new loader
                // save it in a map, load the class file from the disk
                // and define it to the JVM
                String className = resourcePath.toString().split("CLASSES\\.")[1];
                AmoebaClassLoader classLoader = null;
                if(classLoaderMap.contains(className)){
                    // classloader instance already created for this context
                    // use that to load the dynamic class
                    classLoader = classLoaderMap.get(contextName);
                    
                } else {
                    // no classloader for this context
                    // create a new class loader and put it in class loader map
                    classLoader = new AmoebaClassLoader(contextName);
                    classLoaderMap.put(contextName, classLoader);
                }
                
                DynamicRequest dynamicReq = (DynamicRequest) classLoader.loadClass(className).newInstance();
                dynamicReq.process(request, response);
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
                    return;
                }
                                
                // client does not have this resource
                // read from the cache, if exists
                LRUResourceCache lruCache = null;
                ByteBuffer responseBodyByteBuffer = null;
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
                                
                // if compression is enabled and if the resource
                // is compressable, do so now
                if(conf.getCompression() && isCompressable(resourceType)){
                    compress(responseBodyByteBuffer.array());
                } else {
                    // either compression is disabled or resource is not compressable
                    respContentLength = resourceSize;
                    response.setBody(responseBodyByteBuffer);
                }
           }
        } catch (FileNotFoundException fnfe) {
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(fnfe), fnfe);
            respCode = "_404";
            setErrorResponse(respCode, resource);
            return;
        } catch (IOException ioe) {
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            respCode = "_500";
            setErrorResponse(respCode, resource);
            return;
        }
        
        return;
    }

    private void prepareResponseHeaders() throws Exception {
        String statusLine = statusLine(respCode);
        response.setStatusCode(respCode.split("_")[1]);
        response.setStatusLine(statusLine);
        
        // set last modified value and etag value
        // if resource is cacheable
        if(isCacheable(resourceType)){
            response.setLastModified(lastModified);
            response.setETag(eTag);
        }
        
        String contentType = contentType(resourceType);
        response.setContentType(contentType);
        
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
        byte[] eTagSouceBytes = eTagSource.getBytes("UTF-8");
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

    private void setErrorResponse(String respCode, String resource) throws FileNotFoundException, IOException{
        
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
            response.setBody(responseBodyByteBuffer);
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
            
    // multiple threads change the following volatile variables
    private static volatile long resourcesReadFromDisk;
    private static volatile long resourcesReadFromCache;
}
