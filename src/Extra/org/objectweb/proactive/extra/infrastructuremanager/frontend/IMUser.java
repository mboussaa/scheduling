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
package org.objectweb.proactive.extra.infrastructuremanager.frontend;

import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.wrapper.IntWrapper;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;
import org.objectweb.proactive.extra.scheduler.common.scripting.SelectionScript;


/**
 * An interface Front-End for the Infrastructure Manager's User active object.
 * Provides a way to perform user operations in infrastructure manager (IM).
 * We consider the ProActive scheduler as an 'user' of IM.
 * So the user (scheduler) launch tasks on nodes, it asks node to the IM.
 * and give back nodes at the end of the tasks. That the two operations
 * of an user :<BR>
 * - ask nodes or get nodes.<BR>
 * - give back nodes or free nodes.<BR><BR>
 *
 * Scheduler can ask nodes that verify criteria. selections criteria are
 * defined in a test script which provides kind of boolean result :
 * node suitable or not suitable.<BR>
 * This script is executed in the node before selecting it,
 * If the node match criteria, it is selected, otherwise IM tries the selection script
 * on other nodes.
 *
 *  @see org.objectweb.proactive.extra.scheduler.common.scripting.SelectionScript
 *
 *  @author ProActive team.
 *
 */
public interface IMUser {

    /** echo function */
    public StringWrapper echo();

    /**
     * Provides nbNodes nodes verifying a selection script.
     * If the infrastructure manager (IM) don't have nb free nodes
     * it returns the max of valid free nodes
     * @param nbNodes the number of nodes.
     * @param selectionScript : script to be verified by the returned nodes.
     * @return an array list of nodes.
     */
    public NodeSet getAtMostNodes(IntWrapper nbNodes,
        SelectionScript selectionScript);

    /**
     * Provides exactly nbNodes nodes verifying the selection script.
     * If the infrastructure manager (IM) don't have nb free nodes
     * it returns an empty node set.
     * @param nbNodes the number of nodes.
     * @param selectionScript : script to be verified by the returned nodes.
     * @return an array list of nodes.
     */
    public NodeSet getExactlyNodes(IntWrapper nbNodes,
        SelectionScript selectionScript);

    /**
     * Release the node got by an user previously.
     * @param node : the node to release.
     */
    public void freeNode(Node node);

    /**
     * Release nodes got by an user previously.
     * @param nodes : a table of nodes to release.
     */
    public void freeNodes(NodeSet nodes);
}
