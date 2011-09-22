/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.management;

import in.uglyhunk.amoeba.server.AmoebaThreadPoolExecutor;
import in.uglyhunk.amoeba.server.LRUResourceCache;
import in.uglyhunk.amoeba.server.Main;
import in.uglyhunk.amoeba.server.RequestBean;
import in.uglyhunk.amoeba.server.ResponseCreator;
import in.uglyhunk.amoeba.server.RuntimeData;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author rvengala
 */
public class AmoebaMonitor implements AmoebaMonitorMBean{
    
    public AmoebaMonitor(){
        requestProcessingThreadPool = RuntimeData.getRequestProcessingThreadPool();
        threadPoolQueue = RuntimeData.getThreadPoolQueue();
        requestQueue = RuntimeData.getRequestQueue();
        cacheMap = RuntimeData.getCacheMap();
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
   
    public long getResourcesReadFromCache() {
        return ResponseCreator.getResourcesReadFromCache();
    }

    public long getResourcesReadFromDisk() {
        return ResponseCreator.getResourcesReadFromDisk();
    }
    
    public int getOpenSocketsCount() {
        return Main.getOpenSocketCount();
    }
    
    private static AmoebaThreadPoolExecutor requestProcessingThreadPool;
    private static ArrayBlockingQueue<Runnable> threadPoolQueue;
    private static ArrayBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<String, LRUResourceCache> cacheMap;

}

