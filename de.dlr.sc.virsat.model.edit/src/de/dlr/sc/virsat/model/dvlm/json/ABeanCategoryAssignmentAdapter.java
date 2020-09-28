/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.dvlm.json;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dlr.sc.virsat.model.concept.types.category.ABeanCategoryAssignment;

/**
 * Adapter for an ABeanCategoryAssignment that wraps it into a NotAbstractBeanCategoryAssignment,
 * so that not the whole CA bean get's un-/marshalled
 */
public class ABeanCategoryAssignmentAdapter extends XmlAdapter<NotAbstractBeanCategoryAssignment, ABeanCategoryAssignment> {

	@Override
	public ABeanCategoryAssignment unmarshal(NotAbstractBeanCategoryAssignment v) throws Exception {
		if (v == null) {
			return null;
		}
		
		return v.getBeanCa();
	}

	@Override
	public NotAbstractBeanCategoryAssignment marshal(ABeanCategoryAssignment v) throws Exception {
		if (v == null) {
			return null;
		}
		
		return new NotAbstractBeanCategoryAssignment(v);
	}

}
