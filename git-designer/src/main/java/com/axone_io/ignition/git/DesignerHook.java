package com.axone_io.ignition.git;

import com.axone_io.ignition.git.actions.GitBaseAction;
import com.axone_io.ignition.git.utils.IconUtils;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.SessionInfo;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.project.ChangeOperation;
import com.inductiveautomation.ignition.designer.gui.DesignerToolbar;
import com.inductiveautomation.ignition.designer.gui.StatusBar;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.SaveContext;
import com.jidesoft.action.DockableBarManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.axone_io.ignition.git.managers.GitActionManager.handleCommitPopup;

public class DesignerHook extends AbstractDesignerModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static GitScriptInterface rpc = ModuleRPCFactory.create(
            "com.axone_io.ignition.git",
            GitScriptInterface.class
    );
    public static List<ChangeOperation> changes = new ArrayList<>();
    public static DesignerContext context;
    public static String projectName;
    public static String userName;
    JPanel gitStatusBar;
    Timer gitUserTimer;
    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        /*manager.addScriptModule(
            "system.git",
            new ClientScriptModule(),
            new PropertiesFileDocProvider()
        );*/
    }

    @Override
    public void startup(DesignerContext context, LicenseState activationState) throws Exception {
        super.startup(context, activationState);
        DesignerHook.context = context;
        BundleUtil.get().addBundle("DesignerHook", getClass(), "DesignerHook");

        projectName = context.getProjectName();

        Optional<SessionInfo> sessionInfo = context.getResourceEditManager().getCurrentSessionInfo();
        userName = sessionInfo.isPresent() ? sessionInfo.get().getUsername() : "";

        rpc.setupLocalRepo(projectName, userName);

        initStatusBar();
        initToolBar();

    }

    private void initStatusBar(){
        StatusBar statusBar = context.getStatusBar();
        gitStatusBar = new JPanel();

        JLabel gitIconLabel = new JLabel(IconUtils.getIcon("/com/axone_io/ignition/git/icons/ic_git.svg"));
        gitIconLabel.setSize(35, 35);
        gitStatusBar.add(gitIconLabel);

        gitStatusBar.add(new JLabel(userName));

        boolean userValid = rpc.isRegisteredUser(projectName, userName);
        String userIconPath = userValid ? "/com/axone_io/ignition/git/icons/ic_verified_user.svg" : "/com/axone_io/ignition/git/icons/ic_unregister_user.svg";
        JLabel labelUserIcon = new JLabel(IconUtils.getIcon(userIconPath));
        labelUserIcon.setSize(35,35);
        gitStatusBar.add(labelUserIcon);

        statusBar.addDisplay(gitStatusBar);

        gitUserTimer = new Timer(10000, e -> {
            boolean valid = rpc.isRegisteredUser(projectName, userName);
            String userIconPath1 = valid ? "/com/axone_io/ignition/git/icons/ic_verified_user.svg" : "/com/axone_io/ignition/git/icons/ic_unregister_user.svg";
            labelUserIcon.setIcon(IconUtils.getIcon(userIconPath1));
        });

        gitUserTimer.start();
    }

    private void initToolBar() {
        DockableBarManager toolBarManager = context.getToolbarManager();
        DesignerToolbar toolbar = new DesignerToolbar("Git", "DesignerHook.Toolbar.Name");
        toolbar.add(new GitBaseAction(GitBaseAction.GitActionType.PUSH));
        toolbar.add(new GitBaseAction(GitBaseAction.GitActionType.PULL));
        toolbar.add(new GitBaseAction(GitBaseAction.GitActionType.COMMIT));
        toolbar.add(new GitBaseAction(GitBaseAction.GitActionType.EXPORT));

        toolBarManager.addDockableBar(toolbar);
    }

    @Override
    public void notifyProjectSaveStart(SaveContext save) {
        changes = context.getProject().getChanges();
        super.notifyProjectSaveStart(save);
    }

    @Override
    public void notifyProjectSaveDone(){
        super.notifyProjectSaveDone();

        handleCommitPopup(projectName, userName);
    }

    @Override
    public void shutdown() {
        super.shutdown();

        DockableBarManager toolBarManager = context.getToolbarManager();
        toolBarManager.removeDockableBar("Git");

        StatusBar statusBar = context.getStatusBar();
        statusBar.removeDisplay(gitStatusBar);

        gitUserTimer.stop();
    }


}
