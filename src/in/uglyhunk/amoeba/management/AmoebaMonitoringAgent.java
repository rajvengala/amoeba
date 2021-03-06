/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.management;

import in.uglyhunk.amoeba.configuration.KernelProps;
import in.uglyhunk.amoeba.server.Utilities;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
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
        ObjectName amoebaObjectName = null;

        try {
             // Uniquely identify the MBeans and register them with the platform MBeanServer 
             amoebaObjectName = new ObjectName("in.uglyhunk.amoeba.management:name=amoeba");
             mbs.registerMBean(njasMbean, amoebaObjectName);
        } catch(MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            KernelProps.getLogger().log(Level.WARNING, Utilities.stackTraceToString(e), e);
        }
    }
 
    private MBeanServer mbs = null;
}
