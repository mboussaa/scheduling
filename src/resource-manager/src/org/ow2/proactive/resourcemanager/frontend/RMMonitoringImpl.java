/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.ow2.proactive.resourcemanager.frontend;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;
import org.ow2.proactive.resourcemanager.common.RMConstants;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.core.RMCoreInterface;


/**
 * Active object designed for the Monitoring of the Resource Manager.
 * This class provides a way for a monitor to ask at
 * Resource Manager to throw events
 * generated by nodes and nodes sources management. RMMonitoring dispatch
 * events thrown by {@link RMCore} to all its monitors.
 *
 *        //TODO methods to add and remove because RM GUI is in development,
 *        so Java Doc not yet up to date for this component
 *
 * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener
 *
 * @author The ProActive Team
 * @since ProActive 3.9
 */
public class RMMonitoringImpl implements RMMonitoring, RMEventListener, InitActive {
    private static final Logger logger = ProActiveLogger.getLogger(Loggers.RM_MONITORING);

    // Attributes
    private RMCoreInterface rmcore;
    private HashMap<UniqueID, RMEventListener> RMListeners;
    private String MonitoringUrl = null;

    // ----------------------------------------------------------------------//
    // CONSTRUTORS

    /** ProActive empty constructor */
    public RMMonitoringImpl() {
    }

    /**
     * Creates the RMMonitoring active object.
     * @param rmcore Stub of the RMCore active object.
     */
    public RMMonitoringImpl(RMCoreInterface rmcore) {
        RMListeners = new HashMap<UniqueID, RMEventListener>();
        this.rmcore = rmcore;
    }

    /**
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    public void initActivity(Body body) {
        try {
            MonitoringUrl = "//" + PAActiveObject.getNode().getVMInformation().getHostName() + "/" +
                RMConstants.NAME_ACTIVE_OBJECT_RMMONITORING;
            PAActiveObject.register(PAActiveObject.getStubOnThis(), this.MonitoringUrl);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NodeException e) {
            e.printStackTrace();
        }
    }

    /** Register a new Resource manager listener.
     * Way to a monitor object to ask at RMMonitoring to throw
     * RM events to it.
     * @param listener a listener object which implements {@link RMEventListener}
     * interface.
     * @param events list of wanted events that must be received.
     * @return RMInitialState snapshot of RM's current state : nodes and node sources.
     *  */
    public RMInitialState addRMEventListener(RMEventListener listener, RMEventType... events) {
        UniqueID id = PAActiveObject.getContext().getCurrentRequest().getSourceBodyID();

        this.RMListeners.put(id, listener);
        return rmcore.getRMInitialState();
    }

    /**
     * Dispatch events thrown by the RMCore to all known monitors of the RM.
     * @param methodName method name corresponding to the event.
     * @param types Object types associated with the method call.
     * @param params Object associated with the method call.
     */
    private void dispatch(RMEventType methodName, Class<?>[] types, Object... params) {
        try {
            Method method = RMEventListener.class.getMethod(methodName.toString(), types);

            Iterator<UniqueID> iter = this.RMListeners.keySet().iterator();
            while (iter.hasNext()) {
                UniqueID id = iter.next();
                try {
                    method.invoke(RMListeners.get(id), params);
                } catch (Exception e) {
                    iter.remove();
                    logger.error("RM has detected that a listener is not connected anymore !");
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMMonitoring#echo()
     */
    public StringWrapper echo() {
        return new StringWrapper("I'm the RMonitoring");
    }

    /** inherited from RMEventListener methods
     */

    /** Dispatch the shutdown event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmShutDownEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmShutDownEvent(RMEvent evt) {
        evt.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.SHUTDOWN, new Class<?>[] { RMEvent.class }, evt);
    }

    /** Dispatch the shutting down event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmShuttingDownEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmShuttingDownEvent(RMEvent evt) {
        evt.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.SHUTTING_DOWN, new Class<?>[] { RMEvent.class }, evt);
    }

    /** Dispatch the RM started event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmStartedEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmStartedEvent(RMEvent evt) {
        evt.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.STARTED, new Class<?>[] { RMEvent.class }, evt);
    }

    /** Dispatch the node added event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeSourceAddedEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent)
     */
    public void nodeSourceAddedEvent(RMNodeSourceEvent ns) {
        ns.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODESOURCE_CREATED, new Class<?>[] { RMNodeSourceEvent.class }, ns);
    }

    /** Dispatch the node removed event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeSourceRemovedEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent)
     */
    public void nodeSourceRemovedEvent(RMNodeSourceEvent ns) {
        ns.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODESOURCE_REMOVED, new Class<?>[] { RMNodeSourceEvent.class }, ns);
    }

    /** Dispatch the node added event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeAddedEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeAddedEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_ADDED, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Dispatch the node freed event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeFreeEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeFreeEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_FREE, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Dispatch the node busy event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeBusyEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeBusyEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_BUSY, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Dispatch the node to release event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeToReleaseEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeToReleaseEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_TO_RELEASE, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Dispatch the node down event to all listeners.
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeDownEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeDownEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_DOWN, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Dispatch the node removed event to all listeners
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeRemovedEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeRemovedEvent(RMNodeEvent n) {
        n.setRMUrl(this.MonitoringUrl);
        dispatch(RMEventType.NODE_REMOVED, new Class<?>[] { RMNodeEvent.class }, n);
    }

    /** Stop and remove monitoring active object
     */
    public void shutdown() {
        //throwing shutdown event
        rmShutDownEvent(new RMEvent());
        PAActiveObject.terminateActiveObject(false);
    }
}
