package com.axone_io.ignition.git;

import com.axone_io.ignition.git.records.GitReposUsersRecord;
import com.axone_io.ignition.git.records.GitProjectsConfigRecord;
import com.axone_io.ignition.git.web.GitProjectsConfigPage;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.clientcomm.ClientReqSession;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class GatewayHook extends AbstractGatewayModuleHook {
    static public String MODULE_NAME = "Git";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private GatewayScriptModule scriptModule;
    public static GatewayContext context;

    public static final ConfigCategory CONFIG_CATEGORY =
            new ConfigCategory(MODULE_NAME, "bundle_git.Config.Git.MenuTitle", 700);

    @Override
    public List<? extends IConfigTab> getConfigPanels() {
        return Collections.singletonList(GitProjectsConfigPage.MENU_ENTRY);
    }

    @Override
    public List<ConfigCategory> getConfigCategories() {
        return Collections.singletonList(CONFIG_CATEGORY);
    }


    @Override
    public void setup(GatewayContext gatewayContext) {
        try {
            context = gatewayContext;
            scriptModule = new GatewayScriptModule();
            BundleUtil.get().addBundle("bundle_git", getClass(), "bundle_git");
            verifySchema(gatewayContext);
        } catch (Exception e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }


        logger.info("setup()");
    }

    private void verifySchema(GatewayContext context) {
        try {
            context.getSchemaUpdater().updatePersistentRecords(GitProjectsConfigRecord.META, GitReposUsersRecord.META);
        } catch (SQLException e) {
            logger.error("Error verifying persistent record schemas for HomeConnect records.", e);
        }
    }

    @Override
    public void startup(LicenseState licenseState) {

        logger.info("startup()");
    }

    @Override
    public void shutdown() {
        logger.info("shutdown()");
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        /*manager.addScriptModule(
                "system.git",
                scriptModule,
                new PropertiesFileDocProvider());*/
    }

    @Override
    public boolean isFreeModule() {
        return Boolean.TRUE;
    }

    @Override
    public Object getRPCHandler(ClientReqSession session, String projectName) {
        return scriptModule;
    }
}
