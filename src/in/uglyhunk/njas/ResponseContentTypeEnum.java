/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

/**
 *
 * @author uglyhunk
 */
public enum ResponseContentTypeEnum {
    HTML("text/html", false), TXT("text/plain", false), XML("application/atom+xml", false),
    CSS("text/css", false), JS("application/x-javascript", false), PDF("application/pdf", false),
    JPG("image/jpg", true), JPEG("image/jpeg", true), PNG("image/png", true), BMP("image/bmp", true),
    GIF("image/gif", true), ZIP("application/zip", true), GZ("application/x-gzip", true);

    ResponseContentTypeEnum(String contentType, boolean isBinary) {
        this.contentType = contentType;
        this.isBinary = isBinary;
    }

    public String getContentType(){
        return this.contentType;
    }

    public boolean isBinary(){
        return this.isBinary;
    }

    private String contentType;
    private boolean isBinary;
}
