/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

/**
 *
 * @author uglyhunk
 */
public enum ContentTypeEnum {

    //filextension("mimetype,binary,compess,cache)
    HTML("text/html", false, true, false),
    TXT("text/plain", false, true, false),
    XML("application/atom+xml", false, true, false),
    CSS("text/css", false, true, true),
    JS("application/x-javascript", false, true, true),
    PDF("application/pdf", false, true, false),
    JPG("image/jpg", true, false, true),
    JPEG("image/jpeg", true, false, true),
    PNG("image/png", true, false, true),
    BMP("image/bmp", true, false, true),
    GIF("image/gif", true, false, true),
    ZIP("application/zip", true, false, false),
    GZ("application/x-gzip", true, false, false),
    SWF("application/x-shockwave-flash", true, false, true),
    MP4("video/mp4",true,false,false),
    AVI("video/x-msvideo",true,false,false);

    ContentTypeEnum(String contentType, boolean isBinary, boolean isCompressable, boolean isCacheable) {
        this.contentType = contentType;
        this.isBinary = isBinary;
        this.isCompressable = isCompressable;
        this.isCacheable = isCacheable;
    }

    public String getContentType() {
        return this.contentType;
    }

    public boolean isBinary() {
        return this.isBinary;
    }

    public boolean isCompressable() {
        return this.isCompressable;
    }

    public boolean isCacheable() {
        return this.isCacheable;
    }
    private String contentType;
    private boolean isBinary;
    private boolean isCompressable;
    private boolean isCacheable;
}