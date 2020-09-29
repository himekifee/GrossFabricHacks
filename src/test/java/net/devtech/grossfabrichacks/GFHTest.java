package net.devtech.grossfabrichacks;

import net.devtech.grossfabrichacks.unsafe.UnsafeUtil;
import net.gudenau.lib.unsafe.Unsafe;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

@Testable
class GFHTest {
//    static Logger LOGGER = LogManager.getLogger();
//    static final int iterations = 5000;

    @Test
    void test() {
        // we have achieved OpenJ9 support

        final byte[] byteArray = new byte[8];
        final int[] intArray = new int[2];
        final long[] longArray = new long[1];
        final GFHTest test = new GFHTest();
        final GFHTest test1 = new GFHTest();
        final GFHTest test2 = new GFHTest();
        final GFHTest test3 = new GFHTest();
        final GFHTest test4 = new GFHTest();
        final Testt testt = new Testt();

        printThings(byteArray);
        printThings(intArray);
        printThings(longArray);
        printThings(test);
        printThings(test1);
        printThings(test2);
        printThings(test3);
        printThings(test4);

        System.out.println(test4.getClass());
        Unsafe.putInt(test4, UnsafeUtil.KLASS_OFFSET, Unsafe.getInt(testt, UnsafeUtil.KLASS_OFFSET));
        System.out.println(test4.getClass());
    }

    void printThings(final Object object) {
        for (int i = 0; i < 16; i++) {
            System.out.printf("%08X ", Unsafe.getInt(object, 4 * i));
        }

        System.out.println();
    }

    static class Testt {}
}
