package org.eclipse.jdt.internal.debug.ui.launcher;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.internal.debug.ui.IHelpContextIds;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.SWTUtil;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.LibraryLocation;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.viewers.CheckStateChangedEvent;import org.eclipse.jface.viewers.CheckboxTableViewer;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.DoubleClickEvent;import org.eclipse.jface.viewers.ICheckStateListener;import org.eclipse.jface.viewers.IDoubleClickListener;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.actions.WorkspaceModifyOperation;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;/* * The page for setting the default Java runtime preference. */public class VMPreferencePage extends PreferencePage implements IWorkbenchPreferencePage,																	IAddVMDialogRequestor {		private CheckboxTableViewer fVMList;	private Button fAddButton;	private Button fRemoveButton;	private Button fEditButton;		protected Text fJreLib;	protected Text fJreSource;	protected Text fPkgRoot;		private IVMInstallType[] fVMTypes;	private List fVMStandins;	private List fRemovedVMs;		private IPath[] fClasspathVariables= new IPath[3];	public VMPreferencePage() {		super();		setDescription(LauncherMessages.getString("vmPreferencePage.message")); //$NON-NLS-1$	}	/**	 * @see IWorkbenchPreferencePage#init(IWorkbench)	 */	public void init(IWorkbench workbench) {	}		private List createFakeVMInstalls(IVMInstallType[] vmTypes) {		ArrayList vms= new ArrayList();		for (int i= 0; i < vmTypes.length; i++) {			IVMInstall[] vmInstalls= vmTypes[i].getVMInstalls();			for (int j= 0; j < vmInstalls.length; j++) 				vms.add(new VMStandin(vmInstalls[j]));		}		return vms;	}		private void initDefaultVM(List fakeVMs) {		IVMInstall realDefault= JavaRuntime.getDefaultVMInstall();		if (realDefault != null) {			Iterator iter= fakeVMs.iterator();			while (iter.hasNext()) {				IVMInstall fakeVM= (IVMInstall)iter.next();				if (isSameVM(fakeVM, realDefault)) {					setDefaultVM(fakeVM);					break;				}			}		}	}		/*	 * @see PreferencePage#createContents(Composite)	 */	protected Control createContents(Composite ancestor) {		fVMTypes= JavaRuntime.getVMInstallTypes();		fVMStandins= createFakeVMInstalls(fVMTypes);		fRemovedVMs= new ArrayList();		noDefaultAndApplyButton();				Composite parent= new Composite(ancestor, SWT.NULL);		GridLayout layout= new GridLayout();		layout.numColumns= 2;		parent.setLayout(layout);						Table table= new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);				GridData data= new GridData(GridData.FILL_BOTH);		data.widthHint= convertWidthInCharsToPixels(80);		data.heightHint= convertHeightInCharsToPixels(10);		table.setLayoutData(data);						table.setHeaderVisible(true);		table.setLinesVisible(true);				TableLayout tableLayout= new TableLayout();		table.setLayout(tableLayout);		TableColumn column1= new TableColumn(table, SWT.NULL);		column1.setText(LauncherMessages.getString("vmPreferencePage.jreType")); //$NON-NLS-1$			TableColumn column2= new TableColumn(table, SWT.NULL);		column2.setText(LauncherMessages.getString("vmPreferencePage.jreName")); //$NON-NLS-1$				TableColumn column3= new TableColumn(table, SWT.NULL);		column3.setText(LauncherMessages.getString("vmPreferencePage.jreLocation")); //$NON-NLS-1$				tableLayout.addColumnData(new ColumnWeightData(30));		tableLayout.addColumnData(new ColumnWeightData(30));		tableLayout.addColumnData(new ColumnWeightData(50));				fVMList= new CheckboxTableViewer(table);				fVMList.setSorter(new ViewerSorter() {			public int compare(Viewer viewer, Object e1, Object e2) {				if ((e1 instanceof IVMInstall) && (e2 instanceof IVMInstall)) {					IVMInstall left= (IVMInstall)e1;					IVMInstall right= (IVMInstall)e2;					String leftType= left.getVMInstallType().getName();					String rightType= right.getVMInstallType().getName();					int res= leftType.compareToIgnoreCase(rightType);					if (res != 0)						return res;					return left.getName().compareToIgnoreCase(right.getName());				}				return super.compare(viewer, e1, e2);			}						public boolean isSorterProperty(Object element, String property) {				return true;			}		});							fVMList.setLabelProvider(new VMLabelProvider());		fVMList.setContentProvider(new ListContentProvider(fVMList, fVMStandins));				fVMList.addSelectionChangedListener(new ISelectionChangedListener() {			public void selectionChanged(SelectionChangedEvent evt) {				enableButtons();			}		});				fVMList.addCheckStateListener(new ICheckStateListener() {			public void checkStateChanged(CheckStateChangedEvent event) {				IVMInstall vm=  (IVMInstall)event.getElement();				if (event.getChecked())					setDefaultVM(vm);				fVMList.setCheckedElements(new Object[] { vm });			}		});				fVMList.addDoubleClickListener(new IDoubleClickListener() {			public void doubleClick(DoubleClickEvent e) {				editVM();			}		});				Composite buttons= new Composite(parent, SWT.NULL);		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));		layout= new GridLayout();		layout.marginHeight= 0;		layout.marginWidth= 0;		buttons.setLayout(layout);				fAddButton= new Button(buttons, SWT.PUSH);		fAddButton.setLayoutData(getButtonGridData(fAddButton));		fAddButton.setText(LauncherMessages.getString("vmPreferencePage.add")); //$NON-NLS-1$		fAddButton.addListener(SWT.Selection, new Listener() {			public void handleEvent(Event evt) {				addVM();			}		});				fEditButton= new Button(buttons, SWT.PUSH);		fEditButton.setLayoutData(getButtonGridData(fEditButton));		fEditButton.setText(LauncherMessages.getString("vmPreferencePage.edit")); //$NON-NLS-1$		fEditButton.addListener(SWT.Selection, new Listener() {			public void handleEvent(Event evt) {				editVM();			}		});				fRemoveButton= new Button(buttons, SWT.PUSH);		fRemoveButton.setLayoutData(getButtonGridData(fRemoveButton));		fRemoveButton.setText(LauncherMessages.getString("vmPreferencePage.remove")); //$NON-NLS-1$		fRemoveButton.addListener(SWT.Selection, new Listener() {			public void handleEvent(Event evt) {				removeVMs();			}		});				Composite jreVarsContainer= new Composite(parent, SWT.NULL);		jreVarsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));		GridLayout jreLayout= new GridLayout();		jreLayout.numColumns= 2;		jreVarsContainer.setLayout(jreLayout);								Label l= new Label(jreVarsContainer, SWT.NULL);		l.setText(JavaRuntime.JRELIB_VARIABLE);		l.setLayoutData(new GridData());				fJreLib= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fJreLib.setLayoutData(new GridData(data.FILL_HORIZONTAL));		 		l= new Label(jreVarsContainer, SWT.NULL);		l.setText(JavaRuntime.JRESRC_VARIABLE);		l.setLayoutData(new GridData());				fJreSource= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fJreSource.setLayoutData(new GridData(data.FILL_HORIZONTAL));		l= new Label(jreVarsContainer, SWT.NULL);		l.setText(JavaRuntime.JRESRCROOT_VARIABLE);		l.setLayoutData(new GridData());				fPkgRoot= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fPkgRoot.setLayoutData(new GridData(data.FILL_HORIZONTAL));				fVMList.setInput(JavaRuntime.getVMInstallTypes());		initDefaultVM(fVMStandins);		enableButtons();		WorkbenchHelp.setHelp(parent, new DialogPageContextComputer(this, IHelpContextIds.JRE_PREFERENCE_PAGE));				return parent;	}		private GridData getButtonGridData(Button button) {		GridData gd= new GridData(GridData.FILL_HORIZONTAL);		gd.widthHint= SWTUtil.getButtonWidthHint(button);		gd.heightHint= SWTUtil.getButtonHeigthHint(button);		return gd;	}				/**	 * @see IAddVMDialogRequestor#isDuplicateName(IVMInstallType, String)	 */	public boolean isDuplicateName(IVMInstallType type, String name) {		for (int i= 0; i < fVMStandins.size(); i++) {			IVMInstall vm= (IVMInstall)fVMStandins.get(i);			if (vm.getVMInstallType() == type) {				if (vm.getName().equals(name))					return true;			}		}		return false;	}				private void addVM() {		AddVMDialog dialog= new AddVMDialog(this, getShell(), fVMTypes, null);		dialog.setTitle(LauncherMessages.getString("vmPreferencePage.addJRE.title")); //$NON-NLS-1$		if (dialog.open() != dialog.OK)			return;		fVMList.refresh();		updateJREVariables(getCurrentDefaultVM());	}		/**	 * @see IAddVMDialogRequestor#vmAdded(IVMInstall)	 */	public void vmAdded(IVMInstall vm) {		fVMStandins.add(vm);		fVMList.refresh();		if (getCurrentDefaultVM() == null)			setDefaultVM(vm);	}		private void removeVMs() {		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();		Iterator elements= selection.iterator();		while (elements.hasNext()) {			Object o= elements.next();			fRemovedVMs.add(o);			fVMStandins.remove(o);		}		fVMList.refresh();		// this is order dependent. Must first refresh to work with 		// the new state of affairs		if (getCurrentDefaultVM() == null) {			if (fVMList.getTable().getItemCount() > 0) {				setDefaultVM((IVMInstall)fVMList.getElementAt(0));			}		}	}			// editing	private void editVM() {		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();		// assume it's length one, otherwise this will not be called		IVMInstall vm= (IVMInstall)selection.getFirstElement();		editVM(vm);	}		private void editVM(IVMInstall vm) {		AddVMDialog dialog= new AddVMDialog(this, getShell(), fVMTypes, vm);		dialog.setTitle(LauncherMessages.getString("vmPreferencePage.editJRE.title")); //$NON-NLS-1$		if (dialog.open() != dialog.OK)			return;		fVMList.refresh(vm);		if (isSameVM(getCurrentDefaultVM(), vm))			updateJREVariables(vm);	}		private void updateJREVariables(IVMInstall defaultVM) {		String jreLib= ""; //$NON-NLS-1$		String jreSrc= ""; //$NON-NLS-1$		String pkgPath= ""; //$NON-NLS-1$		if (defaultVM != null) {			LibraryLocation location= JavaRuntime.getLibraryLocation(defaultVM);			jreLib= location.getSystemLibraryPath().toOSString();			jreSrc= location.getSystemLibrarySourcePath().toOSString();			pkgPath= location.getPackageRootPath().toString();		}		fJreLib.setText(jreLib);		fJreSource.setText(jreSrc);		fPkgRoot.setText(pkgPath);	}			private boolean isSameVM(IVMInstall left, IVMInstall right) {		if (left == right)			return true;		if (left != null && right != null)			return left.getId().equals(right.getId());		return false;	}	/**	 * @see IPreferencePage#performOk()	 */	public boolean performOk() {		try {			commitVMInstalls();			JavaRuntime.saveVMConfiguration();		} catch (CoreException e) {			ExceptionHandler.handle(e, LauncherMessages.getString("vmPreferencePage.error.title"), LauncherMessages.getString("vmPreferencePage.error.exception")); //$NON-NLS-2$ //$NON-NLS-1$		}		return super.performOk();	}		private void commitVMInstalls() {		for (int i= 0; i < fRemovedVMs.size(); i++) {			VMStandin standin= (VMStandin)fRemovedVMs.get(i);			standin.getVMInstallType().disposeVMInstall(standin.getId());		}				for (int i= 0; i < fVMStandins.size(); i++) {			VMStandin standin= (VMStandin)fVMStandins.get(i);			standin.convertToRealVM();		}				IVMInstall fakeDefault= getCurrentDefaultVM();		if (fakeDefault != null) {			IVMInstallType defaultType= fakeDefault.getVMInstallType();			IVMInstall realDefault= defaultType.findVMInstall(fakeDefault.getId());			updateDefaultVMInstall(realDefault);		}	}		private void updateDefaultVMInstall(final IVMInstall newDefault) {		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());		try {			dialog.run(true, true, new WorkspaceModifyOperation() {				public void execute(IProgressMonitor monitor) throws InvocationTargetException{					try {						JavaRuntime.setDefaultVMInstall(newDefault, monitor);					} catch (CoreException e) {						throw new InvocationTargetException(e);					}				}			});		} catch (InterruptedException e) {			// opearation canceled by user		} catch (InvocationTargetException e) {			ExceptionHandler.handle(e, getShell(), LauncherMessages.getString("VMPreferencePage.Installed_JREs_1"), LauncherMessages.getString("VMPreferencePage.Could_not_set_classpath_variables._2")); //$NON-NLS-2$ //$NON-NLS-1$		}	}				private IVMInstall getCurrentDefaultVM() {		Object[] checked= fVMList.getCheckedElements();		if (checked.length > 0)			return (IVMInstall)checked[0];		return null;	}		private void vmSelectionChanged() {		enableButtons();	}	private void enableButtons() {		fAddButton.setEnabled(fVMTypes.length > 0);		int selectionCount= ((IStructuredSelection)fVMList.getSelection()).size();		fEditButton.setEnabled(selectionCount == 1);		fRemoveButton.setEnabled(selectionCount > 0 && selectionCount < fVMList.getTable().getItemCount());	}		private void setDefaultVM(IVMInstall vm) {		if (vm != null) {			fVMList.setCheckedElements(new Object[] { vm });		} else {			fVMList.setCheckedElements(new Object[0]);		}		updateJREVariables(vm);	}		/**	 * @see IDialogPage#setVisible(boolean)	 */	public void setVisible(boolean visible) {		super.setVisible(visible);		if (visible)			setTitle(LauncherMessages.getString("vmPreferencePage.title")); //$NON-NLS-1$	}}