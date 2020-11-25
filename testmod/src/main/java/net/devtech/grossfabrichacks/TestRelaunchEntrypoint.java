package net.devtech.grossfabrichacks;

import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestRelaunchEntrypoint implements RelaunchEntrypoint {

    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks/ArcblrothBreaksThings2");

    @Override
    public void onRelaunch() {
        LOGGER.info("Running relaunchEntrypoint...");
    }

}
