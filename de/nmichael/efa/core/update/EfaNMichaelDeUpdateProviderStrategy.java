package de.nmichael.efa.core.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.EfaUtil;

public class EfaNMichaelDeUpdateProviderStrategy implements UpdateProviderStrategy {

    private final String eouUrl;

    public EfaNMichaelDeUpdateProviderStrategy(String eouUrl) {
        this.eouUrl = eouUrl;
    }

    public String getName() {
        return "efa.nmichael.de";
    }
    /**
	 * Fetches available updates from the provider.
	 * Here, it downloads an XML file from the specified URL and parses it to extract update information.
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
                result.add(new UpdateCandidate(
                    v.versionId,
                    v.releaseDate,
                    v.downloadUrl,
                    v.downloadSize,
                    new ArrayList<String>(v.getChanges()),
                    getName(),
                    false,
                    eouUrl
                ));
            }
            return result;
        } finally {
            EfaUtil.deleteFile(versionFile);
        }
    }
}
