/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.njas.management;

/**
 *
 * @author rvengala
 */
public interface NjasMonitorMBean {
    public int getRequestProcessingThreadPoolSize();
    public int getRequestProcessingThreadPoolActiveCount();
    public long getRequestProcessingThreadPoolCompletedTaskCount();
    public int getRequestProcessingThreadPoolLargestPoolSize();
    public int getRequestProcessingThreadPoolQueueLength();
    public int getRequestQueueLength();
    public String getCacheInfo();
}
