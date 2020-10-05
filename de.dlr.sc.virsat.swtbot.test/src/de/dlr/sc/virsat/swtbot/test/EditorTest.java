/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.swtbot.test;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;

import de.dlr.sc.virsat.model.dvlm.structural.StructuralElementInstance;
import de.dlr.sc.virsat.model.extension.ps.model.ConfigurationTree;
import de.dlr.sc.virsat.model.extension.ps.model.Document;
import de.dlr.sc.virsat.model.extension.ps.model.ElementConfiguration;
import de.dlr.sc.virsat.model.extension.tests.model.TestCategoryAllProperty;
import de.dlr.sc.virsat.project.Activator;
import de.dlr.sc.virsat.project.editingDomain.VirSatEditingDomainRegistry;
import de.dlr.sc.virsat.project.editingDomain.VirSatTransactionalEditingDomain;
import de.dlr.sc.virsat.project.structure.VirSatProjectCommons;
import de.dlr.sc.virsat.swtbot.util.SwtBotSection;

/**
 * This class tests editor operations
 * @author bell_Er
 *
 */
public class EditorTest extends ASwtBotTestCase {
	
	SWTBotTreeItem repositoryNavigatorItem;
	SWTBotTreeItem configurationTree;
	SWTBotTreeItem elementConfiguration;
	SWTBotTreeItem document;
	SWTBotTreeItem allProperty;
	
	@Before
	public void before() throws Exception {
		super.before();
		repositoryNavigatorItem = bot.tree().expandNode(SWTBOT_TEST_PROJECTNAME, "Repository");
		configurationTree = addElement(ConfigurationTree.class, conceptPs, repositoryNavigatorItem);
		elementConfiguration = addElement(ElementConfiguration.class, conceptPs, configurationTree);
		document = addElement(Document.class, conceptPs, elementConfiguration);
	}
	
	@Test
	public void closeEditorForRemovedSeiByNavigator() throws CoreException {
		openEditor(configurationTree);
		openEditor(elementConfiguration);
		openEditor(document);
		
		bot.viewByTitle("VirSat Navigator").show();
		SWTBotTreeItem elementConfigurationTreeItem = bot.tree().getTreeItem("SWTBotTestProject").getNode("Repository").getNode("CT: ConfigurationTree").getNode("EC: ElementConfiguration").select();
		elementConfigurationTreeItem.select();
	
		buildCounter.executeInterlocked(() -> {
			elementConfigurationTreeItem.contextMenu("Delete").click();
		});
		
		List<String> editorTitles = bot.editors().stream().map(SWTBotEditor::getTitle).collect(Collectors.toList());

		assertEquals("There are just two editors open", 2, editorTitles.size());
		assertThat("Repository Editor is still open", editorTitles, hasItems("Repository", "CT: ConfigurationTree -> ConfigurationTree"));
	}
	
	@Test
	public void closeEditorForRemovedSeiByWorkspaceResource() throws CoreException {
		openEditor(configurationTree);
		openEditor(elementConfiguration);
		bot.saveAllEditors();
		
		// Now remove the Editor for the Editor for the ConfigurationTree by deleting the 
		// Workspace Resources of this item and also remove it from the Repository in one got
		VirSatTransactionalEditingDomain ed = VirSatEditingDomainRegistry.INSTANCE.getEd(project);
		VirSatProjectCommons projectCommons = new VirSatProjectCommons(project);
		
		ResourcesPlugin.getWorkspace().run((monitor) -> {
			
			// Remove the SEI Folder
			StructuralElementInstance seiCT = ed.getResourceSet().getRepository().getRootEntities().get(0);
			StructuralElementInstance seiEC = seiCT.getChildren().get(0);
			projectCommons.getStructuralElemntInstanceFolder(seiCT).delete(true, monitor);
			projectCommons.getStructuralElemntInstanceFolder(seiEC).delete(true, monitor);
			
			// And remove the link from the repository File			
			try {
				// Get the file content
				File repositoryFile = new File(projectCommons.getRepositoryFile().getRawLocation().toOSString());
				String content = new String(Files.readAllBytes(repositoryFile.toPath()), StandardCharsets.UTF_8);
		
				// Create a fake root entities string as it is expected in the file and replace it by an empty one
				String replaceString = "<rootEntities href=.*\\/>";
				content = content.replaceAll(replaceString, "");
				
				// And finally write the file again
				Files.write(repositoryFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.getPluginId(),
						"SWTBotEditorTest.closeEditorForRemovedSei: Failed to fake external change on repository file.", e));
			}
			
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}, new NullProgressMonitor());
		
		buildCounter.executeInterlocked(() -> {
			List<String> editorTitles = bot.editors().stream().map(SWTBotEditor::getTitle).collect(Collectors.toList());

			assertEquals("There is just one editor open", 1, editorTitles.size());
			assertThat("Repository Editor is still open", editorTitles, hasItem("Repository"));
		});
	}
	
	@Test
	public void openEditorsforSEIsendCAsTest() {
		openEditor(configurationTree);		
		assertText(ConfigurationTree.class.getSimpleName(), bot.textWithLabel("Name"));
		openEditor(document);	
		assertText(Document.class.getSimpleName(), bot.textWithLabel("Name"));
	}
	
	@Test
	public void addAndremoveCategoriesFromSEIEditor() {
		final int EXPECTED_EC_CHILDREN = 3;
		
		openEditor(elementConfiguration);
		bot.button("Add Document").click();
		assertEquals(EXPECTED_EC_CHILDREN, elementConfiguration.getItems().length);
		
		save();
		bot.tableWithLabel(getSectionName(Document.class)).click(0, 0);
		bot.button("Remove Document").click();
		assertEquals(2, elementConfiguration.getItems().length);
	}
	
	@Test
	public void collapseSections() {
		openEditor(elementConfiguration);
		SwtBotSection documentSection = getSWTBotSection(Document.class);
		
		// Initially the section is expanded
		assertTrue(documentSection.isExpanded());
		
		// Collapse the section
		documentSection.click();
		assertFalse(documentSection.isExpanded());
		
		// The section is still collapsed after closing and reopening the editor
		bot.closeAllEditors();
		openEditor(elementConfiguration);
		assertFalse(documentSection.isExpanded());
	}
	
	@Test 
	public void editPrimitiveProperties() {
		final String NEW_NAME = "NewName";
		final String NEW_NAME_2 = "NewNewName";
		final String NEW_FLOAT = "6.976";
		final String NEW_FLOAT_2 = "8.569";
		final String NEW_INT = "35";
		final String NEW_INT_2 = "42";
		final String NEW_BOOL = "true";
		final String NEW_BOOL_2 = "false";
//		final String NEW_RESOURCE = "TODO";
//		final String NEW_RESOURCE = "TODO";
		final String NEW_ENUM = "MEDIUM=20";
		final String NEW_ENUM_2 = "HIGH=25";
		
		final int COLUMN_STRING = 1;
		final int COLUMN_INT = 2;
		final int COLUMN_FLOAT = 3;
		final int COLUMN_BOOL = 4;
		final int COLUMN_RESOURCE = 5;
		final int COLUMN_ENUM = 6;
		
		allProperty = addElement(TestCategoryAllProperty.class, conceptTest, elementConfiguration);
		// Edit from editor
		openEditor(allProperty);
		
		renameField(TestCategoryAllProperty.PROPERTY_TESTSTRING, NEW_NAME);
		renameField(TestCategoryAllProperty.PROPERTY_TESTFLOAT, NEW_FLOAT);
		renameField(TestCategoryAllProperty.PROPERTY_TESTINT, NEW_INT);
		bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTBOOL).setSelection(NEW_BOOL);
		bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTENUM).setSelection(NEW_ENUM);
		
		assertText(NEW_NAME, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTSTRING));
		assertText(NEW_FLOAT, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTFLOAT));
		assertText(NEW_INT, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTINT));
		assertText(NEW_BOOL, bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTBOOL));
		assertText(NEW_ENUM, bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTENUM));
		
		SWTBotTable allPropertyTable = getSWTBotTable(elementConfiguration, TestCategoryAllProperty.class);
		
		// change the values from the table
		setTableValue(allPropertyTable, 0, COLUMN_STRING, NEW_NAME, NEW_NAME_2);
		setTableValue(allPropertyTable, 0, COLUMN_FLOAT, NEW_FLOAT, NEW_FLOAT_2);
		setTableValue(allPropertyTable, 0, COLUMN_INT, NEW_INT, NEW_INT_2);
		allPropertyTable.doubleClick(0, COLUMN_BOOL);
		bot.ccomboBox().setSelection(NEW_BOOL_2);
//		allPropertyTable.doubleClick(0, COLUMN_ENUM);
//		bot.ccomboBox().setSelection(NEW_ENUM_2);
		
		// test the new values
		openEditor(allProperty);
		
		assertText(NEW_NAME_2, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTSTRING));
		assertText(NEW_FLOAT_2, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTFLOAT));
		assertText(NEW_INT_2, bot.textWithLabel(TestCategoryAllProperty.PROPERTY_TESTINT));
		assertText(NEW_BOOL_2, bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTBOOL));
//		assertText(NEW_ENUM_2, bot.comboBoxWithLabel(TestCategoryAllProperty.PROPERTY_TESTENUM));
	}
}
