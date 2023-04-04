package com.axone_io.ignition.git.web;

import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.CompoundRecordModel;
import com.inductiveautomation.ignition.gateway.web.models.RecordTypeNameModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

public class GitProjectsConfigEditPage extends RecordEditForm {
    public GitProjectsConfigEditPage(IConfigPage configPage, GitProjectsConfigPage returnPanel, GitProjectsConfigRecord record) {
        super(configPage, returnPanel, new RecordTypeNameModel(GitProjectsConfigRecord.META, "RecordActionTable." + (
                record.isNewRow() ? "New" : "Edit") + "RecordAction.PanelTitle"), new CompoundRecordModel(new GitProjectsConfigRecord[] { record }));
    }
}

