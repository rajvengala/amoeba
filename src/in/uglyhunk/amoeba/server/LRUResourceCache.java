/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author rvengala
 */
public class LRUResourceCache extends LinkedHashMap<String, byte[]> {
    
    public LRUResourceCache(int initCacheSize, float loadFactor, int cacheCapacity){
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
        
    private int cacheCapacity;
    private Lock cacheLock = new ReentrantLock();
}
