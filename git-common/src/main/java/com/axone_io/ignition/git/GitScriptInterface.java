package com.axone_io.ignition.git;

import com.inductiveautomation.ignition.common.Dataset;

import java.util.List;

public interface GitScriptInterface {

    boolean pull(String projectName, String userName) throws Exception;
    boolean push(String projectName, String userName) throws Exception;
    boolean commit(String projectName, String userName, List<String> changes, String message);
    Dataset getUncommitedChanges(String projectName, String userName);
    boolean isRegisteredUser(String projectName, String userName);
    boolean exportConfig(String projectName);
    void setupLocalRepo(String projectName, String userName) throws Exception;


}
