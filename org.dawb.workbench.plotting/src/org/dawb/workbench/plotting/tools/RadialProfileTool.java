package org.dawb.workbench.plotting.tools;

import java.util.Arrays;
import java.util.Collection;

import org.dawb.common.ui.plot.AbstractPlottingSystem;
import org.dawb.common.ui.plot.region.IRegion;
import org.dawb.common.ui.plot.region.IRegion.RegionType;
import org.dawb.common.ui.plot.trace.IImageTrace;
import org.dawb.common.ui.plot.trace.ILineTrace;
import org.dawb.common.ui.plot.trace.ITrace;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;

public class RadialProfileTool extends ProfileTool {


	@Override
	protected void createAxes(AbstractPlottingSystem plotter) {

		// Do nothing for now
	}

	@Override
	protected void createProfile(IImageTrace  image, 
			                     IRegion      region, 
			                     ROIBase      rbs, 
			                     boolean      tryUpdate,
			                     IProgressMonitor monitor) {
        
		if (monitor.isCanceled()) return;
		if (image==null) return;
		
		if (!isRegionTypeSupported(region.getRegionType())) return;

		final SectorROI sroi = (SectorROI) (rbs==null ? region.getROI() : rbs);
		if (sroi==null) return;
		if (!region.isVisible()) return;

		if (monitor.isCanceled()) return;
		
		// TODO This is intentionally slow but detailed. Irakli looking into best way
		// to run this algorithm (radial and azimuthal separately?) 
		// NOTE image.getDownsampled() is also possible as they will not normally be
		// viewing at full resolution anyway but this means they may see a wrong integration.
		AbstractDataset[] profile = ROIProfile.sector(image.getData(), image.getMask(), sroi, true, false, true);
		
        if (profile==null) return;
				
		final AbstractDataset radialIntegral = profile[0];
		radialIntegral.setName("Radial Integration "+region.getName());
		
		final AbstractDataset xi = DatasetUtils.linSpace(sroi.getRadius(0), sroi.getRadius(1), radialIntegral.getSize(), AbstractDataset.FLOAT32);
		xi.setName("Radius (pixel)");
		
		if (tryUpdate) {
			final ILineTrace x_trace = (ILineTrace)plotter.getTrace("Radial Integration "+region.getName());
			
			if (x_trace!=null) {
				getControl().getDisplay().syncExec(new Runnable() {
					public void run() {
						x_trace.setData(xi, radialIntegral);
					}
				});
			}		
			
		} else {
						
			Collection<ITrace> plotted = plotter.createPlot1D(xi, Arrays.asList(new AbstractDataset[]{radialIntegral}), monitor);
			registerTraces(region, plotted);			
		}
			
	}
	
	@Override
	protected boolean isRegionTypeSupported(RegionType type) {
		return type==RegionType.SECTOR || type==RegionType.RING;
	}

	@Override
	protected RegionType getCreateRegionType() {
		return RegionType.SECTOR;
	}

}
