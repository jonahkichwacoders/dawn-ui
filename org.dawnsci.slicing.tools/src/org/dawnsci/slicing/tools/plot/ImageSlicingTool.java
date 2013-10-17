package org.dawnsci.slicing.tools.plot;

import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.slicing.api.system.DimsDataList;
import org.dawnsci.slicing.api.system.AxisType;
import org.dawnsci.slicing.api.tool.AbstractSlicingTool;

/**
 * This is a simple type of slice tool based on the available plot
 * options of the plotting system. However custom slice tools may
 * exist which have more complex UI.
 * 
 * @author fcp94556
 *
 */
public class ImageSlicingTool extends AbstractSlicingTool {

	@Override
	public void militarize() {
		
		getSlicingSystem().setSliceType(getSliceType());
		
		final DimsDataList dimsDataList = getSlicingSystem().getDimsDataList();
		if (dimsDataList!=null) dimsDataList.setTwoAxesOnly(AxisType.X, AxisType.Y);   		
		getSlicingSystem().refresh();
		getSlicingSystem().update(false);
	}

	@Override
	public Enum getSliceType() {
		return PlotType.IMAGE;
	}
}