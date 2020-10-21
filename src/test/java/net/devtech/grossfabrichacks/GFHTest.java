package net.devtech.grossfabrichacks;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.devtech.grossfabrichacks.instrumentation.InstrumentationApi;
import net.devtech.grossfabrichacks.util.Debug;
import net.gudenau.lib.unsafe.Unsafe;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;
import user11681.reflect.Classes;

@Testable
class GFHTest {
    static final GFHTest instance = new GFHTest();
    //    static Logger LOGGER = LogManager.getLogger();
    static final int iterations = 5000;

    @Test
    void classPointer() {
        final Class<?> c = GFHTest.class;
        final Class<?> d = Debug.class;
        final IntArrayList filter = IntArrayList.wrap(new int[]{8, 12, 20, 24, 28, 32, 36, 40, 48});

        int offset;

        for (offset = 0; offset < 1024; offset += 4) {
            if (Unsafe.getInt(c, offset) == Unsafe.getInt(d, offset)) {
                if (!filter.contains(offset)) {
                    System.out.println(offset);

                    Unsafe.putInt(instance, Classes.classOffset, Unsafe.getInt(d, offset));

                    if (instance.getClass() == d) {
                        break;
                    }
                }
            }
        }
    }

    @Test
    void instrumentation() {
        InstrumentationApi.instrumentation.getAllLoadedClasses();
    }

    @Test
    void test() throws Throwable {
        final Object[] objects = {new GFHTest(), Unsafe.allocateInstance(Block.class)};
        final int offset = Unsafe.arrayBaseOffset(objects.getClass());
        final int scale = Unsafe.arrayIndexScale(objects.getClass());
        final long address0 = (Unsafe.getInt(objects, offset) & 0xFFFFFFFFL) * Classes.addressFactor;
        final long address1 = (Unsafe.getInt(objects, offset + scale) & 0xFFFFFFFFL) * Classes.addressFactor;

        System.out.println(Long.toHexString(address0));
        System.out.println(Long.toHexString(address1));
    }

    static {
        System.setProperty("fabric.development", "true");
    }
}
