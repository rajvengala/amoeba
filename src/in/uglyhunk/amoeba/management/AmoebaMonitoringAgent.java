/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.management;

import in.uglyhunk.amoeba.server.Main;
import in.uglyhunk.amoeba.server.Utilities;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 *
 * @author rvengala
 */
public class AmoebaMonitoringAgent {

    public AmoebaMonitoringAgent() {

        // Get the platform MBeanServer
        mbs = ManagementFactory.getPlatformMBeanServer();

        // Unique identification of MBeans
        AmoebaMonitorMBean njasMbean = new AmoebaMonitor();
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
