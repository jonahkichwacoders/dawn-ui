/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.plotting.javafx.trace.volume;

import java.util.List;

import javafx.scene.Node;

import org.dawnsci.plotting.histogram.service.PaletteService;
import org.dawnsci.plotting.javafx.ServiceLoader;
import org.dawnsci.plotting.javafx.SceneDisplayer;
import org.dawnsci.plotting.javafx.trace.Image3DTrace;
import org.dawnsci.plotting.javafx.trace.JavafxTrace;
import org.eclipse.dawnsci.plotting.api.IPlottingSystemViewer;
import org.eclipse.dawnsci.plotting.api.trace.IVolumeRenderTrace;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IntegerDataset;
import org.eclipse.swt.widgets.Display;

/**
 * 
 * @author Joel Ogden
 *
 * @Internal
 */
public class VolumeTrace  extends JavafxTrace implements IVolumeRenderTrace
{
	private VolumeRender volume; 
	
	public VolumeTrace(IPlottingSystemViewer<?> plotter, SceneDisplayer newScene, String name) {
		super(plotter, name, newScene);		
	}

	@Override
	public void setPalette(String paletteName) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public IDataset getData() {
		return DatasetFactory.zeros(IntegerDataset.class, 1, 1);
	}

	@Override
	public void setData(
			final int[] size, 
			final IDataset dataset, 
			final double intensityValue, 
			final double opacityValue,
			final double[] maxMinValue,
			final double[] maxMinCulling,
			final List<? extends IDataset> axes)
	{
		if (volume == null)
			volume = new VolumeRender();
		
		final PaletteService pservice = (PaletteService) ServiceLoader.getPaletteService();
		this.axes = (List<IDataset>) axes;
		
		
		// needs to run in javafx thread
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				volume.compute(
						size,
						dataset, 
						intensityValue, 
						opacityValue, 
						pservice,
						maxMinValue,
						maxMinCulling);
	    	}
	    });
		
	}

	@Override
	public Node getNode() {
		return volume;
	}
	
	@Override
	public void setOpacity(double opacity) {
		
	}

	@Override
	public void setColour(int red, int green, int blue) {
		
	}
	@Override
	public boolean isLit() {
		return false;
	}

	
}


