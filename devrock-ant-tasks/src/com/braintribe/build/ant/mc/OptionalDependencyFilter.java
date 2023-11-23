package com.braintribe.build.ant.mc;

import java.util.function.Predicate;

import com.braintribe.devrock.mc.api.transitive.ArtifactPathElement;
import com.braintribe.devrock.mc.api.transitive.DependencyPathElement;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.compiled.CompiledDependency;

public class OptionalDependencyFilter implements Predicate<DependencyPathElement> {
	private static final String SCOPE_PROVIDED = "provided";
	private static final String SCOPE_COMPILE = "compile";
	private static final String SCOPE_RUNTIME = "runtime";
	private String scope;
	
	public OptionalDependencyFilter(String scope) {
		this.scope = scope;
		
	}

	@Override
	public boolean test(DependencyPathElement t) {
		boolean isTerminalDependency = isTerminalDependency( t);
		AnalysisDependency dependency = t.getDependency();
		CompiledDependency compiledDependency = dependency.getOrigin();
		
		// dependency scope, default is 'compile'
		String dependencyScope = compiledDependency.getScope();
		if (dependencyScope == null) {
			dependencyScope = SCOPE_COMPILE;
		}
		
		// if 'runtime' magick scope and 'provided' dependency scope -> filter  
		if (scope.equals( SCOPE_RUNTIME) && dependencyScope.equals( SCOPE_PROVIDED)) {
			return false;
		}
			
		// if not a terminal dependency and optional -> filter	
		if (!isTerminalDependency && compiledDependency.getOptional()) {			
			return false;
		}
		// passed
		return true;
	
	}

	private boolean isTerminalDependency(DependencyPathElement dependencyPathElement) {
		ArtifactPathElement parent = dependencyPathElement.getParent();
		return parent == null || parent.getParent() == null;
	}
	
}
