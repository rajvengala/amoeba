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
    HTML("text/html", false, true), TXT("text/plain", false, true), XML("application/atom+xml", false, true),
    CSS("text/css", false, true), JS("application/x-javascript", false, true), PDF("application/pdf", false, true),
    JPG("image/jpg", true, false), JPEG("image/jpeg", true, false), PNG("image/png", true, false), BMP("image/bmp", true, false),
    GIF("image/gif", true, false), ZIP("application/zip", true, false), GZ("application/x-gzip", true, false);

    ResponseContentTypeEnum(String contentType, boolean isBinary, boolean isCompressable) {
        this.contentType = contentType;
        this.isBinary = isBinary;
        this.isCompressable = isCompressable;
    }

    public String getContentType(){
        return this.contentType;
    }

    public boolean isBinary(){
        return this.isBinary;
    }
    
    public boolean isCompressable(){
        return this.isCompressable;
    }

    private String contentType;
    private boolean isBinary;
    private boolean isCompressable;
}
