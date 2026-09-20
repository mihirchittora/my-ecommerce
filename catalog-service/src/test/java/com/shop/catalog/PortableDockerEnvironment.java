package com.shop.catalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Supplies a repository-owned fallback for local Docker runtimes when Maven is
 * launched without DOCKER_HOST. An explicit DOCKER_HOST remains authoritative.
 */
final class PortableDockerEnvironment {
    private PortableDockerEnvironment() {
    }

    static void configure() {
        TestcontainersConfiguration configuration = TestcontainersConfiguration.getInstance();
        if (hasText(System.getenv("DOCKER_HOST"))
                || hasText(System.getProperty("docker.host"))
                || hasText(configuration.getUserProperty("docker.host", null))
                || hasText(configuration.getClasspathProperties().getProperty("docker.host"))) {
            return;
        }

        Path home = Path.of(System.getProperty("user.home"));
        List<Path> candidates = List.of(
                home.resolve(".colima/default/docker.sock"),
                home.resolve(".docker/run/docker.sock"));

        candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .ifPresent(socket -> configuration.getUserProperties()
                        .setProperty("docker.host", "unix://" + socket));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
