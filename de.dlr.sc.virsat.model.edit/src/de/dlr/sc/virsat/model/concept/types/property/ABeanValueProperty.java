/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.concept.types.property;

import de.dlr.sc.virsat.model.dvlm.categories.propertyinstances.ValuePropertyInstance;

//TODO: update
/**
 * Abstract implementation to the interface dealing with Attributes without QUDV unit
 * @author fisc_ph
 * 
 * @param <V_TYPE> Value type of the Bean
 *
 */
public abstract class ABeanValueProperty<V_TYPE> extends ABeanProperty<ValuePropertyInstance, V_TYPE> implements IBeanProperty<ValuePropertyInstance, V_TYPE> {
	
	@Override
	public boolean isSet() {
		return ti.getValue() != null;
	}
	
	@Override
	public void unset() {
		ti.setValue(null);
	}

}
