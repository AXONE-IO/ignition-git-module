package com.axone_io.ignition.git.commissioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitCommissioningConfig {
    private String repoURI;
    private String repoBranch;
    private String ignitionProjectName;
    private String ignitionUserName;
    private String userName;
    private String userPassword;
    private String sshKey;
    private String userEmail;

    private boolean importImages = false;
    private boolean importTags = false;
    private boolean importThemes = false;

    private String initDefaultBranch;

    public String getRepoURI() {
        return repoURI;
    }

    public void setRepoURI(String repoURI) {
        this.repoURI = repoURI;
    }

    public String getRepoBranch() {
        return repoBranch;
    }

    public void setRepoBranch(String repoBranch) {
        this.repoBranch = repoBranch;
    }

    public String getIgnitionProjectName() {
        return ignitionProjectName;
    }

    public void setIgnitionProjectName(String ignitionProjectName) {
        this.ignitionProjectName = ignitionProjectName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getIgnitionUserName() {
        return ignitionUserName;
    }

    public void setIgnitionUserName(String ignitionUserName) {
        this.ignitionUserName = ignitionUserName;
    }

    public boolean isImportImages() {
        return importImages;
    }

    public void setImportImages(boolean importImages) {
        this.importImages = importImages;
    }

    public boolean isImportTags() {
        return importTags;
    }

    public void setImportTags(boolean importTags) {
        this.importTags = importTags;
    }

    public boolean isImportThemes() {
        return importThemes;
    }

    public void setImportThemes(boolean importThemes) {
        this.importThemes = importThemes;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public void setSecretFromFilePath(Path filePath, boolean isSSHAuth) throws IOException {
        if (filePath.toFile().exists() && filePath.toFile().isFile()) {
            String secret = Files.readString(filePath, StandardCharsets.UTF_8);
            if (isSSHAuth) {
                this.sshKey = secret;
            } else {
                this.userPassword = secret;
            }
        }
    }

    public String getInitDefaultBranch() {
        return initDefaultBranch;
    }

    public void setInitDefaultBranch(String initDefaultBranch) {
        this.initDefaultBranch = initDefaultBranch;
    }
}
