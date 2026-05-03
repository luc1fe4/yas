package com.yas.sampledata.viewmodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewModelTest {

    @Test
    void testSampleDataVm() {
        SampleDataVm vm = new SampleDataVm("test");
        assertEquals("test", vm.message());
    }

    @Test
    void testErrorVm() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Detail");
        assertEquals("400", vm.statusCode());
        assertEquals("Bad Request", vm.title());
        assertEquals("Detail", vm.detail());
    }
}
