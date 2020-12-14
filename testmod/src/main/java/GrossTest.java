import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.instrumentation.InstrumentationApi;
import net.devtech.grossfabrichacks.relaunch.Relauncher;

public class GrossTest implements PrePrePreLaunch {
    @Override
    public void onPrePrePreLaunch() {
        for (final Class<?> klass : InstrumentationApi.instrumentation.getAllLoadedClasses()) {
            System.out.println(klass);
        }

        Relauncher.ensureRelaunched();
    }
}
