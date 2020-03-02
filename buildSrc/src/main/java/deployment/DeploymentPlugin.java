package deployment;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Locale;

public class DeploymentPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final NamedDomainObjectContainer<DeploymentProcessConfiguration> configs = project.container(DeploymentProcessConfiguration.class);
        configs.all(config -> {
            config.environmentSteps = new ArrayList<>();
            project.getTasks().register(String.format(Locale.ROOT, "deploymentRequest_%s", config.name), DeploymentRequestProcessTask.class, config);
        });
        project.getExtensions().add("deployments", configs);
    }

}
