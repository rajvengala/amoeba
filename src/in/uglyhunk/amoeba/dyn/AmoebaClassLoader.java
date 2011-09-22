/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.dyn;

import in.uglyhunk.amoeba.server.Configuration;
import in.uglyhunk.amoeba.server.Utilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 *
 * @author rvengala
 */
public class AmoebaClassLoader extends ClassLoader {
    
    public AmoebaClassLoader(String contextPath){
        this.contextPath = contextPath;
        loadedDynamicClasses = new CopyOnWriteArrayList<String>();
    }
    
    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException{
        byte[] classBytes = null;
        try{
            classBytes = loadClassBytes(className);
            loadedDynamicClasses.add(className);
        } catch(IOException ioe){
            Configuration.getLogger().log(Level.WARNING, Utilities.stackTraceToString(ioe), ioe);
            throw new ClassNotFoundException(className);
        }
        
        Class<?> cl = defineClass(className, classBytes, 0, classBytes.length);
        if(cl == null)
            throw new ClassNotFoundException(className);
        
        return cl;
    }
    
    private byte[] loadClassBytes(String className) throws IOException{
        FileInputStream fis = null;
        FileChannel fc = null;
        ByteBuffer classByteBuffer = null;
        try {
            File f = new File(contextPath + File.separator + className + ".class");
            fis = new FileInputStream(f);
            int classSize  = (int)f.length();
            fc = fis.getChannel();
            classByteBuffer = ByteBuffer.allocate(classSize);
            fc.read(classByteBuffer);
            classByteBuffer.flip();
        } finally {
            fis.close();
            fc.close();
        }
        return classByteBuffer.array();
    }
    
    public boolean isClassLoaded(String className){
        if(loadedDynamicClasses.contains(className)){
            return true;
        } 
        return false;
    }
    
    private CopyOnWriteArrayList<String> loadedDynamicClasses;
    private String contextPath;
}
