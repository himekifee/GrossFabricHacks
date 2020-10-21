package net.devtech.grossfabrichacks;

import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.fabricmc.loader.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestPrePrePrePreLaunch implements RelaunchEntrypoint {

    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks/ArcblrothBreaksThings");

    @Override
    public void onRelaunch() {
        LOGGER.info("Running preprepreprelaunch...");

        try {
            FabricLoader.INSTANCE.getGameProvider();
            LOGGER.info("Fabric is loaded!");
        } catch (IllegalStateException e) {
            LOGGER.info("Fabric isn't loaded yet!");
        }
    }

}
