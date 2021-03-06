/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.configuration;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 *
 * @author rvengala
 */
public class KernelProps {

    
    private KernelProps(){}
    
    public static KernelProps getInstance(){
        if(conf == null){
            conf = new KernelProps();
        }
        return conf;
    }
    
    /**
     * @return the largeFileMinSize
     */
    public static int getLargeFileStartSize() {
        return largeFileStartSize;
    }

    /**
     * @return the partialResponseSize
     */
    public static int getPartialResponseSize() {
        return partialResponseSize;
    }

    /**
     * @return the QUERY_STRING_SEPERATOR
     */
    public static String getParamSeperator() {
        return PARAM_SEPERATOR;
    }

    /**
     * @return the FORM_ENCODING
     */
    public static String getFormEcoding() {
        return FORM_ENCODING;
    }

    /**
     * @return the MULTIPART_FORM_ENCODING
     */
    public static String getMultipartFormEncoding() {
        return MULTIPART_FORM_ENCODING;
    }

    /**
     * @return the CHARSET
     */
    public static String getCharsetName() {
        return CHARSET_NAME;
    }

    /**
     * @return the charset
     */
    public static Charset getCharset() {
        return charset;
    }
    
    /**
     * @return simpleDateFormat instance
     */
    public static SimpleDateFormat getSimpleDateFormat() {
        return sdf;
    }

    /**
     * @return the logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * @return the CONF_FILE
     */
    public static String getConfFile() {
        return CONF_FILE;
    }
    
    /**
     * @return the CONF_FILE
     */
    public static String getContextConfFile() {
        return CONTEXT_CONF;
    }

    /**
     * @return the initialConnectionsSize
     */
    public static int getInitialSelectionKeysSize() {
        return initialSelectionKeysSize;
    }

    /**
     * @return the totalConnectionsSize
     */
    public static int getTotalSelectionKeysSize() {
        return totalSelectionKeysSize;
    }

    /**
     * @return the connectiosLoadFactor
     */
    public static float getSelectionKeyTimestampMapLoadFactor() {
        return selectionKeyTimestampMapLoadFactor;
    }

    /**
     * @return the njasHome
     */
    public String getAmoebaHome() {
        return amoebaHome;
    }

    /**
     * @param aNjasHome the njasHome to set
     */
    public void setAmoebaHome(String amoebaHome) {
        this.amoebaHome = amoebaHome;
    }

    /**
     * @return the readBufferCapacity
     */
    public int getReadBufferCapacity() {
        return readBufferCapacity;
    }

    /**
     * @param aReadBufferCapacity the readBufferCapacity to set
     */
    public void setReadBufferCapacity(int readBufferCapacity) {
        this.readBufferCapacity = readBufferCapacity;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param aHostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param aPort the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the minRequestProcessingThreads
     */
    public int getMinRequestProcessingThreads() {
        return minRequestProcessingThreads;
    }

    /**
     * @param aCoreRequestProcessingThreads the minRequestProcessingThreads to set
     */
    public void setMinRequestProcessingThreads(int minRequestProcessingThreads) {
        this.minRequestProcessingThreads = minRequestProcessingThreads;
    }

    /**
     * @return the maxRequestProcessingThreads
     */
    public int getMaxRequestProcessingThreads() {
        return maxRequestProcessingThreads;
    }

    /**
     * @param aMaxRequestProcessingThreads the maxRequestProcessingThreads to set
     */
    public void setMaxRequestProcessingThreads(int maxRequestProcessingThreads) {
        this.maxRequestProcessingThreads = maxRequestProcessingThreads;
    }

    /**
     * @return the ttlForNonCoreThreads
     */
    public long getTtlForNonCoreThreads() {
        return ttlForNonCoreThreads;
    }

    /**
     * @return the ERRLOGFILSIZE
     */
    public int getErrLogFileSize() {
        return errLogFileSize;
    }

    /**
     * @param aERRLOGFILSIZE the ERRLOGFILSIZE to set
     */
    public void setErrLogFileSize(int errLogFileSize) {
        this.errLogFileSize = errLogFileSize;
    }

    /**
     * @return the ERRLOGFILECOUNT
     */
    public int getErrLogFileCount() {
        return errLogFileCount;
    }

    /**
     * @param aERRLOGFILECOUNT the ERRLOGFILECOUNT to set
     */
    public void setErrLogFileCount(int errLogFileCount) {
        this.errLogFileCount = errLogFileCount;
    }

    /**
     * @return the documentRoot
     */
    public String getDocumentRoot() {
        return documentRoot;
    }

    /**
     * @param aDocumentRoot the documentRoot to set
     */
    public void setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    /**
     * @return the virtualHost
     */
    public boolean isVirtualHost() {
        return virtualHost;
    }

    /**
     * @param aVirtualHost the virtualHost to set
     */
    public void setVirtualHost(boolean virtualHost) {
        this.virtualHost = virtualHost;
    }

    /**
     * @return the compression
     */
    public boolean getCompression() {
        return compression;
    }

    /**
     * @param aCompression the compression to set
     */
    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    /**
     * @return the maintenance
     */
    public boolean isMaintenance() {
        return maintenance;
    }

    /**
     * @param aMaintenance the maintenance to set
     */
    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    /**
     * @return the maxAge
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * @param aMaxAge the maxAge to set
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * @return the initialCacheCapacity
     */
    public int getInitialCacheSize() {
        return initialCacheSize;
    }

    /**
     * @param aInitialCacheCapacity the initialCacheCapacity to set
     */
    public void setInitialCacheSize(int initialCacheSize) {
        this.initialCacheSize = initialCacheSize;
    }

    /**
     * @return the cacheLoadFactor
     */
    public float getCacheLoadFactor() {
        return cacheLoadFactor;
    }

    /**
     * @return the cacheSize
     */
    public int getCacheCapacity() {
        return cacheCapacity;
    }

    /**
     * @param aCacheSize the cacheSize to set
     */
    public void setCacheCapacity(int capacity) {
        cacheCapacity = capacity;
    }

    /**
     * @return the CLASSES
     */
    public static String getDynamicClassTag() {
        return DYN_CLASS_TAG;
    }

    /**
     * @return the DEFAULT_CONTEXT
     */
    public static String getDefaultContext() {
        return DEFAULT_CONTEXT;
    }

    /**
     * @return the ERROR_PAGE_FOLDER
     */
    public String getErrorPageFolder() {
        return getAmoebaHome() + File.separator + ERROR_PAGE_FOLDER;
    }

    public String getTmpFolder(){
        return getAmoebaHome() + File.separator + TMP_FOLDER;
    }
    /**
     * @return the SERVER_HEADER
     */
    public static String getSeverHeader() {
        return SERVER_HEADER;
    }
    
    
    /**
     * @return the idleChannelTimeout
     */
    public int getIdleChannelTimeout() {
        return idleChannelTimeout;
    }

    /**
     * @param idleChannelTimeout the idleChannelTimeout to set
     */
    public void setIdleChannelTimeout(int idleChannelTimeout) {
        this.idleChannelTimeout = idleChannelTimeout;
    }
     
    private int readBufferCapacity; // in bytes
    private String hostname;
    private int port;
    private int minRequestProcessingThreads;
    private int maxRequestProcessingThreads;
      
    private int errLogFileSize; // KB
    private int errLogFileCount;// max log file count
    private String documentRoot;
    private boolean virtualHost;
    private boolean compression;
    private boolean maintenance;
    private long maxAge;
    
    private int initialCacheSize;
    private static final float cacheLoadFactor = 0.75F;
    private int cacheCapacity;
    private String amoebaHome; // environment variable
    
    private int idleChannelTimeout; // seconds
    private static final int initialSelectionKeysSize = 200;
    private static final int totalSelectionKeysSize = 1000;
    private static final float selectionKeyTimestampMapLoadFactor = 0.75F;
    
    // This does not have any bearing on the thread pool
    // as the core threads and max threads are equal
    private static final long ttlForNonCoreThreads = 360; // seconds
    
    // classes dynamic requests are kept in CLASSES folder
    // inside each CONTEXT folder
    private static final String DYN_CLASS_TAG = "classes"; 
    private static final String DEFAULT_CONTEXT = "default";
    private static final String ERROR_PAGE_FOLDER = "error";
    private static final String TMP_FOLDER = "tmp"; // default is <AMOEBA_HOME>/tmp
    private static final String SERVER_HEADER = "Amoeba 0.1.0";
    private static final String CONF_FILE = "amoeba.xml";
    private static final String CONTEXT_CONF = "context.conf";
    private static final String PARAM_SEPERATOR = "&";
    private static final String FORM_ENCODING = "application/x-www-form-urlencoded";
    private static final String MULTIPART_FORM_ENCODING = "multipart/form-data";
    private static final String CHARSET_NAME = "UTF-8";
    private static Charset charset = Charset.forName(CHARSET_NAME);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    private static final Logger logger = Logger.getLogger("in.uglyhunk.amoeba");
    private static final int largeFileStartSize = 1024 * 1024; // 1 MB
    private static final int partialResponseSize = 8096; // 8KB
    private static KernelProps conf;
}
