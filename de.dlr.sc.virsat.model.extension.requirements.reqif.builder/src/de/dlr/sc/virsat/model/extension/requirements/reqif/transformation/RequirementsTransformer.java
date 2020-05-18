/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/**
 * 
 */
package de.dlr.sc.virsat.model.extension.requirements.reqif.transformation;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.rmf.reqif10.ReqIF;

import de.dlr.sc.virsat.model.extension.requirements.model.RequirementsSpecification;
import de.dlr.sc.virsat.model.extension.requirements.reqif.transformation.command.InitializeReqIFCommand;
import de.dlr.sc.virsat.model.extension.requirements.reqif.transformation.command.UpdateReqIFContentCommand;

/**
 * @author fran_tb
 *
 */
public class RequirementsTransformer {

	protected final TransactionalEditingDomain editingDomain;

	/**
	 * Create a transformer object
	 * 
	 * @param editingDomain
	 *            the edidting domain
	 */
	public RequirementsTransformer(TransactionalEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	/**
	 * Create a new reqif file for a specification
	 * 
	 * @param resource
	 *            the new resource
	 * @param specification
	 *            the specification as concept
	 */
	public void createReqIFModel(Resource resource, RequirementsSpecification specification) {

		Command initCommand = new InitializeReqIFCommand(editingDomain, resource, specification);
		editingDomain.getCommandStack().execute(initCommand);
		updateReqIFModel(resource, specification);
	}

	/**
	 * Update an existing reqif file for a specification
	 * 
	 * @param resource
	 *            the reqif resource
	 * @param specification
	 *            the specification as concept
	 */
	public void updateReqIFModel(Resource resource, RequirementsSpecification specification) {

		for (EObject root : resource.getContents()) {
			if (root instanceof ReqIF) {
				Command updateCommand = new UpdateReqIFContentCommand(editingDomain, ((ReqIF) root).getCoreContent(),
						specification);
				editingDomain.getCommandStack().execute(updateCommand);
			}
		}
		try {
			resource.save(Collections.EMPTY_MAP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
