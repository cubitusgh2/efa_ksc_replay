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

import de.nmichael.efa.data.*;
import java.util.Stack;

public class MenuDestinations extends MenuData {

    public MenuDestinations(CLI cli) {
        super(cli);
        this.storageObject = cli.getPersistence(Destinations.class, 
                Project.STORAGEOBJECT_DESTINATIONS, Destinations.DATATYPE);
        this.storageObjectDescription = "destinations";
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }

}
