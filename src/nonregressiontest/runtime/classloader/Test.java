/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive-support@inria.fr
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package nonregressiontest.runtime.classloader;

import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;

import testsuite.test.FunctionalTest;


/**
 * 2 steps hierarchical deployment with dynamic classloading through runtimes.
 *
 * @author Matthieu Morel
 */
public class Test extends FunctionalTest {
    ProActiveDescriptor descriptor;

    public Test() {
        super("remote classloading with custom classloader",
            "remote classloading with custom classloader");
    }

    /**
     * @see testsuite.test.FunctionalTest#action()
     */
    public void action() throws Exception {
        A a = (A) ProActive.newActive("nonregressiontest.runtime.classloader.A",
                new Object[] {  }, descriptor.getVirtualNode("VN1").getNode());
        a.createActiveObjectB();
    }

    /**
     * @see testsuite.test.AbstractTest#initTest()
     */
    public void initTest() throws Exception {
        //descriptor = ProActive.getProactiveDescriptor(getClass().getResource("/nonregressiontest/runtime/classloader/deployment.xml").getPath());
        System.setProperty("proactive.classloader", "enable");
        descriptor = ProActive.getProactiveDescriptor(getClass()
                                                          .getResource("/nonregressiontest/runtime/classloader/deployment.xml")
                                                          .getPath());
        descriptor.activateMappings();
    }

    /**
     * @see testsuite.test.AbstractTest#endTest()
     */
    public void endTest() throws Exception {
        descriptor.killall(false);
        System.setProperty("proactive.classloader", "disable");
    }

    public boolean postConditions() throws Exception {
        return true;
    }
}
