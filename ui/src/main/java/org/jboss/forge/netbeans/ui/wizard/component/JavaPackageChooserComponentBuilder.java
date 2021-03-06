/* 
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    <a href="mailto:ggastald@redhat.com">George Gastaldi</a> - initial API and implementation and/or initial documentation
 */
package org.jboss.forge.netbeans.ui.wizard.component;

import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.netbeans.runtime.FurnaceService;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@ServiceProvider(position = 110, service = ComponentBuilder.class)
public class JavaPackageChooserComponentBuilder extends ComponentBuilder<JComboBox> {

    @Override
    public JComboBox build(Container container, final InputComponent<?, Object> input, final CommandController controller, final ChangeSupport changeSupport) {
        org.jboss.forge.addon.projects.Project forgeProject = Projects.getSelectedProject(FurnaceService.INSTANCE.lookup(ProjectFactory.class), controller.getContext());
        FileObject dir = null;
        if (forgeProject != null) {
            dir = FileUtil.toFileObject((File) forgeProject.getRoot().getUnderlyingResourceObject());
        }
        try {
            ComboBoxModel model;
            if (dir != null) {
                Project project = ProjectManager.getDefault().findProject(dir);
                Sources sources = ProjectUtils.getSources(project);
                // TODO: Add another combo to select source groups?
                SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                model = PackageView.createListView(groups[0]);
            } else {
                model = new DefaultComboBoxModel();
            }
            model.setSelectedItem(input.getValue());
            final JComboBox combo = new JComboBox(model);
            if (input instanceof UIInput) {
                combo.setEditable(true);
                final JTextComponent tc = (JTextComponent) combo.getEditor().getEditorComponent();
                tc.getDocument().addDocumentListener(new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        controller.setValueFor(input.getName(), tc.getText());
                        changeSupport.fireChange();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        controller.setValueFor(input.getName(), tc.getText());
                        changeSupport.fireChange();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        controller.setValueFor(input.getName(), tc.getText());
                        changeSupport.fireChange();
                    }
                });
            }
            combo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    // To prevent nullifying input's value when model is cleared
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        controller.setValueFor(input.getName(), combo.getSelectedItem());
                        changeSupport.fireChange();
                    }
                }
            });
            combo.setRenderer(PackageView.listRenderer());
            container.add(new JLabel(InputComponents.getLabelFor(input, true)));
            container.add(combo);
            return combo;
        } catch (IOException | IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected Class<String> getProducedType() {
        return String.class;
    }

    @Override
    protected String getSupportedInputType() {
        return InputType.JAVA_PACKAGE_PICKER;
    }

    @Override
    protected Class<?>[] getSupportedInputComponentTypes() {
        return new Class<?>[]{UIInput.class, UISelectOne.class};
    }

}
