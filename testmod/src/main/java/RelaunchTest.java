import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.devtech.grossfabrichacks.relaunch.TransformingClassLoader;

public class RelaunchTest implements RelaunchEntrypoint {
    @Override
    public void onRelaunch() {
        TransformingClassLoader.registerAsmTransformer(node -> {
            System.out.println(node.name);
            return true;
        });
    }
}
