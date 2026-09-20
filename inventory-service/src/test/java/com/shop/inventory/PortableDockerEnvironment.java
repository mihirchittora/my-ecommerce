package com.shop.inventory;

import org.testcontainers.utility.TestcontainersConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Repository-owned Colima/Docker socket fallback for Maven-launched tests. */
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
        candidates.stream().filter(Files::exists).findFirst().ifPresent(socket ->
                configuration.getUserProperties().setProperty("docker.host", "unix://" + socket));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
