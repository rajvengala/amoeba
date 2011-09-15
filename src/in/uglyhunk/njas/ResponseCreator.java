/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.DatatypeConverter;

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

    public ResponseBean process() throws NoSuchAlgorithmException, FileNotFoundException, IOException, UnsupportedEncodingException {
            // If virtual host is set to true in njws.conf
            // append host string value from the http request header
            // to the document root and then append the requested
            // resourcePath name to get the absolute path

            // If virtual host is set to false, append "default" and then
            // append resourcePath name to the document root for absolute path
            resourcePath = new StringBuilder();
            String resource = request.getResource();
            if(resource.equals("/"))
                resource = "/index.html";
        
            if(resource.contains("."))
                resourceType = resource.split("\\.")[1];
            else
                resourceType = "html";
            
            String documentRoot = conf.getDocumentRoot();
            if(conf.isVirtualHost()) 
                contextName = request.getHost();
            else
                contextName = conf.getDefaultContext();
            
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

    private void prepareResponseBody(String resource) throws NoSuchAlgorithmException, FileNotFoundException, IOException{
       try {
            // if the resourcePath absolute path contains "dynbin" string
            // pass them to user classes in classes directory directory
            if(resourcePath.toString().contains(conf.getClasses())) {
                // have a properties file in each webapp folder
                // and load appropriate class
                // class loaded should be different for each
                // webapp
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
                    lruCache = Main.getCache(contextName);
                    lruCache.getCacheLock().lock();
                    
                    if(lruCache.containsKey(eTag)){
                        byte[] cachedResource = lruCache.get(eTag);
                        resourceSize = cachedResource.length;
                        responseBodyByteBuffer = ByteBuffer.allocate(resourceSize);
                        responseBodyByteBuffer.put(cachedResource);
                        responseBodyByteBuffer.flip();
                        respCode = "_200";
                        response.setresponseCacheTag("[Cache]");
                        
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
                    }
                } finally{
                    lruCache.getCacheLock().unlock();
                }
                                
                // if compression is enabled and if the resource
                // is compressable, do so now
                if(conf.getCompression() && isCompressable(resourceType)){
                    compress(responseBodyByteBuffer.array());
                } else {
                    // either compression is enabler or resource
                    // is not compressable
                    respContentLength = resourceSize;
                    response.setBody(responseBodyByteBuffer);
                }
           }
        } catch (FileNotFoundException fnfe) {
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(fnfe), fnfe);
            respCode = "_404";
            setErrorResponse(respCode, resource);
            return;
        } catch (IOException ioe) {
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            respCode = "_500";
            setErrorResponse(respCode, resource);
            return;
        }
        
        return;
    }

    private void prepareResponseHeaders() throws UnsupportedEncodingException {
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
        response.setServer(conf.getSeverHeader());
        return;
    }

    private String calculateETag() throws NoSuchAlgorithmException, UnsupportedEncodingException {
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
        return "text/html";
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
    private static Configuration conf = Main.getConf();
}
