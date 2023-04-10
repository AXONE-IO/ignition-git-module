package com.axone_io.ignition.git.managers;

import java.util.ArrayList;
import java.util.List;

import com.axone_io.ignition.git.CommitPopup;
import com.axone_io.ignition.git.DesignerHook;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.project.ChangeOperation;
import com.inductiveautomation.ignition.common.project.resource.ProjectResourceId;

import static com.axone_io.ignition.git.DesignerHook.context;
import static com.axone_io.ignition.git.DesignerHook.rpc;
import static com.axone_io.ignition.git.actions.GitBaseAction.handleCommitAction;

public class GitActionManager {
    public static Object[][] getCommitPopupData(String projectName, String userName) {
        List<ChangeOperation> changes = DesignerHook.changes;

        Dataset ds = rpc.getUncommitedChanges(projectName, userName);
        Object[][] data = new Object[ds.getRowCount()][];

        List<String> resourcesChangedId = new ArrayList<>();
        for (ChangeOperation c : changes) {
            ProjectResourceId pri = ChangeOperation.getResourceIdFromChange(c);
            resourcesChangedId.add(pri.getResourcePath().toString());
        }

        for(int i = 0; i < ds.getRowCount(); i++){
            String resource = (String) ds.getValueAt(i,"resource");

            boolean toAdd = resourcesChangedId.contains(resource);
            Object[] row = {toAdd, resource, ds.getValueAt(i,"type"), ds.getValueAt(i,"actor")};
            data[i] = row;
        }

        return data;
    }

    public static void showCommitPopup(String projectName, String userName) {
        new CommitPopup(GitActionManager.getCommitPopupData(projectName, userName), context.getFrame()) {
            @Override
            public void onActionPerformed(List<String> changes, String commitMessage) {
                handleCommitAction(changes, commitMessage);
            }
        };
    }
}
