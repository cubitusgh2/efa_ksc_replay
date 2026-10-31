package de.nmichael.efa.core.update;

import de.nmichael.efa.Daten;

/**
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * 
 * Production releases only, i.e. no pre-releases or drafts. 
 * Pre-releases and drafts are only loaded by GithubBetaReleaseUpdateProviderStrategy.
 * 
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */
public class GithubProductionReleaseUpdateProviderStrategy extends GithubReleaseUpdateProviderStrategy {

    public static final String GITHUBPROD = "GitHub (Production Release)";
    
    
    public GithubProductionReleaseUpdateProviderStrategy() {
    	super(Daten.ONLINEUPDATE_INFO_GITHUB_OWNER,
    			Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY,
				// no pre-releases. Github can mark a release as pre-realese.
				// we rely on that, and don't check the version number for a "beta" or "alpha" string.
    			false, // no pre-releases
    			false, // no drafts
    			true, // only production releases
    			null, 15000, 30000);
    }
    
    public String getName() {
        return GITHUBPROD;
    }
}