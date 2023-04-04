package com.axone_io.ignition.git.web.ProjectList;

import com.inductiveautomation.ignition.gateway.localdb.persistence.FormMeta;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode;
import com.inductiveautomation.ignition.gateway.web.components.editors.IEditorSource;
import org.apache.wicket.Component;
import simpleorm.dataset.SRecordInstance;

/**
 * IEditorSource implementation for project source list.
 * @see IEditorSource
 */
public class ProjectListEditorSource implements IEditorSource{
    /**
     * Shared constant instance of this ProjectListEditorSource.
     */
    static final ProjectListEditorSource _instance = new ProjectListEditorSource();

    /**
     * Get the shared instance of this ProjectListEditorSource.
     * @return shared instance
     */
    public static ProjectListEditorSource getSharedInstance() {
        return _instance;
    }

    /**
     * Default constructor for ProjectListEditorSource. Performs no operations.
     */
    public ProjectListEditorSource() {

    }

    /**
     * Creates a new ProjectSourceEditorSource with given information.
     * @param id identifier
     * @param editMode edit mode
     * @param record record instance
     * @param formMeta form meta
     * @return created ProjectSourceEditorSource
     */
    @Override
    public Component newEditorComponent(String id, RecordEditMode editMode, SRecordInstance record,
                                        FormMeta formMeta) {
        return new ProjectSourceEditor(id, formMeta, editMode, record);
    }
}
