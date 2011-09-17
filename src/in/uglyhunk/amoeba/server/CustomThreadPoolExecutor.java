/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 *
 * @author uglyhunk
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor{
    
    public CustomThreadPoolExecutor(int coreRequestProcessingThreads,
                                    int maxRequestProcessingThreads,
                                    long ttlForNonCoreThreads,
                                    TimeUnit units,
                                    BlockingQueue<Runnable> requestProcessorQueue){
        super(coreRequestProcessingThreads,
                maxRequestProcessingThreads,
                ttlForNonCoreThreads,
                units,
                requestProcessorQueue);

    }

    @Override
    public void afterExecute(Runnable r, Throwable t){
       super.afterExecute(r, t);
       if(t != null){
           Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(t), t);
       }
    }
}
