/*
 * Copyright (c) 2013-2014, Neuro4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neuro4j.studio.debug.ui.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.neuro4j.studio.debug.core.launching.IPDAConstants;

/**
 * Tab to specify the PDA program to run/debug.
 */
public class DebugConfigMainTab extends AbstractLaunchConfigurationTab {

    private Text fProgramText;
    private Button fProgramButton;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        GridLayout topLayout = new GridLayout();
        topLayout.verticalSpacing = 0;
        topLayout.numColumns = 3;
        comp.setLayout(topLayout);
        comp.setFont(font);

        createVerticalSpacer(comp, 3);

        Label programLabel = new Label(comp, SWT.NONE);
        programLabel.setText("&Program:");
        GridData gd = new GridData(GridData.BEGINNING);
        programLabel.setLayoutData(gd);
        programLabel.setFont(font);

        fProgramText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fProgramText.setLayoutData(gd);
        fProgramText.setFont(font);
        fProgramText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
            }
        });

        fProgramButton = createPushButton(comp, "&Browse...", null); //$NON-NLS-1$
        fProgramButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                browsePDAFiles();
            }
        });
    }

    /**
     * Open a resource chooser to select a PDA program
     */
    protected void browsePDAFiles() {
        ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
        dialog.setTitle("PDA Program");
        dialog.setMessage("Select PDA Program");
        // TODO: single select
        if (dialog.open() == Window.OK) {
            Object[] files = dialog.getResult();
            IFile file = (IFile) files[0];
            fProgramText.setText(file.getFullPath().toString());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            String program = configuration.getAttribute(IPDAConstants.ATTR_PDA_PROGRAM, (String) null);
            if (program != null) {
                fProgramText.setText(program);
            }
        } catch (CoreException e) {
            setErrorMessage(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String program = fProgramText.getText().trim();
        if (program.length() == 0) {
            program = null;
        }
        configuration.setAttribute(IPDAConstants.ATTR_PDA_PROGRAM, program);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Main";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public boolean isValid(ILaunchConfiguration launchConfig) {
        String text = fProgramText.getText();
        if (text.length() > 0) {
            IPath path = new Path(text);
            if (ResourcesPlugin.getWorkspace().getRoot().findMember(path) == null) {
                setErrorMessage("Specified program does not exist");
                return false;
            }
        } else {
            setMessage("Specify a program");
        }
        return super.isValid(launchConfig);
    }
}
