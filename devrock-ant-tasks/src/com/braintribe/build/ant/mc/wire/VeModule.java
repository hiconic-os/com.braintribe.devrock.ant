package com.braintribe.build.ant.mc.wire;

import com.braintribe.devrock.mc.core.wirings.venv.contract.VirtualEnvironmentContract;
import com.braintribe.ve.api.VirtualEnvironment;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;

public class VeModule implements WireModule, VirtualEnvironmentContract {
	private VirtualEnvironment ve;
	
	public VeModule(VirtualEnvironment ve) {
		this.ve = ve;
	}
	
	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		contextBuilder.bindContract(VirtualEnvironmentContract.class, this);
	}
	
	@Override
	public VirtualEnvironment virtualEnvironment() {
		return ve;
	}
}
