
package com.atakmap.math;

import gov.tak.test.KernelJniTest;

import org.junit.Test;

public class PlaneTest extends KernelJniTest {
    @Test
    public void create_plane() {
        Plane p = new Plane(new Vector3D(0d, 0d, 1d), new PointD(0d, 0d, 0d));
    }
}
