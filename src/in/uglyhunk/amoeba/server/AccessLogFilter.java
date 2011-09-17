/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.amoeba.server;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author uglyhunk
 */
public class AccessLogFilter implements Filter{
    public boolean isLoggable(LogRecord lr){
        if(lr.getLevel() == Level.FINER)
            return true;

        return false;
    }
}
