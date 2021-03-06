/*
 * Copyright (c) 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.fileviewer.table;

import java.io.File;

import org.dawnsci.fileviewer.Utils;
import org.dawnsci.fileviewer.Utils.SortType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class RetrieveFileListJob extends Job {
	private File[] dirList;
	private File workerStateDir;
	private SortType sortType;
	private int direction;
	private String filter;
	private boolean useRegex;
	
	public RetrieveFileListJob(File workerStateDir, SortType sortType, int direction, String filter, boolean useRegex) {
		super("Retrieving file list..");
		this.workerStateDir = workerStateDir;
		this.sortType = sortType;
		this.direction = direction;
		this.filter = filter;
		this.useRegex = useRegex;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		dirList = Utils.getDirectoryList(workerStateDir, sortType, direction, filter, useRegex, monitor);
		if (dirList == null)
			return Status.CANCEL_STATUS;
		return Status.OK_STATUS;
	}

	public File[] getDirList() {
		return dirList;
	}
}