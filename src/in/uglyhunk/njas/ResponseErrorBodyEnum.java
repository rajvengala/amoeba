/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

/**
 *
 * @author uglyhunk
 */
public enum ResponseErrorBodyEnum {
    _404("<title>404</title><body><h1>Requested page is not found</h1></body></html>"),
    _500("<title>500</title><body><h1>Error while processing your request</h1></body></html>"),
    _503("<title>503</title><body><h1>Service is down</h1></body><html>");

    ResponseErrorBodyEnum(String errorMessage){
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public static String getHeader(){
        return HEADER;
    }

    private String errorMessage;
    private static final String HEADER = "<html>" +
                                         "<head>" +
                                         "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">";
}
