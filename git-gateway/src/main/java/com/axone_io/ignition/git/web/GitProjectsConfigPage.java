package com.axone_io.ignition.git.web;

import com.axone_io.ignition.git.GatewayHook;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.RecordActionTable;
import com.inductiveautomation.ignition.gateway.web.components.actions.AbstractRecordInstanceAction;
import com.inductiveautomation.ignition.gateway.web.components.actions.EditRecordAction;
import com.inductiveautomation.ignition.gateway.web.components.actions.NewRecordAction;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.models.RecordTypeNameModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import static com.axone_io.ignition.git.GatewayHook.CONFIG_CATEGORY;


public class GitProjectsConfigPage extends RecordActionTable<GitProjectsConfigRecord> {
    public static final IConfigTab MENU_ENTRY = DefaultConfigTab.builder()
            .category(CONFIG_CATEGORY)
            .name("projects")
            .i18n("bundle_git.Config.Git.Projects.MenuTitle")
            .page(GitProjectsConfigPage.class)
            .terms(new String[] { "git", "projects", "users", "ssh"})
            .build();


    public GitProjectsConfigPage(IConfigPage configPage) {
        super(configPage);
    }
    @Override
    protected RecordMeta<GitProjectsConfigRecord> getRecordMeta() {
        return GitProjectsConfigRecord.META;
    }

    protected WebMarkupContainer newRecordAction(String id) {
        return new NewRecordAction<>(id, this.configPage, this, GitProjectsConfigRecord.META) {
            private static final long serialVersionUID = 1L;

            protected ConfigPanel newRecordEditPanel(GitProjectsConfigRecord newRecord) {
                return new GitProjectsConfigEditPage(getConfigPage(), GitProjectsConfigPage.this, newRecord);
            }

            protected void setupNewRecord(GitProjectsConfigRecord record) {
            }
        };
    }

    protected WebMarkupContainer newEditRecordAction(String id, GitProjectsConfigRecord record) {
        if (record.getId() == -1L)
            return null;
        return new EditRecordAction<>(id, this.configPage, this, record) {
            private static final long serialVersionUID = 1L;

            protected ConfigPanel createPanel(GitProjectsConfigRecord record) {
                return new GitProjectsConfigEditPage(getConfigPage(), GitProjectsConfigPage.this, record);
            }
        };
    }

    @Override
    protected void addRecordInstanceActions (RepeatingView view, GitProjectsConfigRecord main) {
        super.addRecordInstanceActions(view, main);
        view.add(new ManageAction(view.newChildId(), this.configPage, this, main));
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_ENTRY.getMenuLocation();
    }

    private static class ManageAction extends AbstractRecordInstanceAction<GitProjectsConfigRecord> {
        public ManageAction(String id, IConfigPage configPage, ConfigPanel parentPanel, GitProjectsConfigRecord record) {
            super(id, configPage, parentPanel, record);
        }

        protected ConfigPanel createPanel(GitProjectsConfigRecord record) {
            return new GitReposUsersPage(getConfigPage(), record);
        }

        protected String getCssClass() {
            return "view";
        }

        public IModel getLabel() {
            return new RecordTypeNameModel(GitProjectsConfigRecord.META, "Config.Projects.UserLink");
        }
    }
}
