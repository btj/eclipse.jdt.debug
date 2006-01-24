/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.tests.refactoring;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class PullUpFieldUnitTests extends AbstractRefactoringDebugTest {

	public PullUpFieldUnitTests(String name) {
		super(name);
	}

	public void testWatchPoint() throws Exception {
		cleanTestFiles();
		
		try {
			//create Breakpoint to test
			IJavaWatchpoint wp = createWatchpoint("a.b.c.MoveeChild", "aChildInt", true, true);
			//refactor
			Refactoring ref = setupRefactor("MoveeChild","aChildInt","src","a.b.c","MoveeChild.java");
			performRefactor(ref);
			//test breakpoints
			IBreakpoint[] breakPoints = getBreakpointManager().getBreakpoints();
			assertEquals("wrong number of watchpoints", 1, breakPoints .length);
			IJavaWatchpoint watchPoint = (IJavaWatchpoint) breakPoints [0];
			assertEquals("wrong type name", "a.b.c.Movee", watchPoint.getTypeName());
			assertEquals("breakpoint attached to wrong field", "aChildInt", watchPoint.getFieldName());
			
		} catch (Exception e) {
			throw e;
		} finally {
			removeAllBreakpoints();
		}
	}//end testBreakPoint
		
	
/////////////////////////////////////////
	
	private Refactoring setupRefactor(String parentClassName, String className, String root, String targetPackageName, String cuName) throws Exception {
		IJavaProject javaProject = getJavaProject();
		ICompilationUnit cunit = getCompilationUnit(javaProject, root, targetPackageName, cuName);
		IType parentClas= cunit.getType(parentClassName);
		IField clas= parentClas.getField(className);
		
		PullUpRefactoring ref= new PullUpRefactoring(new IMember[] {clas},JavaPreferencesSettings.getCodeGenerationSettings(javaProject));
		
		ITypeHierarchy hierarchy = parentClas.newSupertypeHierarchy(new NullProgressMonitor());
		IType inheritedType[] = hierarchy.getAllSupertypes(parentClas);
		ref.setTargetClass(inheritedType[0]);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		return ref;
	}

	protected final void performRefactor(final Refactoring refactoring) throws Exception {
		CreateChangeOperation create= new CreateChangeOperation(refactoring);
		refactoring.checkFinalConditions(new NullProgressMonitor());
		PerformChangeOperation perform= new PerformChangeOperation(create);
		ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
		waitForBuild();
	}	

}