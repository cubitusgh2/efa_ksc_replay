package de.nmichael.efa.gui.util;

/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

import java.awt.*;

import javax.swing.SwingUtilities;

public class BringToFrontThread extends Thread {

    private Frame frame;
    private int afterMs;

    public BringToFrontThread(Window w, int afterMs) {
        this.frame = frame;
        this.afterMs = afterMs;
    }

    public void run() {
    	this.setName("BringToFrontThread");
        try {
            Thread.sleep(afterMs);
            if (frame.getState() == Frame.ICONIFIED) {
                frame.setState(Frame.NORMAL);
                Thread.sleep(afterMs);
            }
            // not thread safe
            //frame.toFront();
            
        	SwingUtilities.invokeLater(new Runnable() {
        	      public void run() {
        	            frame.toFront();
        	      }
          	});            

        } catch (Exception e) {
        }
    }

    public static void bringToFront(Window w, int afterMs) {
        BringToFrontThread thr = new BringToFrontThread(w, afterMs);
        thr.start();
    }
}
