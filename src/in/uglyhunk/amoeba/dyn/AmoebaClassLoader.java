/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.dyn;

import in.uglyhunk.amoeba.configuration.KernelProps;
import in.uglyhunk.amoeba.server.Utilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

/**
 *
 * @author rvengala
 */
public class AmoebaClassLoader extends ClassLoader {
    
    public AmoebaClassLoader(String absoluteContextPath){
        this.absoluteContextPath = absoluteContextPath;
    }
    
    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        byte[] classBytes = null;
        
        try{
            classBytes = loadClassBytes(className);
        } catch(IOException ioe){
            KernelProps.getLogger().log(Level.SEVERE, Utilities.stackTraceToString(ioe), ioe);
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
            String classNamePath = className.replace('.', '/');
            File f = new File(absoluteContextPath + File.separator + KernelProps.getDynamicClassTag() + File.separator + classNamePath + ".class");
            fis = new FileInputStream(f);
            int classSize  = (int)f.length();
            fc = fis.getChannel();
            classByteBuffer = ByteBuffer.allocate(classSize);
            fc.read(classByteBuffer);
            classByteBuffer.flip();
      
        }finally {
            if(fis != null) {
                fis.close();
                fc.close();
            }
        } 
        return classByteBuffer.array();
    }

    private String absoluteContextPath;
}
