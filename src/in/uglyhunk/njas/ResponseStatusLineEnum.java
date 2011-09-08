/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

/**
 *
 * @author uglyhunk
 */
public enum ResponseStatusLineEnum {
    _200("HTTP/1.1 200 OK"), _404("HTTP/1.1 404 Not Found"), _500("HTTP/1.1 500 Internal Server Error"),
    _503("HTTP/1.1 503 Service Unavailable"), NONE("NONE");

    ResponseStatusLineEnum(String statusCodeDesc) {
        this.statusCodeDesc = statusCodeDesc;
    }

    public String getStatusLine(){
        return statusCodeDesc;
    }

    private String statusCodeDesc;
}
