/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.plotting.tools.masking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.dawb.common.ui.image.ShapeType;
import org.dawnsci.plotting.AbstractPlottingViewer;
import org.dawnsci.plotting.tools.Activator;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.DefaultOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.api.roi.IRectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.axis.IAxis;
import org.eclipse.dawnsci.plotting.api.histogram.ImageServiceBean.ImageOrigin;
import org.eclipse.dawnsci.plotting.api.preferences.PlottingConstants;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.january.dataset.BooleanDataset;
import org.eclipse.january.dataset.Comparisons;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IndexIterator;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is taken directly out of the class of the same name in SDA
 * 
 * The intention is to make the maths available separate to the side plot system.
 * 
 * @author Matthew Gerring
 *
 */
public class MaskObject {

	private static final Logger logger = LoggerFactory.getLogger(MaskObject.class);
	
	enum MaskRegionType {
		REGION_FROM_MASKING;
	}
	
	public enum MaskMode {
		/**
		 * Draw the mask, ie do the mask
		 */
		DRAW, 
		
		/**
		 * Toggle the masked state
		 */
		TOGGLE, 
		
		/**
		 * Remove the mask at the location.
		 */
		ERASE
	}

	private MaskMode        maskMode;
    private boolean         squarePen           = false;
    private boolean         ignoreAlreadyMasked = false;
   /**
     * The booleans are false to mask and
     * true to leave, that way multiply will work.
     */
    private BooleanDataset  maskDataset;
    private Dataset imageDataset;
    
    /**
     * Used for undoable masking operations.
     */
	private DefaultOperationHistory operationManager;
	private ForkJoinPool pool;
	private PrecisionPoint brushThreshold;
	private ImageOrigin imageOrigin = ImageOrigin.TOP_LEFT;

	MaskObject() {
		this.operationManager = new DefaultOperationHistory();
		operationManager.setLimit(MaskOperation.MASK_CONTEXT, 20);	
		this.pool = new ForkJoinPool();
	}
	
	/**
	 * 
	 * @param maskObject
	 */
	public void sync(MaskObject maskObject) {
		setMaskDataset((BooleanDataset)maskObject.maskDataset.clone(), false);
	}

    /**
     * Designed to copy data in an incoming mask onto this one as best as possible.
     * @param savedMask
     */
	public void process(BooleanDataset savedMask) {
		createMaskIfNeeded();
		
        MaskOperation op = new MaskOperation(maskDataset, savedMask.getSize()/16);
		final int[] shape = savedMask.getShape();
		for (int y = 0; y<shape[0]; ++y) {
			for (int x = 0; x<shape[1]; ++x) {
		        try {
		        	// We only add the falses 
		        	if (!savedMask.getBoolean(y,x)) {
		        		toggleMask(op, Boolean.FALSE, y,x);
		        	}
		        } catch (Throwable ignored) {
		        	continue;
		        }
			}
		}
		
        try {
        	if (op.getSize()>0) operationManager.execute(op, null, null);
		} catch (ExecutionException e) {
			logger.error("Internal error processing external mask.", e);
		}

	}

	private static int[] getPoint(int x, int y, IAxis xAxis, IAxis yAxis, boolean transpose) {
		int px = (int)xAxis.getPositionValue(x);
		int py = (int)yAxis.getPositionValue(y);
		int[] point = transpose ? new int[]{py, px} : new int[]{px, py};
		return point;
	}

	/**
	 * Attempts to process a pen shape at a given draw2d location.
	 * 
	 * Translate from click location using 'system' parameter.
	 * 
	 * @param loc
	 * @param system
	 */
	public void process(final Point finishLocation, final IImageTrace image, final IPlottingSystem<?> system, IProgressMonitor monitor) {
				
		ShapeType penShape = ShapeType.valueOf(Activator.getPlottingPreferenceStore().getString(PlottingConstants.MASK_PEN_SHAPE));
        if (penShape==ShapeType.NONE) return;
        
		createMaskIfNeeded();
       
		boolean maskOut        = Activator.getPlottingPreferenceStore().getBoolean(PlottingConstants.MASK_PEN_MASKOUT);
        final int     penSize  = Activator.getPlottingPreferenceStore().getInt(PlottingConstants.MASK_PEN_SIZE);
        final int     rad1     = (int)Math.ceil(penSize/2f);
        final int     rad2     = (int)Math.floor(penSize/2f);

        final IAxis xAxis = system.getSelectedXAxis();
        final IAxis yAxis = system.getSelectedYAxis();

		AbstractPlottingViewer viewer = (AbstractPlottingViewer)system.getAdapter(AbstractPlottingViewer.class);
        
		final Point startLocation = viewer.getShiftPoint();
        final Collection<Point> locations;
        if (startLocation==null) {
        	locations = new HashSet<Point>(7);
        	locations.add(finishLocation);
        } else {
        	locations = lineBresenham(startLocation, finishLocation);
        }
        
        MaskOperation op = new MaskOperation(maskDataset, 300);
		final boolean transpose = !imageOrigin.isOnLeadingDiagonal();
        for (final Point loc : locations) {
        	
	        monitor.worked(1);
	        
	        final List<int[]> span = new ArrayList<int[]>(3);
	        Display.getDefault().syncExec(new Runnable() {
	        	public void run() {
	        		span.add(getPoint(loc.x-rad2, loc.y-rad2, xAxis, yAxis, transpose));
	        		span.add(getPoint(loc.x+rad1, loc.y+rad1, xAxis, yAxis, transpose));
	        		span.add(getPoint(loc.x, loc.y, xAxis, yAxis, transpose));
	        	}
	        });
	        int[]  cen   = span.get(2);
	        int radius   = Math.abs(span.get(1)[1]-cen[1]); // do not clip radius
	        int[]  start = clip(span.get(0));
	        int[]  end   = clip(span.get(1));

	        boolean mv = maskOut ? Boolean.FALSE : Boolean.TRUE;

	        sortLimits(start, end);
	        for (int y = start[1]; y<=end[1]; ++y) {
	        	for (int x = start[0]; x<=end[0]; ++x) {
	        		     
	        		if (brushThreshold!=null) {
	        			final double intensity = imageDataset.getDouble(y,x);
	        			if (intensity<brushThreshold.preciseX() ||
	        			    intensity>brushThreshold.preciseY()) {
	        				System.out.println("Ignored ("+x+", "+y+")");
	        				continue;
	        			}
	        		}
	        		
					if (penShape == ShapeType.SQUARE) {
						toggleMask(op, mv, y, x);

					} else if (penShape == ShapeType.CIRCLE || penSize < 3) {

						double r = Math.hypot(x - cen[0], y - cen[1]);
						if (r <= radius) {
							toggleMask(op, mv, y, x);
						}

					} else if (penShape == ShapeType.TRIANGLE) {
						double ydel;
						double xdel;
						switch (imageOrigin) {
						case BOTTOM_LEFT:
							xdel = cen[1] - y;
							ydel = end[0] - x;
							break;
						case BOTTOM_RIGHT:
							ydel = end[1] - y;
							xdel = x - cen[0];
							break;
						case TOP_RIGHT:
							xdel = cen[1] - y;
							ydel = x - start[0];
							break;
						case TOP_LEFT:
						default:
							ydel = y - start[1];
							xdel = x - cen[0];
							break;
						}
						if (ydel > 2 * Math.abs(xdel)) {
							toggleMask(op, mv, y, x);
						}

					}
	        	}
	        }
       }
        
        try {
        	if (op.getSize()>0) operationManager.execute(op, null, null);
		} catch (ExecutionException e) {
			logger.error("Problem processing mask draw.", e);
		}
	}

	private void sortLimits(int[] start, int[] end) {
		for (int i = 0; i < start.length; i++) {
			int t = start[i];
			if (t > end[i]) {
				start[i] = end[i];
				end[i] = t;
			}
		}
	}

	private final static Collection<Point> lineBresenham(Point from, Point to) {
		
		
		int x0 =from.x, x1 = to.x, y0 = from.y, y1 = to.y;
		int dy = y1 - y0;
		int dx = x1 - x0;
		int stepx, stepy;

		if (dy < 0) {
			dy = -dy;
			stepy = -1;
		} else {
			stepy = 1;
		}
		if (dx < 0) {
			dx = -dx;
			stepx = -1;
		} else {
			stepx = 1;
		}
		dy <<= 1; // dy is now 2*dy
		dx <<= 1; // dx is now 2*dx

		final List<Point> ret = new ArrayList<Point>(31);
		ret.add(new Point(x0, y0));
		if (dx > dy) {
			int fraction = dy - (dx >> 1); // same as 2*dy - dx
			while (x0 != x1) {
				if (fraction >= 0) {
					y0 += stepy;
					fraction -= dx; // same as fraction -= 2*dx
				}
				x0 += stepx;
				fraction += dy; // same as fraction -= 2*dy
				ret.add(new Point(x0, y0));
			}
		} else {
			int fraction = dx - (dy >> 1);
			while (y0 != y1) {
				if (fraction >= 0) {
					x0 += stepx;
					fraction -= dy;
				}
				y0 += stepy;
				fraction += dx;
				ret.add(new Point(x0, y0));
			}
		}
		
		return ret;
	}


	/**
	 * Toggles a pixel and adds the pixels toggled state to the
	 * AbstractOperation to allow undo/redo to work.
	 * @param mv
	 * @param y
	 * @param x
	 */
	private void toggleMask(MaskOperation op, boolean mv, int y, int x) {
		if (maskDataset.getBoolean(y,x)!=mv) {
			op.addVertex(mv, y, x);
		}
	}
	
	public void dispose() {
		if (operationManager!=null) {
			operationManager.dispose(MaskOperation.MASK_CONTEXT, true, true, true);
		}
		if (pool!=null) {
			pool.shutdownNow();
		}
	}

	/**
	 * Clip to span of image dataset (not 1)
	 * @param is
	 * @return in-place clipped
	 */
	private int[] clip(int[] is) {
		int[] imageShape = imageDataset.getShapeRef();
		int maxX = imageShape[1]-1;
		if (is[0]>maxX) is[0] = maxX;
		if (is[0]<0)    is[0] = 0;
		
		int maxY = imageShape[0]-1;
		if (is[1]>maxY) is[1] = maxY;
		if (is[1]<0)    is[1] = 0;
        return is;
	}

	/**
	 * Designed to be called after processBounds(...) has been called at least once.
	 * Deals with fact that that may leave us with no mask and will create one if needed.
	 * 
	 * @param region
	 * @return
	 */
	public boolean process(IRegion region, IProgressMonitor monitor) {
        return process(null, null, Arrays.asList(new IRegion[]{region}), monitor);
	}

	/**
	 * Processes the whole of the dataset and sets those values in bounds 
	 * to be false and those outside to be true in the mask.
	 * 
	 * Nullifies the mask if max and min are null.
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public boolean process(final Number              minNumber, 
			               final Number              maxNumber, 
			               final Collection<IRegion> regions, 
			               final IProgressMonitor    monitor) {
		
		createMaskIfNeeded();
		monitor.worked(1);
	
        // Slightly wrong Dataset loop, but it is faster...
		if (minNumber!=null || maxNumber!=null) {
			final int           as  = imageDataset.getElementsPerItem();
			if (as!=1) throw new RuntimeException("Cannot deal with mulitple elements in mask processing!");
			
			double              lo  = minNumber!=null ? minNumber.doubleValue() : Double.NaN;
			double              hi  = maxNumber!=null ? maxNumber.doubleValue() : Double.NaN;
			IndexIterator it = imageDataset.getIterator();
			for (int i = 0; it.hasNext(); i++) {
				double x = imageDataset.getElementDoubleAbs(it.index);
				boolean isValid = isValid(x, lo, hi);
				if (ignoreAlreadyMasked && isValid && !maskDataset.getAbs(i)) continue;
				maskDataset.setAbs(i, isValid);
			}
		}

		if (regions != null) {
			// Remove invalid regions first to make processing faster.
			final List<IRegion> validRegions = new ArrayList<IRegion>(regions.size());
			for (IRegion region : regions) {
				if (region == null)             continue;
				if (!isSupportedRegion(region)) continue;
				if (region.getUserObject()!=MaskRegionType.REGION_FROM_MASKING)     continue;
				validRegions.add(region);
			}

			if (validRegions.isEmpty()) return true;

			final MaskOperation op  = new MaskOperation(maskDataset, maskDataset.getSize()/16);
			
			int[] imageShape = imageDataset.getShapeRef();
			if (Boolean.getBoolean("org.dawnsci.plotting.tools.masking.no.thread.pool")) {
				MAIN_LOOP: for (int y = 0; y<imageShape[0]; ++y) {
					for (int x = 0; x<imageShape[1]; ++x) {
						for (IRegion region : validRegions) {
							monitor.worked(1);
							try {
								if (region.getCoordinateSystem().isDisposed()) break MAIN_LOOP;
								if (region.getROI().containsPoint(x, y)) {
									toggleMask(op, !region.isMaskRegion(), y, x);
								}
							} catch (Throwable ne) {
								logger.trace("Cannot process point "+(new Point(x,y)), ne);
								continue;
							}
						}
					}
				}
			} else {
				// NORMALLY
				pool.invoke(new MaskRegionsAction(op, imageShape, validRegions, monitor));
			}

			try {
				if (op.getSize()>0) operationManager.execute(op, null, null);
			} catch (ExecutionException e) {
				logger.error("Internal error processing region mask.", e);
			}   
		}
		

		return true;
	}
	
	private static int INC = 100; // The amount of columns to do for each task.
	
		
	private class MaskRegionsAction extends RecursiveAction {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -4810609286048487303L;

		protected MaskOperation    op;
		protected   int[]          shape;
		protected IProgressMonitor monitor;
		private final Collection<IRegion> regions;
		
		MaskRegionsAction(final MaskOperation op, final int[] shape, 
			                final Collection<IRegion> regions, 
			                final IProgressMonitor monitor) {
		
			this.op      = op;
			this.shape   = shape;
			this.regions = regions;
			this.monitor = monitor;
		}

		@Override
		protected void compute() {

			final Collection<RegionAction> actions = new ArrayList<RegionAction>(regions.size());

			for (IRegion region : regions) {

				final IROI    roi       = region.getROI();
				final boolean isMasking = region.isMaskRegion();
				actions.add(new RegionAction(op, shape, roi, getScreenPixelWidth(region), isMasking, monitor));
			}
			invokeAll(actions);
		}
		
        /**
         * Get pixel width in data coordinates.
         * @param region
         * @return
         */
		public double getScreenPixelWidth(IRegion region) {
			final int widPix = region.getLineWidth();
			double[] s = region.getCoordinateSystem().getValueFromPosition(new double[]{0, 0});
			double[] e = region.getCoordinateSystem().getValueFromPosition(new double[]{widPix, widPix});
			//return Math.pow((Math.pow(e[0]-s[0], 2)+Math.pow(e[1]-s[1], 2)), 0.5);
			// FIXME This is not right but works for many images that we have. 
			// Those with significantly different axis scales, it will not.
			return Math.min(e[0]-s[0], e[1]-s[1]);
		}

	}

	private class RegionAction extends MaskRegionsAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7438635693096574242L;
		
		protected boolean          isMasking;
		protected IROI             roi;
		protected double           lineWidth;

		/**
		 * 
		 * @param op
		 * @param shape
		 * @param roi
		 * @param lineWidth in data coordinates (ROI)
		 * @param isMasking
		 * @param monitor
		 */
		public RegionAction(MaskOperation op, int[] shape, IROI roi, double lineWidth,
				            boolean isMasking, IProgressMonitor monitor) {
			super(op, shape, null, monitor);
			this.isMasking = isMasking;
			this.roi = roi;
			this.lineWidth = lineWidth;
		}

		@Override
		protected void compute() {
			
			
			// We use the bounding box of the region.
			final IRectangularROI bounds = roi.getBounds();
			if (bounds == null)
				return; // unbounded region

			final double[] beg = bounds.getPoint();
			if (roi!=null && roi.getClass() == PointROI.class) {
				int x = Math.max(0, (int) Math.round(beg[0]));
				int y = Math.max(0, (int) Math.round(beg[1]));
				op.addVertex(!isMasking, y, x);
				return; // We done it innit!
			}
			
			
			final double[] end = bounds.getEndPoint();

			if (roi instanceof LinearROI) { // special case where isNearOutline is used for mask
				double distance = Math.max(0.5, lineWidth/2.);
				beg[0] -= distance;
				beg[1] -= distance;
				end[0] += distance;
				end[1] += distance;
			}

			int xStart = Math.max(0, (int) Math.round(beg[0]));
			int xEnd   = Math.min(shape[1] - 1, (int) Math.round(end[0]));
			
			int yStart = Math.max(0, (int) Math.round(beg[1]));
			int yEnd   = Math.min(shape[0] - 1, (int) Math.round(end[1]));

			final Collection<RegionAction> actions = new ArrayList<RegionAction>(yEnd/INC);
			// We loop all pixels here because looping bounds boxes of rois did not work yet.

			for (int y=yStart; y<yEnd; y+=INC) { 
				
				final int yMax = Math.min(yStart+INC, yEnd);
				actions.add(new PixelAction(op, xStart, xEnd, yStart, yMax, isMasking, roi, lineWidth, monitor));
				yStart+=INC;
				if (monitor.isCanceled()) return;
			}
			invokeAll(actions);
		}
	}
	
	/**
	 * Each PixelAction does around 100,000 of the pixels.
	 * 
	 * @author Matthew Gerring
	 */
	private class PixelAction extends RegionAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6539202270251672297L;
		
		private int xStart, xEnd;
		private int yStart, yEnd;

		public PixelAction(MaskOperation op, 
				           int xStart, int xEnd,
				           int yStart, int yEnd,
				           boolean isMasking, IROI roi, double lineWidth, 
				           IProgressMonitor monitor) {
			
			super(op, null, roi, lineWidth, isMasking, monitor);
			this.xStart  = xStart;
			this.xEnd    = xEnd;
			this.yStart  = yStart;
			this.yEnd    = yEnd;
		}

		@Override
		protected void compute() {
			if (roi instanceof LinearROI) {
				double distance = Math.max(0.5, lineWidth/2.);
				for (int y = yStart; y < yEnd; ++y) {
					if (monitor.isCanceled()) return;
					monitor.worked(1);

					for (int x = xStart; x < xEnd; ++x) {

						if (maskDataset.getBoolean(y, x) != isMasking)
							continue;
						try {
							if (roi.isNearOutline(x, y, distance)) {
								op.addVertex(!isMasking, y, x);
							}
						} catch (Throwable ne) {
							logger.trace("Cannot process point " + (new Point(x, y)), ne);
							return;
						}
					}
				}
			} else {
				row_loop:
				for (int y = yStart; y < yEnd; ++y) {
					if (monitor.isCanceled())
						return;
					monitor.worked(1);

					// use scanlines
					double[] xs = roi.findHorizontalIntersections(y);
					if (xs == null)
						continue row_loop;

					int xe = (int) xs[0];
					if (xe >= xEnd)
						continue row_loop;
					if (xs.length == 1) {
						if (xe >= xStart) {
							toggleMask(op, !isMasking, y, xe);
						}
						continue row_loop;
					}

					int i = 1;
					int xb;
					do { // find first pair where end is past the start
						xb = xe;
						if (i >= xs.length)
							continue row_loop;
						xe = (int) xs[i++];
					} while (xe < xStart);

					while (true) {
						if (xb >= xEnd) {
							continue row_loop;
						}

						if (roi.containsPoint((xb + xe)/2, y)) {
							for (int k = Math.max(xStart, xb); k <= Math.min(xEnd - 1, xe); k++) {
								toggleMask(op, !isMasking, y, k);
							}
						}

						xb = xe;
						if (i >= xs.length)
							continue row_loop;
						xe = (int) xs[i++];
					}
				}
			}
		}
	}

	private void createMaskIfNeeded() { // FIXME shape...
		if (maskDataset == null || !maskDataset.isCompatibleWith(imageDataset)) {
			maskDataset = DatasetFactory.ones(BooleanDataset.class, imageDataset.getShapeRef());
			maskDataset.setName("mask");
		}
		if (operationManager ==null)  {
			operationManager = new DefaultOperationHistory();
			operationManager.setLimit(MaskOperation.MASK_CONTEXT, 20);
		}
	}

	private static final boolean isValid(double val, double min, double max) {
		if (!Double.isNaN(min) && val<=min) return false;
		if (!Double.isNaN(max) && val>=max) return false;
		return true;
	}

	/**
	 * TODO Add more than just line, free and box - ring would be useful!
	 * @param region
	 * @return
	 */
	public boolean isSupportedRegion(IRegion region) {
		
		if (!region.isVisible())    return false;
		if (!region.isUserRegion()) return false;
		if (region.getROI()==null)  return false;
		
		return true;
	}

	public MaskMode getMaskMode() {
		return maskMode;
	}

	public void setMaskMode(MaskMode paintMode) {
		this.maskMode = paintMode;
	}
	public boolean isSquarePen() {
		return squarePen;
	}

	public void setSquarePen(boolean squarePen) {
		this.squarePen = squarePen;
	}

	public void setImageOrigin(ImageOrigin origin) {
		imageOrigin = origin;
	}

	public BooleanDataset getMaskDataset() {
		return maskDataset == null ? null : DatasetUtils.rotate90(maskDataset, imageOrigin.ordinal());
	}

	/**
	 * The booleans get filled true when this is set.
	 * @param maskDataset
	 */
	public void setMaskDataset(BooleanDataset maskDataset, boolean requireFill) {
		this.maskDataset = maskDataset;
		if (maskDataset != null && requireFill) {
			maskDataset.fill(true);
		}
	}

	public Dataset getImageDataset() {
		return imageDataset;
	}

	public void setImageDataset(Dataset imageDataset) {
		this.imageDataset = imageDataset;
	}

	public void reset() {
		if (operationManager!=null) {
			operationManager.dispose(MaskOperation.MASK_CONTEXT, true, true, true);
		}
	}

	public IOperationHistory getOperationManager() {
		return operationManager;
	}

	public void undo() {
		try {
			operationManager.undo(MaskOperation.MASK_CONTEXT, null, null);
		} catch (ExecutionException e) {
			logger.error("Internal error - unable to undo!", e);
		}
	}
	public void redo() {
		try {
			operationManager.redo(MaskOperation.MASK_CONTEXT, null, null);
		} catch (ExecutionException e) {
			logger.error("Internal error - unable to redo!", e);
		}
	}

	public void invert() {
		if (maskDataset != null) {
			maskDataset = Comparisons.logicalNot(maskDataset);
		}
	}

	public boolean isIgnoreAlreadyMasked() {
		return ignoreAlreadyMasked;
	}

	public void setIgnoreAlreadyMasked(boolean ignoreAlreadyMasked) {
		this.ignoreAlreadyMasked = ignoreAlreadyMasked;
	}

	public void setBrushThreshold(PrecisionPoint precisionPoint) {
		this.brushThreshold = precisionPoint;
	}
}
