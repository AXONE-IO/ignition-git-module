package com.axone_io.ignition.git;

import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.Dataset;

import java.util.List;

public class ClientScriptModule extends AbstractScriptModule {

    private final GitScriptInterface rpc;

    public ClientScriptModule() {
        rpc = ModuleRPCFactory.create(
            "com.axone_io.ignition.git",
            GitScriptInterface.class
        );
    }

    @Override
    protected boolean pullImpl(String projectName, String userName) throws Exception {
        return rpc.pull(projectName, userName);
    }

    @Override
    protected boolean pushImpl(String projectName, String userName) throws Exception {
        return rpc.push(projectName, userName);
    }

    @Override
    protected boolean commitImpl(String projectName, String userName, List<String> changes, String message) {
        return rpc.commit(projectName, userName, changes, message);
    }

    @Override
    protected Dataset getUncommitedChangesImpl(String projectName, String userName) {
        return rpc.getUncommitedChanges(projectName, userName);
    }

    @Override
    protected boolean isRegisteredUserImpl(String projectName, String userName){
        return rpc.isRegisteredUser(projectName, userName);
    }

    @Override
    protected boolean exportConfigImpl(String projectName) {
        return rpc.exportConfig(projectName);
    }

    @Override
    protected void setupLocalRepoImpl(String projectName, String userName) throws Exception {
        rpc.setupLocalRepo(projectName, userName);
    }
}
