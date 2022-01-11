package com.github.skjolber.packing;

import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created: 11.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class BagContainerTest {

	@Test
	void testEquals() {
		assertEquals(new BagContainer(1, 1, 1, 0), new BagContainer(new Dimension(1, 1, 1), 1));
	}

	@Test
	void testGetUsedSpaceWhenEmpty() {
		assertEquals(new Dimension(0, 0, 0), new BagContainer(0, 0, 0, 0).getUsedSpace());
	}

	@Test
	void testGetUsedSpaceWhenOneBox() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);
		container.addLevel();
		container.add(new Placement(new Space(), new Box(2, 3, 4, 0)));
		assertEquals(new Dimension(2, 3, 4), container.getUsedSpace());
	}

	@Test
	void testGetUsedSpaceWhenTwoBoxesSameLevel() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);
		container.addLevel();
		container.add(new Placement(new Space(10, 10, 10, 0, 0, 0), new Box(2, 3, 7, 0)));
		container.add(new Placement(new Space(10, 10, 10, 2, 3, 0), new Box(1, 2, 7, 0)));
		assertEquals(new Dimension(3, 5, 7), container.getUsedSpace());
	}

	@Test
	void testGetUsedSpaceWhenTwoBoxesTwoLevels() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);
		container.addLevel();
		container.add(new Placement(new Space(10, 10, 4, 0, 0, 0), new Box(2, 3, 4, 0)));
		container.addLevel();
		container.add(new Placement(new Space(10, 10, 6, 0, 0, 4), new Box(1, 2, 2, 0)));
		assertEquals(new Dimension(2, 3, 6), container.getUsedSpace());
	}

	@Test
	void testFoldDown() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);

		assertTrue(container.canHoldWithFolding(new Box(11,10,3,0)));
		assertTrue(container.canHoldWithFolding(new Box(10,11,3,0)));
		assertFalse(container.canHoldWithFolding(new Box(12,10,9,0)));
		assertFalse(container.canHoldWithFolding(new Box(10,12,9,0)));
	}

	@Test
	void testFoldBoxToBaseAreaDown() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);

		container.foldBoxToBaseArea(11,10);

		assertEquals(11, container.width);
		assertEquals(10, container.depth);
		assertEquals(9, container.height);
	}

	@Test
	void testFoldBoxToBaseAreaUp() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);

		container.foldBoxToBaseArea(8,9);

		assertEquals(9, container.width);
		assertEquals(9, container.depth);
		assertEquals(11, container.height);
	}

	@Test
	void testFoldBoxToBaseAreaNoChange() {
		final BagContainer container = new BagContainer(10, 10, 10, 0);

		container.foldBoxToBaseArea(8,10);

		assertEquals(10, container.width);
		assertEquals(10, container.depth);
		assertEquals(10, container.height);
	}


}
