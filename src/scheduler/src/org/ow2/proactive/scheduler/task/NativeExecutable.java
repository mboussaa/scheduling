/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
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
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.Executable;
import org.ow2.proactive.scheduler.exception.RunningProcessException;
import org.ow2.proactive.scheduler.exception.StartProcessException;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.scheduler.util.process.ProcessTreeKiller;
import org.ow2.proactive.scheduler.util.process.ThreadReader;
import org.ow2.proactive.scripting.GenerationScript;


/**
 * This is the execution entry point for the native task.
 * The execute(TaskResult...) method will be override by the scheduler to launch the native process.
 * This class provide a getProcess method that will return the current running native process.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
public class NativeExecutable extends Executable {
    /**
     *
     */
    private static final long serialVersionUID = 10L;

    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.CORE);

    /** Process that start the native task */
    private transient Process process;

    /** Command that should be executed */
    private String command[];

    /** Environment variables */
    private String[] envp;

    /**
     * HM of environment variables used for
     * for kill action of the task, processes that export
     * theses environment variables will be killed.
     * Used by ProcessTreeKiller
     */
    private Map<String, String> modelEnvVar = null;

    /** Command generated by a script */
    private GenerationScript generated;

    /**
     * Create a new native task that execute command.
     * 
     * @param command the command to be executed.
     * @param generated generation script if the command is generated by a script
     */
    public NativeExecutable(String command[], GenerationScript generated) {
        this.command = command;
        this.generated = generated;
    }

    /**
     * Create a new native task that execute command.
     * 
     * @param command the command to be executed.
     */
    public NativeExecutable(String[] command) {
        this.command = command;
    }

    /**
     * Return the current native running process.
     * It is used by the scheduler to allow it to kill the process.
     *
     * @return the current native running process.
     */
    public Process getProcess() {
        return this.process;
    }

    /**
     * Return the generation script if any, null otherwise.
     * It is used by the task launcher to generate the command.
     *
     * @return the generation script.
     */
    public GenerationScript getGenerationScript() {
        return generated;
    }

    /**
     * Sets the command to the given command value.
     *
     * @param command the command to set.
     */
    public void setCommand(String[] command) {
        this.command = command;
    }

    /**
     * Set the environment variables.
     *
     * @param envp the environment variables to be set.
     */
    public void setEnvp(String[] envp) {
        this.envp = envp;
    }

    /**
     * Set the environment variables as a model (hashMap).
     *
     * @param model the environment variables to be set.
     */
    public void setModelEnvVar(Map<String, String> model) {
        this.modelEnvVar = model;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.executable.Executable#execute(org.ow2.proactive.scheduler.common.task.TaskResult[])
     */
    @Override
    public Serializable execute(TaskResult... results) {

        //WARNING : if this.command is unknown, it will create a defunct process
        //it's due to a known java bug
        try {
            process = Runtime.getRuntime().exec(this.command, this.envp);
        } catch (Exception e) {
            //in this case, the error is certainly due to the user (ie : command not found)
            //we have to inform him about the cause.
            logger_dev.info("", e);
            System.err.println(e);
            throw new StartProcessException(e.getMessage());
        }

        try {
            // redirect streams
            BufferedReader sout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader serr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            Thread tsout = new Thread(new ThreadReader(sout, System.out, this));
            Thread tserr = new Thread(new ThreadReader(serr, System.err, this));
            tsout.start();
            tserr.start();
            // wait for process completion
            process.waitFor();
            // wait for log flush
            tsout.join();
            tserr.join();

            //killTreeProcess(process);
            return process.exitValue();
        } catch (Exception e) {
            logger_dev.error("", e);
            //exception during process
            //means that for most cases, user is not responsible
            throw new RunningProcessException(e.getMessage());
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.executable.Executable#init(java.util.Map)
     */
    @Override
    public final void init(Map<String, String> args) throws Exception {
        throw new RuntimeException("This method should have NEVER been called in this context !!");
    }

    /**
     * interrupt native process and its children (if launched)
     * set killedState boolean to finalize ThreadReaders 
     * which listen SDTOUT/STDERR of the native process
     */
    @Override
    public void kill() {
        super.kill();
        if (process != null) {
            ProcessTreeKiller.get().kill(process, modelEnvVar);
            //WARN jlscheef destroy() may be useless but it's not working yet without it.
            //processTreeKiller seems not to kill current process...
            process.destroy();
        }
    }
}
