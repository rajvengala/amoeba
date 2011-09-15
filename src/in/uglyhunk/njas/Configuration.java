/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.njas;

import java.io.File;

/**
 *
 * @author rvengala
 */
public class Configuration {

    /**
     * @return the njasHome
     */
    public String getNjasHome() {
        return njasHome;
    }

    /**
     * @param aNjasHome the njasHome to set
     */
    public void setNjasHome(String njasHome) {
        this.njasHome = njasHome;
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
    public void setReadBufferCapacity(int aReadBufferCapacity) {
        readBufferCapacity = aReadBufferCapacity;
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
    public void setHostname(String aHostname) {
        hostname = aHostname;
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
    public void setPort(int aPort) {
        port = aPort;
    }

    /**
     * @return the threadPoolQueueLength
     */
    public int getThreadPoolQueueLength() {
        return threadPoolQueueLength;
    }

    /**
     * @param aTasksQueueLength the threadPoolQueueLength to set
     */
    public void setThreadPoolQueueLength(int threadPoolQueueLength) {
        this.threadPoolQueueLength = threadPoolQueueLength;
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
     * @return the requestQueueLength
     */
    public int getRequestQueueLength() {
        return requestQueueLength;
    }

    /**
     * @param aRequestQueueLength the requestQueueLength to set
     */
    public void setRequestQueueLength(int aRequestQueueLength) {
        requestQueueLength = aRequestQueueLength;
    }

    /**
     * @return the responseOrderQueueLength
     */
    public int getRequestsTimestampQueueLength() {
        return requestsTimestampQueueLength;
    }

    /**
     * @param aResponseOrderQueueLength the responseOrderQueueLength to set
     */
    public void setRequestsTimestampQueueLength(int requestsTimestampQueueLength) {
        this.requestsTimestampQueueLength = requestsTimestampQueueLength;
    }

    /**
     * @return the ttlForNonCoreThreads
     */
    public long getTtlForNonCoreThreads() {
        return ttlForNonCoreThreads;
    }

    /**
     * @param aTtlForNonCoreThreads the ttlForNonCoreThreads to set
     */
    public void setTtlForNonCoreThreads(long aTtlForNonCoreThreads) {
        ttlForNonCoreThreads = aTtlForNonCoreThreads;
    }

    /**
     * @return the ERRLOGFILSIZE
     */
    public int getERRLOGFILSIZE() {
        return ERRLOGFILSIZE;
    }

    /**
     * @param aERRLOGFILSIZE the ERRLOGFILSIZE to set
     */
    public void setERRLOGFILSIZE(int aERRLOGFILSIZE) {
        ERRLOGFILSIZE = aERRLOGFILSIZE;
    }

    /**
     * @return the ERRLOGFILECOUNT
     */
    public int getERRLOGFILECOUNT() {
        return ERRLOGFILECOUNT;
    }

    /**
     * @param aERRLOGFILECOUNT the ERRLOGFILECOUNT to set
     */
    public void setERRLOGFILECOUNT(int aERRLOGFILECOUNT) {
        ERRLOGFILECOUNT = aERRLOGFILECOUNT;
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
    public void setDocumentRoot(String aDocumentRoot) {
        documentRoot = aDocumentRoot;
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
    public void setVirtualHost(boolean aVirtualHost) {
        virtualHost = aVirtualHost;
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
    public void setMaintenance(boolean aMaintenance) {
        maintenance = aMaintenance;
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
    public void setMaxAge(long aMaxAge) {
        maxAge = aMaxAge;
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
     * @param aCacheLoadFactor the cacheLoadFactor to set
     */
    public void setCacheLoadFactor(float aCacheLoadFactor) {
        cacheLoadFactor = aCacheLoadFactor;
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
    public String getClasses() {
        return CLASSES;
    }

    /**
     * @return the DEFAULT_CONTEXT
     */
    public String getDefaultContext() {
        return DEFAULT_CONTEXT;
    }

    /**
     * @return the ERROR_PAGE_FOLDER
     */
    public String getErrorPageFolder() {
        return getNjasHome() + File.separator + ERROR_PAGE_FOLDER;
    }

    /**
     * @return the SERVER_HEADER
     */
    public String getSeverHeader() {
        return SERVER_HEADER;
    }
    
    private int readBufferCapacity; // in bytes
    private String hostname;
    private int port;
    private int threadPoolQueueLength;
    private int minRequestProcessingThreads;
    private int maxRequestProcessingThreads;
    private int requestQueueLength;
    private int requestsTimestampQueueLength;
    private long ttlForNonCoreThreads;
    private int ERRLOGFILSIZE; // KB
    private int ERRLOGFILECOUNT;// max log file count
    private String documentRoot;
    private boolean virtualHost;
    private boolean compression;
    private boolean maintenance;
    private long maxAge;
    private int initialCacheSize;
    private float cacheLoadFactor;
    private int cacheCapacity;
    private String njasHome; // environment variable
    private static final String CLASSES = "bin";
    private static final String DEFAULT_CONTEXT = "default";
    private static final String ERROR_PAGE_FOLDER = "error";
    private static final String SERVER_HEADER = "Nano Java App Server 0.1";
    
}
