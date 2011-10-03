/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.management;

import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.server.AmoebaThreadPoolExecutor;
import in.uglyhunk.amoeba.server.LRUResourceCache;
import in.uglyhunk.amoeba.server.Main;
import in.uglyhunk.amoeba.server.RequestBean;
import in.uglyhunk.amoeba.server.ResponseBean;
import in.uglyhunk.amoeba.server.ResponseCreator;
import in.uglyhunk.amoeba.server.RuntimeData;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

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
        responseMap = RuntimeData.getResponseMap();
        selectionKeyQueue = RuntimeData.getSelectionKeyQueue();
        classLoaderMap = RuntimeData.getClassLoaderMap();
        selectionKeyTimestampMap = RuntimeData.getSelectionKeyTimestampMap();
    }

    public int getRequestProcessingThreadPoolSize() {
        return requestProcessingThreadPool.getPoolSize();
    }

    public int getRequestProcessingThreadPoolActiveCount() {
        return requestProcessingThreadPool.getActiveCount();
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
    
    public int getActiveChannelsCount() {
        return Main.getActiveChannelsCount();
    }
    
    public int getResponseMapSize() {
       return responseMap.size();
    }

    public int getSelectionKeyQueueLength() {
        return selectionKeyQueue.size();
    }
    
    public int getClassLoaderCount() {
        return classLoaderMap.size();
    }

    public int getSelectionKeyTimestampMapSize() {
        return selectionKeyTimestampMap.size();
    }

    private static AmoebaThreadPoolExecutor requestProcessingThreadPool;
    private static LinkedBlockingQueue<Runnable> threadPoolQueue;
    private static LinkedBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<String, LRUResourceCache> cacheMap;
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap;
    private static LinkedBlockingQueue<SelectionKey> selectionKeyQueue;
    private static ConcurrentHashMap<String, AmoebaClassLoader> classLoaderMap;
    private static HashMap<SelectionKey, Long> selectionKeyTimestampMap;
}

