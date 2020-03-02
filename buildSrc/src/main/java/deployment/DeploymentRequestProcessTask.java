package deployment;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import github.Deployment;

public class DeploymentRequestProcessTask extends DefaultTask {

    private final DeploymentProcessConfiguration mConfiguration;

    @Input
    public int stepIndex;

    @Inject
    public DeploymentRequestProcessTask(DeploymentProcessConfiguration configuration) {
        mConfiguration = configuration;
        setGroup("Publishing");
        setDescription("Request new deployment of " + getEnvironmentName(configuration, 0));
    }

    @TaskAction
    public void deploymentRequestAction() {
        try {
            deploymentRequest(stepIndex, getProject().getProperties(), mConfiguration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void deploymentRequest(int step, Map<String, ?> properties, DeploymentProcessConfiguration configuration) throws Exception {
        final DeploymentCommandLineArgs data = new DeploymentCommandLineArgs(
                properties.get("requestDeploy.sha").toString(),
                properties.get("requestDeploy.api_user_name").toString(),
                properties.get("requestDeploy.api_user_token").toString());

        Deployment deployment = new Deployment(data.apiUsername, data.apiUserToken);
        if (step == 0) {
            requestNewDeploy(deployment, data, configuration);
        } else {
            throw new UnsupportedOperationException("step " + step + " for " + configuration.name + " is not implemented!");
        }
    }

    private static void requestNewDeploy(Deployment deployment, DeploymentCommandLineArgs data, DeploymentProcessConfiguration environment) throws Exception {
        final String environmentToDeploy = getEnvironmentName(environment, 0);
        final List<String> environmentsToKill = environment.environmentSteps
                .stream()
                .map(name -> getEnvironmentName(environment.name, name))
                .filter(env -> !env.equals(environmentToDeploy))
                .collect(Collectors.toList());

        final Deployment.Response response = deployment.requestDeployment(new Deployment.Request(
                data.sha, "deploy", false,
                environmentToDeploy, String.format(Locale.ROOT, "Deployment for '%s' request by '%s'.", environmentToDeploy, data.apiUsername),
                Collections.singletonList("master-green-requirement"),
                new Deployment.RequestPayloadField(environmentsToKill)));

        System.out.println(String.format(Locale.ROOT,
                "Deploy request response: id %s, sha %s, environment %s, task %s.", response.id, response.sha, response.environment, response.task));
    }

    private static String getEnvironmentName(String environmentName, String stepName) {
        return String.format(Locale.ROOT, "%s_%s", environmentName, stepName);
    }

    private static String getEnvironmentName(DeploymentProcessConfiguration environment, int index) {
        return getEnvironmentName(environment.name, environment.environmentSteps.get(index));
    }
}
