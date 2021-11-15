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
import com.github.skjolber.packing.points3d.ExtremePoints3D;
import com.github.skjolber.packing.points3d.Point3D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class LargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager<Point3D> {

	protected LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> factory;
	
	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;
		private LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> configurationBuilderFactory;

		public LargestAreaFitFirstPackagerBuilder setConfigurationBuilderFactory(LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> configurationBuilder) {
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
		
		public LargestAreaFitFirstPackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(configurationBuilderFactory == null) {
				configurationBuilderFactory = new DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory<>();
			}
			return new LargestAreaFitFirstPackager(containers, checkpointsPerDeadlineCheck, configurationBuilderFactory);
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

	public LargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck, factory);
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> stackables, Container targetContainer, BooleanSupplier interrupt) {
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

		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());

		LargestAreaFitFirstPackagerConfiguration<Point3D> configuration = factory.newBuilder().withContainer(targetContainer).withExtremePoints(extremePoints3D).withStack(stack).build();
		
		StackableComparator firstComparator = configuration.getFirstComparator();
		StackValueComparator<Point3D> firstStackValueComparator = configuration.getFirstStackValueComparator();
		
		StackableComparator nextComparator = configuration.getNextComparator();
		StackValueComparator<Point3D> nextStackValueComparator = configuration.getNextStackValueComparator();

		while(!scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}
			
			int maxWeight = stack.getFreeWeightLoad();

			Point3D value = extremePoints3D.getValue(0);
			
			int firstIndex = -1;
			StackValue firstStackValue = null;
			Stackable firstBox = null;
			
			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}
				if(firstBox != null && firstComparator.compare(firstBox, box) <= 0) {
					continue;
				}
				for (StackValue stackValue : box.getStackValues()) {
					if(!stackValue.fitsInside3D(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz())) {
						continue;
					}
					if(firstStackValue != null && firstStackValueComparator.compare(value, firstStackValue, value, stackValue) <= 0) {
						continue;
						
					}
					if(constraint != null && !constraint.supports(stack, box, stackValue, 0, 0, 0)) {
						continue;
					}
					firstIndex = i;
					firstStackValue = stackValue;
					firstBox = box;
				}
			}

			if(firstIndex == -1) {
				break;
			}
			Stackable stackable = scopedStackables.remove(firstIndex);
			remainingStackables.remove(stackable);
			
			DefaultContainerStackValue levelStackValue = stack.getContainerStackValue(firstStackValue.getDz());
			Stack levelStack = new DefaultStack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, firstStackValue, 0, 0, 0, -1, -1);

			levelStack.add(first);
			
			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints3D.add(0, first);
			
			while(!extremePoints3D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				
				long maxPointVolume = extremePoints3D.getMaxVolume();
				
				int bestPointIndex = -1;
				int bestIndex = -1;
				StackValue bestStackValue = null;
				Stackable bestStackable = null;
				
				List<Point3D> points = extremePoints3D.getValues();
				for (int i = 0; i < scopedStackables.size(); i++) {
					Stackable box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(constraint != null && !constraint.accepts(stack, box)) {
						continue;
					}

					if(bestStackValue != null && nextComparator.compare(box, bestStackable) > 0) {
						continue;
					}
					for (StackValue stackValue : box.getStackValues()) {
						// pick the point with the lowest area
						int bestStackValuePointIndex = -1;
						
						for(int k = 0; k < points.size(); k++) {
							Point3D point3d = points.get(k);
							if(!point3d.fits3D(stackValue)) {
								continue;
							}
							if(bestStackValuePointIndex != -1 && nextStackValueComparator.compare(point3d, stackValue, points.get(bestStackValuePointIndex), bestStackValue) <= 0) {
								continue;
							}
							if(constraint != null && !constraint.supports(stack, box, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
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
				Point3D point = extremePoints3D.getValue(bestPointIndex);
				
				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ(), -1, -1);
				levelStack.add(stackPlacement);
				extremePoints3D.add(bestPointIndex, stackPlacement);

				maxRemainingLevelWeight -= remove.getWeight();
				
				remainingStackables.remove(remove);
			}
			
			extremePoints3D.reset();
		}
		
		return new LargestAreaFitFirstPackagerResult(remainingStackables, stack, new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack));
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
