package vn.edu.it209;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void reportsHealthyPipeline() {
        assertEquals("Pipeline is healthy", App.status());
    }
}
