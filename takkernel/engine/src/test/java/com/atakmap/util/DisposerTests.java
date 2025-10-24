
package com.atakmap.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DisposerTests {
    @Test
    public void direct_invocation() {
        final boolean[] disposed = new boolean[] {
                false
        };
        Disposer d = new Disposer(new gov.tak.api.util.Disposable() {
            @Override
            public void dispose() {
                disposed[0] = true;
            }
        });
        d.close();

        assertTrue(disposed[0]);
    }

    @Test
    public void try_with_resources() {
        final boolean[] disposed = new boolean[] {
                false
        };
        try (Disposer d = new Disposer(new gov.tak.api.util.Disposable() {
            @Override
            public void dispose() {
                disposed[0] = true;
            }
        })) {
        }

        assertTrue(disposed[0]);
    }
}
