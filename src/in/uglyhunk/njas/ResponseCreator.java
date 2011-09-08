/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;

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

    public ResponseBean process() throws UnsupportedEncodingException {
            // If virtual host is set to true in njws.conf
            // append host string value from the http request header
            // to the document root and then append the requested
            // resource name to get the absolute path

            // If virtual host is set to false, append "default" and then
            // append resource name to the document root for absolute path
            StringBuilder resource = new StringBuilder();
            String rawResource = request.getResource();
            if(rawResource.equals("/"))
                rawResource = "/index.html";

            currWebApp = request.getHost();

            if(Main.isVirtualHost()) {
                resource.append(documentRoot).append(File.separator)
                        .append(currWebApp).append(rawResource);
            } else {
                resource.append(documentRoot).append(File.separator)
                        .append(DEFAULT_WEBAPP_FOLDER).append(rawResource);
            }

            response.setAbsoluteResource(resource.toString());

            prepareResponseBody(resource.toString());
            prepareResponseHeaders(resource.toString());
            
            return response;
    }

    private void prepareResponseBody(String resource) {
       try {
            // if the resource absolute path contains dyn-req string
            // pass them to user classes un dyn-req directory
            if(resource.contains(DYN_RES_FOLDER)) {
                // have a properties file in each webapp folder
                // and load appropriate class
                // class loaded should be different for each
                // webapp
            } else {
                // if the request is for a static resource
                // read the file from the file system
                File f = new File(resource);
                FileInputStream fis = new FileInputStream(f);
                respContentLength = (int)f.length();
                
                FileChannel fc = fis.getChannel();
                ByteBuffer responseBodyByteBuffer = ByteBuffer.allocate(respContentLength);
                fc.read(responseBodyByteBuffer);
                responseBodyByteBuffer.flip();
                respCode = "_200";

                response.setBody(responseBodyByteBuffer);
                fis.close();
                fc.close();
           }
            
        } catch (FileNotFoundException fnfe) {
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(fnfe), fnfe);
            respCode = "_404";
            getErrorResponse(respCode);
            return;
        } catch (IOException ioe) {
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            respCode = "_500";
            getErrorResponse(respCode);
            return;
        }
        return;
    }

    private void prepareResponseHeaders(String resource) throws UnsupportedEncodingException {
        String statusLine = statusLine(respCode);
        response.setStatusCode(respCode.split("_")[1]);
        response.setStatusLine(statusLine);
        
        String resourceFileExtension = resource.split("\\.")[1];
        String contentType = contentType(resourceFileExtension);
        response.setContentType(contentType);
        response.setContentLength(respContentLength + "");
        response.setServer(SERVER_HEADER);

        return;
    }

    private String statusLine(String targetStatusCode){

        for(ResponseStatusLineEnum enumStatusCode : ResponseStatusLineEnum.values()) {

            if(targetStatusCode.equals(enumStatusCode.toString()))
                return enumStatusCode.getStatusLine();

        }
        return null;
    }

    private String contentType(String fileExtension) {
        for(ResponseContentTypeEnum enumContentType : ResponseContentTypeEnum.values()){
            if(fileExtension.equalsIgnoreCase(enumContentType.toString())){
                if(enumContentType.isBinary()) {
                    return enumContentType.getContentType();
                } else {
                    return enumContentType.getContentType() + "; charset=UTF-8";
                }
            }
        }
        return "text/html";
    }

    private String body(String curWebApp, String targetStatusCode){

        for(ResponseErrorBodyEnum enumStatusCode : ResponseErrorBodyEnum.values()) {

            if(targetStatusCode.equals(enumStatusCode.toString()))
                return ResponseErrorBodyEnum.getHeader() + enumStatusCode.getErrorMessage();

        }
        return null;
    }

    private void getErrorResponse(String statusCode){
        String errorPage = body(currWebApp, statusCode); // still pending

        ByteBuffer responseErrorPageByteBuffer = charset.encode(errorPage);
        responseErrorPageByteBuffer.flip();
        respContentLength = responseErrorPageByteBuffer.capacity();

        response.setBody(responseErrorPageByteBuffer);
        return;
    }
     
    private String respCode;
    private int respContentLength;
    private String currWebApp;
    private RequestBean request;
    private ResponseBean response;
    private static final String SERVER_HEADER = "Nano Java Web Server 0.1";
    private static Charset charset = Charset.forName("UTF-8");
    private static String documentRoot = Main.getDocumentRoot();
    private static final String DYN_RES_FOLDER = "DYN-RES";
    private static final String DEFAULT_WEBAPP_FOLDER = "default";
    
}
