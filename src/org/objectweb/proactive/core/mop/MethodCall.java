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
package org.objectweb.proactive.core.mop;

import sun.rmi.server.MarshalInputStream;
import sun.rmi.server.MarshalOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Instances of this class represent method calls performed on reified
 * objects. They are generated by a <I>stub object</I>, whose role is to act
 * as a representative for the reified object.
 *
 * @author Julien Vayssi&egrave;re
 */
public final class MethodCall implements java.io.Serializable {
    // COMPONENTS added a tag for identification of component requests
    private String tag;

    // COMPONENTS added a field for the Fractal interface name 
    // (the name of the interface containing the method called)
    private String fcFunctionalInterfaceName;

    //
    // --- STATIC MEMBERS -----------------------------------------------------------------------
    //

    /**
     *        The size of the pool we use for recycling MethodCall objects.
     */
    private static int RECYCLE_POOL_SIZE = 30;

    /**
     * The pool of recycled methodcall objects
     */
    private static MethodCall[] recyclePool;

    /**
     * Position inside the pool
     */
    private static int index;

    /**        Indicates if the recycling of MethodCall object is on. */
    private static boolean recycleMethodCallObject;
    private static java.util.Hashtable reifiedMethodsTable = new java.util.Hashtable();

    //tag for component calls
    public static final String COMPONENT_TAG = "component-methodCall";

    /**
     * Initializes the recycling of MethodCall objects to be enabled by default.
     */
    static {
        MethodCall.setRecycleMethodCallObject(true);
    }

    //
    // --- PRIVATE MEMBERS -----------------------------------------------------------------------
    //

    /**
     * The array holding the argments of the method call
     */
    private Object[] effectiveArguments;

    /**
     * The method corresponding to the call
     */
    private transient Method reifiedMethod;

    /**
     * The internal ID of the methodcall
     */
    private long methodCallID;
    private String key;

    /**
     * byte[] to store effectiveArguments. Requiered to optimize multiple serialization
     * in some case (such as group communication) or to create a stronger
     * asynchronism (serialization of parameters then return to the thread of
     * execution before the end of the rendez-vous).
     */
    private byte[] serializedEffectiveArguments = null;

    /**
     * transform the effectiveArguments into a byte[]
     * */
    public void transformEffectiveArgumentsIntoByteArray() {
        if ((serializedEffectiveArguments == null) &&
                (effectiveArguments != null)) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                //  ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                MarshalOutputStream objectOutputStream = new MarshalOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(effectiveArguments);
                objectOutputStream.flush();
                objectOutputStream.close();
                byteArrayOutputStream.close();
                serializedEffectiveArguments = byteArrayOutputStream.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            effectiveArguments = null;
        }
    }

    /**
     * Sets recycling of MethodCall objects on/off. Note that turning the recycling
     * off and on again results in the recycling pool being flushed, thus damaging
     * performances.
     * @param value        sets the recycling on if <code>true</code>, otherwise turns it off.
     */
    public static synchronized void setRecycleMethodCallObject(boolean value) {
        if (recycleMethodCallObject == value) {
            return;
        } else {
            recycleMethodCallObject = value;
            if (value) {
                // Creates the recycle poll for MethodCall objects
                recyclePool = new MethodCall[RECYCLE_POOL_SIZE];
                index = 0;
            } else {
                // If we do not want to recycle MethodCall objects anymore,
                // let's free some memory by permitting the reyclePool to be
                // garbage-collecting
                recyclePool = null;
            }
        }
    }

    /**
     * Indicates if the recycling of MethodCall objects is currently running or not.
     *
     * @return                        <code>true</code> if recycling is on, <code>false</code> otherwise
     */
    public static synchronized boolean getRecycleMethodCallObject() {
        return MethodCall.recycleMethodCallObject;
    }

    /**
     *        Factory method for getting MethodCall objects
     *
     *        @param reifiedMethod a <code>Method</code> object that represents
     *        the method whose invocation is reified
     *        @param effectiveArguments   the effective arguments of the call. Arguments
     *        that are of primitive type need to be wrapped
     *         within an instance of the corresponding wrapper
     *  class (like <code>java.lang.Integer</code> for
     *  primitive type <code>int</code> for example).
     *        @return        a MethodCall object representing an invocation of method
     *        <code>reifiedMethod</code> with arguments <code>effectiveArguments</code>
     */
    public synchronized static MethodCall getMethodCall(Method reifiedMethod,
        Object[] effectiveArguments) {
        if (MethodCall.getRecycleMethodCallObject()) {
            // Finds a recycled MethodCall object in the pool, cleans it and
            // eventually returns it
            if (MethodCall.index > 0) {
                // gets the object from the pool
                MethodCall.index--;
                MethodCall result = MethodCall.recyclePool[MethodCall.index];
                MethodCall.recyclePool[MethodCall.index] = null;
                // Refurbishes the object
                result.reifiedMethod = reifiedMethod;
                result.effectiveArguments = effectiveArguments;
                result.key = buildKey(reifiedMethod);
                return result;
            } else {
                return new MethodCall(reifiedMethod, effectiveArguments);
            }
        } else {
            return new MethodCall(reifiedMethod, effectiveArguments);
        }
    }

    /**
     * Returns a MethodCall object with extra info for component calls (the
     * possible name of the functional interface invoked).
     * @param reifiedMethod
     * @param effectiveArguments
     * @param fcFunctionalInterfaceName fractal interface name, whose value is :
     *  - null if the call is non-functional
     *  - the name of the functional interface otherwise
     * @return MethodCall
     */
    public synchronized static MethodCall getComponentMethodCall(
        Method reifiedMethod, Object[] effectiveArguments,
        String fcFunctionalInterfaceName) {
        // COMPONENTS
        MethodCall mc = MethodCall.getMethodCall(reifiedMethod,
                effectiveArguments);
        mc.setTag(COMPONENT_TAG);
        mc.setFcFunctionalInterfaceName(fcFunctionalInterfaceName);
        return mc;
    }

    /**
     *        Tells the recyclying process that the MethodCall object passed as parameter
     *        is ready for recycling. It is the responsibility of the caller of this
     *        method to make sure that this object can safely be disposed of.
     */
    public synchronized static void setMethodCall(MethodCall mc) {
        if (MethodCall.getRecycleMethodCallObject()) {
            // If there's still one slot left in the pool
            if (MethodCall.recyclePool[MethodCall.index] == null) {
                // Cleans up a MethodCall object
                // It is prefereable to do it here rather than at the moment
                // the object is picked out of the pool, because it allows
                // garbage-collecting the objects referenced in here
                mc.reifiedMethod = null;
                mc.effectiveArguments = null;
                mc.key = null;
                // Inserts the object in the pool
                MethodCall.recyclePool[MethodCall.index] = mc;
                MethodCall.index++;
                if (MethodCall.index == RECYCLE_POOL_SIZE) {
                    MethodCall.index = RECYCLE_POOL_SIZE - 1;
                }
            }
        }
    }

    /**
     *        Builds a new MethodCall object. This constructor is private to this class
     *        because we want to enforce the use of factory methods for getting fresh
     * instances of this class (see <I>Factory</I> pattern in GoF).
     */
    public MethodCall(Method reifiedMethod, Object[] effectiveArguments) {
        this.reifiedMethod = reifiedMethod;
        this.effectiveArguments = effectiveArguments;
        this.key = buildKey(reifiedMethod);
    }

    /**
     *        Executes the instance method call represented by this object.
     *
     * @param targetObject        the Object the method is called on
     * @throws MethodCallExecutionFailedException thrown if the refleciton of the
     * call failed.
     * @throws InvocationTargetException thrown if the execution of the reified
     * method terminates abruptly by throwing an exception. The exception
     * thrown by the execution of the reified method is placed inside the
     * InvocationTargetException object.
     * @return the result of the invocation of the method. If the method returns
     * <code>void</code>, then <code>null</code> is returned. If the method
     * returned a primitive type, then it is wrapped inside the appropriate
     * wrapper object.
     */
    public Object execute(Object targetObject)
        throws InvocationTargetException, MethodCallExecutionFailedException {
        // A test at how non-public methods can be reflected
        if ((serializedEffectiveArguments != null) &&
                (effectiveArguments == null)) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedEffectiveArguments);

                //ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                MarshalInputStream objectInputStream = new MarshalInputStream(byteArrayInputStream);
                effectiveArguments = (Object[]) objectInputStream.readObject();
                objectInputStream.close();
                byteArrayInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            serializedEffectiveArguments = null;
        }

        if (reifiedMethod.getParameterTypes().length > 0) {
            reifiedMethod.setAccessible(true);
        }
        try {
            return reifiedMethod.invoke(targetObject, effectiveArguments);
        } catch (IllegalAccessException e) {
            throw new MethodCallExecutionFailedException(
                "Access rights to the method denied: " + e);
        }
    }

    protected void finalize() {
        MethodCall.setMethodCall(this);
    }

    public Method getReifiedMethod() {
        return reifiedMethod;
    }

    public String getName() {
        return reifiedMethod.getName();
    }

    public int getNumberOfParameter() {
        return this.effectiveArguments.length;
    }

    public Object getParameter(int index) {
        return this.effectiveArguments[index];
    }

    public void setEffectiveArguments(Object[] o) {
        effectiveArguments = o;
    }

    /**
     * Make a deep copy of all arguments of the constructor
     */
    public void makeDeepCopyOfArguments() throws java.io.IOException {
        effectiveArguments = (Object[]) Utils.makeDeepCopy(effectiveArguments);
    }

    /**
     * accessor for the functional name ot the invoked Fractal interface
     * @return the functional name of the invoked Fractal interface
     */
    public String getFcFunctionalInterfaceName() {
        return fcFunctionalInterfaceName;
    }

    /**
     * setter for the functional name of the invoked Fractal interface
     * @param the functional name of the invoked Fractal interface
     */
    public void setFcFunctionalInterfaceName(String string) {
        fcFunctionalInterfaceName = string;
    }

    /**
     * setter for the tag of the method call
     */
    public void setTag(String string) {
        tag = string;
    }

    /**
     * accessor for the tag of the method call
     * @return the tag of the method call
     */
    public String getTag() {
        return tag;
    }

    //
    // --- PRIVATE METHODS -----------------------------------------------------------------------
    //
    private Class[] fixBugRead(FixWrapper[] para) {
        Class[] tmp = new Class[para.length];
        for (int i = 0; i < para.length; i++) {
            //	System.out.println("fixBugRead for " + i + " value is " +para[i]);
            tmp[i] = para[i].getWrapped();
        }
        return tmp;
    }

    private FixWrapper[] fixBugWrite(Class[] para) {
        FixWrapper[] tmp = new FixWrapper[para.length];
        for (int i = 0; i < para.length; i++) {
            //	System.out.println("fixBugWrite for " + i + " out of " + para.length + " value is " +para[i] );	
            tmp[i] = new FixWrapper(para[i]);
        }
        return tmp;
    }

    private static String buildKey(Method reifiedMethod) {
        StringBuffer sb = new StringBuffer();
        sb.append(reifiedMethod.getDeclaringClass().getName());
        sb.append(reifiedMethod.getName());
        Class[] parameters = reifiedMethod.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            sb.append(parameters[i].getName());
        }
        return sb.toString();
    }

    //
    // --- PRIVATE METHODS FOR SERIALIZATION --------------------------------------------------------------
    //
    private void writeObject(java.io.ObjectOutputStream out)
        throws java.io.IOException {
        out.defaultWriteObject();
        // The Method object needs to be converted
        out.writeObject(reifiedMethod.getDeclaringClass());
        out.writeObject(reifiedMethod.getName());
        out.writeObject(fixBugWrite(reifiedMethod.getParameterTypes()));
    }

    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        reifiedMethod = (Method) reifiedMethodsTable.get(key);
        if (reifiedMethod == null) {
            // Reads several pieces of data that we need for looking up the method
            Class declaringClass = (Class) in.readObject();
            String simpleName = (String) in.readObject();
            Class[] parameters = this.fixBugRead((FixWrapper[]) in.readObject());

            // Looks up the method
            try {
                reifiedMethod = declaringClass.getMethod(simpleName, parameters);
                reifiedMethodsTable.put(key, reifiedMethod);
            } catch (NoSuchMethodException e) {
                throw new InternalException("Lookup for method failed: " + e +
                    ". This may be caused by having different versions of the same class on different VMs. Check your CLASSPATH settings.");
            }
        } else { //added to avoid an ibis bug
            in.readObject();
            in.readObject();
            in.readObject();
        }
        if ((serializedEffectiveArguments != null) &&
                (effectiveArguments == null)) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedEffectiveArguments);

                //	    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                MarshalInputStream objectInputStream = new MarshalInputStream(byteArrayInputStream);
                effectiveArguments = (Object[]) objectInputStream.readObject();
                objectInputStream.close();
                byteArrayInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            serializedEffectiveArguments = null;
        }
    }

    //
    // --- INNER CLASSES -----------------------------------------------------------------------
    //
    public class FixWrapper implements java.io.Serializable {
        public boolean isPrimitive;
        public Class encapsulated;

        public FixWrapper() {
        }

        /**
         * Encapsulate primitives types into Class
         */
        public FixWrapper(Class c) {
            if (!c.isPrimitive()) {
                encapsulated = c;
                return;
            }
            isPrimitive = true;
            if (c.equals(Boolean.TYPE)) {
                encapsulated = Boolean.class;
            } else if (c.equals(Byte.TYPE)) {
                encapsulated = Byte.class;
            } else if (c.equals(Character.TYPE)) {
                encapsulated = Character.class;
            } else if (c.equals(Double.TYPE)) {
                encapsulated = Double.class;
            } else if (c.equals(Float.TYPE)) {
                encapsulated = Float.class;
            } else if (c.equals(Integer.TYPE)) {
                encapsulated = Integer.class;
            } else if (c.equals(Long.TYPE)) {
                encapsulated = Long.class;
            } else if (c.equals(Short.TYPE)) {
                encapsulated = Short.class;
            }
        }

        /**
         * Give back the original class
         */
        public Class getWrapped() {
            if (!isPrimitive) {
                return encapsulated;
            }
            if (encapsulated.equals(Boolean.class)) {
                return Boolean.TYPE;
            }
            if (encapsulated.equals(Byte.class)) {
                return Byte.TYPE;
            }
            if (encapsulated.equals(Character.class)) {
                return Character.TYPE;
            }
            if (encapsulated.equals(Double.class)) {
                return Double.TYPE;
            }
            if (encapsulated.equals(Float.class)) {
                return Float.TYPE;
            }
            if (encapsulated.equals(Integer.class)) {
                return Integer.TYPE;
            }
            if (encapsulated.equals(Long.class)) {
                return Long.TYPE;
            }
            if (encapsulated.equals(Short.class)) {
                return Short.TYPE;
            }
            throw new InternalException("FixWrapper encapsulated class unkown " +
                encapsulated);
        }

        public String toString() {
            return "FixWrapper: " + encapsulated.toString();
        }
    }

    // end inner class FixWrapper
}
