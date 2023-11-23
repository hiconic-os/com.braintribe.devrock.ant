package com.braintribe.build.ant.mc.wire.space;

import com.braintribe.devrock.mc.core.wirings.resolver.contract.RepositoryConfigurationEnrichingContract;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;
import com.braintribe.wire.api.space.WireSpace;

@Managed
public class OfflineSpace implements WireSpace {
	@Import
	RepositoryConfigurationEnrichingContract repositoryConfigurationEnriching;

	@Override
	public void onLoaded(WireContextConfiguration configuration) {	
		repositoryConfigurationEnriching.enrichRepositoryConfiguration(() -> this::enrichRepositoryConfiguration);
	}
	
	private void enrichRepositoryConfiguration( RepositoryConfiguration repositoryConfiguration) {
		String property = System.getProperty("offline");
		if (Boolean.TRUE.toString().equals( property)) {
			repositoryConfiguration.setOffline(true);
		}
	}
}
