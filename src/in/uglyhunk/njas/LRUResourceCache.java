/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.njas;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author rvengala
 */
public class LRUResourceCache extends LinkedHashMap<String, byte[]> {
    
    public LRUResourceCache(int cacheSize){
        super(conf.getInitialCacheCapacity(), conf.getCacheLoadFactor(), true);
        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<String, byte[]> eldest) {
        return super.size() > cacheSize;
    }
    
    public static Lock getCacheLock(){
        return cacheLock;
    }
    
    private int cacheSize;
    private static Configuration conf = Main.getConf();
    private static Lock cacheLock = new ReentrantLock();
}
