package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.api.Placement3D;

public class Default3DPlanePoint3D extends Point3D implements XZPlanePoint3D, YZPlanePoint3D, XYPlanePoint3D {


	/** range constrained to current minX */
	private final Placement3D yzPlane;
	
	/** range constrained to current minY */
	private final Placement3D xzPlane;

	/** range constrained to current minZ */
	private final Placement3D xyPlane;

	public Default3DPlanePoint3D(
			int minX, int minY, int minZ, 
			int maxX, int maxY, int maxZ,
			Placement3D yzPlane,
			Placement3D xzPlane,
			Placement3D xyPlane
			) {
		super(minX, minY, minZ, maxX, maxY, maxZ);

		this.yzPlane = yzPlane;
		this.xzPlane = xzPlane;
		this.xyPlane = xyPlane;
	}

	public int getSupportedYZPlaneMinY() {
		return yzPlane.getAbsoluteY();
	}
	
	public int getSupportedYZPlaneMaxY() {
		return yzPlane.getAbsoluteEndY();
	}
	@Override
	public int getSupportedYZPlaneMinZ() {
		return yzPlane.getAbsoluteZ();
	}
	
	@Override
	public int getSupportedYZPlaneMaxZ() {
		return yzPlane.getAbsoluteEndZ();
	}
	
	@Override
	public boolean isSupportedYZPlane(int y, int z) {
		return yzPlane.getAbsoluteY() <= y && y <= yzPlane.getAbsoluteEndY() && yzPlane.getAbsoluteZ() <= z && z <= yzPlane.getAbsoluteEndZ();
	}
	
	public boolean isYZPlaneEdgeZ(int z) {
		return yzPlane.getAbsoluteEndZ() == z - 1;
	}

	public boolean isYZPlaneEdgeY(int y) {
		return yzPlane.getAbsoluteEndY() == y - 1;
	}
	

	public int getSupportedXZPlaneMinX() {
		return xzPlane.getAbsoluteX();
	}
	
	public int getSupportedXZPlaneMaxX() {
		return xzPlane.getAbsoluteEndX();
	}
	
	@Override
	public int getSupportedXZPlaneMaxZ() {
		return xzPlane.getAbsoluteZ();
	}
	
	@Override
	public int getSupportedXZPlaneMinZ() {
		return xzPlane.getAbsoluteEndZ();
	}

	@Override
	public boolean isSupportedXZPlane(int x, int z) {
		return xzPlane.getAbsoluteX() <= x && x <= xzPlane.getAbsoluteEndX() && xzPlane.getAbsoluteZ() <= z && z <= xzPlane.getAbsoluteEndZ();
	}
	
	public boolean isXZPlaneEdgeX(int x) {
		return xzPlane.getAbsoluteX() == x - 1;
	}

	public boolean isXZPlaneEdgeZ(int z) {
		return xzPlane.getAbsoluteEndZ() == z - 1;
	}


	public int getSupportedXYPlaneMinY() {
		return xyPlane.getAbsoluteY();
	}
	
	public int getSupportedXYPlaneMaxY() {
		return xyPlane.getAbsoluteEndY();
	}
	@Override
	public int getSupportedXYPlaneMinX() {
		return xyPlane.getAbsoluteX();
	}
	
	@Override
	public int getSupportedXYPlaneMaxX() {
		return xyPlane.getAbsoluteEndX();
	}
	
	@Override
	public boolean isSupportedXYPlane(int x, int y) {
		return xyPlane.getAbsoluteX() <= x && x <= xyPlane.getAbsoluteEndX() && xyPlane.getAbsoluteY() <= y && y <= xyPlane.getAbsoluteEndY();
	}
	
	public boolean isXYPlaneEdgeX(int x) {
		return xyPlane.getAbsoluteEndX() == x - 1;
	}

	public boolean isXYPlaneEdgeY(int y) {
		return xyPlane.getAbsoluteEndY() == y - 1;
	}

	@Override
	public Point3D clone(int maxX, int maxY, int maxZ) {
		return new Default3DPlanePoint3D(minX, minY, minZ, 
				maxX, maxY, maxZ,
				yzPlane, xzPlane, xyPlane
				);
	}


	@Override
	public Placement3D getXZPlane() {
		return xzPlane;
	}
	
	@Override
	public Placement3D getYZPlane() {
		return yzPlane;
	}

	@Override
	public Placement3D getXYPlane() {
		return xyPlane;
	}
}
