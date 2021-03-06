/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.common.widgets.gda.function.internal;

import org.dawnsci.common.widgets.gda.function.internal.JexlContentProposalListener;
import org.dawnsci.common.widgets.celleditor.TextCellEditorWithContentProposal;
import org.dawnsci.common.widgets.gda.function.FunctionTreeViewer;
import org.dawnsci.common.widgets.gda.function.descriptors.IFunctionDescriptor;
import org.dawnsci.common.widgets.gda.function.descriptors.IFunctionDescriptorProvider;
import org.dawnsci.common.widgets.gda.function.internal.model.FunctionModelElement;
import org.dawnsci.common.widgets.gda.function.internal.model.FunctionModifiedEvent;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;

public class FunctionSelectionEditingSupport extends EditingSupport implements ITextEditingSupport {
	
	private TextCellEditorWithContentProposal cellEditor;
	private FunctionTreeViewer functionTreeViewer;

	public FunctionSelectionEditingSupport(FunctionTreeViewer functionTreeViewer) {
		super(functionTreeViewer.getTreeViewer());
		this.functionTreeViewer = functionTreeViewer;
		cellEditor = new TextCellEditorWithContentProposal(this.functionTreeViewer.getTreeViewer().getTree(), null, null);
		cellEditor.setLabelProvider(new ContentProposalLabelProvider());
		cellEditor.addContentProposalListener(new JexlContentProposalListener(cellEditor.getControl()));
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof FunctionModelElement) {
			FunctionModelElement modelElement = (FunctionModelElement) element;
			return modelElement.canEdit();
		}
		return false;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof FunctionModelElement) {
			FunctionModelElement modelElement = (FunctionModelElement) element;
			return modelElement.getEditingValue();
		}
		return "";
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (!(element instanceof FunctionModelElement)) {
			return;
		}

		FunctionModelElement modelElement = (FunctionModelElement) element;
		FunctionModifiedEvent event = modelElement.setEditingValue(value.toString());

		getViewer().refresh(true);
		if (event != null) {
			IFunction afterFunction = event.getAfterFunction();
			functionTreeViewer.expandFunction(afterFunction);
		}
	}

	/**
	 * @return the cellEditor
	 */
	@Override
	public TextCellEditor getTextCellEditor() {
		return cellEditor;
	}

	public void setFunctionDesciptorProvider(
			IFunctionDescriptorProvider functionDesciptorProvider) {
		IFunctionDescriptor[] descriptors = functionDesciptorProvider
				.getFunctionDescriptors();
		IContentProposalProvider contentProposalProvider = new FunctionContentProposalProvider(
				descriptors);

		cellEditor.setContentProposalProvider(contentProposalProvider);
	}
}