package org.dawnsci.mapping.ui.datamodel;

import org.dawnsci.mapping.ui.LivePlottingUtils;
import org.dawnsci.mapping.ui.MappingUtils;
import org.eclipse.dawnsci.plotting.api.trace.MetadataPlotUtils;
import org.eclipse.january.DatasetException;
import org.eclipse.january.MetadataException;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IDatasetConnector;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.january.metadata.AxesMetadata;
import org.eclipse.january.metadata.MetadataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappedData extends AbstractMapData{

	private static final Logger logger = LoggerFactory.getLogger(MappedData.class);
	
	
	public MappedData(String name, ILazyDataset map, MappedDataBlock parent, String path) {
		super(name, map, parent, path);
	}
	
	public MappedData(String name, IDatasetConnector map, MappedDataBlock parent, String path) {
		super(name, map, parent, path);
	}
	
	public void replaceLiveDataset(ILazyDataset dataset) {
		live = false;
		disconnect();
		this.map = dataset;
		setRange(calculateRange(map));
	}
	
	protected double[] calculateRange(ILazyDataset map){
		
		if (map instanceof IDatasetConnector) return null;
		
		
		IDataset[] ax = MetadataPlotUtils.getAxesAsIDatasetArray(map);
		
		MapScanDimensions mapDims = parent.getMapDims();
		
		int yDim = mapDims.getyDim();
		int xDim = mapDims.getxDim();
		
		return MappingUtils.calculateRangeFromAxes(new IDataset[]{ax[yDim],ax[xDim]});

	}
	

	public IDataset getSpectrum(double x, double y) {
		int[] indices = MappingUtils.getIndicesFromCoOrds(map, x, y);
		if (indices == null) return null;
		ILazyDataset spectrum = parent.getSpectrum(indices[0], indices[1]);
		if (spectrum == null) return null;
		IDataset s = null;
		try {
			s = spectrum.getSlice();
		} catch (DatasetException e) {
			e.printStackTrace();
		}
		return s;
	}
	
	public MappedData makeNewMapWithParent(String name, IDataset ds) {
		return new MappedData(name, ds, parent, path);
	}

	
	public boolean isLive() {
		return live;
	}
	
	public void update() {
		if (!live) return;
		if (!connected) {			
			try {
				connect();
			} catch (Exception e) {
				logger.debug("Could not connect",e);

			}
		}

		//gets a slice
		IDataset ma = LivePlottingUtils.getUpdatedMap(baseMap, oParent, this.toString());
		if (ma == null) return;
		
		IDataset[] ax = MetadataPlotUtils.getAxesAsIDatasetArray(ma);
		
		
		setRange(MappingUtils.calculateRangeFromAxes(new IDataset[]{ax[0],ax[1]}));

		map  = ma;
	}

	@Override
	public ILazyDataset getData() {
		return map;
	}
}
