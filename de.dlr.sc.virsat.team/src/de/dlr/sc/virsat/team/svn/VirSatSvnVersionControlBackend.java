/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.team.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.file.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.file.CheckoutAsOperation;
import org.eclipse.team.svn.core.operation.file.CommitOperation;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.operation.local.NotifyProjectStatesChangedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation;
import org.eclipse.team.svn.core.operation.local.management.ShareProjectOperation.IFolderNameMapper;
import org.eclipse.team.svn.core.operation.remote.management.AddRepositoryLocationOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.resource.events.ProjectStatesChangedEvent;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

import de.dlr.sc.virsat.team.Activator;
import de.dlr.sc.virsat.team.IVirSatVersionControlBackend;

public class VirSatSvnVersionControlBackend implements IVirSatVersionControlBackend {

	@Override
	public void commit(IProject project, String message, IProgressMonitor monitor) throws Exception {
		SubMonitor commitMonitor = SubMonitor.convert(monitor, "Virtual Satellite svn commit", 1);
	
		File projectWorkingCopy = new File(FileUtility.getWorkingCopyPath(project));
		
		List<File> filesList = new ArrayList<>();
	
		// The subversive implementation sets a flag that requires the parent of a file that should be commited,
		// to be under version control as well. We check here if the parent is under version control,
		// and if so, then we can simply commit the project file. If not, we commit the commitable files under the
		// project. We do not go any deeper and let SVN itself handle the recursive search for further commitables.
		if (SVNUtility.hasSVNFolderInOrAbove(projectWorkingCopy.getParentFile())) {
			filesList.add(projectWorkingCopy);
		} else {
			IResource[] resources = FileUtility.getResourcesRecursive(project.members(), IStateFilter.SF_UNVERSIONED, 0);
			for (IResource resource : resources) {
				filesList.add(new File(FileUtility.getWorkingCopyPath(resource)));
			}
		}
		
		// Perform an add + commit operation
		File[] files = filesList.toArray(new File[0]);
		CommitOperation commitOperation = new CommitOperation(files, message, true, false);
		CompositeOperation compositeOperation = new CompositeOperation(commitOperation.getId(), commitOperation.getMessagesClass());
		// SVN requires adding files to the repository before commiting them
		compositeOperation.add(new AddToSVNOperation(files, true));
		compositeOperation.add(commitOperation);
		
		checkStatus(compositeOperation);
		
		compositeOperation.run(commitMonitor);
		
		checkStatus(compositeOperation);
		
		commitMonitor.split(1);
	}

	@Override
	public void checkout(IProjectDescription projectDescription, String remoteUri, IProgressMonitor monitor)
			throws Exception {
		SubMonitor checkoutMonitor = SubMonitor.convert(monitor, "Virtual Satellite svn checkout", 1);
		File pathRepoLocal = new File(projectDescription.getLocationURI());
		IRepositoryResource remoteRepo = SVNUtility.asRepositoryResource(remoteUri, true);

		CheckoutAsOperation checkoutAsOperation = new CheckoutAsOperation(pathRepoLocal, remoteRepo, SVNDepth.INFINITY, true, true);
		CompositeOperation compositeOperation = new CompositeOperation(checkoutAsOperation.getId(), checkoutAsOperation.getMessagesClass());
		compositeOperation.add(checkoutAsOperation);
		compositeOperation.add(new RefreshResourcesOperation(new IResourceProvider() {
			@Override
			public IResource[] getResources() {
				// Makes the SVN refresh operation update the meta information on the newly checked out project
				IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());
				return new IResource[] { newProject };
			}
		}));
		
		compositeOperation.run(checkoutMonitor);
		
		checkStatus(compositeOperation);
		
		checkoutMonitor.split(1);
	}

	@Override
	public void checkin(IProject project, String remoteUri, IProgressMonitor monitor) throws Exception {
		SubMonitor checkInMonitor = SubMonitor.convert(monitor, "Virtual Satellite svn init", 1);
		
		IRepositoryLocation remoteRepoLocation = SVNUtility.asRepositoryResource(remoteUri, true).getRepositoryLocation();
		
		// The signature requires the project to be passed as an array
		IProject[] projects = { project };
		IFolderNameMapper folderNameMapper = (p -> p.getName());
		ShareProjectOperation shareProjectOperation = new ShareProjectOperation(projects, remoteRepoLocation, 
				folderNameMapper, project.getName(), ShareProjectOperation.LAYOUT_SINGLE, false);
		CompositeOperation compositeOperation = new CompositeOperation(shareProjectOperation.getId(), shareProjectOperation.getMessagesClass());
		compositeOperation.add(new AddRepositoryLocationOperation(remoteRepoLocation));
		compositeOperation.add(shareProjectOperation);
		// Send notifications that the project is now shared
		compositeOperation.add(new NotifyProjectStatesChangedOperation(projects, ProjectStatesChangedEvent.ST_POST_SHARED));
		compositeOperation.run(checkInMonitor);
		
		checkStatus(compositeOperation);
		
		checkInMonitor.split(1);
	}

	@Override
	public void update(IProject project, IProgressMonitor monitor) throws Exception {
		SubMonitor updateMonitor = SubMonitor.convert(monitor, "Virtual Satellite svn update", 1);
		
		// The default UpdateOperation provided by SubVersive was unstable and sometimes failed.
		// The following is basically the update functionality without all the baggage.
		AbstractActionOperation updateProjectOperation = new AbstractActionOperation("Operation_UpdateProject", SVNMessages.class) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				File workingCopy = new File(FileUtility.getWorkingCopyPath(project));
				IRepositoryLocation location = SVNFileStorage.instance().asRepositoryResource(workingCopy, false).getRepositoryLocation();
				final ISVNConnector proxy = location.acquireSVNProxy();
				String[] paths = FileUtility.asPathArray(new File[] { workingCopy });
				
				try {
					proxy.update(paths, SVNRevision.HEAD, SVNDepth.INFINITY, ISVNConnector.Options.ALLOW_UNVERSIONED_OBSTRUCTIONS, new SVNProgressMonitor(this, monitor, null));
				} finally {
					location.releaseSVNProxy(proxy);
				}
			}	
		};
		
		updateProjectOperation.run(updateMonitor);
		
		checkStatus(updateProjectOperation);
		
		updateMonitor.split(1);
	}
	
	/**
	 * The SVN implementation catches all problems internally and doesn't give any notices of errors.
	 * Checks the status of the operation and gives some approriate handling
	 * either by throwing the internal exception that causes and error
	 * or by logging if the status is only a warning
	 * @param operation the operation to check
	 * @throws Exception
	 */
	protected void checkStatus(AbstractActionOperation operation) throws Exception {
		IStatus status = operation.getStatus();
		if (status.getSeverity() == IStatus.ERROR) {
			if (status instanceof Exception) {
				throw (Exception) status.getException();
			} else {
				// Handles unexpected throwables that are not exceptions 
				throw new RuntimeException(status.getException());
			}
		} else if (status.getSeverity() != IStatus.OK) {
			Activator.getDefault().getLog().log(status);
		}
	}
}

