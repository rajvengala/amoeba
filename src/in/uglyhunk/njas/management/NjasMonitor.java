/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.njas.management;

import in.uglyhunk.njas.CustomThreadPoolExecutor;
import in.uglyhunk.njas.LRUResourceCache;
import in.uglyhunk.njas.Main;
import in.uglyhunk.njas.RequestBean;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author rvengala
 */
public class NjasMonitor implements NjasMonitorMBean{
    
    public NjasMonitor(){
        requestProcessingThreadPool = Main.getRequestProcessingThreadPool();
        threadPoolQueue = Main.getThreadPoolQueue();
        requestQueue = Main.getRequestQueue();
        cacheMap = Main.getCacheMap();
    }

    public int getRequestProcessingThreadPoolSize() {
        return requestProcessingThreadPool.getPoolSize();
    }

    public int getRequestProcessingThreadPoolActiveCount() {
        return requestProcessingThreadPool.getActiveCount();
    }

    public long getRequestProcessingThreadPoolCompletedTaskCount() {
        return requestProcessingThreadPool.getCompletedTaskCount();
    }

    public int getRequestProcessingThreadPoolLargestPoolSize() {
        return requestProcessingThreadPool.getLargestPoolSize();
    }

    public int getRequestProcessingThreadPoolQueueLength() {
        return threadPoolQueue.size();
    }
    
    public int getRequestQueueLength() {
        return requestQueue.size();
    }

    public String getCacheInfo() {
        StringBuilder cacheInfo = new StringBuilder();
        Set<String> cacheNames = cacheMap.keySet();
        for(String cacheName : cacheNames){
            LRUResourceCache cache = (LRUResourceCache)cacheMap.get(cacheName);
            cacheInfo.append(cacheName).append(" - ").append(cache.size()).append(";");
        }
        return cacheInfo.toString();
    }
    
    private static CustomThreadPoolExecutor requestProcessingThreadPool;
    private static ArrayBlockingQueue<Runnable> threadPoolQueue;
    private static ArrayBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<String, LRUResourceCache> cacheMap;
}
