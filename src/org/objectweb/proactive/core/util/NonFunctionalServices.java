/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2005 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
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

package org.objectweb.proactive.core.util;

import java.lang.reflect.Method;


import org.objectweb.proactive.core.mop.MethodCall;
import org.objectweb.proactive.core.mop.Proxy;

/**
 *  <i><font size="-1" color="#FF0000">**For internal use only** </font></i><br>
 *  <p>
 *  This class is a way to add non functionnal services to managed active objects.
 *  
 * The methods are reifed in the active object's Stub, so that their implementation is transparent for the programmer.
 *   
 * @author mozonne
 *
 */

public class NonFunctionalServices {
		
	static Class stubObjectClass = null;
	static Method terminateAOMethod = null;
	static Method terminateAOImmediatlyMethod = null;
	static Class paramTypes[];
	
	
	static {
		
		try {
			// "terminateAO" and "terminateAOImmediatly" are declared in the StubObject interface.
			stubObjectClass = java.lang.Class.forName ("org.objectweb.proactive.core.mop.StubObject");
			paramTypes = new Class[1];
			paramTypes [0] = java.lang.Class.forName ("org.objectweb.proactive.core.mop.Proxy");
			terminateAOMethod = stubObjectClass.getMethod("terminateAO", paramTypes);
			terminateAOImmediatlyMethod = stubObjectClass.getMethod("terminateAOImmediatly", paramTypes);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (SecurityException es) {
			es.printStackTrace();

		}catch (NoSuchMethodException en) {
				en.printStackTrace();
		} 
	}

	/**
	 * Reify the "terminateAOMethod" call in the active object's stub. 
	 * @param proxy 
	 * @throws Throwable
	 */
	public static void terminateAO (Proxy proxy) throws Throwable {
		proxy.reify(MethodCall.getMethodCall(terminateAOMethod, paramTypes));		
	}
	
	/**
	 * Reify the "terminateAOMethod" call in the active object's stub.
	 * A call on this method is an immediateService. 
	 * @param proxy 
	 * @throws Throwable
	 */
	public static void terminateAOImmediatly(Proxy proxy) throws Throwable {
		proxy.reify(MethodCall.getMethodCall(terminateAOImmediatlyMethod, paramTypes));
	}

}
