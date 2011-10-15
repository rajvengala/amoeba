/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.dyn.DynamicRequest;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
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
    public static ConcurrentHashMap<SelectionKey, ResponseBean> getResponseMap() {
        return responseMap;
    }

    /**
     * @param aResponseMap the responseMap to set
     */
    public static void setResponseMap(ConcurrentHashMap<SelectionKey, ResponseBean> aResponseMap) {
        responseMap = aResponseMap;
    }

    /**
     * @return the requestsTimestampQueue
     */
    public static LinkedBlockingQueue<SelectionKey> getSelectionKeyQueue() {
        return selectionKeyQueue;
    }

    /**
     * @param aRequestsTimestampQueue the requestsTimestampQueue to set
     */
    public static void setSelectionKeyQueue(LinkedBlockingQueue<SelectionKey> aSelectionKeyQueue) {
        selectionKeyQueue = aSelectionKeyQueue;
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
    public static HashMap<SelectionKey, Long> getSelectionKeyTimestampMap() {
        return selectionKeylTimestampMap;
    }

    /**
     * @param aIdleChannelMap the idleChannelMap to set
     */
    public static void setSelectionKeyTimestampMap(HashMap<SelectionKey, Long> aSelectionKeyTimestampMap) {
        selectionKeylTimestampMap = aSelectionKeyTimestampMap;
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
    
    
    public static ArrayList<SelectionKey> getIdleSelectionKeyList(){
        return idleSelectionKeyList;
    }
    
    public static void setIdleSelectionKeyList(ArrayList<SelectionKey> aIdleSelectionKeyList){
        idleSelectionKeyList = aIdleSelectionKeyList;
    }
    
    /**
     * @return the selectionKeyMappedByteBufferMap
     */
    public static HashMap<SelectionKey, Boolean> getSelectionKeyLargeFileMap() {
        return selectionKeyLargeFileMap;
    }

    /**
     * @param aSelectionKeyMappedByteBufferMap the selectionKeyMappedByteBufferMap to set
     */
    public static void setSelectionKeyLargeFileMap(HashMap<SelectionKey, Boolean> _selectionKeyLargeFileMap) {
       selectionKeyLargeFileMap = _selectionKeyLargeFileMap;
    }
        
    
    /**
     * @return the partialRequestMap
     */
    public static HashMap<SelectionKey,RequestProperties> getPartialRequestMap() {
        return partialRequestMap;
    }

    /**
     * @param aPartialRequestMap the partialRequestMap to set
     */
    public static void setPartialRequestMap(HashMap<SelectionKey, RequestProperties> aPartialRequestMap) {
        partialRequestMap = aPartialRequestMap;
    }
    
    // ************* Members *****************
     
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
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap;
    
    /*
     * SelectionKey of each request will be pushed into the queue and 
     * removed from the queue after the response is sent to the client
     */
    private static LinkedBlockingQueue<SelectionKey> selectionKeyQueue;
    
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
    private static HashMap<SelectionKey, Long> selectionKeylTimestampMap;
    
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
    
    /*
     * List of SelectionKeys which can be discarded
     * Selection keys are added to the list after
     * timeout for a channel has reached
     */
    private static ArrayList<SelectionKey> idleSelectionKeyList;
    
    /*
     * Maps selection keys to memorymappedbuffer
     * This is used when sending large file response
     * 
     */
    private static HashMap<SelectionKey, Boolean> selectionKeyLargeFileMap;
    
    /*
     * Maps selection keys to byte array of partially received requests
     */
    private static HashMap<SelectionKey, RequestProperties> partialRequestMap;

    

}
