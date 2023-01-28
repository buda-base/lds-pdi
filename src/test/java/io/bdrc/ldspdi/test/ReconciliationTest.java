package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.bdrc.ldspdi.rest.controllers.ReconciliationController;

public class ReconciliationTest {

    public void assertNormalize(final String orig, final String expected) {
        final String repl = ReconciliationController.normalize(orig, "person");
        System.err.println(orig+" -> "+repl);
        assertTrue(repl.equals(expected));
    }
    
    @Test
    public void testNormalization() {
        System.out.println("test");
        assertNormalize("so and so rin po che", "so and so");
        assertNormalize("@sog mo so and so rin po che/ (1687-1725)", "so and so");
        assertNormalize("gter ston chen po so and so", "so and so");
        assertNormalize("rgyal ba rin po che", "rgyal ba rin po che");
    }
    
}
