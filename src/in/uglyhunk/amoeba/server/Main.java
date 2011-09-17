package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.management.AmoebaMonitoringAgent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Contains the <b>main</b> method to run the application server
 * {@link #main(java.lang.String[])} 
 * @author uglyhunk
 * 
 * 
 */
public class Main {

    /**
     * Kick-starts the execution of the application server. <br/>
     * It executes the following methods in the specified order <br/>
     * {@link #loadConfiguration()}
     * {@link #readConfiguration()}
     * {@link #setupHandlers()} 
     * {@link #logConfiguration()}
     * {@link #setupDataStructures()}
     * {@link #setupWorkerThreads()} 
     * 
     * @param args not used
     * 
     */
    public static void main(String[] args) {
        try {
            loadConfiguration();
            readConfiguration();
            setupHandlers();
            logConfiguration();
            setupDataStructures();
            setupWorkerThreads();
            startMonitoringAgent();
            runDaemon();
        } catch (Exception e) {
            logger.log(Level.SEVERE, Utilities.stackTraceToString(e), e);
            System.exit(1);
        }
    }

    /**
     * - Sets the timezone for SimpleDateFormat instance to GMT <br/>
     * - Reads AMOEBA_HOME environment variable <br/>
     * - Loads amoeba.conf properties file in AMOEBA_HOME/conf directory <br/>
     */
    private static void loadConfiguration() {
        // initialize configuration
        conf = new Configuration();
        
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String njasHome = System.getenv("AMOEBA_HOME");
        conf.setNjasHome(njasHome);
        if (njasHome == null) {
            System.out.println("AMOEBA_HOME environemnt variable is not set");
            System.exit(1);
        }

        props = new Properties();
        String propFilePath = njasHome + File.separator + "conf" + File.separator + CONF_FILE;
        try {
            FileReader reader = new FileReader(propFilePath);
            props.load(reader);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Configuration file not found");
            System.out.println(fnfe.toString());
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Exception while loading configuration file");
            System.out.println(ioe.toString());
            System.exit(1);
        }
    }

    /**
     * Read properties from njas.conf file in NJAS_HOME/conf directory.
     */
    private static void readConfiguration() {
        conf.setHostname(props.getProperty("hostname"));
        conf.setPort(Integer.parseInt(props.getProperty("port")));

        // read buffer properties
        conf.setReadBufferCapacity(Integer.parseInt(props.getProperty("readBufferCapacity")) * 1024);
        
        // request processing threads
        conf.setMinRequestProcessingThreads(Integer.parseInt(props.getProperty("minRequestProcessingThreads")));
        conf.setMaxRequestProcessingThreads(Integer.parseInt(props.getProperty("maxRequestProcessingThreads")));
        conf.setTtlForNonCoreThreads(Integer.parseInt(props.getProperty("ttlForNonCoreThreads")));
        
        // tasks queue
        conf.setThreadPoolQueueLength(Integer.parseInt(props.getProperty("threadPoolQueueLength")));
        
        // request queue
        int reqQueueLength = Integer.parseInt(props.getProperty("requestQueueLength"));
        conf.setRequestQueueLength(reqQueueLength);
        // queue length of request timestamps is same as that of for raw requests
        conf.setRequestsTimestampQueueLength(reqQueueLength);
        
        // web root
        conf.setDocumentRoot(props.getProperty("documentRoot"));
        conf.setVirtualHost(Boolean.parseBoolean(props.getProperty("virtualHost")));

        // compression
        conf.setCompression(Boolean.parseBoolean(props.getProperty("compression")));
        
        // log file
        conf.setERRLOGFILSIZE(1024 * Integer.parseInt(props.getProperty("errorLogFileSize")));
        conf.setERRLOGFILECOUNT(Integer.parseInt(props.getProperty("totalErrorLogFiles")));

        // maintenance
        conf.setMaintenance(Boolean.parseBoolean(props.getProperty("maintenance")));
        
        // cache
        conf.setMaxAge(Long.parseLong(props.getProperty("maxAge")));
        conf.setInitialCacheSize(Integer.parseInt(props.getProperty("initialCacheSize")));
        conf.setCacheLoadFactor(Float.parseFloat(props.getProperty("cacheLoadFactor")));
        conf.setCacheCapacity(Integer.parseInt(props.getProperty("cacheCapacity")));
    }

    /**
     * Create console and file handlers
     */
    private static void setupHandlers() {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        console = new ConsoleHandler();
        // set to info in production
        console.setLevel(Level.INFO);
        console.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord logRecord) {
                return logRecord.getLevel() + " : " + formatMessage(logRecord) + Utilities.getEOL();
            }
        });
        logger.addHandler(console);

        // create logs directory
        logDir = new File(conf.getNjasHome() + File.separator + "logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        String message = "Log dir - " + logDir.getPath();
        logger.info(message);

        try {
            logFile = new FileHandler(logDir + File.separator + "njas_%g.log", conf.getERRLOGFILSIZE(), conf.getERRLOGFILECOUNT());
            accessLog = new FileHandler(logDir + File.separator + "njas_access_%g.log", conf.getERRLOGFILSIZE(), conf.getERRLOGFILECOUNT());
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, ioe.toString(), ioe);
            System.exit(1);
        }

        logFile.setLevel(Level.FINE);
        logFile.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord logRecord) {
                String timestamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.S").format(new Date(logRecord.getMillis()));
                return logRecord.getLevel() + " - " + timestamp + " - " + formatMessage(logRecord) + Utilities.getEOL();
            }
        });

        accessLog.setLevel(Level.FINER);
        accessLog.setFilter(new AccessLogFilter());
        accessLog.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord logRecord) {
                String timestamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.S").format(new Date(logRecord.getMillis()));
                return timestamp + " - " + formatMessage(logRecord) + Utilities.getEOL();
            }
        });

        logger.addHandler(logFile);
        logger.addHandler(accessLog);
    }

    /**
     * Log configuration parameters read from njas.conf to console/file
     */
    private static void logConfiguration() {
        logger.log(Level.INFO, "Hostname - {0}", conf.getHostname());
        logger.log(Level.INFO, "Port - {0}", conf.getPort());

        logger.log(Level.INFO, "Request buffer capacity - {0} KB", conf.getReadBufferCapacity() / 1024);

        logger.log(Level.INFO, "Min. request processing threads in the thread pool - {0}", conf.getMinRequestProcessingThreads());
        logger.log(Level.INFO, "Max. request processing threads in the thread pool - {0}", conf.getMaxRequestProcessingThreads());
        logger.log(Level.INFO, "Time to live for non-core request processing threads - {0} seconds", conf.getTtlForNonCoreThreads());

        logger.log(Level.INFO, "Queue length of the request beans - {0}", conf.getRequestQueueLength());
        logger.log(Level.INFO, "Queue length of the request processing thread pool - {0}", conf.getThreadPoolQueueLength());

        logger.log(Level.INFO, "DocumentRoot - {0}", conf.getDocumentRoot());
        logger.log(Level.INFO, "VirtualHost - {0}", conf.isVirtualHost());
        logger.log(Level.INFO, "Compression - {0}", conf.getCompression());

        logger.log(Level.INFO, "Error log file size - {0} KB", conf.getERRLOGFILSIZE() / 1024);
        logger.log(Level.INFO, "Total error log files - {0}", conf.getERRLOGFILECOUNT());

        logger.log(Level.INFO, "In maintenance - {0}", conf.isMaintenance());
        logger.log(Level.INFO, "Max Age - {0} seconds for cacheable resources", conf.getMaxAge());

        logger.log(Level.INFO, "Cache : Initial size - {0}", conf.getInitialCacheSize());
        logger.log(Level.INFO, "Cache : Load factor - {0}", conf.getCacheLoadFactor());
        logger.log(Level.INFO, "Cache : Capacity - {0}", conf.getCacheCapacity());
    }

    /**
     * Sets up the following data structures <br/>
     * <i>requestQueue</i> - Queue of size <i>requestQueueLength</i> to hold the requests from clients <br/>
     * <i>requestsTimestampQueue</i> - Queue of size <i>requestsTimestampQueue</i> to hold the timestamps of requests 
     * from the clients in the order their arrival <br/>
     * <i>responseMap</i> - processed requests from the requestQueue will be stored in this map with timestamp
     * of the request as key <br/>
     * 
     */
    private static void setupDataStructures() {
        requestQueue = new ArrayBlockingQueue<RequestBean>(conf.getRequestQueueLength(), true);
        responseMap = new ConcurrentHashMap<Long, ResponseBean>();
        requestsTimestampQueue = new ArrayBlockingQueue<Long>(conf.getRequestsTimestampQueueLength(), true);
        cacheMap = new ConcurrentHashMap<String, LRUResourceCache>();
    }

    /**
     * requestProcessingThreadPool - Each thread in the pool takes the request from the requestQueue and saves the
     * response in the response map. If the response is cacheable, it saves the response in the lruCache. Thread pool
     * has the default running threads of <i>coreRequestProcessingThreads</i>, maximum threads of 
     * <i>maxRequestProcessingThreads</i>
     * and a timeout for non-core running threads of <i>ttlForNonCoreThreads</i>. This pool has an associated queue of length
     * <i>tasksQueueLength</i>. This queue will be used to hold the requests from requestQueue temporarily if thread pool
     * can not allocate any more threads to process the request.
     * 
     */
    private static void setupWorkerThreads() {
        // each http request from the request queue is prepared as task
        // and submitted to a pool of threads
        threadPoolQueue = new ArrayBlockingQueue<Runnable>(conf.getThreadPoolQueueLength());

        requestProcessingThreadPool = new CustomThreadPoolExecutor(
                                        conf.getMinRequestProcessingThreads(),
                                        conf.getMaxRequestProcessingThreads(),
                                        conf.getTtlForNonCoreThreads(),
                                        TimeUnit.SECONDS,
                                        threadPoolQueue);
    }

    /**
     * 
     * @throws Exception 
     */
    private static void runDaemon() throws Exception {
        // create a selector
        selector = SelectorProvider.provider().openSelector();
        logger.info("Selector created");

        // create non-blocking server socket channel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        // bind server socket channel to host:port
        InetSocketAddress isa = new InetSocketAddress(conf.getHostname(), conf.getPort());
        serverSocketChannel.socket().bind(isa);
        logger.log(Level.INFO, "Server listening at {0}", isa.toString());

        // register server socket channel to the previously
        // created selector and indicate the interest
        // in accepting new connections
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.fine("Registered channel with selector to alert for clien accept connection request arrives ");

        while (true) {
            // wait for an event on one of the registered channels
            selector.select();
            
            // iterate over the keys and respond to the events
            Iterator selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = (SelectionKey) selectedKeys.next();
                selectedKeys.remove();

                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        acceptConnections(key);
                    } else if (key.isReadable()) {
                        readDataFromChannel(key);
                    } else if (key.isWritable()) {
                        writeToChannel(key);
                    }
                }
            }
            
            // ugly work around to read the requests 
            // from the browser in the correct order
            Thread.sleep(EVENT_LOOP_DELAY); 
        }
    }
    
    private static void startMonitoringAgent(){
        new AmoebaMonitoringAgent();
    }

    /**
     * accept the client connection and make the channel generate alerts when the data is ready to be read
     * @param key selection key
     */
    private static void acceptConnections(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel socketChannel = serverChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            openSocketsCount++;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
        }

    }

    /**
     * read the data from the channel, save it in a buffer and add it to the request processing queue
     * @param key selection key
     */
    private static void readDataFromChannel(SelectionKey key) {
        ByteBuffer readBuffer = ByteBuffer.allocate(conf.getReadBufferCapacity());

        SocketChannel socketChannel = null;
        int bytesRead;
        try {
            socketChannel = (SocketChannel) key.channel();
            bytesRead = socketChannel.read(readBuffer);

            if (bytesRead == -1) {
                // channel has reached the end of the stream
                // remote socket shutdown was clean
                socketChannel.close();
                key.cancel();
                return;
            }

            long timestamp = System.nanoTime();

            // create a request bean from raw data
            RequestBean reqBean = new RequestBean();
            reqBean.setRawRequestBytes(readBuffer.array());
            reqBean.setSelectionKey(key);
            reqBean.setTimestamp(timestamp);
            
            //System.out.println(new String(readBuffer.array()).split("\r\n")[0]);

            // put the request timestamp in the queue.
            // responses will be sent in the order of requests
            // as appeared in this queue
            requestsTimestampQueue.put(timestamp);

            // put the request in request queue
            requestQueue.put(reqBean);

            // pass on the data read from the channel to
            // a request processing thread pool
            RequestProcessor requestProcessor = new RequestProcessor();
            requestProcessingThreadPool.execute(requestProcessor);
            
        } catch (IOException ioe) {
            // connection abruptly closed
            // cancel the selection key and close the channel
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            key.cancel();
            try {
                socketChannel.close();
                openSocketsCount--;
            } catch (IOException ioe2) {
                logger.log(Level.WARNING, Utilities.stackTraceToString(ioe2), ioe2);
            }
            return;
        } catch (InterruptedException ie) {
            logger.log(Level.WARNING, Utilities.stackTraceToString(ie), ie);
        }
    }

    public static void writeToChannel(SelectionKey key) {
        SocketChannel socketChannel = null;
        try {
            Long timestamp = requestsTimestampQueue.peek();
            if (timestamp != null && responseMap.containsKey(timestamp)) {
                ResponseBean respBean = responseMap.get(timestamp);

                // remove the request timestamp from requestsTimestampQueue
                // remove the response bean object from responseMap
                responseMap.remove(requestsTimestampQueue.poll());

                String statusLine = respBean.getStatusLine();
                String contentType = respBean.getContentType();
                String contentLength = respBean.getContentLength();
                String server = respBean.getServer();
                String statusCode = respBean.getStatusCode();
                String resource = respBean.getAbsoluteResource();
                long lastModified = respBean.getLastModified();
                String respCacheTag = respBean.getresponseCacheTag();
                if (respCacheTag == null) {
                    respCacheTag = "";
                }

                String eTag = respBean.getETag();
                ByteBuffer respBodyBuffer = respBean.getBody();
                if (respBodyBuffer != null) {
                    respBodyBuffer.flip();
                }

                StringBuilder respHeaders = new StringBuilder();
                respHeaders.append(statusLine).append(Utilities.getHTTPEOL()).append("Content-Type: ").append(contentType).append(Utilities.getHTTPEOL()).append("Server: ").append(server).append(Utilities.getHTTPEOL());

                if (contentLength != null) {
                    respHeaders.append("Content-Length: ").append(contentLength).append(Utilities.getHTTPEOL());
                }

                if(conf.getCompression()) {
                    String contentEncoding = respBean.getContentEncoding();
                    if (contentEncoding != null) {
                        respHeaders.append("Content-Encoding: ").append(respBean.getContentEncoding()).append(Utilities.getHTTPEOL());
                    }
                }

                if (lastModified != 0) {
                    String lastModifiedValue = sdf.format(new Date(lastModified));
                    respHeaders.append("Last-Modified: ").append(lastModifiedValue).append(Utilities.getHTTPEOL());
                }

                if (eTag != null) {
                    respHeaders.append("ETag: \"").append(eTag).append("\"").append(Utilities.getHTTPEOL());
                    respHeaders.append("Cache-Control: max-age=").append(conf.getMaxAge()).append(Utilities.getHTTPEOL());
                }

                respHeaders.append("Date: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(Utilities.getHTTPEOL());
                respHeaders.append(Utilities.getHTTPEOL());

                byte headerBytes[] = respHeaders.toString().getBytes("UTF-8");
                ByteBuffer respHeadersBuffer = ByteBuffer.allocate(headerBytes.length);
                respHeadersBuffer.put(headerBytes);
                respHeadersBuffer.flip();

                int respSize = 0;
                if (respBodyBuffer != null) {
                    respSize = respBodyBuffer.capacity() + respHeadersBuffer.capacity();
                } else {
                    respSize = respHeadersBuffer.capacity();
                }

                ByteBuffer respByteBuffer = ByteBuffer.allocate(respSize);
                respByteBuffer.put(respHeadersBuffer.array());

                if (respBodyBuffer != null) {
                    respByteBuffer.put(respBodyBuffer.array());
                }

                respByteBuffer.flip();
                socketChannel = (SocketChannel) key.channel();
                int totalBytesSent = socketChannel.write(respByteBuffer);

                String clientAddr = socketChannel.socket().getRemoteSocketAddress().toString().split("/")[1];

                Main.getLogger().log(Level.FINER, "{0} - {1} - {2} - {3} bytes {4} ",
                        new Object[]{clientAddr, resource,
                            statusCode, totalBytesSent, respCacheTag});
            } else {
                key.interestOps(SelectionKey.OP_WRITE);
                key.selector().wakeup();
                return;
            }
            key.interestOps(SelectionKey.OP_READ);
            key.selector().wakeup();
            return;
        } catch (IOException ioe) {
            // connection abruptly closed
            // cancel the selection key and close the channel
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            key.cancel();
            try {
                socketChannel.close();
                openSocketsCount--;
            } catch (IOException ioe2) {
                logger.log(Level.WARNING, Utilities.stackTraceToString(ioe2), ioe2);
            }
            return;
        }
    }

    public static Logger getLogger() {
        return logger;
    }
    
    public static CustomThreadPoolExecutor getRequestProcessingThreadPool(){
        return requestProcessingThreadPool;
    }

    public static ArrayBlockingQueue<RequestBean> getRequestQueue() {
        return requestQueue;
    }
    
    public static ArrayBlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }
    
    public static ConcurrentHashMap<Long, ResponseBean> getResponseMap() {
        return responseMap;
    }
    
    public static ConcurrentHashMap<String, LRUResourceCache> getCacheMap(){
        return cacheMap;
    }
   
    public static SimpleDateFormat getDateFormat() {
        return sdf;
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
        if (cacheMap.containsKey(contextName)) {
            lruCache = (LRUResourceCache) cacheMap.get(contextName);
        } else {
            lruCache = new LRUResourceCache(conf.getCacheCapacity());
            cacheMap.put(contextName, lruCache);
        }
        return lruCache;
    }
    
    public static Configuration getConf(){
        return conf;
    }
    
    public static int getOpenSocketCount(){
        return openSocketsCount;
    }
    
    private static Properties props;
    private static ConsoleHandler console;
    private static FileHandler logFile;
    private static FileHandler accessLog;
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;
    private static File logDir;
    private static Configuration conf;
    private static CustomThreadPoolExecutor requestProcessingThreadPool;
    private static ArrayBlockingQueue<Runnable> threadPoolQueue;
    private static ArrayBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<Long, ResponseBean> responseMap;
    private static ArrayBlockingQueue<Long> requestsTimestampQueue;
    private static ConcurrentHashMap<String, LRUResourceCache> cacheMap;
    private static int openSocketsCount;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    private static final Logger logger = Logger.getLogger("in.uglyhunk.amoeba");
    private static final String CONF_FILE = "amoeba.conf";
    private static final long EVENT_LOOP_DELAY = 30; // milli seconds
}
