package de.nmichael.efa.core.update;

import de.nmichael.efa.Daten;

/**
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * 
 * Beta releases only, i.e. pre-releases.
 * 
 * Drafts are not loaded, as they are not intended for public beta testing.
 * Drafts are only loaded to by a maintainer of the repository, to test the release 
 * before publishing it (either pre-release or production release).
 * 
 * Production releases are only loaded by GithubProductionReleaseUpdateProviderStrategy.
 * 
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */

public class GithubBetaReleaseUpdateProviderStrategy extends GithubReleaseUpdateProviderStrategy {

    public static final String GITHUBBETA = "GitHub (Beta/Pre-Release)";
    
    
    public GithubBetaReleaseUpdateProviderStrategy() {
		//just call super constructor with includePrereleases = true, includeDrafts = false
		super(Daten.ONLINEUPDATE_INFO_GITHUB_OWNER, 
				Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY, 
    			true, // only pre-releases
    			false, // no drafts
    			false, // no production releases
    			null, 15000, 30000);
	}
    
    public String getName() {
        return GITHUBBETA;
    }
}
