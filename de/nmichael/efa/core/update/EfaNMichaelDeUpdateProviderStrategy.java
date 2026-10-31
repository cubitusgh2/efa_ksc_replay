package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

/**
 * An implementation of UpdateProviderStrategy that fetches update information
 * from the efa.nmichael.de server. It downloads an XML file from the specified
 * URL and parses it to extract update information.
 * 
 * efa.nmichael.de only provides a single EOU file for the newest version, and
 * does not natively distinguish between production and beta releases.
 * 
 * Beta releases can only be determined by the version ID, which contains a "#"
 * character for beta versions.
 * 
 * efa.nmichael.de is only used as a fallback update source if the GitHub update
 * source is not available, OR for the DRV version of efa, which is not hosted
 * on GitHub.
 * 
 */
public class EfaNMichaelDeUpdateProviderStrategy implements UpdateProviderStrategy {

	private final String eouUrl;

	public EfaNMichaelDeUpdateProviderStrategy(String eouUrl) {
		this.eouUrl = eouUrl;
	}

	public String getSource() {
		return eouUrl;
	}

	public String getName() {
		return "efa.nmichael.de";
	}
	
	public String getDisplayName() {
		return International.getString("efa.nmichael.de (Historisch)");
    }

	public String getUserHint() {
		return International.getString("Diese Update-Quelle ist aus historischen Gründen vorhanden"
				+ " und bildet die bisherige Online-Update Funktionalität nach."
				+ " Sie dient Testzwecken oder dafür, wenn GitHub nicht verfügbar sein sollte."
				+ " Diese Update-Quelle lädt ausschließlich die neueste efa-Version von efa.nmichael.de "
				+ " (egal, ob Beta oder Production Release).");
	}

	/**
	 * Fetches available updates from the provider. Here, it downloads an XML file
	 * from the specified URL and parses it to extract update information.
	 * 
	 * @return List of available updates, sorted by newest first.
	 * @throws Exception if an error occurs while fetching updates.
	 */
	public List<UpdateCandidate> fetchAvailableUpdates() throws Exception {
		String versionFile = Daten.efaTmpDirectory + "eou.xml";
		if (!DownloadThread.getFile(null, eouUrl, versionFile, true)) {
			return new ArrayList<UpdateCandidate>();
		}

		try {
			XMLReader parser = EfaUtil.getXMLReader();
			OnlineUpdateFileParser ou = new OnlineUpdateFileParser();
			parser.setContentHandler(ou);
			parser.parse(versionFile);

			Vector<OnlineUpdateInfo> versions = ou.getVersions();
			List<UpdateCandidate> result = new ArrayList<UpdateCandidate>();
			for (OnlineUpdateInfo v : versions) {
				result.add(new UpdateCandidate(v.versionId, v.releaseDate, v.downloadUrl, v.downloadSize,
						new ArrayList<String>(v.getChanges()), getName(), false, // efa.nmichael.de does not provide
																					// draft releases
						v.versionId.contains("#"), // but it provides beta releases, which can be determined by the
													// version ID containing a "#" character
						eouUrl, v.versionId));
			}
			// Ensure newest first even if API order changes.
			Collections.sort(result, new Comparator<UpdateCandidate>() {
				@Override
				public int compare(UpdateCandidate a, UpdateCandidate b) {
					// descending (newest first)
					return VersionIdComparator.compare(a.getVersionId(), b.getVersionId());
				}
			});

			return result;
		} finally {
			EfaUtil.deleteFile(versionFile);
		}
	}

	/**
	 * Fetches the newest EOU file from the provider. This method is used to obtain
	 * the latest EOU file, which contains information about available updates.
	 * 
	 * @param parent          The parent window for any dialogs that may be
	 *                        displayed during the download process.
	 * @param newerCandidates A sorted list (newest first) of update candidates that
	 *                        are newer than the current version.
	 * @return The newest EOU file or null if the download fails.
	 * @throws Exception
	 */
	public File fetchNewestEOUFile(Window parent, List<UpdateCandidate> newerCandidates) throws Exception {

		String versionFile = Daten.efaTmpDirectory + "eou.xml";
		if (!DownloadThread.getFile(parent, this.eouUrl, versionFile, true)) {
			// No candidate with a valid EOU download URL found.
			Logger.log(Logger.INFO, Logger.MSG_STAT_IGNOREDENTRIES, "No EOU download URL found in newer candidates.");
			return null;
		} else {
			Logger.log(Logger.INFO, Logger.MSG_STAT_IGNOREDENTRIES, "Downloaded EOU file from: " + this.eouUrl);
			return new File(versionFile);
		}
	}

	@Override
	public boolean isEOUFileTemporary() {
		return true; // the EOU file is always a temporary file that is deleted after parsing
	};

}
