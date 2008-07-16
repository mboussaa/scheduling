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
package org.ow2.proactive.scheduler.common.exception;

import org.objectweb.proactive.annotation.PublicAPI;


/**
 * Exceptions Generated by the scheduler or its proxy.<br>
 * This is the generic exception used to inform the user of a problem.
 *
 * @author The ProActive Team
 * @since ProActive 3.9
 */
@PublicAPI
public class SchedulerException extends Exception {

    /**
     * Create a new instance of SchedulerException with the given message.
     *
     * @param msg the message to attach.
     */
    public SchedulerException(String msg) {
        super(msg);
    }

    /**
     * Create a new instance of SchedulerException.
     */
    public SchedulerException() {
        super();
    }

    /**
     * Create a new instance of SchedulerException with the given message and cause
     *
     * @param msg the message to attach.
     * @param cause the cause of the exception.
     */
    public SchedulerException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a new instance of SchedulerException with the given cause.
     *
     * @param cause the cause of the exception.
     */
    public SchedulerException(Throwable cause) {
        super(cause);
    }
}
