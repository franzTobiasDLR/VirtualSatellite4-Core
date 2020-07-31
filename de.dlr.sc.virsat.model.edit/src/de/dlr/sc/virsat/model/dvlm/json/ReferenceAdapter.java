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

import org.eclipse.emf.ecore.resource.ResourceSet;

import de.dlr.sc.virsat.model.concept.types.ABeanObject;
import de.dlr.sc.virsat.model.concept.types.factory.BeanTypeInstanceFactory;
import de.dlr.sc.virsat.model.dvlm.categories.ATypeInstance;

@SuppressWarnings("rawtypes")
/**
 * Adapter for a referenced ABeanObject from/to a UUID
 * that uses the TypeInstanceAdapter
 */
public class ReferenceAdapter extends XmlAdapter<String, ABeanObject> {

	private ResourceSet resourceSet;
	
	public ReferenceAdapter() { };
	
	public ReferenceAdapter(ResourceSet resourceSet) {
		this.resourceSet = resourceSet;
	}
	
	@Override
	public ABeanObject unmarshal(String uuid) throws Exception {
		// Get the type instance from the uuid
		TypeInstanceAdapter typeInstanceAdapter = new TypeInstanceAdapter(resourceSet);
		ATypeInstance object = typeInstanceAdapter.unmarshal(uuid);
		
		return (ABeanObject) new BeanTypeInstanceFactory().getInstanceFor(object);
	}

	@Override
	public String marshal(ABeanObject v) throws Exception {
		return v.getUuid();
	}

}
