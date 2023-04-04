package com.axone_io.ignition.git.web.component;

import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.web.models.RecordListModel;

public class CustomRecordListModel extends RecordListModel {
    long value;
    public CustomRecordListModel(RecordMeta meta, long value) {
        super(meta);
        this.value = value;
    }

    // TODO : Find a way to deport GitReposUsersRecord.ProjectId in the constructor parameters
    // If a LongField is added in the parameters of the constructor, a NullPointerException is thrown during a "backward to last page" from the web browser.
    @Override
    protected boolean filter(PersistentRecord record) {
        return record.getLong(GitReposUsersRecord.ProjectId) != value;
    }

}
