/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.sourcelookup;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.NewVariableEntryDialog;
import org.eclipse.jdt.launching.sourcelookup.containers.ClasspathVariableSourceContainer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

/**
 * Used to choose a classpath variable.
 * 
 * @since 3.0
 */
public class ClasspathVariableSourceContainerBrowser extends AbstractSourceContainerBrowser {
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser#createSourceContainers(org.eclipse.swt.widgets.Shell, org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		NewVariableEntryDialog dialog= new NewVariableEntryDialog(shell);
		dialog.setTitle(SourceLookupMessages.getString("ClasspathVariableSourceContainerBrowser.0")); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {			
			IPath[] paths= dialog.getResult();
			ISourceContainer[] containers = new ISourceContainer[paths.length];
			for (int i = 0; i < containers.length; i++) {
				containers[i] = new ClasspathVariableSourceContainer(paths[i]);
			}
			return containers;
		}
		return new ISourceContainer[0];
	}
}
