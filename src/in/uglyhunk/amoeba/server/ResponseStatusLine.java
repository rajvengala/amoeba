/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

/**
 *
 * @author uglyhunk
 */
public class ResponseStatusLine {

    public static String getStatusLine(int statusCode){
        switch(statusCode){
            case 200:
                return STATUS_CODE_200;
            
            case 206:
                return STATUS_CODE_206;
                
            case 304:
                return STATUS_CODE_304;
                
            case 404:
                return STATUS_CODE_404;
                
            case 500:
                return STATUS_CODE_500;
                
            case 503:
                return STATUS_CODE_503;
                
            default:
                return STATUS_CODE_500;
        }
    }
    
    private static final String STATUS_CODE_200 = "HTTP/1.1 200 OK";
    private static final String STATUS_CODE_206 = "HTTP/1.1 206 Partial Content";
    private static final String STATUS_CODE_304 = "HTTP/1.1 304 Not Modified";
    private static final String STATUS_CODE_404 = "HTTP/1.1 404 Not Found";
    private static final String STATUS_CODE_500 = "HTTP/1.1 500 Internal Server Error";
    private static final String STATUS_CODE_503 = "HTTP/1.1 503 Service Unavailable";
}
