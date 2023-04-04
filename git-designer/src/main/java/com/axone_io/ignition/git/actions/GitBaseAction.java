package com.axone_io.ignition.git.actions;

import com.axone_io.ignition.git.managers.GitActionManager;
import com.inductiveautomation.ignition.client.util.action.BaseAction;
import com.inductiveautomation.ignition.common.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.axone_io.ignition.git.DesignerHook.*;
import static com.axone_io.ignition.git.managers.GitActionManager.*;
import static com.axone_io.ignition.git.managers.GitActionManager.handleConfirmPopup;

public class GitBaseAction extends BaseAction {
    private static final Logger logger = LoggerFactory.getLogger(GitBaseAction.class);

    public enum GitActionType {
        PULL,
        PUSH,
        COMMIT,
        EXPORT
    }

    GitActionType type;
    public GitBaseAction(GitActionType type) {
        super(getBundleKey(type), getIcon(type));
        this.type = type;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        handleAction(type);
    }

    // Todo : Find a way to refacto with handleAction
    public static void handleCommitAction(List<String> changes, String commitMessage){
        String message = BundleUtil.get().getStringLenient(getBundleKey(GitActionType.COMMIT) + ".ConfirmMessage");
        int messageType = JOptionPane.INFORMATION_MESSAGE;

        try {
            rpc.commit(projectName, userName, changes, commitMessage);
        } catch (Exception ex) {
            message = ex.getMessage();
            messageType = JOptionPane.ERROR_MESSAGE;
        }

        String finalMessage = message;
        int finalMessageType = messageType;
        SwingUtilities.invokeLater(new Thread(() -> handleConfirmPopup(finalMessage, finalMessageType)));
    }

    public static void handleAction(GitActionType type){
        String message = BundleUtil.get().getStringLenient(getBundleKey(type) + ".ConfirmMessage");
        int messageType = JOptionPane.INFORMATION_MESSAGE;
        boolean confirmPopup = Boolean.TRUE;

        try {
            switch (type){
                case PULL:
                    rpc.pull(projectName, userName);
                    break;
                case PUSH:
                    rpc.push(projectName, userName);
                    break;
                case COMMIT:
                    confirmPopup = Boolean.FALSE;
                    handleCommitPopup(projectName, userName);
                    break;
                case EXPORT:
                    rpc.exportConfig(projectName);
                    break;
            }
        } catch (Exception ex) {
            logger.info(ex.getMessage());
            message = ex.getMessage();
            messageType = JOptionPane.ERROR_MESSAGE;
        }

        String finalMessage = message;
        int finalMessageType = messageType;
        if(confirmPopup) SwingUtilities.invokeLater(new Thread(() -> handleConfirmPopup(finalMessage, finalMessageType)));
    }
}
