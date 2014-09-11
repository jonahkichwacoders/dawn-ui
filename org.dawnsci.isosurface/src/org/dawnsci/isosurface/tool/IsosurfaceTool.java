package org.dawnsci.isosurface.tool;

import javafx.embed.swt.FXCanvas;

import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.common.widgets.decorator.FloatDecorator;
import org.dawnsci.common.widgets.decorator.IntegerDecorator;
import org.dawnsci.isosurface.Activator;
import org.dawnsci.isosurface.impl.MarchingCubesModel;
import org.dawnsci.isosurface.impl.Surface;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.slicing.api.system.AxisChoiceEvent;
import org.eclipse.dawnsci.slicing.api.system.AxisChoiceListener;
import org.eclipse.dawnsci.slicing.api.system.AxisType;
import org.eclipse.dawnsci.slicing.api.system.DimensionalEvent;
import org.eclipse.dawnsci.slicing.api.system.DimensionalListener;
import org.eclipse.dawnsci.slicing.api.system.DimsDataList;
import org.eclipse.dawnsci.slicing.api.system.SliceSource;
import org.eclipse.dawnsci.slicing.api.tool.AbstractSlicingTool;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.processing.IOperation;
import uk.ac.diamond.scisoft.analysis.processing.IOperationService;

/**
 * 
 * @author nnb55016
 * Class for visualising isosurfaces in DAWN
 */
public class IsosurfaceTool extends AbstractSlicingTool {
	
	private static final Logger logger = LoggerFactory.getLogger(IsosurfaceTool.class);

    // Listeners
	private DimensionalListener dimensionalListener;
	private AxisChoiceListener  axisChoiceListener;
	
	// Maths
	private IOperation<MarchingCubesModel, Surface> generator;
	private IsosurfaceJob       job;
	
	// UI Stuff
	private Control   originalPlotControl;
	private FXCanvas  canvas;
	private Composite controls;
	private Scale     isovalue;
	private Text      isoText, xDim, yDim, zDim;
	
	@SuppressWarnings("unchecked")
	public IsosurfaceTool() {
		
		final IOperationService service = (IOperationService)Activator.getService(IOperationService.class);
		try {
			generator = (IOperation<MarchingCubesModel, Surface>) service.create("org.dawnsci.isosurface.marchingCubes");
		} catch (Exception e) {
			logger.error("Cannot get operation service!", e);
		}

		this.dimensionalListener = new DimensionalListener() {			
			@Override
			public void dimensionsChanged(DimensionalEvent evt) {
				update();
			}
		};

		this.axisChoiceListener = new AxisChoiceListener() {
			@Override
			public void axisChoicePerformed(AxisChoiceEvent evt) {
				update();
			}
		};
	}
	
	@Override
	public void dispose() {
		super.dispose();
        if (canvas!=null) canvas.dispose();
	}
	
	/**
	 *  Create controls for the surface in the user interface
	 */
	public void createToolComponent(Composite parent) {

		controls = new Composite(parent, SWT.NONE);
		controls.setLayout(new GridLayout(4, false));
		controls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label isovalueLabel = new Label(controls, SWT.NONE);
		isovalueLabel.setText("Isovalue");
		
		this.isovalue = new Scale(controls, SWT.NONE);		
 		isovalue.setMaximum(1000);
		isovalue.setMinimum(0);
		isovalue.setIncrement(1);
		isovalue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		this.isoText  = new Text(controls, SWT.BORDER);
		isoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Label boxLabel = new Label(controls, SWT.NONE);
		boxLabel.setText("Box Size   ");

	    this.zDim = new Text(controls, SWT.BORDER);
		zDim.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		this.xDim = new Text(controls, SWT.BORDER);
		xDim.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		this.yDim = new Text(controls, SWT.BORDER);
	    yDim.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
	    
		isovalue.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (!isOn()) return;
				int currentValue = isovalue.getSelection();
				
				MarchingCubesModel model = generator.getModel();
				double isoVal = ((model.getIsovalueMax() - model.getIsovalueMin()) / 1000.0) * currentValue + model.getIsovalueMin();
				isoText.setText(String.valueOf(isoVal));
				model.setIsovalue(isoVal);
				job.compute();
			}

		});

		isoText.addModifyListener(new ModifyListener(){

			FloatDecorator floatText = new FloatDecorator(isoText);

			@Override
			public void modifyText(ModifyEvent e) {
				if (!isOn()) return;
				double currentValue = floatText.getValue().doubleValue();

				MarchingCubesModel model = generator.getModel();
				isovalue.setSelection((int) ((currentValue-model.getIsovalueMin())*1000.0 / (model.getIsovalueMax() - model.getIsovalueMin())));
				model.setIsovalue(currentValue);
				job.compute();
			}

		});

		xDim.addModifyListener(new ModifyListener(){

			IntegerDecorator intText = new IntegerDecorator(xDim);

			@Override
			public void modifyText(ModifyEvent e) {
				if (!isOn()) return;
				double xSize =  intText.getValue().doubleValue();
				
				MarchingCubesModel model = generator.getModel();
				if (xSize > 0 && xSize < model.getLazyData().getShape()[2]){
					int[] boxSize = new int[] {(int) xSize, model.getBoxSize()[1], model.getBoxSize()[2]};
					model.setBoxSize(boxSize);
				}
			}

		});
		xDim.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				job.compute();
			}
		});

		yDim.addModifyListener(new ModifyListener(){

			IntegerDecorator intText = new IntegerDecorator(yDim);

			@Override
			public void modifyText(ModifyEvent e) {
				if (!isOn()) return;
				double ySize = intText.getValue().doubleValue();

				MarchingCubesModel model = generator.getModel();
				if(ySize > 0 && ySize < model.getLazyData().getShape()[1]){
					int[] boxSize = new int[] {model.getBoxSize()[0], (int) ySize, model.getBoxSize()[2]};
					model.setBoxSize(boxSize);
				}
			}

		});
		yDim.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				job.compute();
			}
		});

		zDim.addModifyListener(new ModifyListener() {

			IntegerDecorator intText = new IntegerDecorator(zDim);

			@Override
			public void modifyText(ModifyEvent e) {
				if (!isOn()) return;
				double zSize = intText.getValue().doubleValue();

				MarchingCubesModel model = generator.getModel();
				if(zSize > 0 && zSize < model.getLazyData().getShape()[0]){
					int[] boxSize = new int[] {model.getBoxSize()[0], model.getBoxSize()[1], (int) zSize};
					model.setBoxSize(boxSize);
				}
			}
		});
		zDim.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				job.compute();
			}
		});
		
		setControlsVisible(false);

	}

	/**
	 * Method that shows the display of the isosurface while the corresponding button is selected
	 */
	@Override
	public void militarize() {
		
	    getSlicingSystem().setSliceType(getSliceType());

		final IPlottingSystem plotSystem = getSlicingSystem().getPlottingSystem();
        
		// We make the 3D canvas and job the first time.
		if (canvas==null){
			canvas = new FXCanvas(plotSystem.getPlotComposite(), SWT.NONE);
			job    = new IsosurfaceJob("Computing isosurface", this);
		}
		originalPlotControl = plotSystem.setControl(canvas, false);
				
		setControlsVisible(true);
		
 		final DimsDataList dimsDataList = getSlicingSystem().getDimsDataList();
		if (dimsDataList!=null) dimsDataList.setThreeAxesOnly(AxisType.X, AxisType.Y, AxisType.Z);   		
		
 		getSlicingSystem().update(false);
		getSlicingSystem().addDimensionalListener(dimensionalListener);
		getSlicingSystem().addAxisChoiceListener(axisChoiceListener);
		
		update();
	}


	private void setControlsVisible(boolean vis) {
		GridUtils.setVisible(controls, vis);
		controls.getParent().layout();
	}

	/**
	 * Called to update when lazy data changed.
	 */
	private void update() {
		
		try {
			final SliceSource data = getSlicingSystem().getData();
			
			MarchingCubesModel model = generator.getModel();
			if (data.getLazySet()==model.getLazyData()) return;
			
			ILazyDataset slice = data.getLazySet().getSliceView(getSlices());
			slice = slice.squeeze();
			slice.setName("Sliced "+data.getLazySet().getName());
			if (slice.getRank()!=3) throw new RuntimeException("Invalid slice for isosurface tool!");
			if (slice==model.getLazyData()) return; // Unlikely, will be new instances
			
			job.compute(slice);
			
		} catch (Exception e) {
			logger.error("Cannot compute iso-surface!", e);
		}
	}

	/**
	 * Does nothing unless overridden.
	 */
	@Override
	public void demilitarize() {

		if (dimensionalListener!=null) {
			getSlicingSystem().removeDimensionalListener(dimensionalListener);
		}
		if (axisChoiceListener!=null) {
			getSlicingSystem().removeAxisChoiceListener(axisChoiceListener);
		}

		setControlsVisible(false);
		
		if (originalPlotControl==null) return;
        final IPlottingSystem plotSystem = getSlicingSystem().getPlottingSystem();
		plotSystem.setControl(originalPlotControl, true);
		
		originalPlotControl = null;

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enum getSliceType() {
		return PlotType.ISOSURFACE;
	}
	
	@Override
	public boolean isSliceRequired() {
		return false;
	}
	
	@Override
	public boolean isAdvancedSupported() {
		return false;
	}
	
	protected IOperation<MarchingCubesModel, Surface> getGenerator() {
		return generator;
	}

	protected FXCanvas getCanvas() {
		return canvas;
	}

	protected void updateUI() {
		
		if (!isOn()) return;
		try {
			setOn(false);
			
			MarchingCubesModel model = generator.getModel();
			isovalue.setSelection((int) ((model.getIsovalue()- model.getIsovalueMin())*1000/(model.getIsovalueMax()-model.getIsovalueMin()) ));
			isoText.setText(String.valueOf(model.getIsovalue()));
			xDim.setText(String.valueOf(model.getBoxSize()[0]));
			yDim.setText(String.valueOf(model.getBoxSize()[1]));
			zDim.setText(String.valueOf(model.getBoxSize()[2]));
			
		} finally {
			setOn(true);
		}
	}
}
