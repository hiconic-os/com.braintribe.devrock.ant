package com.braintribe.build.ant.mc.wire;

import java.io.File;
import java.util.List;

import com.braintribe.devrock.mc.core.wirings.classpath.contract.ClasspathResolverContract;
import com.braintribe.devrock.mc.core.wirings.codebase.CodebaseRepositoryModule;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.util.Lists;

public class AntMcCodebaseWireModule implements WireTerminalModule<ClasspathResolverContract> {
	private CodebaseRepositoryModule codebaseRepositoryModule;
	private AntMcWireModule antMcWireModule;
	
	public AntMcCodebaseWireModule(File devEnvFolder, File codebaseRoot, String codebasePattern) {
		codebaseRepositoryModule = new CodebaseRepositoryModule(codebaseRoot, codebasePattern);
		antMcWireModule = new AntMcWireModule(devEnvFolder, false);
	}
	
	@Override
	public List<WireModule> dependencies() {
		return Lists.list( antMcWireModule, codebaseRepositoryModule);
	}
}
