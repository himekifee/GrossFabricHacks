package net.devtech.grossfabrichacks;

import net.devtech.grossfabrichacks.instrumentation.InstrumentationApi;
import net.devtech.grossfabrichacks.unsafe.UnsafeUtil;
import net.gudenau.lib.unsafe.Unsafe;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

@Testable
class GFHTest {
    static final GFHTest instance = new GFHTest();
    //    static Logger LOGGER = LogManager.getLogger();
    static final int iterations = 5000;

    @Test
    void test() throws Throwable {
        final Object[] objects = {new GFHTest(), Unsafe.allocateInstance(Block.class)};
        final int offset = Unsafe.arrayBaseOffset(objects.getClass());
        final int scale = Unsafe.arrayIndexScale(objects.getClass());
        final long address0 = (Unsafe.getInt(objects, offset) & 0xFFFFFFFFL) * UnsafeUtil.addressFactor;
        final long address1 = (Unsafe.getInt(objects, offset + scale) & 0xFFFFFFFFL) * UnsafeUtil.addressFactor;

        System.out.println(Long.toHexString(address0));
        System.out.println(Long.toHexString(address1));
    }

    void printValues(final Object object) {
        for (int i = 0; i < InstrumentationApi.instrumentation.getObjectSize(object); i += 4) {
            System.out.printf("%08X ", Unsafe.getInt(object, i));
        }

        System.out.println();
    }
}
