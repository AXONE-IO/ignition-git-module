package com.axone_io.ignition.git.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.web.components.editors.PasswordEditorSource;
import com.inductiveautomation.ignition.gateway.web.components.editors.TextAreaEditorSource;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SFieldFlags;

public class GitReposUsersRecord extends PersistentRecord {
    private static final Logger logger = LoggerFactory.getLogger(GitReposUsersRecord.class);

    public static final RecordMeta<GitReposUsersRecord> META = new RecordMeta<>(
            GitReposUsersRecord.class, "GitReposUsersRecord");

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }

    public static final IdentityField Id = new IdentityField(META);
    public static final LongField ProjectId = new LongField(META, "ProjectId");
    public static final ReferenceField<GitProjectsConfigRecord> ProjectName = new ReferenceField<>(META, GitProjectsConfigRecord.META, "ProjectName", ProjectId);
    public static final ReferenceField<GitProjectsConfigRecord> URI = new ReferenceField<>(META, GitProjectsConfigRecord.META, "URI", ProjectId);

    public static final StringField IgnitionUser = new StringField(META, "IgnitionUser", SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
    public static final StringField SSHKey = new StringField(META, "SSHKey");

    public static final StringField UserName = new StringField(META, "UserName", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);

    public static final StringField Email = new StringField(META, "Email", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE).setDefault("");
    public static final EncodedStringField Password = new EncodedStringField(META, "Password");

    static final Category UserProperties = new Category("GitReposUsersRecord.Category.UserProperties", 1000).include(ProjectName, IgnitionUser, UserName, Email, SSHKey, Password);

    public int getId() {
        return this.getInt(Id);
    }

    public int getProjectId() {
        return this.getInt(ProjectId);
    }

    public String getUserName() {
        return this.getString(UserName);
    }

    public String getEmail() {
        return this.getString(Email);
    }

    public String getIgnitionUser() {
        return this.getString(IgnitionUser);
    }

    public String getProjectName() {
        return this.getString(ProjectName);
    }

    public String getPassword() {
        return this.getString(Password);
    }

    public String getSSHKey() {
        return this.getString(SSHKey);
    }

    public void setUserName(String userName) {
        setString(UserName, userName);
    }

    public void setPassword(String password) {
        setString(Password, password);
    }

    public void setIgnitionUser(String ignitionUser) {
        setString(IgnitionUser, ignitionUser);
    }

    public void setSSHKey(String sshKey) {
        setString(SSHKey, sshKey);
    }

    public void setEmail(String email) {
        setString(Email, email);
    }

    public void setProjectId(long projectId) {
        this.setLong(ProjectId, projectId);
    }

    static {
        ProjectName.getFormMeta().setEnabled(false);
        URI.getFormMeta().setVisible(false);

        IgnitionUser.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.IgnitionUser.Desc");
        IgnitionUser.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.IgnitionUser.NewDesc");
        IgnitionUser.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.IgnitionUser.EditDesc");

        ProjectName.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.ProjectName.Desc");
        ProjectName.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.ProjectName.NewDesc");
        ProjectName.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.ProjectName.EditDesc");

        SSHKey.getFormMeta().setEditorSource(new TextAreaEditorSource());
        SSHKey.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.SSHKey.Desc");
        SSHKey.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.SSHKey.NewDesc");
        SSHKey.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.SSHKey.EditDesc");
        SSHKey.setWide();

        UserName.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.UserName.Desc");
        UserName.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.UserName.NewDesc");
        UserName.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.UserName.EditDesc");

        Email.getFormMeta().addValidator(EmailAddressValidator.getInstance());
        Email.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.Email.Desc");
        Email.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.Email.NewDesc");
        Email.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.Email.EditDesc");

        Password.getFormMeta().setFieldDescriptionKey("GitReposUsersRecord.Password.Desc");
        Password.getFormMeta().setFieldDescriptionKeyAddMode("GitReposUsersRecord.Password.NewDesc");
        Password.getFormMeta().setFieldDescriptionKeyEditMode("GitReposUsersRecord.Password.EditDesc");
        Password.getFormMeta().setEditorSource(PasswordEditorSource.getSharedInstance());
    }
}

