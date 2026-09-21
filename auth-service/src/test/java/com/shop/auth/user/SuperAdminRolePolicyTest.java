package com.shop.auth.user;

import com.shop.auth.role.Role;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminRolePolicyTest {
    @Test
    void superAdminReceivesEveryApplicationRole() {
        Role superAdmin = role("SUPER_ADMIN");
        Role customer = role("CUSTOMER");
        Role catalog = role("CATALOG_ADMIN");
        Role inventory = role("INVENTORY_ADMIN");
        Set<Role> assigned = new HashSet<>(Set.of(superAdmin));

        SuperAdminRolePolicy.ensureAllApplicationRoles(assigned, List.of(customer, catalog, inventory, superAdmin));

        assertThat(assigned).extracting(Role::getName)
                .containsExactlyInAnyOrder("CUSTOMER", "CATALOG_ADMIN", "INVENTORY_ADMIN", "SUPER_ADMIN");
    }

    @Test
    void superAdminCannotLoseAnotherApplicationRole() {
        Role superAdmin = role("SUPER_ADMIN");
        Role catalog = role("CATALOG_ADMIN");
        Set<Role> assigned = new HashSet<>(Set.of(superAdmin, catalog));

        assertThatThrownBy(() -> SuperAdminRolePolicy.assertCanRemove(assigned, catalog))
                .isInstanceOf(com.shop.auth.common.ConflictException.class)
                .hasMessage("SUPER_ADMIN users must retain all application roles");
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
