/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.team.ui.action.git;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import de.dlr.sc.virsat.project.editingDomain.VirSatEditingDomainRegistry;
import de.dlr.sc.virsat.project.editingDomain.VirSatTransactionalEditingDomain;
import de.dlr.sc.virsat.project.resources.VirSatProjectResource;
import de.dlr.sc.virsat.team.IVirSatVersionControlBackend;
import de.dlr.sc.virsat.team.git.VirSatGitVersionControlBackend;
import de.dlr.sc.virsat.team.ui.Activator;


@SuppressWarnings("restriction")
public class GitUpdateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);

		Set<IProject> selectedProjects = new HashSet<>();
		List<IStatus> status = new ArrayList<>();
		
		// Get all projects associated with selected resources
		for (Object object : selection.toList()) {
			if (object instanceof EObject) {
				VirSatTransactionalEditingDomain ed = VirSatEditingDomainRegistry.INSTANCE.getEd((EObject) object);
				ed.saveAll();
				IProject project = ed.getResourceSet().getProject();
				selectedProjects.add(project);
			} else if (object instanceof VirSatProjectResource) {
				// Project root object
				IProject project = ((VirSatProjectResource) object).getWrappedProject();
				VirSatTransactionalEditingDomain ed = VirSatEditingDomainRegistry.INSTANCE.getEd(project);
				ed.saveAll();
				selectedProjects.add(project);
			}
		}
		
		IVirSatVersionControlBackend gitBackend = new VirSatGitVersionControlBackend(new EGitCredentialsProvider());
		
		Job job = new Job("Virtual Satellite Git Update") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor subMonitor = SubMonitor.convert(monitor, selectedProjects.size());
				for (IProject project : selectedProjects) {
					VirSatTransactionalEditingDomain ed = VirSatEditingDomainRegistry.INSTANCE.getEd(project);
					try {
						ed.writeExclusive(() -> {
							ed.getCommandStack().flush();
							try {
								gitBackend.update(project, subMonitor);
							} catch (Exception e) {
								status.add(new Status(Status.ERROR, Activator.getPluginId(), "Error during update", e));
							}
						});
					} catch (InterruptedException e) {
						status.add(new Status(Status.ERROR, Activator.getPluginId(), "Transaction interruption during update", e));
					}
				}
				if (!status.isEmpty()) {
					MultiStatus multiStatus = new MultiStatus(Activator.getPluginId(), Status.ERROR, status.toArray(new Status[] {}), "Errors during update", status.get(0).getException());
					ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Update Error", "Error during update", multiStatus);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		
		job.setUser(true);
		job.schedule();
		
		return null;
	}

}
