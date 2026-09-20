package de.nmichael.efa.core.update;

import java.util.List;
import java.util.Vector;
import javax.swing.JDialog;

import de.nmichael.efa.gui.OnlineUpdateDialog;

/**
 * An implementation of UpdateSelectionStrategy that presents a dialog to the user 
 * for selecting an update candidate.
 */
public class DialogSelectionStrategy implements UpdateSelectionStrategy {

    public UpdateCandidate select(JDialog parent, List<UpdateCandidate> newerCandidates, String currentVersion) {
        if (newerCandidates == null || newerCandidates.isEmpty()) {
            return null;
        }

        // Übergangslösung: bestehender Dialog zeigt "ausgewählte Zielversion" an.
        // Für echte Mehrfachauswahl später einen eigenen Selection-Dialog ergänzen.
        UpdateCandidate selected = newerCandidates.get(0);
        Vector<String> changes = new Vector<String>(selected.getChanges());

        OnlineUpdateDialog dlg = new OnlineUpdateDialog(parent,
                selected.getVersionId(), selected.getReleaseDate(), selected.getDownloadSize(), changes);
        dlg.showDialog();
        return dlg.getDialogResult() ? selected : null;
    }
}
