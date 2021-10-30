package com.github.skjolber.packing.points2d;

public class DefaultYSupportPoint2D extends Point2D implements YSupportPoint2D  {

	/** range constrained to current minX */
	private final int ySupportMinY;
	private final int ySupportMaxY;
	
	public DefaultYSupportPoint2D(int minX, int minY, int maxX, int maxY, int ySupportMinY, int ySupportMaxY) {
		super(minX, minY, maxX, maxY);
		this.ySupportMinY = ySupportMinY;
		this.ySupportMaxY = ySupportMaxY;
	}
	
	@Override
	public boolean isYSupport(int y) {
		return ySupportMinY <= y && y <= ySupportMaxY;
	}

	public int getYSupportMinY() {
		return ySupportMinY;
	}
	
	public int getYSupportMaxY() {
		return ySupportMaxY;
	}

	@Override
	public String toString() {
		return "DefaultYSupportPoint2D [minX=" + minX + ", minY=" + minY + ", maxY=" + maxY + ", maxX=" + maxX 
				+ ", ySupportMinY=" + ySupportMinY + ", ySupportMaxY=" + ySupportMaxY + "]";
	}
	
	@Override
	public boolean isYEdge(int y) {
		return ySupportMaxY == y - 1;
	}
	
	public Point2D clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D(minX, minY, maxX, maxY, ySupportMinY, Math.min(maxY, ySupportMaxY));
	}

	
}