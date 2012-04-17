package org.dawb.workbench.plotting.system.swtxy.selection;

import org.csstudio.swt.xygraph.figures.Axis;
import org.dawb.workbench.plotting.system.swtxy.util.RotatablePolygonShape;
import org.dawb.workbench.plotting.system.swtxy.util.RotatableRectangle;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.swt.graphics.Color;

public class RectangularHandle extends SelectionHandle {

	/**
	 * @param xAxis
	 * @param yAxis
	 * @param colour
	 * @param parent
	 * @param side
	 * @param params (first corner's x and y, also centre of rotation)
	 */
	public RectangularHandle(Axis xAxis, Axis yAxis, Color colour, Figure parent, int side, double... params) {
		super(xAxis, yAxis, colour, parent, side, params);
	}

	@Override
	public Shape createHandleShape(Figure parent, int side, double[] params) {
		double angle;
		if (!(parent instanceof RotatablePolygonShape)) {
			angle = 0;
		} else {
			RotatablePolygonShape pg = (RotatablePolygonShape) parent;
			angle = pg.getAngleDegrees();
		}
		location = new PrecisionPoint(params[0], params[1]);
		return new RotatableRectangle(location.x(), location.y(), side, side, angle);
	}
}
