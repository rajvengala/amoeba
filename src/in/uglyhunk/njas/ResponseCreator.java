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
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

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

    public ResponseBean process() throws FileNotFoundException, IOException, UnsupportedEncodingException {
            // If virtual host is set to true in njws.conf
            // append host string value from the http request header
            // to the document root and then append the requested
            // resourcePath name to get the absolute path

            // If virtual host is set to false, append "default" and then
            // append resourcePath name to the document root for absolute path
            StringBuilder resourcePath = new StringBuilder();
            String resource = request.getResource();
            if(resource.equals("/"))
                resource = "/index.html";
        
            if(resource.contains("."))
                resourceType = resource.split("\\.")[1];
            else
                resourceType = "html";
            
            String documentRoot = Main.getDocumentRoot();
            if(Main.isVirtualHost()) {
                resourcePath.append(documentRoot).append(File.separator)
                        .append(request.getHost()).append(resource);
            } else {
                resourcePath.append(documentRoot).append(File.separator)
                        .append(Main.getDefaultWebAppFolder()).append(resource);
            }
            response.setAbsoluteResource(resourcePath.toString());

            // if in maintenance, serve 503 response for all the requests
            if(Main.inMaintenance()){
                respCode = "_503";
                setErrorResponse(respCode, resource);
            } else {
                // non-maintenance mode operation
                prepareResponseBody(resource, resourcePath.toString());
            }
            prepareResponseHeaders();
            return response;
    }

    private void prepareResponseBody(String resource, String resourcePath) throws FileNotFoundException, IOException{
       try {
            // if the resourcePath absolute path contains "bin" string
            // pass them to user classes un dyn-req directory
            if(resourcePath.contains(Main.getClassesFolderName())) {
                // have a properties file in each webapp folder
                // and load appropriate class
                // class loaded should be different for each
                // webapp
            } else {
                // if the request is for a static resourcePath
                // read the file from the file system
                File f = new File(resourcePath);
                FileInputStream fis = new FileInputStream(f);
                int fileLength = (int)f.length();
                
                FileChannel fc = fis.getChannel();
                ByteBuffer responseBodyByteBuffer = ByteBuffer.allocate(fileLength);
                fc.read(responseBodyByteBuffer);
                responseBodyByteBuffer.flip();
                respCode = "_200";

                fis.close();
                fc.close();
                
                if(Main.toCompress() && isCompressable(resourceType)){
                    compress(responseBodyByteBuffer.array());
                } else {
                    respContentLength = fileLength;
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
        
        String contentType = contentType(resourceType);
        response.setContentType(contentType);
        
        if(Main.toCompress() && isCompressable(resourceType)){
            response.setContentEncoding("gzip");
        }
        
        response.setContentLength(respContentLength + "");
        response.setServer(Main.getServerHeader());

        return;
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

    private void setErrorResponse(String respCode, String resource) throws FileNotFoundException, IOException{
        
        File f = new File(Main.getErrorPageFolderPath() + File.separator + resource);
        if(!f.exists()){
            f = new File(Main.getErrorPageFolderPath() + File.separator + respCode.split("_")[1] + ".html");
        }
        
        FileInputStream fis = new FileInputStream(f);
        int fileLength = (int)f.length();
                
        FileChannel fc = fis.getChannel();
        ByteBuffer responseBodyByteBuffer = ByteBuffer.allocate(fileLength);
        fc.read(responseBodyByteBuffer);
        responseBodyByteBuffer.flip();
        
        fis.close();
        fc.close();
                
        if(Main.toCompress()){
            compress(responseBodyByteBuffer.array());
        } else {
            respContentLength = fileLength;
            response.setBody(responseBodyByteBuffer);
        }
        return;
    }
    
    private void compress(byte[] content)throws IOException {
        ByteArrayOutputStream zippedStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = new GZIPOutputStream(zippedStream);
        gzipStream.write(content, 0, content.length);
        gzipStream.flush();
        gzipStream.close();
        
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
}
