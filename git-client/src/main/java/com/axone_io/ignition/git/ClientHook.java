package com.axone_io.ignition.git;

import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.vision.api.client.AbstractClientModuleHook;

public class ClientHook extends AbstractClientModuleHook {

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        /*manager.addScriptModule(
            "system.git",
            new ClientScriptModule(),
            new PropertiesFileDocProvider()
        );*/
    }

}
