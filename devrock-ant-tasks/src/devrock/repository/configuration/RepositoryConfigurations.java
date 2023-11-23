// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package devrock.repository.configuration;

import java.io.File;

import com.braintribe.devrock.mc.api.repository.configuration.RepositoryConfigurationLocator;
import com.braintribe.devrock.mc.core.configuration.ConfigurableRepositoryConfigurationLoader;
import com.braintribe.devrock.mc.core.configuration.RepositoryConfigurationLocators;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.ve.impl.StandardEnvironment;

public interface RepositoryConfigurations {
	static Maybe<RepositoryConfiguration> load(File configFile) {
		RepositoryConfigurationLocator locator = RepositoryConfigurationLocators.build().addLocation(configFile).done();

		ConfigurableRepositoryConfigurationLoader loader = new ConfigurableRepositoryConfigurationLoader();
		loader.setVirtualEnvironment(StandardEnvironment.INSTANCE);
		loader.setLocator(locator);
		
		return loader.get();
	}
}
