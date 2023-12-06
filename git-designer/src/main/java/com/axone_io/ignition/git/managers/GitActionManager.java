package com.axone_io.ignition.git.managers;

import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.CommitPopup;
import com.axone_io.ignition.git.DesignerHook;
import com.axone_io.ignition.git.PullPopup;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.project.ChangeOperation;
import com.inductiveautomation.ignition.common.project.resource.ProjectResourceId;
import com.inductiveautomation.ignition.common.util.LoggerEx;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;






import static com.axone_io.ignition.git.DesignerHook.context;
import static com.axone_io.ignition.git.DesignerHook.rpc;
import static com.axone_io.ignition.git.actions.GitBaseAction.handleCommitAction;
import static com.axone_io.ignition.git.actions.GitBaseAction.handlePullAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class GitActionManager {

    static CommitPopup commitPopup;
    static PullPopup pullPopup;
    private static final Logger logger = LoggerFactory.getLogger(GitActionManager.class);



    public static Object[][] getCommitPopupData(String projectName, String userName) {
        List<ChangeOperation> changes = DesignerHook.changes;

        // Log the total number of change operations found
        logger.debug("Total number of change operations: {}", changes.size());

        Dataset ds = rpc.getUncommitedChanges(projectName, userName);
        Object[][] data = new Object[ds.getRowCount()][];

        List<String> resourcesChangedId = new ArrayList<>();
        for (ChangeOperation c : changes) {
            ProjectResourceId pri = ChangeOperation.getResourceIdFromChange(c);
            resourcesChangedId.add(pri.getResourcePath().toString());

            // Log each change operation's details
            logger.debug("ChangeOperation Type: {}, Resource: {}", c.getOperationType(), pri.getResourcePath());
        }

        for (int i = 0; i < ds.getRowCount(); i++) {
            String resource = (String) ds.getValueAt(i, "resource");

            boolean toAdd = resourcesChangedId.contains(resource);
            Object[] row = {toAdd, resource, ds.getValueAt(i, "type"), ds.getValueAt(i, "actor")};

            // Log the decision to add or not add the resource to the commit popup
            logger.debug("Resource: {}, Add to commit popup: {}", resource, toAdd);

            data[i] = row;
        }

        return data;
    }


    public static void showCommitPopup(String projectName, String userName) {
        Object[][] data = GitActionManager.getCommitPopupData(projectName, userName);
        if (commitPopup != null) {
            commitPopup.setData(data);
            commitPopup.setVisible(true);
            commitPopup.toFront();
        } else {
            commitPopup = new CommitPopup(data, context.getFrame()) {
                @Override
                public void onActionPerformed(List<String> changes, String commitMessage) {
                    handleCommitAction(changes, commitMessage);
                    resetMessage();
                }
            };
        }
    }
    public static void openRepositoryLink() {
        try {
            Desktop desktop = Desktop.getDesktop();
            String repoLink = "https://github.com/E-I-Engineering-Ltd";
            desktop.browse(new URI(repoLink)); // This line might throw IOException or URISyntaxException
        } catch (IOException | URISyntaxException e) {
            logger.error("Error opening repository link", e);
        }
    }



    public static void showPullPopup(String projectName, String userName) {
        if (pullPopup != null) {
            pullPopup.setVisible(true);
            pullPopup.toFront();
        } else {
            pullPopup = new PullPopup(context.getFrame()) {
                @Override
                public void onPullAction(boolean importTags, boolean importTheme, boolean importImages) {
                    handlePullAction(importTags, importTheme, importImages);
                    resetCheckboxes();
                }
            };
        }
    }

    public static void showConfirmPopup(String message, int messageType) {
        JOptionPane.showConfirmDialog(context.getFrame(),
                message, "Info", JOptionPane.DEFAULT_OPTION, messageType);
    }
}
