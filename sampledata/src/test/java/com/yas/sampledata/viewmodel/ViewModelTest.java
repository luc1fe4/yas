package com.yas.sampledata.viewmodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewModelTest {

    @Test
    void testSampleDataVm() {
        SampleDataVm vm1 = new SampleDataVm("test");
        SampleDataVm vm2 = new SampleDataVm("test");
        assertEquals("test", vm1.message());
        assertEquals(vm1, vm2);
        assertEquals(vm1.hashCode(), vm2.hashCode());
        assertNotNull(vm1.toString());
    }

    @Test
    void testErrorVm() {
        ErrorVm vm1 = new ErrorVm("400", "Bad Request", "Detail");
        ErrorVm vm2 = new ErrorVm("400", "Bad Request", "Detail");
        assertEquals("400", vm1.statusCode());
        assertEquals("Bad Request", vm1.title());
        assertEquals("Detail", vm1.detail());
        // For regular classes, equals/hashCode might not be overridden, but we still cover them
        assertNotNull(vm1.toString());
    }
}
