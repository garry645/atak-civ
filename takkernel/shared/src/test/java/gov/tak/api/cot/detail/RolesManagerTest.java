package gov.tak.api.cot.detail;

import org.junit.Assert;
import org.junit.Test;

public class RolesManagerTest {
    @Test
    public void default_roles_regression_check() {
        RolesManager rolesManager = new RolesManager();
        // parent isn't an assignable role
        Assert.assertFalse(rolesManager.isDefault(new Role.Builder("Default").build()));
        // verify that all legacy assignable roles are detected as default
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("Team Member", "TM").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("Team Lead", "TL").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("HQ", "HQ").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("Sniper", "S").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("Medic", "M").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("Forward Observer", "FO").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("RTO", "R").build()));
        Assert.assertTrue(rolesManager.isDefault(new Role.Builder("K9", "K9").build()));
    }
}
