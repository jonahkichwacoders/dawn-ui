/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.ui.editors.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.workbench.ui.editors.AsciiEditor;
import org.dawb.workbench.ui.editors.PlotDataEditor;
import org.dawnsci.plotting.AbstractPlottingSystem;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.axis.IAxis;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * 
 * Testing scalability of plotting.
 * 
 * @author gerring
 *
 */
public class SWTXYAxisTest {


	@Test
	public void testChoerant1() throws Throwable {
		
		createAxisTest(SWTXYTestUtils.createTestArraysCoherent(10, 1000, "Long set "), false);
	}

	@Test
	public void testChoerant2() throws Throwable {
		
		createAxisTest(SWTXYTestUtils.createTestArraysCoherent(5, 1000, "Long set "), true);
	}


	@Test
	public void testRandom1() throws Throwable {
		
		createAxisTest(SWTXYTestUtils.createTestArraysRandom(10, 1000), false);
	}

	@Test
	public void testRandom2() throws Throwable {
		
		createAxisTest(SWTXYTestUtils.createTestArraysRandom(5, 1000), true);
	}


	private void createAxisTest(final List<IDataset> ys, boolean multipleAxes) throws Throwable {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");

		String path = (bun.getLocation()+"src/org/dawb/workbench/ui/editors/test/ascii.dat");
		path = path.substring("reference:file:".length());
		if (path.startsWith("/C:")) path = path.substring(1);
		
		final IWorkbenchPage page      = EclipseUtils.getPage();		
		final IFileStore  externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
		final IEditorPart part         = page.openEditor(new FileStoreEditorInput(externalFile), AsciiEditor.ID);
		
		final AsciiEditor editor       = (AsciiEditor)part;
		final PlotDataEditor plotter   = (PlotDataEditor)editor.getActiveEditor();
		final IPlottingSystem<Composite> sys = plotter.getPlottingSystem();
		
		//if (!(sys instanceof PlottingSystemImpl)) throw new Exception("This test is designed for "+PlottingSystemImpl.class.getName());
		page.setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
			
		sys.clear();
		
		Dataset indices = DatasetFactory.createRange(0, ys.get(0).getSize(), 1, Dataset.INT32);

		
		if (!multipleAxes) {
			final IAxis primaryY       = sys.getSelectedYAxis();
			final IAxis alternateYaxis = sys.createAxis("Alternate", true, SWT.LEFT);
			alternateYaxis.setForegroundColor(sys.getPlotComposite().getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN));
			alternateYaxis.setLog10(true);
			sys.setXFirst(true);

			sys.setSelectedYAxis(alternateYaxis);
			sys.setSelectedYAxis(primaryY);
		
			for (int i = 0; i < ys.size(); i++) {
				
				final IDataset y = ys.get(i);
				if (i%2==0) {
					sys.setSelectedYAxis(primaryY);
				} else {
					sys.setSelectedYAxis(alternateYaxis);
				}
				sys.createPlot1D(indices, Arrays.asList(new IDataset[]{y}), null);

			}

		} else {
			for (int i = 0; i < ys.size(); i++) {
				
				final IDataset y = ys.get(i);
				
				final IAxis alternateYaxis = sys.createAxis("Alternate Y "+y.getName(), true, SWT.RIGHT);
				sys.setSelectedYAxis(alternateYaxis);
				if (i%2==0) alternateYaxis.setLog10(true);
				
				sys.createPlot1D(indices, Arrays.asList(new IDataset[]{y}), null);
				sys.setXFirst(true);
				
				final Color colour = ((AbstractPlottingSystem)sys).get1DPlotColor(y.getName());
				if (colour!=null) alternateYaxis.setForegroundColor(colour);
		    }
		}
			
	    EclipseUtils.delay(2000);	
			
		EclipseUtils.getPage().closeEditor(editor, false);
		System.out.println("Closed: "+path);
	}

	
}
