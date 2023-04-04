package com.axone_io.ignition.git.records;

import com.axone_io.ignition.git.web.ProjectList.ProjectListEditorSource;
import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SFieldFlags;

public class GitProjectsConfigRecord extends PersistentRecord {
    private static final Logger logger = LoggerFactory.getLogger(GitProjectsConfigRecord.class);

    public static final RecordMeta<GitProjectsConfigRecord> META = new RecordMeta<>(
            GitProjectsConfigRecord.class, "GitProjectsConfigRecord");

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }

    public static final IdentityField Id = new IdentityField(META);
    public static final StringField ProjectName = new StringField(META, "ProjectName", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
    public static final StringField URI =
            new StringField(META, "URI", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);


    static final Category ProjectConfiguration = new Category("GitProjectsConfigRecord.Category.ProjectConfiguration", 1000).include(ProjectName, URI);


    public long getId(){
        return this.getLong(Id);
    }
    public String getProjectName(){
        return this.getString(ProjectName);
    }
    public String getURI(){
        return this.getString(URI);
    }

    public boolean isSSHAuthentication(){
        return !this.getString(URI).toLowerCase().startsWith("http");
    }

    static {
        ProjectName.getFormMeta().setEditorSource(ProjectListEditorSource.getSharedInstance());

        URI.getFormMeta().setFieldDescriptionKey("GitProjectsConfigRecord.URI.Desc");
        URI.getFormMeta().setFieldDescriptionKeyAddMode("GitProjectsConfigRecord.URI.NewDesc");
        URI.getFormMeta().setFieldDescriptionKeyEditMode("GitProjectsConfigRecord.URI.EditDesc");
        URI.setWide();

    }
}
