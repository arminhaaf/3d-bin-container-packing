package com.github.skjolber.packing;


/**
 * Created: 09.01.22   by: Armin Haaf
 * <p>
 *
 * @author Armin Haaf
 */
public class BagContainer extends Container {

	private final Box originalBox;

	public BagContainer(final Container pContainer) {
		this(pContainer.getName(), pContainer.getWidth(), pContainer.getDepth(), pContainer.getHeight(), pContainer.getWeight());
	}

	public BagContainer(final Dimension pDimension, final int pWeight) {
		this(pDimension.getName(), pDimension.getWidth(), pDimension.getDepth(), pDimension.getHeight(), pWeight);
	}

	public BagContainer(final int pWidth, final int pDepth, final int pHeight, final int pWeight) {
		this(null, pWidth, pDepth, pHeight, pWeight);
	}

	public BagContainer(final String pName, final int pWidth, final int pDepth, final int pHeight, final int pWeight) {
		super(pName, pWidth, pDepth, pHeight, pWeight);
		originalBox = new Box(this, getWeight());

		rotateLargestAreaDown();
	}

	@Override
	public Container clone() {
		return new BagContainer(this);
	}

	public Box getOriginalBox() {
		return originalBox;
	}

	public int getLargestSideLength() {
		return Math.max(getWidth(), originalBox.getWidth());
	}

	/**
	 * Fits the given dimension into this bag, when use folding
	 */
	public boolean canHoldWithFolding(final Dimension pDimension) {
		// bag logik, passt das mit auffalten
		// grösste Flache nach unten drehen
		final Dimension tLargestAreaDownDim = rotateLargestAreaDown(pDimension);

		// only if folding is possible
		if (tLargestAreaDownDim.getWidth() < width && tLargestAreaDownDim.getDepth() < depth) {
			final Dimension tFoldedDimension = new Dimension(tLargestAreaDownDim.getWidth(), tLargestAreaDownDim.getDepth()
					, calcFoldedHeightForBaseArea(tLargestAreaDownDim.getWidth(), tLargestAreaDownDim.getDepth()));

			return tFoldedDimension.canHold3D(pDimension);
		} else if (tLargestAreaDownDim.getWidth() > width || tLargestAreaDownDim.getDepth() > depth) {
			// downfolding
			final BagContainer tBagContainer = new BagContainer(this);
			tBagContainer.foldBoxToHeight(tLargestAreaDownDim.getHeight());
			return tBagContainer.canHold3D(pDimension);
		} else {
			return false;
		}
	}

	private void rotateLargestAreaDown() {
		final Dimension tDimension = rotateLargestAreaDown(this);

		width = tDimension.getWidth();
		depth = tDimension.getDepth();
		height = tDimension.getHeight();
	}

	Dimension rotateLargestAreaDown(Dimension pDimension) {
		final int tMax1Dim = Math.max(pDimension.getWidth(), Math.max(pDimension.getHeight(), pDimension.getDepth()));
		final int tMax2Dim = Math.min(pDimension.getWidth(), Math.max(pDimension.getHeight(), pDimension.getDepth()));
		final int tMax3Dim = Math.min(pDimension.getWidth(), Math.min(pDimension.getHeight(), pDimension.getDepth()));

		return new Dimension(tMax1Dim, tMax2Dim, tMax3Dim);
	}


	public Level addLevel() {
		// adapt box size to used base area
		adaptDimensions();

		return super.addLevel();
	}

	@Override
	public Dimension getFreeLevelSpace() {
		adaptDimensions();
		return super.getFreeLevelSpace();
	}

	void adaptDimensions() {
		if (levels.size() == 1) {
			// calc base area
			int tMaxX = 0;
			int tMaxY = 0;
			for (Placement tPlacement : levels.get(0)) {
				tMaxX = Math.max(tPlacement.getAbsoluteEndX(), tMaxX);
				tMaxY = Math.max(tPlacement.getAbsoluteEndY(), tMaxY);
			}

			foldBoxToBaseArea(tMaxX, tMaxY);
		}
	}

	void foldBoxToBaseArea(final int pMaxX, final int pMaxY) {
		// 2 possible types of folding
		// gain height -> decrease width and depth
		// gain width (resp. depth its the same) -> decrease height and depth

		// calculation should base on orig dimensions
		final Dimension tOrigDimension = rotateLargestAreaDown(originalBox);
		if (pMaxX <= tOrigDimension.width && pMaxY <= tOrigDimension.depth) {
			// gain height
			final int tFoldLength = Math.min(tOrigDimension.width - pMaxX, tOrigDimension.depth - pMaxY);
			if (tFoldLength < 0) {
				throw new IllegalArgumentException("got fold length less than zero");
			}
			height = tOrigDimension.height + tFoldLength;
			width = tOrigDimension.width - tFoldLength;
			depth = tOrigDimension.depth - tFoldLength;
		} else {
			// gain width (folddownlength is negative!)
			final int tFoldDownLength = Math.min(tOrigDimension.width - pMaxX, tOrigDimension.depth - pMaxY);
			height = tOrigDimension.height + tFoldDownLength;
			width = tOrigDimension.width - tFoldDownLength;
		}
		calculateVolume();
	}

	void foldBoxToHeight(final int pHeight) {
		if (pHeight > height) {
			final int tFoldLength = pHeight - height;
			height += tFoldLength;
			width -= tFoldLength;
			depth -= tFoldLength;
		} else if (pHeight < height) {
			// downfolding -> can gain width or heigth
			final int tFoldDownLength = height - pHeight;
			height -= tFoldDownLength;
			width += tFoldDownLength;
		}
		calculateVolume();

	}

	protected int calcFoldedHeightForBaseArea(int pWidth, int pDepth) {
		if (pWidth < width && pDepth < depth) {
			final int tFoldLength = Math.min(width - pWidth, depth - pDepth);
			return height + tFoldLength;
		} else if (pWidth > width || pDepth > depth) {
			final int tFoldDownLength = Math.min(width - pWidth, depth - pDepth);
			;
			return height + tFoldDownLength;
		} else {
			return height;
		}
	}

	public boolean isEmpty() {
		return levels.isEmpty() || levels.get(0).isEmpty();
	}
}
