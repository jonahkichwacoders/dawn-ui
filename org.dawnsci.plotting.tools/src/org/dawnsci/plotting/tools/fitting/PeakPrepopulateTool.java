package org.dawnsci.plotting.tools.fitting;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.dawnsci.common.widgets.decorator.IntegerDecorator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import uk.ac.diamond.scisoft.analysis.fitting.functions.APeak;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;

public class PeakPrepopulateTool extends Dialog {
	
	private Combo peakTypeCombo;
	private Composite dialogContainer;
	private Text nrPeaksTxtBox;
	private Integer nrPeaks = null;
	private Dataset[] roiLimits;
	
	private FindInitialPeaksJob findStartingPeaksJob;
	private CompositeFunction compFunction = null;
	
	private Map<String, Class <? extends APeak>> peakFnMap = new TreeMap<String, Class <? extends APeak>>();
	private String[] availPeakTypes;
	
	private FunctionFittingTool parentFittingTool;
	
	public PeakPrepopulateTool(Shell parentShell, FunctionFittingTool parentFittingTool, Dataset[] roiLimits) {
		super(parentShell);
		this.parentFittingTool = parentFittingTool;
		this.roiLimits = roiLimits;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Find Initial Peaks");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		//Create/get the base containers and set up the grid layout
		Composite windowArea = (Composite) super.createDialogArea(parent);
		dialogContainer = new Composite(windowArea, SWT.NONE);
		GridLayout toolGridLayout = new GridLayout(2, false);
		dialogContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dialogContainer.setLayout(toolGridLayout);
		
		//Number of peaks text field
		GridData nrPeaksGridData = new GridData();
		nrPeaksGridData.grabExcessHorizontalSpace = true;
		nrPeaksGridData.horizontalAlignment = GridData.FILL;
		
		Label nrPeaksTxtLbl = new Label(dialogContainer, SWT.None);
		nrPeaksTxtLbl.setText("Number of Peaks:");
		
		nrPeaksTxtBox = new Text(dialogContainer, SWT.BORDER); 
		nrPeaksTxtBox.setLayoutData(nrPeaksGridData);
		final IntegerDecorator nrPeaksIDec = new IntegerDecorator(nrPeaksTxtBox);
		nrPeaksIDec.setMinimum(0);
		
		nrPeaksTxtBox.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (!nrPeaksIDec.isError()) {
					getButton(IDialogConstants.PROCEED_ID).setEnabled(true);
					try {
						nrPeaks = Integer.parseInt(nrPeaksTxtBox.getText());
					} catch (NumberFormatException nfe) {
						// Move on.
					}
				} else {
					getButton(IDialogConstants.PROCEED_ID).setEnabled(false);
				}
				
			}
		});
		
		//Profile type combo box
		GridData peakTypeGridData = new GridData();
		peakTypeGridData.grabExcessHorizontalSpace = true;
		peakTypeGridData.horizontalAlignment = GridData.FILL;
		
		Label peakTypeCmbLbl = new Label(dialogContainer, SWT.None);
		peakTypeCmbLbl.setText("Peak Function Type:");
		
		peakTypeCombo = new Combo(dialogContainer, SWT.READ_ONLY);
		setAvailPeakFunctions();
		setDefaultPeakFunction();
		peakTypeCombo.setLayoutData(peakTypeGridData);
		
		return windowArea;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		//Create Close & Find Peaks buttons.
		createButton(parent, IDialogConstants.PROCEED_ID, "Find Peaks", true);
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId){
		if (IDialogConstants.PROCEED_ID == buttonId) {
			findInitialPeaks();
		}
		else if (IDialogConstants.CLOSE_ID == buttonId) {
			setReturnCode(OK); //This is just returning 0 - might not be needed
			close();
		}
	}
	
	private void setAvailPeakFunctions() {
		//Get the list of available function types and set default value
		
		for (final Class<? extends APeak> peak : FittingUtils.getPeakOptions().values()) {
			peakFnMap.put(peak.getSimpleName(), peak);
		}
		Set<String> availPeakTypeSet = peakFnMap.keySet();
		availPeakTypes = (String[]) availPeakTypeSet.toArray(new String[availPeakTypeSet.size()]);
		peakTypeCombo.setItems(availPeakTypes);
	}
	
	private void setDefaultPeakFunction() {
		int defaultPeakFnIndex = Arrays.asList(availPeakTypes).indexOf("PseudoVoigt") == -1 ? Arrays.asList(availPeakTypes).indexOf("Gaussian") : Arrays.asList(availPeakTypes).indexOf("PseudoVoigt");
		if (defaultPeakFnIndex != -1) {
			peakTypeCombo.select(defaultPeakFnIndex);
		}
	}
	
	private Class<? extends APeak> getProfileFunction(){
		String selectedProfileName = peakTypeCombo.getText();
		Class<? extends APeak> selectedProfile = peakFnMap.get(selectedProfileName);
		
		return selectedProfile;
	}
	
	private void findInitialPeaks() {
		if (findStartingPeaksJob == null) {
			findStartingPeaksJob = new FindInitialPeaksJob("Find Initial Peaks");
		}
		
		findStartingPeaksJob.setData(roiLimits[0], roiLimits[1]);
		findStartingPeaksJob.setNrPeaks(nrPeaks);
		findStartingPeaksJob.setPeakFunction(getProfileFunction());
		
		findStartingPeaksJob.schedule();
		
		findStartingPeaksJob.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				parentFittingTool.setInitialPeaks(compFunction);
			}
		});
	}
	
	private class FindInitialPeaksJob extends Job {

		public FindInitialPeaksJob(String name) {
			super(name);
		}
		
		Dataset x;
		Dataset y;
		Integer nrPeaks;
		Class<? extends APeak> peakFunction;
		
		
		public void setData(Dataset x, Dataset y) {
			this.x = x.clone();
			this.y = y.clone();
		}
		
		public void setNrPeaks(Integer nrPeaks) {
			this.nrPeaks = nrPeaks;
		}
		
		public void setPeakFunction(Class<? extends APeak> peakFunction) {
			this.peakFunction = peakFunction;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			compFunction = FittingUtils.getInitialPeaks(x, y, nrPeaks, peakFunction);
			return Status.OK_STATUS;
		}
		
	}
//	private Integer getNrPeaksInteger() {
//		
//		IntegerDecorator text2Integer = new IntegerDecorator(nrPeaksTxtBox);
//		
//		return nrPeaks;
//	}

}
