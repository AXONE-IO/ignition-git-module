package com.axone_io.ignition.git.web;

import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode;
import com.inductiveautomation.ignition.gateway.web.models.CompoundRecordModel;
import com.inductiveautomation.ignition.gateway.web.models.RecordTypeNameModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import simpleorm.dataset.SQuery;

import static com.axone_io.ignition.git.records.GitReposUsersRecord.IgnitionUser;
import static com.axone_io.ignition.git.records.GitReposUsersRecord.ProjectId;

public class GitReposUsersEditPage extends RecordEditForm {
    protected RecordEditMode mode;

    public GitReposUsersEditPage(IConfigPage configPage, GitReposUsersPage returnPanel, GitReposUsersRecord record) {
        super(configPage, returnPanel, new RecordTypeNameModel(GitReposUsersRecord.META, "RecordActionTable." + (
                record.isNewRow() ? "New" : "Edit") + "RecordAction.PanelTitle"), new CompoundRecordModel(new GitReposUsersRecord[]{record}));

        mode = record.isNewRow() ? RecordEditMode.ADD : RecordEditMode.EDIT;
        // IgnitionUser.addValidator(new UniqueUsernameValidator());
    }

    protected String getIgnitionUser() {
        return ((GitReposUsersRecord) getDefaultModelObject()).getIgnitionUser();
    }
    protected int getProjectId() {
        return ((GitReposUsersRecord) getDefaultModelObject()).getProjectId();
    }

    private class UniqueUsernameValidator implements IValidator<String> {
        private RecordEditMode mode;
        private String original;

        private int projectId;

        public UniqueUsernameValidator() {
            this.mode = GitReposUsersEditPage.this.mode;
            this.original = GitReposUsersEditPage.this.getIgnitionUser();

            this.projectId = GitReposUsersEditPage.this.getProjectId();
        }

        protected boolean isUnique(GatewayContext context, String value) {
            SQuery<GitReposUsersRecord> query = new SQuery<>(GitReposUsersRecord.META).eq(ProjectId, projectId).eq(IgnitionUser, value);

            GitReposUsersRecord result = context.getPersistenceInterface().queryOne(query);
            return result == null;
        }

        public void validate(IValidatable<String> validatable) {
            if (this.isEnabled()) {
                String attempt = validatable.getValue();
                if (!StringUtils.isBlank(attempt)) {
                    if (this.mode != RecordEditMode.EDIT || !StringUtils.equalsIgnoreCase(attempt, this.original)) {
                        if (!this.isUnique(((IgnitionWebApp) Application.get()).getContext(), attempt)) {
                            validatable.error((new ValidationError()).addKey("GitReposUsersRecord.IgnitionUser.FieldEditor.Unique"));
                        }
                    }
                }
            }
        }

        protected boolean isEnabled() {
            return true;
        }


    }
}


