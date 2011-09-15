/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.njas.management;

import in.uglyhunk.njas.Main;
import in.uglyhunk.njas.Utilities;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 *
 * @author rvengala
 */
public class NjasMonitoringAgent {

    public NjasMonitoringAgent() {

        // Get the platform MBeanServer
        mbs = ManagementFactory.getPlatformMBeanServer();

        // Unique identification of MBeans
        NjasMonitorMBean njasMbean = new NjasMonitor();
        ObjectName njasObjectName = null;

        try {
             // Uniquely identify the MBeans and register them with the platform MBeanServer 
             njasObjectName = new ObjectName("in.uglyhunk.njas.management:name=njas");
             mbs.registerMBean(njasMbean, njasObjectName);
        } catch(Exception e) {
            Main.getLogger().log(Level.WARNING, Utilities.stackTraceToString(e), e);
        }
    }
 
    private MBeanServer mbs = null;
}
