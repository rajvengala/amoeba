/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author rvengala
 */
public class LRUResourceCache extends LinkedHashMap<String, byte[]> {
    
    private LRUResourceCache(int initCacheSize, float loadFactor, int cacheCapacity){
        super(initCacheSize, loadFactor, true);
        this.cacheCapacity = cacheCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Entry<String, byte[]> eldest) {
        return super.size() > cacheCapacity;
    }
    
    public Lock getCacheLock(){
        return cacheLock;
    }
    
    /**
     * Creates/retrieves LRU cache to hold resources (image, javascript, css files etc)<br/>
     * Each web application has its own cache
     * 
     * @param contextName
     * @return 
     */
    public static LRUResourceCache getCache(String contextName) {
        LRUResourceCache lruCache = null;
        ConcurrentHashMap<String, LRUResourceCache> cacheMap = RuntimeData.getCacheMap();
        Configuration conf = Configuration.getInstance();
        
        if (cacheMap.containsKey(contextName)) {
            lruCache = (LRUResourceCache) cacheMap.get(contextName);
        } else {
            lruCache = new LRUResourceCache(conf.getInitialCacheSize(), conf.getCacheLoadFactor(), conf.getCacheCapacity());
            cacheMap.put(contextName, lruCache);
        }
        return lruCache;
    }
        
    private int cacheCapacity;
    private Lock cacheLock = new ReentrantLock();
}
