/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.management;

/**
 *
 * @author rvengala
 */
public interface AmoebaMonitorMBean {
    public int getRequestProcessingThreadPoolSize();
    public int getRequestProcessingThreadPoolActiveCount();
    public int getRequestProcessingThreadPoolQueueLength();
    public int getRequestQueueLength();
    public String getCacheInfo();
    public long getResourcesReadFromCache();
    public long getResourcesReadFromDisk();
    public int getOpenChannelsCount();
    public int getResponseMapSize();
    public int getSelectionKeyQueueLength();
    public int getClassLoaderCount();
    public int getSelectionKeyTimestampMapSize();
}

