/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

public class DownloadThread {

    final static int PROGRESS_TIMETOPOPUP = 1;
    final static int PROGRESS_TIMERINTERVAL = 500;

    private ProgressMonitor progressMonitor;
    private URLConnection conn;
    private volatile String localFile;
    private volatile boolean waitfor;
    private ExecuteAfterDownload afterDownload;
    private Thread thread;
    private volatile int downDone, downTotal;
    private volatile InputStream i;
    private volatile FileOutputStream o;
    private volatile String exceptionText = null;
    private javax.swing.Timer timer;
    private volatile boolean aborted = false;

    public DownloadThread(ProgressMonitor progressMonitor, 
            URLConnection conn, 
            String local, 
            boolean waitfor, 
            ExecuteAfterDownload afterDownload) {
        this.progressMonitor = progressMonitor;
        this.conn = conn;
        this.localFile = local;
        this.waitfor = waitfor;
        this.afterDownload = afterDownload;
        this.downDone = 0;
        this.downTotal = 100; // nur ini-Wert
    }

    Thread go() {
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                return new ActualTask(afterDownload);
            }
        };
        return (thread = worker.start());
    }

    int getLengthOfTask() {
        return downTotal;
    }

    int getCurrent() {
        return downDone;
    }

    void stop() {
        try {
            if (i != null) {
                i.close();
            }
            if (o != null) {
                o.close();
            }
        } catch (IOException e) {
            // ignorieren, Stream war evtl. schon zu
        } finally {
            i = null;
            o = null;
            exceptionText = International.getString("Abbruch");
        }
    }

    boolean done() {
        return (downTotal == downDone);
    }

    String getMessage() {
        return International.getMessage("{bytesDone} Bytes von {bytesTotal} Bytes ...", downDone, downTotal);
    }

    void setAborted() {
        aborted = true;
    }

    void exit() {
        // thr.destroy();
    }

    private boolean runDownload() {
        timer = new javax.swing.Timer(PROGRESS_TIMERINTERVAL, new TimerListener(this));
        if (progressMonitor != null) {
            progressMonitor.setProgress(0);
            progressMonitor.setMaximum(conn.getContentLength());
            progressMonitor.setMillisToDecideToPopup(PROGRESS_TIMETOPOPUP);
        }
        go();
        timer.start();
        if (waitfor) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        // if progrssMonitor == null, we run in silent mode
        if (exceptionText != null && progressMonitor != null) {
            Dialog.error(LogString.operationFailed(International.getString("Download"), exceptionText) + "\n"
                    + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            return false;
        }
        return true;
    }

    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, boolean waitfor) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, waitfor, null);
        return downloadThread.runDownload();
    }

    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, ExecuteAfterDownload afterDownload) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, false, afterDownload);
        return downloadThread.runDownload();
    }

    public static boolean getFile(Window frame, String remote, String local, boolean wait) {
        try {
        	if (remote == null || remote.toLowerCase().startsWith("file://")) {
        		return copyLocalFile(remote, local);
        	} else {
	            URLConnection conn = new URL(remote).openConnection();
	            conn.connect();
	            return runDownload(frame, conn, remote, local, wait);
        	}
        } catch (Exception e) {
            // if frame==null, we run in "silent" mode
            if (frame != null) {
               Dialog.error(LogString.operationFailed(International.getString("Download"), e.toString()) + "\n"
                       + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            return false;
        }
    }

    public static boolean getFile(Window frame, String remote, String local, ExecuteAfterDownload afterDownload) {
        try {
        	if (remote == null || remote.toLowerCase().startsWith("file://")) {
        		if (copyLocalFile(remote, local)) {
			        afterDownload.success();
					return true;
        		} else {
					afterDownload.failure(International.getString("Fehler beim Kopieren der Datei"));
					return false;
				}
        	} else {
	            URLConnection conn = new URL(remote).openConnection();
	            conn.connect();
	            return runDownload(frame, conn, remote, local, afterDownload);
        	}
        } catch (Exception e) {
            if (frame != null) {
                Dialog.error(LogString.operationFailed(International.getString("Download"),e.toString()) + "\n"
                        + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            return false;
        }
    }

    private static boolean copyLocalFile(String remote, String local) {
        if (remote == null || local == null) {
            Dialog.error(LogString.operationFailed(International.getString("Download"),
                    "remote/local file path is null") + "\n");
            return false;
        }

        // "file://" bzw. "file:///" Präfix robust entfernen (case-insensitive)
        String path = remote;
        if (path.toLowerCase().startsWith("file://")) {
            path = path.substring(7);
        }
        // Windows: "file:///C:/..." -> nach dem Entfernen von "file://" bleibt "/C:/...";
        // führenden Slash vor Laufwerksbuchstaben entfernen
        if (path.matches("^/[a-zA-Z]:.*")) {
            path = path.substring(1);
        }

        try {
            Path source = Paths.get(path);
            Path target = Paths.get(local);

            if (!Files.exists(source)) {
                Dialog.error(LogString.operationFailed(International.getString("Download"),
                        "Quelldatei nicht gefunden: " + source) + "\n");
                return false;
            }

            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO, "Datei erfolgreich kopiert: " + source + " -> " + target);
        } catch (IOException e) {
            Dialog.error(LogString.operationFailed(International.getString("Download"), e.toString()) + "\n");
            return false;
        }
        return true;
    }


    /**
	 * The actual download task that runs in a separate thread.
	 */
    class ActualTask {

    	ActualTask(ExecuteAfterDownload afterDownload) {
            final int BUFSIZE = 32768; // 32 KB
            byte[] buf = new byte[BUFSIZE];
            downDone = 0;
            downTotal = conn.getContentLength();
            //try-with-resources: InputStream und FileOutputStream werden automatisch geschlossen, auch bei Exception
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(localFile)) {

                // weiterhin über die Instanzfelder zugreifbar (z.B. für stop()/TimerListener)
                i = in;
                o = out;

                int c;
                // read from the stream and write to the file, until EOF or aborted
                // we do not depend on the size (downtotal) being known, 
                // as some servers do not provide a Content-Length header
                while ((c = in.read(buf, 0, BUFSIZE)) > 0 && !aborted) {
                    out.write(buf, 0, c);
                    downDone += c;
                }

	             // Sicherstellen, dass done() korrekt erkennt, dass der Download beendet ist
	             // (relevant z.B. wenn der Server keinen Content-Length-Header liefert,
	             // dann ist downTotal == -1 und würde sonst nie == downDone werden)
	             if (!aborted) {
	                 downTotal = downDone;
	             }
                
                if (afterDownload != null) {
                    if (!aborted && downDone == downTotal) {
                        invokeOnEdt(afterDownload::success);
                    } else {
                        invokeOnEdt(() -> afterDownload.failure(International.getString("Abbruch")));
                    }
                }
            } catch (IOException e) {
                exceptionText = e.getMessage();
                if (afterDownload != null) {
                    invokeOnEdt(() -> afterDownload.failure(e.getMessage()));
                }
            } finally {
                // Felder zurücksetzen, damit stop() nach Abschluss nicht erneut auf
                // bereits geschlossene Streams zugreift
                i = null;
                o = null;
            }
        }

        private void invokeOnEdt(Runnable r) {
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeLater(r);
            }
        }
    }


    class TimerListener implements ActionListener {

        private DownloadThread downloadThread;

        public TimerListener(DownloadThread downloadThread) {
            this.downloadThread = downloadThread;
        }

        public void actionPerformed(ActionEvent evt) {
            if (progressMonitor != null) {
                if (progressMonitor.isCanceled() || downloadThread.done()) {
                    if (progressMonitor.isCanceled()) {
                        downloadThread.setAborted();
                        downloadThread.stop();   // <-- reaktiviert: schließt Streams & setzt exceptionText
                    }
                    progressMonitor.close();
                    downloadThread.exit();
                    timer.stop();
                } else {
                    progressMonitor.setNote(downloadThread.getMessage());
                    progressMonitor.setMaximum(downloadThread.getLengthOfTask());
                    progressMonitor.setProgress(downloadThread.getCurrent());
                }
            } else {
                // Silent-Mode: kein ProgressMonitor vorhanden, trotzdem Timer stoppen,
                // sobald der Download fertig ist - sonst läuft der Timer endlos weiter.
                if (downloadThread.done()) {
                    downloadThread.exit();
                    timer.stop();
                }
            }
        }

    }
}
