/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.ext.core.test;

// *****************************************************************
// * Import Statements
// *****************************************************************

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.dlr.sc.virsat.model.ext.core.core.CorePackageTest;
import de.dlr.sc.virsat.model.ext.core.core.impl.GenericCategoryTest;
import de.dlr.sc.virsat.model.ext.core.core.util.CoreResourceFactoryImplTest;
import de.dlr.sc.virsat.model.ext.core.validator.StructuralElementInstanceValidatorTest;
import junit.framework.JUnit4TestAdapter;


/**
 * 
 */
@RunWith(Suite.class)

@SuiteClasses({StructuralElementInstanceValidatorTest.class,
	CorePackageTest.class,
	GenericCategoryTest.class,
	CoreResourceFactoryImplTest.class,
	de.dlr.sc.virsat.model.ext.core.model.GenericCategoryTest.class
	})

/**
 * 
 * Test Collection
 *
 */
public class AllTests {

	/**
	 * Constructor
	 */
	private AllTests() {
	}
	
	/**
	 * Test Adapter
	 * @return Executable JUnit Tests
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}	
}
