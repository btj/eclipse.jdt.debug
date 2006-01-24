/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.launching;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/**
 * Abstract implementation of a VM install.
 * <p>
 * Clients implementing VM installs must subclass this class.
 * </p>
 */
public abstract class AbstractVMInstall implements IVMInstall, IVMInstall2, IVMInstall3 {

	private IVMInstallType fType;
	private String fId;
	private String fName;
	private File fInstallLocation;
	private LibraryLocation[] fSystemLibraryDescriptions;
	private URL fJavadocLocation;
	private String fVMArgs;
	// whether change events should be fired
	private boolean fNotify = true;
	
	private class PropertiesEventListener implements IDebugEventSetListener {
		
		private IProcess fProcess;
		private ILaunch fLaunch;
		private Object fLock;
		
		PropertiesEventListener(ILaunch launch, Object lock) {
			fLaunch = launch;
			fLock = lock;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
		 */
		public void handleDebugEvents(DebugEvent[] events) {
			for (int i = 0; i < events.length; i++) {
				DebugEvent event = events[i];
				if (event.getSource() instanceof IProcess) {
					IProcess process = (IProcess) event.getSource();
					if (fLaunch.equals(process.getLaunch())) {
						if (event.getKind() == DebugEvent.TERMINATE) {
							synchronized (fLock) {
								fProcess = process;
								fLock.notifyAll();
							}
						}
					}
				}
			}
			
		}
		
		public IProcess getProcess() {
			return fProcess;
		}
		
	}
	
	/**
	 * Constructs a new VM install.
	 * 
	 * @param	type	The type of this VM install.
	 * 					Must not be <code>null</code>
	 * @param	id		The unique identifier of this VM instance
	 * 					Must not be <code>null</code>.
	 * @throws	IllegalArgumentException	if any of the required
	 * 					parameters are <code>null</code>.
	 */
	public AbstractVMInstall(IVMInstallType type, String id) {
		if (type == null)
			throw new IllegalArgumentException(LaunchingMessages.vmInstall_assert_typeNotNull); 
		if (id == null)
			throw new IllegalArgumentException(LaunchingMessages.vmInstall_assert_idNotNull); 
		fType= type;
		fId= id;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#setName(String)
	 */
	public void setName(String name) {
		if (!name.equals(fName)) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_NAME, fName, name);
			fName= name;
			if (fNotify) {
				JavaRuntime.fireVMChanged(event);
			}
		}
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getInstallLocation()
	 */
	public File getInstallLocation() {
		return fInstallLocation;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#setInstallLocation(File)
	 */
	public void setInstallLocation(File installLocation) {
		if (!installLocation.equals(fInstallLocation)) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_INSTALL_LOCATION, fInstallLocation, installLocation);
			fInstallLocation= installLocation;
			if (fNotify) {
				JavaRuntime.fireVMChanged(event);
			}
		}
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getVMInstallType()
	 */
	public IVMInstallType getVMInstallType() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see IVMInstall#getVMRunner(String)
	 */
	public IVMRunner getVMRunner(String mode) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#getLibraryLocations()
	 */
	public LibraryLocation[] getLibraryLocations() {
		return fSystemLibraryDescriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#setLibraryLocations(org.eclipse.jdt.launching.LibraryLocation[])
	 */
	public void setLibraryLocations(LibraryLocation[] locations) {
		if (locations == fSystemLibraryDescriptions) {
			return;
		}
		LibraryLocation[] newLocations = locations;
		if (newLocations == null) {
			newLocations = getVMInstallType().getDefaultLibraryLocations(getInstallLocation()); 
		}
		LibraryLocation[] prevLocations = fSystemLibraryDescriptions;
		if (prevLocations == null) {
			prevLocations = getVMInstallType().getDefaultLibraryLocations(getInstallLocation()); 
		}
		
		if (newLocations.length == prevLocations.length) {
			int i = 0;
			boolean equal = true;
			while (i < newLocations.length && equal) {
				equal = newLocations[i].equals(prevLocations[i]);
				i++;
			}
			if (equal) {
				// no change
				return;
			}
		}

		PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_LIBRARY_LOCATIONS, prevLocations, newLocations);
		fSystemLibraryDescriptions = locations;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);		
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#getJavadocLocation()
	 */
	public URL getJavadocLocation() {
		return fJavadocLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#setJavadocLocation(java.net.URL)
	 */
	public void setJavadocLocation(URL url) {
		if (url == fJavadocLocation) {
			return;
		}
		if (url != null && fJavadocLocation != null) {
			if (url.equals(fJavadocLocation)) {
				// no change
				return;
			}
		}
		
		PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_JAVADOC_LOCATION, fJavadocLocation, url);		
		fJavadocLocation = url;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);
		}
	}

	/**
	 * Whether this VM should fire property change notifications.
	 * 
	 * @param notify
	 * @since 2.1
	 */
	protected void setNotify(boolean notify) {
		fNotify = notify;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
     * @since 2.1
	 */
	public boolean equals(Object object) {
		if (object instanceof IVMInstall) {
			IVMInstall vm = (IVMInstall)object;
			return getVMInstallType().equals(vm.getVMInstallType()) &&
				getId().equals(vm.getId());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 * @since 2.1
	 */
	public int hashCode() {
		return getVMInstallType().hashCode() + getId().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#getDefaultVMArguments()
	 * @since 3.0
	 */
	public String[] getVMArguments() {
		String args = getVMArgs();
		if (args == null) {
		    return null;
		}
		ExecutionArguments ex = new ExecutionArguments(args, ""); //$NON-NLS-1$
		return ex.getVMArgumentsArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#setDefaultVMArguments(java.lang.String[])
	 * @since 3.0
	 */
	public void setVMArguments(String[] vmArgs) {
		if (vmArgs == null) {
			setVMArgs(null);
		} else {
		    StringBuffer buf = new StringBuffer();
		    for (int i = 0; i < vmArgs.length; i++) {
	            String string = vmArgs[i];
	            buf.append(string);
	            buf.append(" "); //$NON-NLS-1$
	        }
			setVMArgs(buf.toString().trim());
		}
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.jdt.launching.IVMInstall2#getVMArgs()
     */
    public String getVMArgs() {
        return fVMArgs;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.launching.IVMInstall2#setVMArgs(java.lang.String)
     */
    public void setVMArgs(String vmArgs) {
        if (fVMArgs == null) {
            if (vmArgs == null) {
                // No change
                return;
            }
        } else if (fVMArgs.equals(vmArgs)) {
    		// No change
    		return;
    	}
        PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_VM_ARGUMENTS, fVMArgs, vmArgs);
        fVMArgs = vmArgs;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);		
		}
    }	
    
    /* (non-Javadoc)
     * Subclasses should override.
     * @see org.eclipse.jdt.launching.IVMInstall2#getJavaVersion()
     */
    public String getJavaVersion() {
        return null;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall3#evaluateSystemProperties(java.lang.String[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Map evaluateSystemProperties(String[] properties, IProgressMonitor monitor) throws CoreException {
		//locate the launching support jar - it contains the main program to run
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		Map map = new HashMap();
		File file = LaunchingPlugin.getFileInPlugin(new Path("lib/launchingsupport.jar")); //$NON-NLS-1$
		if (file.exists()) {
			String javaVersion = getJavaVersion();
			boolean hasXMLSupport = false;
			if (javaVersion != null) {
				hasXMLSupport = true;
				if (javaVersion.startsWith(JavaCore.VERSION_1_1) ||
						javaVersion.startsWith(JavaCore.VERSION_1_2) ||
						javaVersion.startsWith(JavaCore.VERSION_1_3)) {
					hasXMLSupport = false;
				}
			}
			String mainType = null;
			if (hasXMLSupport) {
				mainType = "org.eclipse.jdt.internal.launching.support.SystemProperties"; //$NON-NLS-1$
			} else {
				mainType = "org.eclipse.jdt.internal.launching.support.LegacySystemProperties"; //$NON-NLS-1$
			}
			VMRunnerConfiguration config = new VMRunnerConfiguration(mainType, new String[]{file.getAbsolutePath()});
			IVMRunner runner = getVMRunner(ILaunchManager.RUN_MODE);
			if (runner == null) {
				abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
			}
			config.setProgramArguments(properties);
			Launch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
			Object lock = new Object();
			PropertiesEventListener listener = new PropertiesEventListener(launch, lock);
			DebugPlugin.getDefault().addDebugEventListener(listener);
			if (monitor.isCanceled()) {
				return map;
			}
			monitor.beginTask(LaunchingMessages.AbstractVMInstall_1, 2);
			runner.run(config, launch, monitor);
			try {
				synchronized (lock) {
					if (listener.getProcess() == null) {
						try {
							lock.wait(JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT));
						} catch (InterruptedException e) {
						}
					}
				}
			} finally {
				if (!launch.isTerminated()) {
					launch.terminate();
				}
			}
			monitor.worked(1);
			IProcess process = listener.getProcess();
			if (process == null) {
				abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
			}
			if (monitor.isCanceled()) {
				return map;
			}
			monitor.subTask(LaunchingMessages.AbstractVMInstall_3);
			IStreamsProxy streamsProxy = process.getStreamsProxy();
			String text = null;
			if (streamsProxy != null) {
				text = streamsProxy.getOutputStreamMonitor().getContents();
			}
			if (text != null && text.length() > 0) {
				try {
					DocumentBuilder parser = LaunchingPlugin.getParser();
					Document document = parser.parse(new ByteArrayInputStream(text.getBytes()));
					Element envs = document.getDocumentElement();
					NodeList list = envs.getChildNodes();
					int length = list.getLength();
					for (int i = 0; i < length; ++i) {
						Node node = list.item(i);
						short type = node.getNodeType();
						if (type == Node.ELEMENT_NODE) {
							Element element = (Element) node;
							if (element.getNodeName().equals("property")) { //$NON-NLS-1$
								String name = element.getAttribute("name"); //$NON-NLS-1$
								String value = element.getAttribute("value"); //$NON-NLS-1$
								map.put(name, value);
							}
						}
					}			
				} catch (SAXException e) {
					abort(LaunchingMessages.AbstractVMInstall_4, e, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
				} catch (IOException e) {
					abort(LaunchingMessages.AbstractVMInstall_4, e, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
				}
			} else {
				abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
			}
			monitor.worked(1);
			monitor.done();
		}
		return map;
	}
	
	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message the status message
	 * @param exception lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code error code
	 * @throws CoreException the "abort" core exception
	 * @since 3.2
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, LaunchingPlugin
				.getUniqueIdentifier(), code, message, exception));
	}	
    
}