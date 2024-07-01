// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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
package com.braintribe.devrock.ant.test.setup;

import java.io.File;

import com.braintribe.devrock.ant.test.common.HasCommonFilesystemNode;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolution;
import com.braintribe.devrock.mc.api.resolver.ArtifactResolver;
import com.braintribe.devrock.mc.api.resolver.DependencyResolver;
import com.braintribe.devrock.mc.core.configuration.StandaloneRepositoryConfigurationLoader;
import com.braintribe.devrock.mc.core.wirings.configuration.contract.RepositoryConfigurationContract;
import com.braintribe.devrock.mc.core.wirings.env.configuration.EnvironmentSensitiveConfigurationWireModule;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.mc.core.wirings.transitive.TransitiveResolverWireModule;
import com.braintribe.devrock.mc.core.wirings.transitive.contract.TransitiveResolverContract;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.utils.archives.Archives;
import com.braintribe.ve.api.VirtualEnvironment;
import com.braintribe.ve.impl.OverridingEnvironment;
import com.braintribe.ve.impl.StandardEnvironment;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;

/**
 * prepares the environment to run the ant tests  
 * <br/>
 * - gets the ant installation from 'com.braintribe.devrock.ant:ant-distribution#[1.10.1,2.0) / contents:zip'
 * - gets the bt-ant-tasks-ng from 'com.braintribe.devrock.ant:bt-ant-tasks-ng#[1.0, 2.0) / libs:zip'
 * - unpacks both 
 * 		ant into 'ant' (gets bin and lib),
 * 		bt ant tasks into lib. 
 *   
 *  if you rename the btAntTasksCdi you can control which bt-ant-tasks is tested. NOTE THAT CURRENTLY THE TESTS ARE VALIDATING FOR 
 *  BT-ANT-TASKS-NG, so if you switch back, some tests may fail. See actual tests for details
 * @author pit
 *
 */
public class AntSetterUpper implements HasCommonFilesystemNode {
	private static CompiledDependencyIdentification antCdi = CompiledDependencyIdentification.parse("com.braintribe.devrock.ant:ant-distribution#[1.0,2.0)");
	private static CompiledDependencyIdentification btAntTasksCdi = CompiledDependencyIdentification.parse("com.braintribe.devrock.ant:devrock-ant-tasks#[1.0, 2.0)");
	//private static CompiledDependencyIdentification btAntTasksCdi = CompiledDependencyIdentification.parse("com.braintribe.devrock.ant:bt-ant-tasks#[1.0, 2.0)");
	
			
	private File antHome;	
	{		
		antHome = new File( "res/ant");
	}
	
	private Maybe<RepositoryConfiguration> provideRepositoryConfiguration(File base) {
		
		StandaloneRepositoryConfigurationLoader marshaller = new StandaloneRepositoryConfigurationLoader();
		
		VirtualEnvironment ve = new OverridingEnvironment(StandardEnvironment.INSTANCE);
		ve.getEnvironmentOverrrides().put( "config.base", base.getAbsolutePath());
		
		marshaller.setVirtualEnvironment(ve);
		
		File file = new File( "res/input/repository-configuration.yaml");
		return marshaller.loadRepositoryConfiguration(file);			
	}
	
	
	public void prepareTestEnviroment(File base) {
		// check if ant repository exists ... existance test may be too cheap here 
		if (antHome.exists()) {
			return; 
		}
		Maybe<RepositoryConfiguration> myRepositoryConfigurationMaybe = provideRepositoryConfiguration(base);
		
		// wired stuff 				
	    try (               
	            WireContext<TransitiveResolverContract> resolverContext = Wire.contextBuilder( TransitiveResolverWireModule.INSTANCE)
	                .bindContract(RepositoryConfigurationContract.class, () -> myRepositoryConfigurationMaybe)
	                .build();
	        ) {
			        						
			buildTestEnvironment(resolverContext);			
		}
		catch (Exception e) {
			throw Exceptions.uncheckedAndContextualize(e, "cannot setup test enviroment", IllegalStateException::new);
		}
	}
	
	public void prepareTestEnviroment() {
		// check if ant repository exists ... existance test may be too cheap here 
		if (antHome.exists()) {
			return; 
		}
		
		
		// wired stuff 				
	    try (               
	            WireContext<TransitiveResolverContract> resolverContext = Wire.contextBuilder( TransitiveResolverWireModule.INSTANCE, EnvironmentSensitiveConfigurationWireModule.INSTANCE)	    
	                .build();
	        ) {			        					
			buildTestEnvironment(resolverContext);			
		}
		catch (Exception e) {
			throw Exceptions.uncheckedAndContextualize(e, "cannot setup test enviroment", IllegalStateException::new);
		}
	}


	private void buildTestEnvironment(WireContext<TransitiveResolverContract> resolverContext) throws Exception {
		ArtifactDataResolverContract dataResolverContract = resolverContext.contract().dataResolverContract();			
		DependencyResolver dependencyResolver = dataResolverContract.dependencyResolver();			
		ArtifactResolver artifactResolver = dataResolverContract.artifactResolver();

		//
		// step one : resolve dependencies to get the newest versions
		//
		
		// ant
		Maybe<CompiledArtifactIdentification> antCaiOptional = dependencyResolver.resolveDependency(antCdi);			
		if (!antCaiOptional.isSatisfied()) {
			throw new IllegalStateException("no artifact found for dependency [" + antCdi.asString()); 
		}
		CompiledArtifactIdentification antCai = antCaiOptional.get();					

		// bt ant tasks
		Maybe<CompiledArtifactIdentification> btAntTasksCaiOptional = dependencyResolver.resolveDependency(btAntTasksCdi);			
		if (!btAntTasksCaiOptional.isSatisfied()) {
			throw new IllegalStateException("no artifact found for dependency [" + btAntTasksCdi.asString()); 
		}
		CompiledArtifactIdentification btAntTasksCai = btAntTasksCaiOptional.get();					
		
		//
		// step two : resolve parts 
		//
		
		// ant
		Maybe<ArtifactDataResolution> antArchiveResolutionOptional = artifactResolver.resolvePart( antCai, PartIdentification.create("contents", "zip"));
		
		if (!antArchiveResolutionOptional.isSatisfied()) {
			throw new IllegalStateException("no installation archive found in [" + antCai.asString());
		}			
		ArtifactDataResolution antArchiveResolution = antArchiveResolutionOptional.get();
		
		// bt-ant-tasks
		Maybe<ArtifactDataResolution> btAntTasksArchiveResolutionOptional = artifactResolver.resolvePart( btAntTasksCai, PartIdentification.create("libs", "zip"));
		
		if (!btAntTasksArchiveResolutionOptional.isSatisfied()) {
			throw new IllegalStateException("no installation archive found in [" + btAntTasksCai.asString());
		}
		ArtifactDataResolution btArtifactDataResolution = btAntTasksArchiveResolutionOptional.get();
		
		//
		// step three : install 
		//
		
		// ant
		try {
			Archives.zip().from( antArchiveResolution.getResource().openStream()).unpack( new File("res"));
		} catch (Exception e) {
			throw Exceptions.contextualize(e, "cannot unpack ant archive from [" + antCai.asString() + "]");
		}
		
		
		// bt-ant-tasks
		try {
			Archives.zip().from( btArtifactDataResolution.getResource().openStream()).unpack( new File(antHome, "lib"));
		}
		catch (Exception e) {
			throw Exceptions.contextualize(e, "cannot unpack ant archive from [" + antCai.asString() + "]");
		}
	}
	
	public static void main(String[] args) {
		AntSetterUpper asu = new AntSetterUpper();
		
		asu.prepareTestEnviroment( new File( args[1]));
	}
}
