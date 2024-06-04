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
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.Logger;
import java.util.Stack;

public class MenuMessages extends MenuData {

    public MenuMessages(CLI cli) {
        super(cli);
        this.storageObject = cli.getPersistence(Messages.class, Project.STORAGEOBJECT_MESSAGES, Messages.DATATYPE);
        this.storageObjectDescription = "messages";
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
