/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.extension.funcelectrical.ui.diagram.features.interfaceends;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

import de.dlr.sc.virsat.graphiti.ui.diagram.feature.VirSatChangeColorFeature;
import de.dlr.sc.virsat.model.extension.funcelectrical.model.InterfaceEnd;

/**
 * Changes the color of an interface end
 * @author muel_s8
 *
 */

public class InterfaceEndChangeColorFeature extends VirSatChangeColorFeature {

	/**
	 * Standard constructor
	 * @param fp the feature provider
	 */
	
	public InterfaceEndChangeColorFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	@Override
	public void execute(ICustomContext context) {
		
		// Delegate the context on the parent shape for interface ends (which is an invisible rectangle)
		// to the rectangle child shape (which is an actually colored rectangle)
		PictogramElement[] pes = context.getPictogramElements();
		PictogramElement[] newPes = new PictogramElement[pes.length];
		
		for (int i = 0; i < pes.length; ++i) {
			if (getBusinessObjectForPictogramElement(pes[i]) instanceof InterfaceEnd) {
				ContainerShape cs = (ContainerShape) pes[i];
				newPes[i] = cs.getChildren().get(0);
			} else {
				newPes[i] = pes[i];
			}
		}
		
		CustomContext newContext = new CustomContext(newPes);
		newContext.setInnerGraphicsAlgorithm(context.getInnerGraphicsAlgorithm());
		newContext.setX(context.getX());
		newContext.setY(context.getY());
		super.execute(newContext);
	}

}
