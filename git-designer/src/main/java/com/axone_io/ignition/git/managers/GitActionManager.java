package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.CommitPopup;
import com.axone_io.ignition.git.DesignerHook;
import com.axone_io.ignition.git.actions.GitBaseAction;
import com.axone_io.ignition.git.utils.IconUtils;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.project.ChangeOperation;
import com.inductiveautomation.ignition.common.project.resource.ProjectResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static com.axone_io.ignition.git.DesignerHook.*;
import static com.axone_io.ignition.git.actions.GitBaseAction.handleCommitAction;

public class GitActionManager {
    private static final Logger logger = LoggerFactory.getLogger(GitActionManager.class);
    public static Object[][] getCommitPopupData(String projectName, String userName){
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

    public static void handleCommitPopup(String projectName, String userName){
        new CommitPopup(GitActionManager.getCommitPopupData(projectName, userName), context.getFrame()) {
            @Override
            public void onActionPerformed(List<String> changes, String commitMessage) {
                handleCommitAction(changes, commitMessage);
            }
        };
    }


    public static String getBundleKey(GitBaseAction.GitActionType type){
        String bundleKey = "";
        switch (type){
            case PULL:
                bundleKey = "DesignerHook.Actions.Pull";
                break;
            case PUSH:
                bundleKey = "DesignerHook.Actions.Push";
                break;
            case COMMIT:
                bundleKey = "DesignerHook.Actions.Commit";
                break;
            case EXPORT:
                bundleKey = "DesignerHook.Actions.ExportGatewayConfig";
                break;
        }
        return bundleKey;
    }

    public static Icon getIcon(GitBaseAction.GitActionType type) {
        String resourcePath = "";
        switch (type){
            case PULL:
                resourcePath = "/com/axone_io/ignition/git/icons/ic_pull.svg";
                break;
            case PUSH:
                resourcePath = "/com/axone_io/ignition/git/icons/ic_push.svg";
                break;
            case COMMIT:
                resourcePath = "/com/axone_io/ignition/git/icons/ic_commit.svg";
                break;
            case EXPORT:
                resourcePath = "/com/axone_io/ignition/git/icons/ic_folder.svg";
                break;
        }

        return IconUtils.getIcon(resourcePath);
    }

    public static void handleConfirmPopup(String message, int messageType){
        JOptionPane.showConfirmDialog(context.getFrame(),
                message, "Info", JOptionPane.DEFAULT_OPTION, messageType);
    }
}
