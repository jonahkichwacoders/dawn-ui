/*
 * Copyright (c) 2015 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.plotting.histogram.functions.classes;

public class FlagGreenTransferFunction extends AbstractTransferFunction {

	@Override
	public double getPoint(double value) {
		double y = Math.sin(value * 31.5 * Math.PI);
		y = y < 0 ? 0 : y;
		y = y > 1 ? 1 : y;
		return y;
	}
}
