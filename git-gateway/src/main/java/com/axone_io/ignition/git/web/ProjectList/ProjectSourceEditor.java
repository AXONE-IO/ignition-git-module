package com.axone_io.ignition.git.web.ProjectList;

import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.FormMeta;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode;
import com.inductiveautomation.ignition.gateway.web.components.editors.AbstractEditor;
import com.inductiveautomation.ignition.gateway.web.models.IRecordFieldComponent;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.DropDownChoice;
import simpleorm.dataset.SFieldMeta;
import simpleorm.dataset.SQuery;
import simpleorm.dataset.SRecordInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Project source editor component
 */
public class ProjectSourceEditor extends AbstractEditor {

    /**
     * Create a new project source editor with given information
     * @param id identifier
     * @param formMeta form meta
     * @param editMode edit mode
     * @param record record instance
     */
    @SuppressWarnings("unchecked")
    public ProjectSourceEditor(String id, FormMeta formMeta, RecordEditMode editMode,
                               SRecordInstance record) {
        super(id, formMeta, editMode, record);

        ProjectDropdownChoice dropdown = new ProjectDropdownChoice("editor", record, editMode);

        formMeta.installValidators(dropdown);

        dropdown.setLabel(new LenientResourceModel(formMeta.getFieldNameKey()));

        add(dropdown);

    }

    /**
     * Dropdown chooser component for project selection.
     */
    private class ProjectDropdownChoice extends DropDownChoice<String>
            implements IRecordFieldComponent {

        /**
         * Create a new dropdown chooser component for project selection.
         * @param id identifier
         * @param record record instance
         */
        @SuppressWarnings("unchecked")
        public ProjectDropdownChoice(String id, SRecordInstance record, RecordEditMode editMode) {
            super(id);

            GatewayContext context = ((IgnitionWebApp) Application.get()).getContext();

            // We manage the list of available projects.
            List<String> stores = context.getProjectManager().getProjectNames();
            SQuery<GitProjectsConfigRecord> query = new SQuery<>(GitProjectsConfigRecord.META);
            List<GitProjectsConfigRecord> results = context.getPersistenceInterface().query(query);
            GitProjectsConfigRecord gitRecord = (GitProjectsConfigRecord) record;

            // We delete the projects already used to avoid duplication.
            // The name of the selected project is not deleted in edit mode so that the selected project can be edited without being modified.
            for(GitProjectsConfigRecord p: results){
                if (!(editMode == RecordEditMode.EDIT && p.getProjectName().equals(gitRecord.getProjectName()))){
                    stores.remove(p.getProjectName());
                }
            }

            setChoices(stores);
        }

        /**
         * Get meta for dropdown chooser field.
         * @return dropdown chooser field meta
         */
        public SFieldMeta getFieldMeta() {
            return getFormMeta().getField();
        }
    }
}
