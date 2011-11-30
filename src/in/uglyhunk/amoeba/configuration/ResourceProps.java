/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.configuration;

import java.util.HashMap;
import java.util.Locale;

/**
 *
 * @author rvengala
 */
public class ResourceProps {

    /**
     * @return the contentTypeBinaryMap
     */
    public static HashMap<String, Boolean> getContentTypeBinaryMap() {
        return contentTypeBinaryMap;
    }

    /**
     * @param aContentTypeBinaryMap the contentTypeBinaryMap to set
     */
    public static void setContentTypeBinaryMap(HashMap<String, Boolean> aContentTypeBinaryMap) {
        contentTypeBinaryMap = aContentTypeBinaryMap;
    }
 

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

   /**
     * @return the isBinary
     */
    public boolean isBinary() {
        return isBinary;
    }

    /**
     * @param isBinary the isBinary to set
     */
    public void setIsBinary(boolean isBinary) {
        this.isBinary = isBinary;
    }

    /**
     * @return the toCompress
     */
    public boolean toCompress() {
        return toCompress;
    }

    /**
     * @param toCompress the toCompress to set
     */
    public void setToCompress(boolean toCompress) {
        this.toCompress = toCompress;
    }

    /**
     * @return the toCache
     */
    public boolean toCache() {
        return toCache;
    }

    /**
     * @param toCache the toCache to set
     */
    public void setToCache(boolean toCache) {
        this.toCache = toCache;
    }
    
    public static HashMap<String, ResourceProps> getResourcePropsMap(){
        return resourcePropsMap;
    }
    
    public static void setResourcePropsMap(HashMap<String, ResourceProps> _resourcesList){
        resourcePropsMap = _resourcesList;
    }
    
    public static String contentType(String resourceType) {
        ResourceProps resourceProps = null;
        String fileExtInUC = resourceType.toLowerCase(Locale.ENGLISH);
        if(resourcePropsMap.containsKey(fileExtInUC)){
            resourceProps = resourcePropsMap.get(fileExtInUC);
            if(resourceProps.isBinary){
                return resourceProps.getContentType();
            } else {
                return resourceProps.getContentType() + "; charset=UTF-8";
            }
        }
        return "application/" + resourceType;
    }
    
    public static boolean isCompressable(String resourceType){
        ResourceProps resourceProps = null;
        String fileExtInUC = resourceType.toLowerCase(Locale.ENGLISH);
        if(resourcePropsMap.containsKey(fileExtInUC)){
            resourceProps = resourcePropsMap.get(fileExtInUC);
            return resourceProps.toCompress;
        }
        return false;
    }
    
    public static boolean isCacheable(String resourceType){
        ResourceProps resourceProps = null;
        String fileExtInUC = resourceType.toLowerCase(Locale.ENGLISH);
        if(resourcePropsMap.containsKey(fileExtInUC)){
            resourceProps = resourcePropsMap.get(fileExtInUC);
            return resourceProps.toCache;
        }
        return false;
    }
    
    public static boolean isContentBinary(String mimeType) {
        if(contentTypeBinaryMap.containsKey(mimeType)){
            return contentTypeBinaryMap.get(mimeType);
        }
        
        // mime type not known, default to non-binary
        return false;
    }
    
    
    private String contentType;
    private boolean isBinary;
    private boolean toCompress;
    private boolean toCache;
    private static HashMap<String, ResourceProps> resourcePropsMap;
    private static HashMap<String, Boolean> contentTypeBinaryMap;
}
