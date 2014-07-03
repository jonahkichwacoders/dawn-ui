package org.dawnsci.spectrum.ui.views;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dawnsci.spectrum.ui.file.SpectrumFileManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.sda.navigator.views.FileView;

public class SpectrumFileView extends FileView {
	
	public static final String ID = "org.dawnsci.spectrum.ui.views.SpectrumFileView";
	
	private static final Logger logger = LoggerFactory.getLogger(SpectrumFileView.class);
	
	@Override
	public void openSelectedFile() {
		final Path file = getSelectedPath();
		if (file==null) return;
		
		if (!Files.isDirectory(file)) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart view = page.findView("org.dawnsci.spectrum.ui.views.SpectrumView");
			if (view==null) return;
			
			final SpectrumFileManager manager = (SpectrumFileManager)view.getAdapter(SpectrumFileManager.class);
			if (manager != null) {
				manager.addFile(file.toAbsolutePath().toString());
			} else {
				logger.error("Could not get file manager");
			}
		}
	}

}
