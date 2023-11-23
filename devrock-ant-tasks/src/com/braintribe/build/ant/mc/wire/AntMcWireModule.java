package com.braintribe.build.ant.mc.wire;

import java.io.File;
import java.util.List;

import com.braintribe.build.ant.mc.wire.space.OfflineSpace;
import com.braintribe.devrock.mc.api.repository.configuration.RepositoryConfigurationLocaterBuilder;
import com.braintribe.devrock.mc.api.repository.configuration.RepositoryConfigurationLocator;
import com.braintribe.devrock.mc.core.configuration.RepositoryConfigurationLocators;
import com.braintribe.devrock.mc.core.wirings.classpath.ClasspathResolverWireModule;
import com.braintribe.devrock.mc.core.wirings.classpath.contract.ClasspathResolverContract;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.DevelopmentEnvironmentContract;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.RepositoryConfigurationLocatorContract;
import com.braintribe.devrock.mc.core.wirings.env.configuration.EnvironmentSensitiveConfigurationWireModule;
import com.braintribe.utils.paths.UniversalPath;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.util.Lists;

public class AntMcWireModule implements WireTerminalModule<ClasspathResolverContract> {
	/*
	 * ${devenv}/artifacts/repository-configuration-devrock-ant-tasks.yaml ENVVAR
	 * DEVROCK_REPOSITORY_CONFIGURATION_DEVROCK_ANT_TASKS
	 * ${anthome}/.devrock/repository-configuration.yaml
	 * ${userhome}/.devrock/repository-configuration-devrock-ant-tasks.yaml
	 */

	private static final String ENV_DEVROCK_REPOSITORY_CONFIGURATION = "REPOSITORY_CONFIGURATION_DEVROCK_ANT_TASKS";
	private static final String FILENAME_REPOSITORY_CONFIGURATION = "repository-configuration-devrock-ant-tasks.yaml";

	private File devEnvFolder;
	private RepositoryConfigurationLocator repoConfigLocator;

	public AntMcWireModule(File devEnvFolder, boolean ant) {
		super();
		this.devEnvFolder = devEnvFolder;

		if (ant) {
			repoConfigLocator = RepositoryConfigurationLocators.build() //
					.add(buildAntLocator()) //
					.add(RepositoryConfigurationLocators.buildDefault()
							.collectorReasonMessage("No repository configuration found at standard locations").done())
					.done();
		} else {
			repoConfigLocator = RepositoryConfigurationLocators.buildDefault().done();
		}
	}

	private static RepositoryConfigurationLocator buildAntLocator() {

		UniversalPath userDirSpecificRepoConfig = UniversalPath
				.start(RepositoryConfigurationLocators.FOLDERNAME_DEVROCK).push(FILENAME_REPOSITORY_CONFIGURATION);
		UniversalPath devEnvSpecificRepoConfig = UniversalPath
				.start(RepositoryConfigurationLocators.FOLDERNAME_ARTIFACTS).push(FILENAME_REPOSITORY_CONFIGURATION);

		RepositoryConfigurationLocaterBuilder builder = RepositoryConfigurationLocators.build() //
				.addDevEnvLocation(devEnvSpecificRepoConfig) //
				.addLocationEnvVariable(ENV_DEVROCK_REPOSITORY_CONFIGURATION);

		String antHome = System.getenv("ANT_HOME");
		if (antHome != null) {
			File antSpecificRepoConfig = UniversalPath.start(antHome)
					.push(RepositoryConfigurationLocators.FOLDERNAME_DEVROCK)
					.push(RepositoryConfigurationLocators.FILENAME_REPOSITORY_CONFIGURATION).toFile();
			builder.addLocation(antSpecificRepoConfig);
		}

		return builder //
				.addUserDirLocation(userDirSpecificRepoConfig) //
				.collectorReasonMessage("No ant specific repository configuration found") //
				.done();
	}

	@Override
	public List<WireModule> dependencies() {
		return Lists.list(ClasspathResolverWireModule.INSTANCE, EnvironmentSensitiveConfigurationWireModule.INSTANCE);
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(DevelopmentEnvironmentContract.class, () -> devEnvFolder);
		contextBuilder.bindContract(RepositoryConfigurationLocatorContract.class, () -> repoConfigLocator);
		contextBuilder.autoLoad(OfflineSpace.class);
	}

}
