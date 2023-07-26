/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.cli;

import java.util.Stack;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.sync.KanuEfbAuditPersons;
import de.nmichael.efa.data.sync.KanuEfbSyncTask;

public class MenuSyncEfb extends MenuBase {

    public static final String CMD_RUN  = "run";
    public static final String CMD_PERSON_AUDIT = "audit_persons";
    
    public MenuSyncEfb(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_RUN,  "[logbook]", "run synchronization with Kanu-eFB");
        printUsage(CMD_PERSON_AUDIT, "", "check efa's person db agains Kanu-eFB person data");
    }

    private int syncEfb(String args) {
        if (!cli.getAdminRecord().isAllowedSyncKanuEfb()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        Vector<String> options = super.getCommandOptions(args);
        if (options != null && options.size() > 1) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }
        
        String logbookName = (options != null && options.size() == 1 ? options.get(0) : 
                Daten.project.getCurrentLogbookEfaBoathouse());
        if (logbookName == null) {
            cli.logerr("Failed to synchronize: No logbook specified.");
            return CLI.RC_COMMAND_FAILED;
        }
                
        Logbook logbook = Daten.project.getLogbook(logbookName, false);
        if (logbook == null) {
            cli.logerr("Failed to synchronize: Could not open logbook '" + logbookName + "'.");
            return CLI.RC_COMMAND_FAILED;
        }
        
        cli.loginfo("Running synchronization for logbook '" + logbookName + "' ...");
        KanuEfbSyncTask syncTask = new KanuEfbSyncTask(logbook, cli.getAdminRecord());
        syncTask.startSynchronization(null);
        try {
            syncTask.join();
        } catch(Exception e) {
            cli.logerr("Error during synchronization: " + e.toString());
            return CLI.RC_COMMAND_FAILED;
        }
        if (syncTask.isSuccessfullyCompleted()) {
            return CLI.RC_OK;
        }
        return CLI.RC_COMMAND_FAILED;
    }

    private int personAudit(String args) {
        if (!cli.getAdminRecord().isAllowedSyncKanuEfb()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        Vector<String> options = super.getCommandOptions(args);
        if (options != null && options.size() > 1) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }
        
        String logbookName = (options != null && options.size() == 1 ? options.get(0) : 
                Daten.project.getCurrentLogbookEfaBoathouse());
        if (logbookName == null) {
            cli.logerr("Failed to synchronize: No logbook specified.");
            return CLI.RC_COMMAND_FAILED;
        }
                
        Logbook logbook = Daten.project.getLogbook(logbookName, false);
        if (logbook == null) {
            cli.logerr("Failed to synchronize: Could not open logbook '" + logbookName + "'.");
            return CLI.RC_COMMAND_FAILED;
        }
        
        cli.loginfo("Running synchronization for logbook '" + logbookName + "' ...");
        
        KanuEfbAuditPersons auditTask = new KanuEfbAuditPersons(logbook, cli.getAdminRecord());
        
        auditTask.startSynchronization(null);
        try {
        	auditTask.join();
        } catch(Exception e) {
            cli.logerr("Error during audit: " + e.toString());
            return CLI.RC_COMMAND_FAILED;
        }
        if (auditTask.isSuccessfullyCompleted()) {
            return CLI.RC_OK;
        }
        return CLI.RC_COMMAND_FAILED;
    }    
    
    
    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_RUN)) {
                return syncEfb(args);
            } else if (cmd.equalsIgnoreCase(CMD_PERSON_AUDIT)) {
            	return personAudit(args);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }
}
