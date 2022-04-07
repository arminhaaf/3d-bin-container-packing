package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.BagContainer;
import com.github.skjolber.packing.BagLargestAreaFitFirstPackager;
import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.PackCallback;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.Space;
import com.github.skjolber.packing.impl.LAFFResult;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class TestVisu extends Application {


	public void start(Stage pStage) {
		final ContainerVisu tContainerVisu = new ContainerVisu();


		final Container tCarton = new BagContainer("1", 200, 200, 100, 0);
		BagLargestAreaFitFirstPackager tPackager = new BagLargestAreaFitFirstPackager(Collections.singletonList(tCarton));

		final List<Box> tItems = new ArrayList<>();


		final Container tBag = new BagContainer("1", 550, 460, 110, 0);
		tItems.add(new Box(485, 74, 7, 0));
//		tItems.add(new Box(220, 80, 5, 0));
		LAFFResult tPackResult = tPackager.pack(tItems, tBag, Long.MAX_VALUE, 1);
		tContainerVisu.show(tPackResult.getContainer());

//
		final Random random = new Random();
		final double tMinSizeFactor = 0.2;
		final double tMaxSizeFactor = 0.7;
		for (int i = 0; i < 10; i++) {
			tItems.add(new com.github.skjolber.packing.Box("" + i,
														   (int)(tCarton.getWidth() * tMinSizeFactor + tCarton.getWidth() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   (int)(tCarton.getDepth() * tMinSizeFactor + tCarton.getDepth() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   (int)(tCarton.getHeight() * tMinSizeFactor + tCarton.getHeight() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   0));
		}


		final ContainerNode tContainerNode = tContainerVisu.show(tCarton);

		tPackager.setPackCallback(new PackCallback() {
			@Override
			public void freeSpacesCalculated(Container pContainer, final List<Space> pFreeSpaces) {
				Platform.runLater(() -> {
					tContainerNode.removeTransientBoxes();
					for (Space tFreeSpace : pFreeSpaces) {
						if (tFreeSpace != null) {
							tContainerNode.addFreeSpace(tFreeSpace);
						}
					}
				});

				waitForNextStep();
			}

			@Override
			public void placementAdded(final Container pContainer, final Placement pPlacement) {

				Platform.runLater(() -> {
					tContainerNode.removeTransientBoxes();
					tContainerNode.addPlacement(pPlacement);
				});

				waitForNextStep();
			}

			@Override
			public void levelAdded(final Container pContainer, final long pNewLevel) {
				// grösse des Containers kann sich geändert haben -> Bag!!
				tContainerNode.updateContainerBox(pContainer);
			}
		});


//
//		for (Container tContainer : tPackager.packList(tItems.stream().map(BoxItem::new).collect(Collectors.toList()), Integer.MAX_VALUE, Long.MAX_VALUE)) {
//			tContainerVisu.show(tContainer);
//		}

		tContainerVisu.addEventHandler(KeyEvent.KEY_TYPED, event -> {
			if (" ".equals(event.getCharacter())) {
				synchronized (tWaitObject) {
					tWaitObject.notify();
				}
			}
		});


		pStage.setScene(tContainerVisu);
		pStage.show();

		new Thread(() -> tPackager.pack(tItems, tCarton, Long.MAX_VALUE, 1)).start();

	}

	Object tWaitObject = new Object();

	private void waitForNextStep() {
		try {
			synchronized (tWaitObject) {
				tWaitObject.wait();
			}
		} catch (InterruptedException pE) {
		}
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException pE) {
//            pE.printStackTrace();
//        }
	}


	public static void main(String[] args) {
		launch();
	}
}
