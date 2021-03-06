package org.dawnsci.surfacescatter.ui;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.surfacescatter.DataModel;
import org.dawnsci.surfacescatter.DummyProcessingClass;
import org.dawnsci.surfacescatter.ExampleModel;
import org.dawnsci.surfacescatter.GeometricParametersModel;
import org.dawnsci.surfacescatter.OverlapUIModel;
import org.dawnsci.surfacescatter.ui.GeometricParametersWindows;
import org.dawnsci.surfacescatter.PlotSystem2DataSetter;
import org.dawnsci.surfacescatter.PlotSystemCompositeDataSetter;
import org.dawnsci.surfacescatter.SuperModel;
import org.dawnsci.surfacescatter.VerticalHorizontalSlices;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.january.dataset.AggregateDataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class GeneralOverlapHandlerView extends Dialog {
	
	private Shell parentShell;
	private SashForm right; 
	private SashForm left;
	private ArrayList<IDataset> xArrayList;
    private ArrayList<IDataset> yArrayList;
    private ArrayList<IDataset> yArrayListError;
    private ArrayList<IDataset> yArrayListFhkl;
    private ArrayList<IDataset> yArrayListFhklError;
    private Button export;
    private SurfaceScatterPresenter ssp;
    private IPlottingSystem<Composite> parentPs;
    private StitchedOverlapCurves stitchedCurves;
	
	public GeneralOverlapHandlerView(Shell parentShell, int style, 
			ArrayList<IDataset> xArrayList,
			ArrayList<IDataset> yArrayList,
			ArrayList<IDataset> yArrayListError,
			ArrayList<IDataset> yArrayListFhkl,
			ArrayList<IDataset> yArrayListFhklError,
			IPlottingSystem<Composite> parentPs,
			SurfaceScatterPresenter ssp){
		
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		this.parentShell = parentShell;
		this.xArrayList = xArrayList;
		this.yArrayList = yArrayList;
	    this.yArrayListError = yArrayListError;
	    this.yArrayListFhkl = yArrayListFhkl;
	    this.yArrayListFhklError = yArrayListFhklError;
	    this.parentPs = parentPs;
	    this.ssp = ssp;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		final Composite container = (Composite) super.createDialogArea(parent);
		
		OverlapUIModel model = new OverlapUIModel();
		
		SashForm sashForm= new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		left = new SashForm(sashForm, SWT.VERTICAL);
		
		
		right = new SashForm(sashForm, SWT.VERTICAL);
		
		sashForm.setWeights(new int[]{50,50});
		
		/////////////////Left SashForm///////////////////////////////////////////////////
		Group topImage = new Group(left, SWT.NONE);
		topImage.setText("Top Image");
		GridLayout topImageLayout = new GridLayout();
		topImage.setLayout(topImageLayout);
		GridData topImageData= new GridData(SWT.FILL, SWT.FILL, true, true);
		topImage.setLayoutData(topImageData);
		
		GridData ld1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		
		OverlapCurves customComposite = new OverlapCurves(topImage, 
														  SWT.NONE, 
														  yArrayList, 
														  xArrayList,
//														  filepaths,
														  "Title", 
														  model);
		customComposite.setLayout(new GridLayout());
		customComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//////////////////////////////////////////////////////////////////////////////

		///////////////////////////////////////////////////////////////////////////////
		/////////////////Right sashform////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////
		
		
		stitchedCurves = new StitchedOverlapCurves(right, 
												   SWT.NONE, 
												   xArrayList,
												   yArrayList,
												   yArrayListError,
												   yArrayListFhkl,
												   yArrayListFhklError,
												   "Overlap Test", 
												   model,
												   ssp);
		
		stitchedCurves.setLayout(new GridLayout());
		stitchedCurves.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		export = new Button(right, SWT.PUSH);
		export.setLayoutData (new GridData(GridData.FILL_HORIZONTAL));
		export.setText("Export Curve");
		export.setSize(export.computeSize(100, 20, true));
		
		right.setWeights(new int[] {90,10});
		
		//////////////////////////////////////////////////////////////////////////////////
		
		
		export.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				IDataset xData = stitchedCurves.getLineTrace1().getXData();
				IDataset yData = stitchedCurves.getLineTrace1().getYData();
				
				ssp.export(parentPs, xData, yData);
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
    
			    
	    return container;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("ExampleDialog");
	}

	@Override
	protected Point getInitialSize() {
		Rectangle rect = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell().getBounds();
		int h = rect.height;
		int w = rect.width;
		
		return new Point((int) Math.round(0.6*w), (int) Math.round(0.8*h));
	}
	
	@Override
	  protected boolean isResizable() {
	    return true;
	}

	
	public Button getExport(){
		return export;
	}
}
