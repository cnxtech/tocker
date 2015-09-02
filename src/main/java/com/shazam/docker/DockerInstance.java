package com.shazam.docker;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;

public class DockerInstance {

    private final String imageName;
    private final String containerName;
    private DefaultDockerClient dockerClient;

    public DockerInstance(String imageName, String containerName) {
        this.imageName = imageName;
        this.containerName = containerName;

        try {
            dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static DockerInstanceBuilder fromImage(String imageName) {
        return new DockerInstanceBuilder(imageName);
    }

    public String host() {
        return dockerClient.getHost();
    }

    public void run() {
        withClient((client) -> {
            try {
                ContainerInfo containerInfo = client.inspectContainer(containerName);
                client.startContainer(containerInfo.id());
            } catch (DockerException de) {
                ContainerConfig containerConfig = ContainerConfig.builder()
                        .image(imageName)
                        .build();
                try {
                    client.inspectImage(imageName);
                } catch (ImageNotFoundException infe) {
                    client.pull(imageName);
                }
                ContainerCreation container = client.createContainer(containerConfig, containerName);
                client.startContainer(container.id());
            }
        });
    }

    public void stop() {
        withClient((client) -> client.stopContainer(containerName, 10));
    }

    private void withClient(DockerCommand consumer) {
        try {
            consumer.runCommand(dockerClient);
        } catch (Exception de) {
            throw new RuntimeException(de);
        }
    }

    private static class DockerInstanceBuilder {
        private String imageName;
        private String containerName;

        public DockerInstanceBuilder(String imageName) {
            this.imageName = imageName;
        }

        public DockerInstance build() {
            return new DockerInstance(imageName, containerName);
        }

        public DockerInstanceBuilder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }
    }

    private static interface DockerCommand {
        public void runCommand(DockerClient dc) throws Exception;
    }
}
