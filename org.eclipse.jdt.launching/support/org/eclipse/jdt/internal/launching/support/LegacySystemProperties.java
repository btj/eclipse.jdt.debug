/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.launching.support;


/**
 * Evaluates system properties passed as program arguments for pre 1.4 VMs.
 *
 * @since 3.2
 */
public class LegacySystemProperties {

	public static void main(String[] args) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<systemProperties>\n");    //$NON-NLS-1$
		for (int i = 0; i < args.length; i++) {
			String name = args[i];
			String value = System.getProperty(name);
			if (value != null) {
				buffer.append("<property "); //$NON-NLS-1$
				buffer.append("\n\tname= \""); //$NON-NLS-1$
				buffer.append(name);
				buffer.append("\"\n\tvalue= \""); //$NON-NLS-1$
				buffer.append(value);
				buffer.append("\"/>\n"); //$NON-NLS-1$
			}
		}
		buffer.append("</systemProperties>");  //$NON-NLS-1$
		System.out.print(buffer.toString());
	}

}
