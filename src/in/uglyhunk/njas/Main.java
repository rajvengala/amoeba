/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author uglyhunk
 */
public class Main {

    public static void main(String[] args) {
        try {
            loadConfiguration();
            readConfiguration();
            setupHandlers();
            logConfiguration();
            setupQueues();
            setupWorkerThreads();
            runDaemon();
        } catch(Exception e) {
            logger.log(Level.SEVERE, Utilities.stackTraceToString(e), e);
            System.exit(1);
        }
    }
    
    private static void loadConfiguration(){
        njasHome = System.getenv("NJAS_HOME");
        if(njasHome == null){
            System.out.println("NJAS_HOME environemnt variable is not set");
            System.exit(1);
        }
        
        props = new Properties();
        String propFilePath = njasHome + File.separator + "conf" + File.separator + "njas.conf";
        try {
            FileReader reader = new FileReader(propFilePath);
            props.load(reader);
        } catch(FileNotFoundException fnfe) {
            System.out.println("Configuration file not found");
            System.out.println(fnfe.toString());
            System.exit(1);
        } catch(IOException ioe) {
            System.out.println("Exception while loading configuration file");
            System.out.println(ioe.toString());
            System.exit(1);
        }
    }
    
     private static void readConfiguration() {
        // server
        hostname = props.getProperty("hostname");
        port = Integer.parseInt(props.getProperty("port"));
        
        // read buffer properties
        readBufferCapacity = Integer.parseInt(props.getProperty("readBufferCapacity")) * 1024;
        
        // request processing threads
        coreRequestProcessingThreads = Integer.parseInt(props.getProperty("coreRequestProcessingThreads"));
        maxRequestProcessingThreads = Integer.parseInt(props.getProperty("maxRequestProcessingThreads"));
        ttlForNonCoreThreads = Integer.parseInt(props.getProperty("ttlForNonCoreThreads"));
        
        // tasks queue
        tasksQueueLength = Integer.parseInt(props.getProperty("tasksQueueLength"));
        
        // request queue
        requestQueueLength = Integer.parseInt(props.getProperty("requestQueueLength"));
        responseOrderQueueLength = requestQueueLength;
        
        // web root
        documentRoot = props.getProperty("documentRoot");
        virtualHost = Boolean.parseBoolean(props.getProperty("virtualHost"));
        
        // compression
        compression = Boolean.parseBoolean(props.getProperty("compression"));
        
        // log file
        ERRLOGFILSIZE = 1024 * Integer.parseInt(props.getProperty("errorLogFileSize"));
        ERRLOGFILECOUNT = Integer.parseInt(props.getProperty("totalErrorLogFiles"));
    }

    private static void setupHandlers() {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        console = new ConsoleHandler();
        // set to info in production
        console.setLevel(Level.INFO); 
        console.setFormatter(new Formatter(){
            @Override
            public String format(LogRecord logRecord){
                return logRecord.getLevel() + " : " + formatMessage(logRecord) + Utilities.getEOL() ;
            }
        });
        logger.addHandler(console);
       
        // create logs directory
        logDir = new File(njasHome + File.separator + "logs");
        if(!logDir.exists()){
            logDir.mkdir();
        }
        String message = "Log dir - " + logDir.getPath();
        logger.info(message);

        try {
            logFile = new FileHandler(logDir + File.separator + "njas_%g.log", ERRLOGFILSIZE, ERRLOGFILECOUNT);
            accessLog = new FileHandler(logDir + File.separator + "njas_access_%g.log", ERRLOGFILSIZE, ERRLOGFILECOUNT);
        } catch(IOException ioe) {
            logger.log(Level.SEVERE, ioe.toString(), ioe);
            System.exit(1);
        }

        logFile.setLevel(Level.FINE); 
        logFile.setFormatter(new Formatter(){
            @Override
            public String format(LogRecord logRecord){
                String timestamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.S").format(new Date(logRecord.getMillis()));
                return logRecord.getLevel() + " - " + timestamp + " - " + formatMessage(logRecord) + Utilities.getEOL();
            }
        });

        accessLog.setLevel(Level.FINER);
        accessLog.setFilter(new AccessLogFilter());
        accessLog.setFormatter(new Formatter(){
            @Override
            public String format(LogRecord logRecord){
                String timestamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.S").format(new Date(logRecord.getMillis()));
                return timestamp + " - " + formatMessage(logRecord) + Utilities.getEOL();
            }
        });

        logger.addHandler(logFile);
        logger.addHandler(accessLog);
    }
    
    private static void logConfiguration(){
        logger.log(Level.INFO, "Hostname - {0}", hostname);
        logger.log(Level.INFO, "Port - {0}", port);
        
        logger.log(Level.INFO, "Request buffer capacity - {0} KB", readBufferCapacity/1024);
        
        logger.log(Level.INFO, "Core request processing threads - {0}", coreRequestProcessingThreads);
        logger.log(Level.INFO, "Max. request processing threads - {0}", maxRequestProcessingThreads);
        logger.log(Level.INFO, "Time to live for non-core request processing threads - {0} seconds", maxRequestProcessingThreads);

        logger.log(Level.INFO, "Queue length of the requests - {0}", requestQueueLength);
        logger.log(Level.INFO, "Queue length of the tasks - {0}", tasksQueueLength);
        
        logger.log(Level.INFO, "DocumentRoot - {0}", documentRoot);
        logger.log(Level.INFO, "VirtualHost - {0}", virtualHost);
        logger.log(Level.INFO, "Compression - {0}", compression);
        
        logger.log(Level.INFO, "Error log file size - {0} KB", ERRLOGFILSIZE/1024);
        logger.log(Level.INFO, "Total error log files - {0}", ERRLOGFILECOUNT);
    }
   
    private static void setupQueues(){
        requestQueue = new ArrayBlockingQueue<RequestBean>(requestQueueLength, true);
        responseMap = new ConcurrentHashMap<Long, ResponseBean>();
        responseOrderQueue = new ArrayBlockingQueue<Long>(responseOrderQueueLength, true);
    }
    
    private static void setupWorkerThreads() {
        // each http request is prepared as task
        // and submitted to a pool of threads
        tasksQueue = new ArrayBlockingQueue<Runnable>(tasksQueueLength);

        requestProcessingThreadPool = new CustomThreadPoolExecutor(
                                            coreRequestProcessingThreads,
                                            maxRequestProcessingThreads,
                                            ttlForNonCoreThreads,
                                            TimeUnit.SECONDS,
                                            tasksQueue);
    }

    private static void runDaemon() throws Exception{
        // create a selector
        selector = SelectorProvider.provider().openSelector();
        logger.info("Selector created");

        // create non-blocking server socket channel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        // bind server socket channel to host:port
        InetSocketAddress isa = new InetSocketAddress(hostname, port);
        serverSocketChannel.socket().bind(isa);
        logger.log(Level.INFO, "Server listening at {0}", isa.toString());

        // register server socket channel to the previously
        // created selector and indicate the interest
        // in accepting new connections
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.fine("Registered channel with selector to alert for clien accept connection request arrives ");
        
        while(true) {
            // wait for an event on one of the registered channels
            selector.select();

            // iterate over the keys and respond to the events
            Iterator selectedKeys = selector.selectedKeys().iterator();
            while(selectedKeys.hasNext()) {
                SelectionKey key = (SelectionKey)selectedKeys.next();
                selectedKeys.remove();

                if(key.isValid()){
                    if(key.isAcceptable()) {
                        acceptConnections(key);
                    } else if(key.isReadable()) {
                        readDataFromChannel(key);
                    } else if(key.isWritable()){
                        writeToChannel(key);
                    }
                }
            }
        }
    }

    // accept the client connection and make the channel
    // generate alerts when the data is ready to be read
    private static void acceptConnections(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel socketChannel = serverChannel.accept();
            //socketChannel.socket().setPerformancePreferences(0, 1, 2);
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            
        } catch (IOException ioe){
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
        }
        
    }

    // read the data from the channel, save it in a buffer
    // and add it to the request processing queue
    private static void readDataFromChannel(SelectionKey key) {
        ByteBuffer readBuffer = ByteBuffer.allocate(readBufferCapacity);
               
        SocketChannel socketChannel = null;
        int bytesRead;
        try {
            socketChannel = (SocketChannel) key.channel();
            bytesRead = socketChannel.read(readBuffer);
            
            if(bytesRead == -1) {
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
            
            // put the request timestamp in the queue
            // responses will be sent in the order of requests
            // as appeared in this queue
            responseOrderQueue.put(timestamp);
            
            //System.out.println(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.S").format(new Date(System.currentTimeMillis())) + " - " + new String(readBuffer.array()).split("\r\n")[0]);
            
            // put the request in request queue
            requestQueue.put(reqBean);
            
            // pass on the data read from the channel to
            // a request processing thread pool
            RequestProcessor requestProcessor = new RequestProcessor();
            requestProcessingThreadPool.execute(requestProcessor);
            Thread.sleep(20);
        } catch(IOException ioe) {
            // connection abruptly closed
            // cancel the selection key and close the channel
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            key.cancel();
            try {
                socketChannel.close();
            } catch (IOException ioe2) {
                logger.log(Level.WARNING, Utilities.stackTraceToString(ioe2), ioe2);
            }
            return;
        } catch(InterruptedException ie){
            logger.log(Level.WARNING, Utilities.stackTraceToString(ie), ie);
        } 
    }

    public static void writeToChannel(SelectionKey key){
        SocketChannel socketChannel = null;
        try {
            Long timestamp = responseOrderQueue.peek();
            if(timestamp != null && responseMap.containsKey(timestamp)){
                //System.out.println(timestamp);
                ResponseBean respBean = responseMap.get(timestamp);

                // remove the request timestamp from responseOrderQueue
                // remove the response bean object from responseMap
                responseMap.remove(responseOrderQueue.poll());

                String statusLine = respBean.getStatusLine();
                String contentType = respBean.getContentType();
                String contentLength = respBean.getContentLength();
                String server = respBean.getServer();
                String statusCode = respBean.getStatusCode();
                String resource = respBean.getAbsoluteResource();
                ByteBuffer respBodyBuffer = respBean.getBody();
                respBodyBuffer.flip();

                StringBuilder respHeaders = new StringBuilder();
                respHeaders.append(statusLine).append(Utilities.getHTTPEOL())
                            .append("Content-Type: ").append(contentType).append(Utilities.getHTTPEOL())
                            .append("Content-Length: ").append(contentLength).append(Utilities.getHTTPEOL())
                            .append("Server: ").append(server).append(Utilities.getHTTPEOL());
                
                if(compression){
                    respHeaders.append("Content-Encoding: ").append(respBean.getContentEncoding()).append(Utilities.getHTTPEOL());
                }
                
                respHeaders.append(Utilities.getHTTPEOL());

                byte headerBytes[] = respHeaders.toString().getBytes("UTF-8");
                ByteBuffer respHeadersBuffer = ByteBuffer.allocate(headerBytes.length);
                respHeadersBuffer.put(headerBytes);
                respHeadersBuffer.flip();

                int respSize = respBodyBuffer.capacity() + respHeadersBuffer.capacity();
                ByteBuffer respByteBuffer = ByteBuffer.allocate(respSize);
                respByteBuffer.put(respHeadersBuffer.array());
                respByteBuffer.put(respBodyBuffer.array());
                respByteBuffer.flip();

                socketChannel = (SocketChannel) key.channel();
                int totalBytesSent = socketChannel.write(respByteBuffer);

                String clientAddr = socketChannel.socket().getRemoteSocketAddress().toString().split("/")[1];
                Main.getLogger().log(Level.FINER, "{0} => {1} <= {2}, {3} ({4}) bytes", new Object[]{clientAddr, resource, statusCode, totalBytesSent, contentLength});
            } else {
                key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
            return;
            }
            key.interestOps(SelectionKey.OP_READ);
            key.selector().wakeup();
            return;
        } catch(IOException ioe) {
            // connection abruptly closed
            // cancel the selection key and close the channel
            logger.log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            key.cancel();
            try {
                socketChannel.close();
            } catch (IOException ioe2) {
                logger.log(Level.WARNING, Utilities.stackTraceToString(ioe2), ioe2);
            }
            return;
        } 
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getDocumentRoot() {
        return documentRoot;
    }

    public static boolean isVirtualHost(){
        return virtualHost;
    }
    
    public static ArrayBlockingQueue<RequestBean> getRequestQueue(){
        return requestQueue;
    }
    
    public static ConcurrentHashMap<Long, ResponseBean> getResponseMap(){
        return responseMap;
    }
    
    public static boolean toCompress(){
        return compression;
    }
    
    public static String getClassesFolderName(){
        return CLASSES;
    }
    
    public static String getDefaultWebAppFolder(){
        return DEFAULT_WEBAPP_FOLDER;
    }
    
    public static String getErrorPageFolderPath(){
        return njasHome + File.separator + ERROR_PAGE_FOLDER;
    }
    
    public static String getServerHeader(){
        return SERVER_HEADER;
    }

    private static Properties props;
    private static final Logger logger = Logger.getLogger("in.uglyhunk.njws");
    private static ConsoleHandler console;
    private static FileHandler logFile;
    private static FileHandler accessLog;
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;
    private static File logDir;

    private static String njasHome; // environment variable

    private static int readBufferCapacity; // in bytes
    private static String hostname;
    private static int port;

    private static ExecutorService requestProcessingThreadPool;
    private static ArrayBlockingQueue<Runnable> tasksQueue;
    
    private static ArrayBlockingQueue<RequestBean> requestQueue;
    private static ConcurrentHashMap<Long, ResponseBean> responseMap;
    private static ArrayBlockingQueue<Long> responseOrderQueue;
    
    private static int tasksQueueLength;
    
    private static int coreRequestProcessingThreads;
    private static int maxRequestProcessingThreads;
    
    private static int requestQueueLength;
    private static int responseOrderQueueLength;
   
    private static long ttlForNonCoreThreads;
 
    private static int ERRLOGFILSIZE; // KB
    private static int ERRLOGFILECOUNT;// max log file count

    private static String documentRoot;
    private static boolean virtualHost;
    private static boolean compression;
    
    private static final String CLASSES = "bin";
    private static final String DEFAULT_WEBAPP_FOLDER = "default";
    private static final String ERROR_PAGE_FOLDER = "error";
    private static final String SERVER_HEADER = "Nano Java App Server 0.1";
}
