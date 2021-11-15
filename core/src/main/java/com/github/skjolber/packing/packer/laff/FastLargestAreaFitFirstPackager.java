package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultContainerStackValue;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.points2d.ExtremePoints2D;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager<Point2D> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;
		private LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D, ?> configurationBuilderFactory;

		public LargestAreaFitFirstPackagerBuilder setConfigurationBuilderFactory(LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D, ?> configurationBuilder) {
			this.configurationBuilderFactory = configurationBuilder;
			return this;
		}
		
		public LargestAreaFitFirstPackagerBuilder withContainers(List<Container> containers) {
			this.containers = containers;
			return this;
		}

		public LargestAreaFitFirstPackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
			this.checkpointsPerDeadlineCheck = n;
			return this;
		}
		
		public FastLargestAreaFitFirstPackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(configurationBuilderFactory == null) {
				configurationBuilderFactory = new DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory<>();
			}
			return new FastLargestAreaFitFirstPackager(containers, checkpointsPerDeadlineCheck, configurationBuilderFactory);
		}	
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public FastLargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck, factory);
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> stackables, Container targetContainer,  BooleanSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);
		
		ContainerStackValue[] stackValues = targetContainer.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		StackConstraint constraint = containerStackValue.getConstraint();
		
		LevelStack stack = new LevelStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter( s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter( s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints2D<StackPlacement> extremePoints2D = new ExtremePoints2D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy());

		LargestAreaFitFirstPackagerConfiguration<Point2D> configuration = factory.newBuilder().withContainer(targetContainer).withExtremePoints(extremePoints2D).withStack(stack).build();
		
		StackableComparator firstComparator = configuration.getFirstComparator();
		StackValueComparator<Point2D> firstStackValueComparator = configuration.getFirstStackValueComparator();
		
		StackableComparator nextComparator = configuration.getNextComparator();
		StackValueComparator<Point2D> nextStackValueComparator = configuration.getNextStackValueComparator();

		while(!scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}
			
			int maxWeight = stack.getFreeWeightLoad();
			int maxHeight = stack.getFreeLoadDz();

			Point2D firstPoint = extremePoints2D.getValue(0);
			
			int bestFirstIndex = -1;
			StackValue bestFirstStackValue = null;
			Stackable bestFirstBox = null;
			
			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(constraint != null && constraint.accepts(stack, box)) {
					continue;
				}
				if(bestFirstBox != null && firstComparator.compare(box, bestFirstBox) <= 0) {
					continue;
				}
				for (StackValue stackValue : box.getStackValues()) {
					if(stackValue.getDz() > maxHeight) {
						continue;
					}
					if(!stackValue.fitsInside2D(containerStackValue.getLoadDx(), containerStackValue.getLoadDy())) {
						continue;
					}
					if(bestFirstStackValue != null && firstStackValueComparator.compare(firstPoint, bestFirstStackValue, firstPoint, stackValue) <= 0) {
						continue;
					}
					if(constraint != null && !constraint.supports(stack, box, stackValue, 0, 0, 0)) {
						continue;
					}
					bestFirstIndex = i;
					bestFirstStackValue = stackValue;
					bestFirstBox = box;
				}
			}

			if(bestFirstIndex == -1) {
				break;
			}
			Stackable stackable = scopedStackables.remove(bestFirstIndex);
			remainingStackables.remove(stackable);
			
			DefaultContainerStackValue levelStackValue = stack.getContainerStackValue(bestFirstStackValue.getDz());
			Stack levelStack = new DefaultStack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, bestFirstStackValue, 0, 0, 0, -1, -1);

			levelStack.add(first);
			
			int levelHeight = levelStackValue.getDz();

			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints2D.add(0, first);
			
			while(!extremePoints2D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				
				long maxPointArea = extremePoints2D.getMaxArea();
				long maxPointVolume = maxPointArea * levelHeight;
				
				int bestPointIndex = -1;
				int bestIndex = -1;
				StackValue bestStackValue = null;
				Stackable bestStackable = null;
				
				List<Point2D> points = extremePoints2D.getValues();
				for (int i = 0; i < scopedStackables.size(); i++) {
					Stackable box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(maxPointArea < box.getMinimumArea()) {
						continue;
					}

					if(bestStackValue != null && nextComparator.compare(bestStackable, box) <= 0) {
						continue;
					}
					for (StackValue stackValue : box.getStackValues()) {
						if(stackValue.getArea() > maxPointArea) {
							continue;
						}
						if(levelHeight < stackValue.getDz()) {
							continue;
						}
						
						// pick the point with the lowest area
						int bestStackValuePointIndex = -1;
						
						for(int k = 0; k < points.size(); k++) {
							Point2D point2d = points.get(k);
							if(point2d.getArea() < stackValue.getArea()) {
								continue;
							}
							
							if(!point2d.fits2D(stackValue)) {
								continue;
							}
							
							if(bestStackValuePointIndex != -1 && nextStackValueComparator.compare(points.get(bestStackValuePointIndex), bestStackValue, point2d, stackValue) <= 0) {
								continue;
							}
							if(constraint != null && !constraint.supports(stack, box, stackValue, point2d.getMinX(), point2d.getMinY(), 0)) {
								continue;
							}

							bestPointIndex = k;
							bestIndex = i;
							bestStackValue = stackValue;
							bestStackable = stackable;
						}
					}
				}
				
				if(bestIndex == -1) {
					break;
				}
				
				Stackable remove = scopedStackables.remove(bestIndex);
				Point2D point = extremePoints2D.getValue(bestPointIndex);
				
				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), 0, -1, -1);
				levelStack.add(stackPlacement);
				extremePoints2D.add(bestPointIndex, stackPlacement);

				maxRemainingLevelWeight -= remove.getWeight();
				
				remainingStackables.remove(remove);
			}
			
			extremePoints2D.reset();
		}
		
		return new LargestAreaFitFirstPackagerResult(remainingStackables, stack, new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack));
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
