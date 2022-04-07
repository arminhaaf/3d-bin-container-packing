package com.github.skjolber.packing;

import java.util.List;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public interface PackCallback {

	PackCallback DEFAULT = new PackCallback() {
	};

	default void freeSpacesCalculated(Container pContainer, List<Space> pFreeSpaces) {
	}

	default void placementAdded(Container pContainer, Placement pPlacement) {
	}

	default void levelAdded(Container pContainer, long pNewLevel) {
	}

	default void packDone(Container pContainer) {
	}

}
