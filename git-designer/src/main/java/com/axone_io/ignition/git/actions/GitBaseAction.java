package com.axone_io.ignition.git.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.Icon;

import com.axone_io.ignition.git.utils.IconUtils;
import com.inductiveautomation.ignition.client.util.action.BaseAction;
import com.inductiveautomation.ignition.client.util.gui.ErrorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.axone_io.ignition.git.DesignerHook.projectName;
import static com.axone_io.ignition.git.DesignerHook.rpc;
import static com.axone_io.ignition.git.DesignerHook.userName;
import static com.axone_io.ignition.git.managers.GitActionManager.showCommitPopup;

public class GitBaseAction extends BaseAction {
    private static final Logger logger = LoggerFactory.getLogger(GitBaseAction.class);

    public enum GitActionType {
        PULL(
            "DesignerHook.Actions.Pull",
            "/com/axone_io/ignition/git/icons/ic_pull.svg"
        ),
        PUSH(
            "DesignerHook.Actions.Push",
            "/com/axone_io/ignition/git/icons/ic_push.svg"
        ),
        COMMIT(
            "DesignerHook.Actions.Commit",
            "/com/axone_io/ignition/git/icons/ic_commit.svg"
        ),
        EXPORT(
            "DesignerHook.Actions.ExportGatewayConfig",
            "/com/axone_io/ignition/git/icons/ic_folder.svg"
        );

        private final String baseBundleKey;
        private final String resourcePath;

        GitActionType(String baseBundleKey, String resourcePath) {
            this.baseBundleKey = baseBundleKey;
            this.resourcePath = resourcePath;
        }

        public Icon getIcon() {
            return IconUtils.getIcon(resourcePath);
        }
    }

    GitActionType type;

    public GitBaseAction(GitActionType type) {
        super(type.baseBundleKey, type.getIcon());
        this.type = type;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handleAction(type);
    }

    // Todo : Find a way to refactor with handleAction
    public static void handleCommitAction(List<String> changes, String commitMessage) {
        try {
            rpc.commit(projectName, userName, changes, commitMessage);
        } catch (Exception ex) {
            ErrorUtil.showError(ex);
        }
    }

    public static void handleAction(GitActionType type) {
        try {
            switch (type) {
                case PULL:
                    rpc.pull(projectName, userName);
                    break;
                case PUSH:
                    rpc.push(projectName, userName);
                    break;
                case COMMIT:
                    showCommitPopup(projectName, userName);
                    break;
                case EXPORT:
                    rpc.exportConfig(projectName);
                    break;
            }
        } catch (Exception ex) {
            ErrorUtil.showError(ex);
        }
    }
}
