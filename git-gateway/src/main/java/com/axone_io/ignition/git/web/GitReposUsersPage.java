package com.axone_io.ignition.git.web;

import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.web.component.CustomRecordListModel;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.RecordActionTable;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode;
import com.inductiveautomation.ignition.gateway.web.components.actions.EditRecordAction;
import com.inductiveautomation.ignition.gateway.web.components.actions.NewRecordAction;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.WebMarkupContainer;

import static com.axone_io.ignition.git.web.GitProjectsConfigPage.MENU_ENTRY;


public class GitReposUsersPage extends RecordActionTable<GitReposUsersRecord> {
    GitProjectsConfigRecord gitProjectsConfigRecord;

    public GitReposUsersPage(IConfigPage configPage, GitProjectsConfigRecord record) {
        super(configPage, new CustomRecordListModel(GitReposUsersRecord.META, record.getId()));
        this.gitProjectsConfigRecord = record;
    }

    @Override
    protected RecordMeta<GitReposUsersRecord> getRecordMeta() {
        return GitReposUsersRecord.META;
    }

    protected WebMarkupContainer newRecordAction(String id) {
        return new NewRecordAction<>(id, this.configPage, this, GitReposUsersRecord.META) {
            private static final long serialVersionUID = 1L;
            protected ConfigPanel newRecordEditPanel(GitReposUsersRecord newRecord) {
                if(gitProjectsConfigRecord != null) newRecord.setProjectId(gitProjectsConfigRecord.getId());
                setupPanel(gitProjectsConfigRecord, newRecord, RecordEditMode.ADD);
                return new GitReposUsersEditPage(getConfigPage(),  new GitReposUsersPage(configPage, gitProjectsConfigRecord), newRecord);
            }

            protected void setupNewRecord(GitReposUsersRecord newRecord) {
            }
        };
    }

    protected WebMarkupContainer newEditRecordAction(String id, GitReposUsersRecord record) {
        if (record.getId() == -1L)
            return null;
        return new EditRecordAction<>(id, this.configPage, this, record) {
            private static final long serialVersionUID = 1L;

            protected ConfigPanel createPanel(GitReposUsersRecord record) {
                setupPanel(gitProjectsConfigRecord, record, RecordEditMode.EDIT);
                return new GitReposUsersEditPage(getConfigPage(), new GitReposUsersPage(configPage, gitProjectsConfigRecord), record);
            }
        };

    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_ENTRY.getMenuLocation();
    }

    void setupPanel(GitProjectsConfigRecord projectRecord, GitReposUsersRecord record, RecordEditMode mode){
        boolean sshAuth = projectRecord.isSSHAuthentication();
        GitReposUsersRecord.SSHKey.getFormMeta().setVisible(sshAuth);
        GitReposUsersRecord.UserName.getFormMeta().setVisible(!sshAuth);
        GitReposUsersRecord.Password.getFormMeta().setVisible(!sshAuth);
    }




}
