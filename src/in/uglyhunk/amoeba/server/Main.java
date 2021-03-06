package in.uglyhunk.amoeba.server;

import in.uglyhunk.amoeba.configuration.KernelProps;
import in.uglyhunk.amoeba.configuration.ResourceProps;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import in.uglyhunk.amoeba.dyn.AmoebaClassLoader;
import in.uglyhunk.amoeba.dyn.DynamicRequest;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Filter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
        loadAmoebaConfig();
        readAmoebaConfig();
        setupHandlers();
        setupRunTimeDataStructures();
        readContextConfig();
        setupWorkerThreads();
        startJMXAgent();
        logConfiguration();
        runDaemon();
    }

    /**
     * - Sets the timezone for SimpleDateFormat instance to GMT <br/>
     * - Reads AMOEBA_HOME environment variable <br/>
     * - Loads amoeba.conf properties file in AMOEBA_HOME/conf directory <br/>
     */
    private static void loadAmoebaConfig() {
        // initialize configuration
        conf = KernelProps.getInstance();
    
        // read amoeba configuration
        sdf = KernelProps.getSimpleDateFormat();
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        String amoebaHome = System.getenv("AMOEBA_HOME");
        conf.setAmoebaHome(amoebaHome);
        if (amoebaHome == null) {
            System.out.println("AMOEBA_HOME environemnt variable is not set");
            System.exit(1);
        }

        String amoebaConfigPath = amoebaHome + File.separator + "conf" + File.separator + KernelProps.getConfFile();
        try {
            File amoebaConfigFile = new File(amoebaConfigPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(amoebaConfigFile);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println(e.toString());
            System.exit(1);
        }
    }

    /**
     * Read properties from amoeba.xml file in AMOEBA_HOME/conf directory.
     */
    private static void readAmoebaConfig() {
        // read user-configurable kernel props
        NodeList kernelNodesList = document.getElementsByTagName("kernel");
        Node kernelNode = kernelNodesList.item(0);
        NamedNodeMap kernelNamedNodeMap = kernelNode.getAttributes();
        
        // read hostname or IP address
        conf.setHostname(kernelNamedNodeMap.getNamedItem("hostname").getNodeValue());
        conf.setPort(Integer.parseInt(kernelNamedNodeMap.getNamedItem("port").getNodeValue()));

        // read buffer properties
        conf.setReadBufferCapacity(Integer.parseInt(kernelNamedNodeMap.getNamedItem("readBufferCapacity").getNodeValue()) * 1024);
        
        // request processing threads, core and max set to same as LinkedBlockingQueue when used 
        // in threadPool does not have take max threads into account.
        int requestProcessingThreadCount = Integer.parseInt(kernelNamedNodeMap.getNamedItem("requestProcessingThreads").getNodeValue());
        conf.setMinRequestProcessingThreads(requestProcessingThreadCount);
        conf.setMaxRequestProcessingThreads(requestProcessingThreadCount);
                
        // web root
        conf.setDocumentRoot(kernelNamedNodeMap.getNamedItem("documentRoot").getNodeValue());
        conf.setVirtualHost(Boolean.parseBoolean(kernelNamedNodeMap.getNamedItem("virtualHost").getNodeValue()));

        // compression
        conf.setCompression(Boolean.parseBoolean(kernelNamedNodeMap.getNamedItem("compression").getNodeValue()));
        
        // log file
        conf.setErrLogFileSize(1024 * Integer.parseInt(kernelNamedNodeMap.getNamedItem("errorLogFileSize").getNodeValue()));
        conf.setErrLogFileCount(Integer.parseInt(kernelNamedNodeMap.getNamedItem("totalErrorLogFiles").getNodeValue()));

        // maintenance
        conf.setMaintenance(Boolean.parseBoolean(kernelNamedNodeMap.getNamedItem("maintenance").getNodeValue()));
        
        // cache
        conf.setMaxAge(Long.parseLong(kernelNamedNodeMap.getNamedItem("maxAge").getNodeValue()));
        conf.setInitialCacheSize(Integer.parseInt(kernelNamedNodeMap.getNamedItem("initialCacheSize").getNodeValue()));
        conf.setCacheCapacity(Integer.parseInt(kernelNamedNodeMap.getNamedItem("cacheCapacity").getNodeValue()));
        
        // channel timeout
        conf.setIdleChannelTimeout(Integer.parseInt(kernelNamedNodeMap.getNamedItem("idleChannelTimeout").getNodeValue()));
        
        // read resource properties eg: file extn, mime type and the resource properties like if the
        // file is binary, or can be compressed or cached
        HashMap<String, ResourceProps> resourcePropsMap = new HashMap<>();
        HashMap<String, Boolean> contentTypeBinaryMap = new HashMap<>();
        NodeList resourceNodesList = document.getElementsByTagName("resource");
        for(int i=0; i < resourceNodesList.getLength(); i++){
            Node resourceNode = resourceNodesList.item(i);
            NamedNodeMap resourceNamedNodeMap = resourceNode.getAttributes();
            
            ResourceProps resourceProps = new ResourceProps();
            
            String fileExt = resourceNamedNodeMap.getNamedItem("extension").getNodeValue();
            String resourceMimeType = resourceNamedNodeMap.getNamedItem("mimeType").getNodeValue();
            boolean isResourceBinary = Boolean.parseBoolean(resourceNamedNodeMap.getNamedItem("binary").getNodeValue());
            boolean toCompressResource = Boolean.parseBoolean(resourceNamedNodeMap.getNamedItem("compress").getNodeValue());
            boolean toCacheResource = Boolean.parseBoolean(resourceNamedNodeMap.getNamedItem("cache").getNodeValue());
            
            resourceProps.setContentType(resourceMimeType);
            resourceProps.setIsBinary(isResourceBinary);
            resourceProps.setToCompress(toCompressResource);
            resourceProps.setToCache(toCacheResource);
            resourcePropsMap.put(fileExt, resourceProps);
            
            // This map is used by RequestProcessing thread to find out if the request
            // content is binary
            contentTypeBinaryMap.put(resourceMimeType, isResourceBinary);
        }
        
        // Initialize ResourceProps list
        ResourceProps.setResourcePropsMap(resourcePropsMap);
        ResourceProps.setContentTypeBinaryMap(contentTypeBinaryMap);
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
        logDir = new File(conf.getAmoebaHome() + File.separator + "logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        String message = "Log dir - " + logDir.getPath();
        logger.info(message);

        try {
            logFile = new FileHandler(logDir + File.separator + "amoeba_%g.log", conf.getErrLogFileSize(), conf.getErrLogFileCount());
            accessLog = new FileHandler(logDir + File.separator + "amoeba_access_%g.log", conf.getErrLogFileSize(), conf.getErrLogFileCount());
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
        accessLog.setFilter(new Filter(){
            public boolean isLoggable(LogRecord lr){
                if(lr.getLevel() == Level.FINER)
                    return true;

                return false;
            }
        });
        
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

    /*
     * Read context configuration file
     */
    private static void readContextConfig(){
        // document root eg: /var/www
        File documentRoot = new File(conf.getDocumentRoot());
        if(!documentRoot.exists() || documentRoot.isFile()){
            logger.log(Level.SEVERE, "Document root does not exist");
            System.exit(1);
        }
        
        // list all the files inside the document root 
        File docRootFiles[] = documentRoot.listFiles();
        ArrayList<String> contextConfPaths = new ArrayList<>();
        ArrayList<String> contexts = new ArrayList<>();
        if(!conf.isVirtualHost()){
            // eg: /var/www/default/CLASSES/context.conf
            String context = KernelProps.getDefaultContext();
            String defaultContextConfPath = documentRoot.toString() + File.separator + context + 
                                            File.separator + KernelProps.getDynamicClassTag() + 
                                            File.separator + KernelProps.getContextConfFile();
            contextConfPaths.add(defaultContextConfPath);
            contexts.add(context);
        } else {
            for(File file : docRootFiles){
                if(file.isDirectory() & !file.getName().equalsIgnoreCase(KernelProps.getDefaultContext())){
                    String context = file.getName();
                    // eg: /var/www/xyz/CLASSES/context.conf
                    String contextConfPath = documentRoot.toString() + File.separator + context +
                                             File.separator + KernelProps.getDynamicClassTag() + 
                                             File.separator + KernelProps.getContextConfFile();
                    contextConfPaths.add(contextConfPath);
                    contexts.add(context);
                }
            }
        }
    
        int index = 0;
        for(String contextConfPath : contextConfPaths){
            Properties contextProps = new Properties();
            try {
                // read the props inside the context configuration file
                FileReader reader = new FileReader(contextConfPath);
                contextProps.load(reader);

                // Following Hashmap is the runtime state of context.conf
                HashMap<String, String> dynamicClassesMap = new HashMap<>();
                Iterator<Entry<Object, Object>> itr = contextProps.entrySet().iterator();
                while(itr.hasNext()){
                    Entry<Object, Object> itrEntry = itr.next();
                    String className = (String) itrEntry.getKey();
                    String fullClassName = (String) itrEntry.getValue();
                    dynamicClassesMap.put(className, fullClassName);
                }
                RuntimeData.getContextMap().put(contexts.get(index), dynamicClassesMap);
            } catch (FileNotFoundException fnfe) {
                logger.log(Level.WARNING, "Configuration file, context.conf, is not found for the context {0}", contexts.get(index));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error while parsing configuration file, context.conf, for the context {0}", contexts.get(index));
            }
            index++;
        }
    }
    
    
    /**
     * Sets up the following data structures <br/>
     * <i>requestQueue</i> - Queue of size <i>requestQueueLength</i> to hold the requests from clients <br/>
     * <i>selectionKeyQueue</i> - Queue of size <i>requestsTimestampQueue</i> to hold the timestamps of requests 
     * from the clients in the order their arrival <br/>
     * <i>responseMap</i> - processed requests from the requestQueue will be stored in this map with timestamp
     * of the request as key <br/>
     * 
     */
    private static void setupRunTimeDataStructures() {
        RuntimeData.setRequestQueue(new LinkedBlockingQueue<RequestBean>());
        RuntimeData.setResponseMap(new ConcurrentHashMap<SelectionKey, ResponseBean>());
        RuntimeData.setSelectionKeyQueue(new LinkedBlockingQueue<SelectionKey>());
        RuntimeData.setCacheMap(new ConcurrentHashMap<String, LRUResourceCache>());
        RuntimeData.setClassLoaderMap(new ConcurrentHashMap<String, AmoebaClassLoader>());
        RuntimeData.setSelectionKeyTimestampMap(new HashMap<SelectionKey, Long>(KernelProps.getInitialSelectionKeysSize(),
                                                               KernelProps.getTotalSelectionKeysSize()));
        RuntimeData.setIdleSelectionKeyList(new ArrayList<SelectionKey>());
        RuntimeData.setContextMap(new ConcurrentHashMap<String, HashMap<String, String>>());
        RuntimeData.setContextDynamicInstanceMap(new ConcurrentHashMap<String, HashMap<String, DynamicRequest>>());
        RuntimeData.setSelectionKeyLargeFileMap(new HashMap<SelectionKey, Boolean>());
        RuntimeData.setPartialRequestMap(new ConcurrentHashMap<SelectionKey, PartialRequest>());
        RuntimeData.setExceptionedSelectionKeyList(new CopyOnWriteArrayList<SelectionKey>());
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
        RuntimeData.setThreadPoolQueue(new LinkedBlockingQueue<Runnable>());
        RuntimeData.setRequestProcessingThreadPool(new AmoebaThreadPoolExecutor(
                                        conf.getMinRequestProcessingThreads(),
                                        conf.getMaxRequestProcessingThreads(),
                                        conf.getTtlForNonCoreThreads(),
                                        TimeUnit.SECONDS,
                                        RuntimeData.getThreadPoolQueue()));
    }
    
     /**
     * Log configuration parameters read from amoeba.conf to console/file
     */
    private static void logConfiguration() {
        logger.log(Level.INFO, "Hostname - {0}", conf.getHostname());
        logger.log(Level.INFO, "Port - {0}", conf.getPort());

        logger.log(Level.INFO, "Request buffer capacity - {0} KB", conf.getReadBufferCapacity() / 1024);

        logger.log(Level.INFO, "Request processing threads in the thread pool - {0}", conf.getMinRequestProcessingThreads());
        
        logger.log(Level.INFO, "DocumentRoot - {0}", conf.getDocumentRoot());
        logger.log(Level.INFO, "VirtualHost - {0}", conf.isVirtualHost());
        logger.log(Level.INFO, "Compress resources - {0}", conf.getCompression());

        logger.log(Level.INFO, "Error log file size - {0} KB", conf.getErrLogFileSize() / 1024);
        logger.log(Level.INFO, "Total error log files - {0}", conf.getErrLogFileCount());

        logger.log(Level.INFO, "In maintenance - {0}", conf.isMaintenance());
        logger.log(Level.INFO, "Max age before the resources becoming stale at the client - {0} seconds", conf.getMaxAge());

        logger.log(Level.INFO, "Server cache : Initial size - {0}", conf.getInitialCacheSize());
        logger.log(Level.INFO, "Server cache : Capacity - {0}", conf.getCacheCapacity());
        
        logger.log(Level.INFO, "TCP Channel : Idle timeout - {0} seconds", conf.getIdleChannelTimeout());
    }
    
    private static void startJMXAgent(){
        amoebaJMXAgent = new AmoebaMonitoringAgent();
    }

    /**
     * 
     * @throws Exception 
     */
    private static void runDaemon() {
        try{
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

            // use references to runtime datastructures in this class
            selectionKeyTimestampMap = RuntimeData.getSelectionKeyTimestampMap();
            requestProcessingThreadPool = RuntimeData.getRequestProcessingThreadPool();
            requestQueue = RuntimeData.getRequestQueue();
            responseMap = RuntimeData.getResponseMap();
            selectionKeyQueue = RuntimeData.getSelectionKeyQueue();
            channelTimeout = conf.getIdleChannelTimeout(); 
            idleSelectionKeyList = RuntimeData.getIdleSelectionKeyList();
            selectionKeyLargeFileMap = RuntimeData.getSelectionKeyLargeFileMap();
            partialRequestMap = RuntimeData.getPartialRequestMap();
            exceptionedSelectionKeyList = RuntimeData.getExceptionedSelectionKeyList();
                    
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
                            if(selectionKeyLargeFileMap.containsKey(key)){
                                bulkWriteChannel(key);
                            } else {
                                writeToChannel(key);
                            }
                        }
                    }
                }

                // clean up selection keys which threw exceptions
                // this clean happens only when RequestProcessor object
                // uses selectionkey which is not valid for various reasons
                ArrayList<SelectionKey> temp = new ArrayList<>();
                Iterator<SelectionKey> itrKeys = exceptionedSelectionKeyList.iterator();
                while(itrKeys.hasNext()){
                    temp.add(itrKeys.next());
                }
                
                for(SelectionKey key : temp){
                    // remove this key from exceptionedSelectionKeyList
                    exceptionedSelectionKeyList.remove(key);
                    
                    // add the key to the idle keys list.
                    idleSelectionKeyList.add(key);
                    
                    // remove selection key from the selectionKeyQueue
                    // this will not block other requests waiting in
                    // the queue from being served
                    if(selectionKeyQueue.contains(key)){
                        selectionKeyQueue.poll();
                    }
                }
                
                // close idle channels in idleSelectionKeyList
                for(SelectionKey key : idleSelectionKeyList){
                    SocketChannel socketChannel = (SocketChannel)key.channel();
                    socketChannel.close();
                    key.cancel();
                    activeChannelsCount--;
                }
                idleSelectionKeyList.clear();
                
            }
        } catch( IOException cce){
            logger.log(Level.SEVERE, Utilities.stackTraceToString(cce), cce);
        }
    }
   

    /**
     * accept the client connection and make the channel generate alerts when the data is ready to be read
     * @param key selection key
     */
    private static void acceptConnections(SelectionKey key) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            activeChannelsCount++;
        } catch(IOException ioe){
            logger.log(Level.SEVERE, Utilities.stackTraceToString(ioe), ioe);
        }
    }

    /**
     * read the data from the channel, save it in a buffer and add it to the request processing queue
     * @param key selection key
     */
    private static void readDataFromChannel(SelectionKey key) {
        ByteBuffer readBuffer = ByteBuffer.allocate(conf.getReadBufferCapacity());
        SocketChannel socketChannel = null;
        RequestBean requestBean = null;
        int bytesRead = 0;
        try {
            socketChannel = (SocketChannel) key.channel();
            bytesRead = socketChannel.read(readBuffer);
            
            // channel has reached the end of the stream
            // remote socket shutdown was clean
            if (bytesRead == -1) {
                idleSelectionKeyList.add(key);
                return;
            }
            
            // flip the buffer
            readBuffer.flip();
                        
            // decode the request
            if(!decodeRequest(key, readBuffer, bytesRead)){
                // register with the selector to trigger a read 
                // when the channel is ready to be read
                key.interestOps(SelectionKey.OP_READ);
                key.selector().wakeup();
                return;
            }
            
            // create a request bean
            requestBean = new RequestBean();
            requestBean.setSelectionKey(key);
                        
            // put the selection key of the request in the queue.
            // responses will be sent back in the order as appeared in this queue
            selectionKeyQueue.put(key);

            // put the request in request queue
            requestQueue.put(requestBean);

            // pass on the request data read from the channel to
            // a request processing thread pool
            RequestProcessor requestProcessor = new RequestProcessor();
            requestProcessingThreadPool.execute(requestProcessor);
            
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, Utilities.stackTraceToString(e), e);
        
            // cancel the selection key and close the channel
            idleSelectionKeyList.add(key);
            
            // remove selection key from the selectionKeyQueue
            // this will not block other requests waiting in
            // the queue from being served
            if(selectionKeyQueue.contains(key)){
                selectionKeyQueue.poll();
            }
            
            // remove requestBean from the requestQueue, if it exists
            // so that other request will be processed
            if(requestQueue.contains(requestBean)){
                requestQueue.poll();
            }
            
            // remove the key from selectionKeyTimestampMap, if it exists
            if(selectionKeyTimestampMap.containsKey(key))
                selectionKeyTimestampMap.remove(key);
        }
    }

    
    public static void writeToChannel(SelectionKey key) {
        SocketChannel socketChannel = null;
        
        try {
            if(key == selectionKeyQueue.peek() && responseMap.containsKey(key)){
                ResponseBean responseBean = responseMap.get(key);
        
                // remove the request timestamp from requestsTimestampQueue
                // remove the response bean object from responseMap
                responseMap.remove(selectionKeyQueue.poll());
        
                socketChannel = responseBean.getSocketChannel();
                String statusLine = responseBean.getStatusLine();
                String contentType = responseBean.getContentType();
                String contentLength = responseBean.getContentLength();
                String server = responseBean.getServer();
                String statusCode = responseBean.getStatusCode();
                String resource = responseBean.getAbsoluteResource();
                long lastModified = responseBean.getLastModified();
                String respCacheTag = responseBean.getResponseCacheTag();
                if (respCacheTag == null) {
                    respCacheTag = "";
                }

                String eTag = responseBean.getETag();
                ByteBuffer respBodyBuffer = responseBean.getBody();
                if (respBodyBuffer != null) {
                    respBodyBuffer.flip();
                }

                StringBuilder respHeaders = new StringBuilder();
                
                // Status line
                respHeaders.append(statusLine).append(Utilities.getHttpEOL());
                
                // Content-Type header
                respHeaders.append("Content-Type: ").append(contentType).append(Utilities.getHttpEOL());

                // Content-Length header
                if (contentLength != null) {
                    respHeaders.append("Content-Length: ").append(contentLength).append(Utilities.getHttpEOL());
                }
                
                // Contet-Encoding header
                if(conf.getCompression()) {
                    String contentEncoding = responseBean.getContentEncoding();
                    if (contentEncoding != null) {
                        respHeaders.append("Content-Encoding: ").append(responseBean.getContentEncoding()).append(Utilities.getHttpEOL());
                    }
                }

                // Last-Modified header
                if (lastModified != 0) {
                    String lastModifiedValue = sdf.format(new Date(lastModified));
                    respHeaders.append("Last-Modified: ").append(lastModifiedValue).append(Utilities.getHttpEOL());
                }

                // etag header
                if (eTag != null) {
                    respHeaders.append("ETag: \"").append(eTag).append("\"").append(Utilities.getHttpEOL());
                    respHeaders.append("Cache-Control: max-age=").append(conf.getMaxAge()).append(Utilities.getHttpEOL());
                }

                // Server Header
                respHeaders.append("Server: ").append(server).append(Utilities.getHttpEOL());
                
                // Date header
                respHeaders.append("Date: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(Utilities.getHttpEOL());
                                
                // Connection header
                if(selectionKeyTimestampMap.containsKey(key)){
                    long idleTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - selectionKeyTimestampMap.get(key));
                    if(idleTime >= channelTimeout){
                        respHeaders.append("Connection: close").append(Utilities.getHttpEOL());
                        idleSelectionKeyList.add(key);
                        selectionKeyTimestampMap.remove(key);
                    } else {
                        respHeaders.append("Connection: Keep-Alive").append(Utilities.getHttpEOL());
                        selectionKeyTimestampMap.put(key, System.nanoTime());
                    }
                } else {
                    respHeaders.append("Connection: Keep-Alive").append(Utilities.getHttpEOL());
                    selectionKeyTimestampMap.put(key, System.nanoTime());
                }
                    
                // new line - end of headers
                respHeaders.append(Utilities.getHttpEOL());

                byte headerBytes[] = respHeaders.toString().getBytes(KernelProps.getCharsetName());
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
                int totalBytesSent = socketChannel.write(respByteBuffer);

                String clientAddr = socketChannel.socket().getRemoteSocketAddress().toString().split("/")[1];
                logger.log(Level.FINER, 
                            "{0} - {1} - {2} - {3} bytes {4} ", 
                            new Object[]{clientAddr, 
                            resource,
                            statusCode, 
                            totalBytesSent, 
                            respCacheTag});
            } else {
                key.interestOps(SelectionKey.OP_WRITE);
                key.selector().wakeup();
                return;
            }
                       
            // set the selection key ready for more reads
            // from the client
            key.interestOps(SelectionKey.OP_READ);
            key.selector().wakeup();
            return;
        } catch (IOException ioe) {
            // connection abruptly closed
           logger.log(Level.SEVERE, Utilities.stackTraceToString(ioe), ioe);
        
            // cancel the selection key and close the channel
            idleSelectionKeyList.add(key);
            
            // remove selection key from the selectionKeyQueue
            // this will not block other requests waiting in
            // the queue from being served
            if(selectionKeyQueue.contains(key)){
                selectionKeyQueue.poll();
            }
            
            // remove the key from selectionKeyTimestampMap, if it exists
            if(selectionKeyTimestampMap.containsKey(key))
                selectionKeyTimestampMap.remove(key);
        }
    }

    /*
     * 
     */
    public static void bulkWriteChannel(SelectionKey key){
        boolean eof = false;
        try{
            SocketChannel socketChannel = (SocketChannel) key.channel();
            ResponseBean responseBean = responseMap.get(key);    
            String clientAddr = socketChannel.socket().getRemoteSocketAddress().toString().split("/")[1];
            String resource = responseBean.getAbsoluteResource();
            String respCacheTag = responseBean.getResponseCacheTag();
            String statusCode = responseBean.getStatusCode();
            int totalBytesSent = 0;
            
            if(!selectionKeyLargeFileMap.get(key).booleanValue()){
                selectionKeyQueue.poll();
                selectionKeyTimestampMap.remove(key);
                selectionKeyLargeFileMap.put(key, Boolean.TRUE);
            
                String statusLine = responseBean.getStatusLine();
                String contentType = responseBean.getContentType();
                String contentLength = responseBean.getContentLength();
                String server = responseBean.getServer();
                String contentRange = responseBean.getContentRange();

                StringBuilder responseHeaders = new StringBuilder();

                // Status line
                responseHeaders.append(statusLine).append(Utilities.getHttpEOL());

                // Content-Type header
                responseHeaders.append("Content-Type: ").append(contentType).append(Utilities.getHttpEOL());

                // Content-Length header
                if (contentLength != null) {
                    responseHeaders.append("Content-Length: ").append(contentLength).append(Utilities.getHttpEOL());
                }

                // Accept-Ranges headers
                responseHeaders.append("Accept-Ranges: ").append("bytes").append(Utilities.getHttpEOL());

                 // format -> Content-Range: bytes 500-1000/1200
                responseHeaders.append("Content-Range: ").append(contentRange).append(Utilities.getHttpEOL());

                // Server Header
                responseHeaders.append("Server: ").append(server).append(Utilities.getHttpEOL());

                responseHeaders.append("Connection: Close").append(Utilities.getHttpEOL());
                // Date header
                //respHeaders.append("Date: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(Utilities.getHTTPEOL());

                // new line - end of headers
                responseHeaders.append(Utilities.getHttpEOL());

                byte headerBytes[] = responseHeaders.toString().getBytes(KernelProps.getCharsetName());
                ByteBuffer responseHeadersBuffer = ByteBuffer.allocate(headerBytes.length);
                responseHeadersBuffer.put(headerBytes);
                responseHeadersBuffer.flip();

                totalBytesSent = socketChannel.write(responseHeadersBuffer);
                
            } else {
                byte[] partialContent = new byte[KernelProps.getPartialResponseSize()];
                MappedByteBuffer mappedByteBuffer = responseBean.getMappedByteBuffer();
                if(mappedByteBuffer.hasRemaining()){
                    int bodySize = Math.min(mappedByteBuffer.remaining(), partialContent.length);
                    mappedByteBuffer.get(partialContent, 0, bodySize);
                    ByteBuffer responseBuffer = ByteBuffer.allocate(partialContent.length);
                    responseBuffer.put(partialContent);
                    responseBuffer.flip();
                    totalBytesSent = socketChannel.write(responseBuffer);
                    // save the current state of the mappedByteBuffer in responseBean
                    responseBean.setMappedByteBuffer(mappedByteBuffer);
                } else {
                    eof = true;
                }
            }
            
            // add the SelectionKey reference to idleKeyList
            // this will be processed in Main as part
            // of even processing loop
            if(!eof){
                KernelProps.getLogger().log(Level.FINER, 
                                            "{0} - {1} - {2} - {3} bytes {4} ", 
                                            new Object[]{clientAddr, 
                                            resource,
                                            statusCode, 
                                            totalBytesSent, 
                                            respCacheTag});
                key.interestOps(SelectionKey.OP_WRITE);
                key.selector().wakeup();
            
            } else {
                idleSelectionKeyList.add(key);
                selectionKeyLargeFileMap.remove(key);
            }
        } catch(IOException ioe){
            KernelProps.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
        
            // cancel the selection key and close the channel
            idleSelectionKeyList.add(key);
            
            // remove selection key from the selectionKeyQueue
            // this will not block other requests waiting in
            // the queue from being served
            if(selectionKeyQueue.contains(key)){
                selectionKeyQueue.poll();
            }
            
            // remove the key from selectionKeyTimestampMap, if it exists
            if(selectionKeyTimestampMap.containsKey(key))
                selectionKeyTimestampMap.remove(key);
        }
    }

    private static boolean decodeRequest(SelectionKey key, ByteBuffer readBuffer, int bytesRead) throws IOException {
        // convert request in bytebuffer to string format
        String rawRequest = KernelProps.getCharset().decode(readBuffer).toString();
        
        // trim the readBuffer into byte array by removing unfilled buffer
        byte[] readBufferBytes = new byte[bytesRead];
        readBuffer.flip();
        readBuffer.get(readBufferBytes);
        
        PartialRequest partialRequest = null;
        int totalBodyLength = 0;
        int bodyLength = 0;

        if(partialRequestMap.containsKey(key)){
            // part of this request has been read previously
            partialRequest = partialRequestMap.get(key);
            totalBodyLength = partialRequest.getTotalBodyLength();
            bodyLength = partialRequest.getPartialBodyLength();
        } else {
            // this is a new request
            partialRequest = new PartialRequest();
            partialRequestMap.put(key, partialRequest);
        }
        partialRequest.setRequestBytes(readBufferBytes);
        
        // check if the new request has Content-Length header
        // if exists, it is not a GET request
        if(totalBodyLength > 0 || rawRequest.contains("Content-Length")){
            // post/multipart-post request
            if(totalBodyLength == 0){
                totalBodyLength = Integer.parseInt(rawRequest
                                                   .split("Content-Length")[1]
                                                   .split(":")[1]
                                                   .split(Utilities.getHttpEOL())[0].trim());
                partialRequest.setTotalBodyLength(totalBodyLength);
            }
            
            // body has never been read
            if(bodyLength == 0){
                // check if end-of-headers marker is present
                if(!partialRequest.isBody() && rawRequest.contains(Utilities.getHttpEOL() + Utilities.getHttpEOL())){
                    partialRequest.setIsBody(true);
                    
                    // all headers have been received for the post request
                    // get the size of the post body present in this read
                    int endOfHeadersIndex = rawRequest.indexOf(Utilities.getHttpEOL() + Utilities.getHttpEOL());
                    bodyLength = readBuffer.limit() - endOfHeadersIndex - (Utilities.getHttpEOL().length() * 2);
                    
                    // save it in a map
                    if(bodyLength < totalBodyLength){
                        partialRequest.setPartialBodyLength(bodyLength);
                        return false;
                    } else {
                        // full post request headers and body are received
                       return true;
                    }
                }
            } 
            
            // readBuffer contains data, combine it with the existing buffer
            partialRequest.setPartialBodyLength(bytesRead);
            if(partialRequest.getPartialBodyLength() == totalBodyLength){
                return true;
            } else {
                return false;
            }
        } else {
            // get request
            if(rawRequest.contains(Utilities.getHttpEOL() + Utilities.getHttpEOL())){
                // full request has been received. decode success, return true
                return true;
            } else {
                // partial request received
                return false;
            } 
        }
    }
    
    public static int getActiveChannelsCount(){
        return activeChannelsCount;
    }
   
    private static Document document;
    private static ConsoleHandler console;
    private static FileHandler logFile;
    private static FileHandler accessLog;
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;
    private static File logDir;
    private static SimpleDateFormat sdf;
    private static final Logger logger = KernelProps.getLogger();;
    private static KernelProps conf;
    private static int channelTimeout;
    private static AmoebaMonitoringAgent amoebaJMXAgent;
    
    // As of now only single thread modified this class variable
    private static volatile int activeChannelsCount;
    
    // Runtime Data structure references
    private static HashMap<SelectionKey, Long> selectionKeyTimestampMap;
    private static AmoebaThreadPoolExecutor requestProcessingThreadPool;
    private static LinkedBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<SelectionKey, ResponseBean> responseMap;
    private static LinkedBlockingQueue<SelectionKey> selectionKeyQueue;
    private static ArrayList<SelectionKey> idleSelectionKeyList;
    
    private static HashMap<SelectionKey, Boolean> selectionKeyLargeFileMap;
    private static ConcurrentHashMap<SelectionKey, PartialRequest> partialRequestMap;
    private static CopyOnWriteArrayList<SelectionKey> exceptionedSelectionKeyList;
}
