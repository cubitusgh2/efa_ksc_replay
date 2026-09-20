package de.nmichael.efa.core.update;

import java.util.List;

/**
 * Interface for update provider strategies that fetch available updates.
 */
public interface UpdateProviderStrategy {
    /**
	 * Returns the name of the update provider.
	 * @return Name of the update provider.
	 */
	String getName();

	/**
	 * Fetches available updates from the provider.
	 * @return List of available updates, sorted by newest first.
	 * @throws Exception if an error occurs while fetching updates.
	 */
    List<UpdateCandidate> fetchAvailableUpdates() throws Exception; // newest first
}
