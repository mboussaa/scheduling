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
package org.ow2.proactive.scheduler.task.launcher;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.TaskTerminateNotification;
import org.ow2.proactive.scheduler.common.exception.UserException;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.util.Tools;
import org.ow2.proactive.scheduler.task.ExecutableContainer;
import org.ow2.proactive.scheduler.task.NativeExecutable;
import org.ow2.proactive.scheduler.task.TaskResultImpl;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.scheduler.util.process.ProcessTreeKiller;
import org.ow2.proactive.scripting.GenerationScript;
import org.ow2.proactive.scripting.ScriptHandler;
import org.ow2.proactive.scripting.ScriptLoader;
import org.ow2.proactive.scripting.ScriptResult;


/**
 * This launcher is the class that will launch a native process.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
public class NativeTaskLauncher extends TaskLauncher {

    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.LAUNCHER);

    /**
     * Environment variable exported to the the process
     * used for kill the task   
     */
    private static String COOKIE_ENV = "PROACTIVE_COOKIE";

    private static String GENERATION_SCRIPT_ERR = "\nNo command eligible was found by generation script.\n"
        + "A generation script must define a variable named 'command' which contains "
        + "the native command to launch. \n" + "Script details :\n";

    /**
     * ProActive Empty Constructor
     */
    public NativeTaskLauncher() {
    }

    /**
     * Constructor of the native task launcher.
     * CONSTRUCTOR USED BY THE SCHEDULER CORE : do not remove.
     *
     * @param initializer represents the class that contains information to initialize this task launcher.
     */
    public NativeTaskLauncher(TaskLauncherInitializer initializer) {
        super(initializer);
    }

    /**
     * Execute the user task as an active object.
     *
     * @param core The scheduler core to be notify
     * @param executableContainer contains the task to execute
     * @param results the possible results from parent tasks.(if task flow)
     * @return a task result representing the result of this task execution.
     */
    public TaskResult doTask(TaskTerminateNotification core, ExecutableContainer executableContainer,
            TaskResult... results) {
        try {
            //execute pre-script
            if (pre != null) {
                this.executePreScript(getNodes().get(0));
            }

            this.currentExecutable = executableContainer.getExecutable();
            NativeExecutable toBeLaunched = (NativeExecutable) this.currentExecutable;

            //launch generation script
            if (toBeLaunched.getGenerationScript() != null) {
                String generationScriptDefinedCommand = this.executeGenerationScript(toBeLaunched
                        .getGenerationScript());

                //no command has been returned by generation script
                if ((generationScriptDefinedCommand == null) ||
                    generationScriptDefinedCommand.equals(GenerationScript.DEFAULT_COMMAND_VALUE)) {

                    logger_dev.error(GENERATION_SCRIPT_ERR + toBeLaunched.getGenerationScript().getId());

                    throw new UserException(GENERATION_SCRIPT_ERR +
                        toBeLaunched.getGenerationScript().getId());
                } else {
                    //generation script has defined a command, so set the command to launch
                    toBeLaunched.setCommand(Tools.parseCommandLine(generationScriptDefinedCommand));
                }
            }

            setEnvironmentVariables(toBeLaunched);

            if (isWallTime()) {
                scheduleTimer();
            }

            //launch task
            logger_dev.debug("Starting execution of task '" + taskId + "'");
            Serializable userResult = toBeLaunched.execute(results);

            //execute post-script
            if (post != null) {
                this.executePostScript(getNodes().get(0));
            }

            //logBuffer is filled up
            TaskResult result = new TaskResultImpl(taskId, userResult, this.getLogs());

            //return result
            return result;
        } catch (Throwable ex) {
            logger_dev.info("", ex);
            // exceptions are always handled at scheduler core level
            return new TaskResultImpl(taskId, ex, this.getLogs());
        } finally {
            if (isWallTime())
                cancelTimer();
            this.finalizeTask(core);
        }
    }

    /**
     * Execute the generationScript on the default node
     * @throws ActiveObjectCreationException if the script handler cannot be created
     * @throws NodeException if the script handler cannot be created
     * @throws UserException if an error occurred during the execution of the script
     * @return the value of the variable GenerationScript.COMMAND_NAME after the script evaluation.
     */
    @SuppressWarnings("unchecked")
    protected String executeGenerationScript(GenerationScript script) throws ActiveObjectCreationException,
            NodeException, UserException {
        ScriptHandler handler = ScriptLoader.createHandler(getNodes().get(0));
        ScriptResult<String> res = handler.handle(script);

        if (res.errorOccured()) {
            res.getException().printStackTrace();
            logger_dev.error("", res.getException());
            throw new UserException("Command generation script execution has failed on the current node");
        }

        return res.getResult();
    }

    /**
     * Build environment variables for the native executable to launch : the task's environment variables :
     * (task name job name, job id, task id...), system environment variables of the JVM, 
     * and the cookie environment variable used by ProcessTreeKiller
     * 
     */
    private void setEnvironmentVariables(NativeExecutable executable) {

        //Set Model environment variable HashMap for the executable;
        //if this process must be killed bu ProcessTreeKiller
        String cookie_value = ProcessTreeKiller.createCookie();
        Map<String, String> modelEnvVar = new HashMap<String, String>();
        modelEnvVar.put(COOKIE_ENV, cookie_value);
        executable.setModelEnvVar(modelEnvVar);

        //Set environment variables array used to launch the external native command,
        //with system environment variables, ProActive Scheduling environment variables,
        //and ProcessTreeKiller environment variable (cookie)

        Map<String, String> taskEnvVariables = new Hashtable<String, String>(4);
        taskEnvVariables.put(SchedulerVars.JAVAENV_JOB_ID_VARNAME.toString(), System
                .getProperty(SchedulerVars.JAVAENV_JOB_ID_VARNAME.toString()));
        taskEnvVariables.put(SchedulerVars.JAVAENV_JOB_NAME_VARNAME.toString(), System
                .getProperty(SchedulerVars.JAVAENV_JOB_NAME_VARNAME.toString()));
        taskEnvVariables.put(SchedulerVars.JAVAENV_TASK_ID_VARNAME.toString(), System
                .getProperty(SchedulerVars.JAVAENV_TASK_ID_VARNAME.toString()));
        taskEnvVariables.put(SchedulerVars.JAVAENV_TASK_NAME_VARNAME.toString(), System
                .getProperty(SchedulerVars.JAVAENV_TASK_NAME_VARNAME.toString()));

        Map<String, String> systemEnvVariables = System.getenv();

        String[] envVarsTab = new String[taskEnvVariables.size() + systemEnvVariables.size() + 1];

        //first we add to the returnTab the task environment variables
        int i = 0;
        for (Map.Entry<String, String> entry : taskEnvVariables.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            envVarsTab[i++] = NativeTaskLauncher.convertJavaenvNameToSysenvName("" + name + "=" + value);
        }

        //after we add to the returnTab the system environment variables
        for (Map.Entry<String, String> entry : systemEnvVariables.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            envVarsTab[i++] = "" + name + "=" + value;
        }

        //then the cookie used by ProcessTreeKiller
        envVarsTab[i] = COOKIE_ENV + "=" + cookie_value;
        executable.setEnvp(envVarsTab);

    }

    /**
     * Convert the Java environment name to the System environment name
     *
     * @param javaenvName the java environment name.
     * @return the System environment name.
     */
    public static String convertJavaenvNameToSysenvName(String javaenvName) {
        return javaenvName.toUpperCase().replace('.', '_');
    }

}
