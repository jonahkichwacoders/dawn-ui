/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.spectrum.ui.processing;

import org.dawnsci.spectrum.ui.file.IContain1DData;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.Maths;

public class AdditionProcess extends AbstractCacheProcess {


	public AdditionProcess(IContain1DData add) {
		super(add);
	}
	
	@Override
	protected Dataset process(Dataset x, Dataset y) {
		Dataset y1 = DatasetUtils.convertToDataset(cachedData.getyDatasets().get(0));
		Dataset out = Maths.add(y, y1);
		out.setName(y.getName()+ "_add_"+y1.getName());
		return out;
	}

	
	@Override
	protected String getAppendingName() {
		return "+"+oCachedData.getName();
	}

}
