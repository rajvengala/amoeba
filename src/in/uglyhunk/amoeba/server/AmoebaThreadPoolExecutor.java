/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.configuration.KernelProps;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 *
 * @author uglyhunk
 */
public class AmoebaThreadPoolExecutor extends ThreadPoolExecutor{
    
    public AmoebaThreadPoolExecutor(int coreRequestProcessingThreads,
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
           KernelProps.getLogger().log(Level.WARNING, Utilities.stackTraceToString(t), t);
       }
    }
}
