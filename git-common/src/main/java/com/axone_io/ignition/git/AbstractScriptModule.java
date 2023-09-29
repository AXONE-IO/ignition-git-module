package com.axone_io.ignition.git;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;

import java.util.List;

public abstract class AbstractScriptModule implements GitScriptInterface {

    static {
        BundleUtil.get().addBundle(
            AbstractScriptModule.class.getSimpleName(),
            AbstractScriptModule.class.getClassLoader(),
            AbstractScriptModule.class.getName().replace('.', '/')
        );
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public boolean pull(@ScriptArg("projectName") String projectName,
                        @ScriptArg("userName") String userName,
                        @ScriptArg("importTags") boolean importTags,
                        @ScriptArg("importTheme") boolean importTheme,
                        @ScriptArg("importImages") boolean importImages) throws Exception {
        return pullImpl(projectName, userName, importTags, importTheme, importImages);
    }


    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public boolean push(@ScriptArg("projectName") String projectName,
                        @ScriptArg("userName")String userName) throws Exception {
        return pushImpl(projectName,userName);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public boolean commit(@ScriptArg("projectName") String projectName,
                          @ScriptArg("userName") String userName,
                          @ScriptArg("changes") List<String> changes,
                          @ScriptArg("message") String message) {
        return commitImpl(projectName, userName, changes, message);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public Dataset getUncommitedChanges(@ScriptArg("projectName") String projectName,
                                        @ScriptArg("userName") String userName) {
        return getUncommitedChangesImpl(projectName, userName);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public boolean isRegisteredUser(@ScriptArg("projectName") String projectName,
                                    @ScriptArg("userName") String userName) {
        return isRegisteredUserImpl(projectName, userName);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public boolean exportConfig(@ScriptArg("projectName") String projectName) {
        return exportConfigImpl(projectName);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void setupLocalRepo(@ScriptArg("projectName") String projectName,
                               @ScriptArg("userName") String userName) throws Exception {
        setupLocalRepoImpl(projectName, userName);
    }

    protected abstract boolean pullImpl(String projectName, String userName, boolean importTags, boolean importTheme,
                                        boolean importImages) throws Exception;
    protected abstract boolean pushImpl(String projectName, String userName) throws Exception;
    protected abstract boolean commitImpl(String projectName, String userName, List<String> changes, String message);
    protected abstract Dataset getUncommitedChangesImpl(String projectName, String userName);
    protected abstract boolean isRegisteredUserImpl(String projectName, String userName);
    protected abstract boolean exportConfigImpl(String projectName);
    protected abstract void setupLocalRepoImpl(String projectName, String userName) throws Exception;

}
