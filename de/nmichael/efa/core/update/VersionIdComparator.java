package de.nmichael.efa.core.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares version ids with following nomenclature:
 * - Production: Major.minor.subversion_buildno (buildno 1-2 digits)
 * - Prerelease: Major.minor.subversion#betano
 *
 * Order:
 * 1) major, minor, subversion ascending
 * 2) for same base version: production > prerelease
 * 3) within same type: higher build/beta number is newer
 */
public final class VersionIdComparator {
	// regex patterns for production and beta version formats
	// Production: Major.minor.subversion_buildno
    private static final Pattern P_PROD = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)_(\\d{1,2})$");
    // Prerelease: Major.minor.subversion#betano
    private static final Pattern P_BETA = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)#(\\d+)$");

    private VersionIdComparator() {
    }

    /**
	 * Compares two version strings. if both are invalid, compares them lexicographically ignoring case. 
	 * If one is invalid, it is considered less than the valid one. 
	 * If both are valid, compares them based on major, minor, subversion, production status, and suffix number.
	 * @param a First version string.
	 * @param b Second version string.
	 * @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
    public static int compare(String a, String b) {
        ParsedVersion va = parse(a);
        ParsedVersion vb = parse(b);

        if (!va.valid && !vb.valid) {
            String sa = a == null ? "" : a.trim();
            String sb = b == null ? "" : b.trim();
            return sa.compareToIgnoreCase(sb);
        }
        if (!va.valid) {
            return -1;
        }
        if (!vb.valid) {
            return 1;
        }

        int c = Integer.compare(va.major, vb.major);
        if (c != 0) {
            return c;
        }

        c = Integer.compare(va.minor, vb.minor);
        if (c != 0) {
            return c;
        }

        c = Integer.compare(va.sub, vb.sub);
        if (c != 0) {
            return c;
        }

        // same base: production is newer than prerelease
        if (va.production != vb.production) {
            return va.production ? 1 : -1;
        }

        return Integer.compare(va.suffixNo, vb.suffixNo);
    }

    /**
	 * Checks if the candidate version is newer than the current version.
	 * @param candidate The candidate version string.
	 * @param current The current version string.
	 * @return true if the candidate is newer than the current, false otherwise.
	 */
    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }
    /**
	 * Parses a version string into its components. If the version string is invalid, returns an
	 * invalid ParsedVersion object.
	 * @param version The version string to parse.
	 * @return A ParsedVersion object representing the parsed version.
	 * */
    private static ParsedVersion parse(String version) {
        if (version == null) {
            return ParsedVersion.invalid();
        }

        String s = version.trim();

        Matcher mProd = P_PROD.matcher(s);
        if (mProd.matches()) {
            return ParsedVersion.prod(
                    Integer.parseInt(mProd.group(1)),
                    Integer.parseInt(mProd.group(2)),
                    Integer.parseInt(mProd.group(3)),
                    Integer.parseInt(mProd.group(4)));
        }

        Matcher mBeta = P_BETA.matcher(s);
        if (mBeta.matches()) {
            return ParsedVersion.beta(
                    Integer.parseInt(mBeta.group(1)),
                    Integer.parseInt(mBeta.group(2)),
                    Integer.parseInt(mBeta.group(3)),
                    Integer.parseInt(mBeta.group(4)));
        }

        return ParsedVersion.invalid();
    }

    /**
	 * Represents a parsed version with its components and validity status.
	 * */
    private static final class ParsedVersion {
        final boolean valid;
        final int major;
        final int minor;
        final int sub;
        final boolean production; // true => _build, false => #beta
        final int suffixNo;       // buildNo or betaNo

        private ParsedVersion(boolean valid, int major, int minor, int sub, boolean production, int suffixNo) {
            this.valid = valid;
            this.major = major;
            this.minor = minor;
            this.sub = sub;
            this.production = production;
            this.suffixNo = suffixNo;
        }

        /**
		 * Creates a ParsedVersion object representing a production version.
		 * @param major The major version number.
		 * @param minor The minor version number.
		 * @param sub The subversion number.
		 * @param buildNo The build number.
		 * @return A ParsedVersion object representing the production version.
		 * */
        static ParsedVersion prod(int major, int minor, int sub, int buildNo) {
            return new ParsedVersion(true, major, minor, sub, true, buildNo);
        }
        /**
		 * Creates a ParsedVersion object representing a beta version.
		 * @param major The major version number.
		 * @param minor The minor version number.
		 * @param sub The subversion number.
		 * @param betaNo The beta number.
		 * @return A ParsedVersion object representing the beta version.
		 * */
        static ParsedVersion beta(int major, int minor, int sub, int betaNo) {
            return new ParsedVersion(true, major, minor, sub, false, betaNo);
        }

        static ParsedVersion invalid() {
            return new ParsedVersion(false, 0, 0, 0, false, 0);
        }
    }
}
