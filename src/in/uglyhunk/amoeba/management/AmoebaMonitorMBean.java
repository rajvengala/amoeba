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
    public long getRequestProcessingThreadPoolCompletedTaskCount();
    public int getRequestProcessingThreadPoolLargestPoolSize();
    public int getRequestProcessingThreadPoolQueueLength();
    public int getRequestQueueLength();
    public String getCacheInfo();
    public long getResourcesReadFromCache();
    public long getResourcesReadFromDisk();
}

