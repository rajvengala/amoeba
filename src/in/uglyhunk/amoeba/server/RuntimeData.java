/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.dyn.DynamicRequest;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author rvengala
 */
public class RuntimeData {
    

    /**
     * @return the requestProcessingThreadPool
     */
    public static AmoebaThreadPoolExecutor getRequestProcessingThreadPool() {
        return requestProcessingThreadPool;
    }

    /**
     * @param aRequestProcessingThreadPool the requestProcessingThreadPool to set
     */
    public static void setRequestProcessingThreadPool(AmoebaThreadPoolExecutor aRequestProcessingThreadPool) {
        requestProcessingThreadPool = aRequestProcessingThreadPool;
    }

    /**
     * @return the threadPoolQueue
     */
    public static LinkedBlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }

    /**
     * @param aThreadPoolQueue the threadPoolQueue to set
     */
    public static void setThreadPoolQueue(LinkedBlockingQueue<Runnable> aThreadPoolQueue) {
        threadPoolQueue = aThreadPoolQueue;
    }

    /**
     * @return the requestQueue
     */
    public static LinkedBlockingQueue<RequestBean> getRequestQueue() {
        return requestQueue;
    }

    /**
     * @param aRequestQueue the requestQueue to set
     */
    public static void setRequestQueue(LinkedBlockingQueue<RequestBean> aRequestQueue) {
        requestQueue = aRequestQueue;
    }

    /**
     * @return the responseMap
     */
    public static ConcurrentHashMap<Long, ResponseBean> getResponseMap() {
        return responseMap;
    }

    /**
     * @param aResponseMap the responseMap to set
     */
    public static void setResponseMap(ConcurrentHashMap<Long, ResponseBean> aResponseMap) {
        responseMap = aResponseMap;
    }

    /**
     * @return the requestsTimestampQueue
     */
    public static LinkedBlockingQueue<Long> getRequestsTimestampQueue() {
        return requestsTimestampQueue;
    }

    /**
     * @param aRequestsTimestampQueue the requestsTimestampQueue to set
     */
    public static void setRequestsTimestampQueue(LinkedBlockingQueue<Long> aRequestsTimestampQueue) {
        requestsTimestampQueue = aRequestsTimestampQueue;
    }

    /**
     * @return the cacheMap
     */
    public static ConcurrentHashMap<String, LRUResourceCache> getCacheMap() {
        return cacheMap;
    }

    /**
     * @param aCacheMap the cacheMap to set
     */
    public static void setCacheMap(ConcurrentHashMap<String, LRUResourceCache> aCacheMap) {
        cacheMap = aCacheMap;
    }

    /**
     * @return the classLoaderMap
     */
    public static ConcurrentHashMap<String, AmoebaClassLoader> getClassLoaderMap() {
        return classLoaderMap;
    }

    /**
     * @param aClassLoaderMap the classLoaderMap to set
     */
    public static void setClassLoaderMap(ConcurrentHashMap<String, AmoebaClassLoader> aClassLoaderMap) {
        classLoaderMap = aClassLoaderMap;
    }

    /**
     * @return the idleChannelMap
     */
    public static LinkedHashMap<SelectionKey, Long> getIdleChannelMap() {
        return idleChannelMap;
    }

    /**
     * @param aIdleChannelMap the idleChannelMap to set
     */
    public static void setIdleChannelMap(LinkedHashMap<SelectionKey, Long> aIdleChannelMap) {
        idleChannelMap = aIdleChannelMap;
    }

    /**
     * @return the contextMap
     */
    public static ConcurrentHashMap<String, HashMap<String, String>> getContextMap() {
        return contextMap;
    }

    /**
     * @param aContextMap the contextMap to set
     */
    public static void setContextMap(ConcurrentHashMap<String, HashMap<String, String>> aContextMap) {
        contextMap = aContextMap;
    }
    
    
    /**
     * @return the dymaicClassInstanceMap
     */
    public static ConcurrentHashMap<String, HashMap<String, DynamicRequest>> getContextDymaicInstanceMap() {
        return contextDymaicInstanceMap;
    }

    /**
     * @param aDymaicClassInstanceMap the dymaicClassInstanceMap to set
     */
    public static void setContextDynamicInstanceMap(ConcurrentHashMap<String, HashMap<String, DynamicRequest>> aContextDymaicInstanceMap) {
        contextDymaicInstanceMap = aContextDymaicInstanceMap;
    }
    
        
    /*
     * Instance of a class that extends ThreadPoolExecutor
     */
    private static AmoebaThreadPoolExecutor requestProcessingThreadPool;
    
    /*
     * Queue to the AmoebaThreadPoolExecutor. When core
     * threads of the pool are active, incoming requests are
     * queued here.
     */
    private static LinkedBlockingQueue<Runnable> threadPoolQueue;
    
    /*
     * All the http requests are first queue in RequestQueue.
     * Thread pool executor retrieves the requestBeans from this queue
     * and processes them with optimal number of threads
     */
    private static LinkedBlockingQueue<RequestBean> requestQueue;
    
    /*
     * Maps the timestamp of the request with the response bean for that request
     */
    private static ConcurrentHashMap<Long, ResponseBean> responseMap;
    
    /*
     * timestamp of each request will be entered into the queue and removed from
     * the queue after the response is sent to the client
     */
    private static LinkedBlockingQueue<Long> requestsTimestampQueue;
    
    /*
     * Maps context name to the LRUResourceCache object.
     * Each context has its own LRU Resource Cache object
     */
    private static ConcurrentHashMap<String, LRUResourceCache> cacheMap;
    
    /*
     * Maps context name to AmoebaClassLoader instance.
     * Each context has its own ClassLoader to load classes that handle dynamic requests
     */
    private static ConcurrentHashMap<String, AmoebaClassLoader> classLoaderMap;
    
    /*
     * Maps selection key to the timestamp of the request.
     */
    private static LinkedHashMap<SelectionKey, Long> idleChannelMap;
    
    /*
     * Maps the context to another map data structure(referred map).
     * Referred HashMap is the runtime state of context.conf file inside
     * each context directory
     */
    private static ConcurrentHashMap<String, HashMap<String, String>> contextMap;
    
    /*
     * Maps the context name to another map data structure(referred map)
     * Referred map maps the className to its instance.
     */
    private static ConcurrentHashMap<String, HashMap<String, DynamicRequest>> contextDymaicInstanceMap;

}
