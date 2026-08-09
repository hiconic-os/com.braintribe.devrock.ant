// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package com.braintribe.devrock.ant.test.dependencies;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * Regression test for the classpath walk over a <b>pom-typed aggregator</b> dependency.
 *
 * <p>The dependencies-task applies its {@code typeFilter} as a <i>dependency traversal</i> filter. A
 * non-null default of {@code "jar"} therefore silently pruned {@code type=pom} aggregator/BOM edges
 * together with their entire transitive subtree (the assemble of a reflex application funnels its whole
 * dependency set through one such pom edge, so nothing but the terminal remained). The default is now
 * {@code null} (no traversal filter); traversal follows Maven's fixed rules and the aggregator's
 * transitive jars are on the classpath. Which parts are <i>projected</i> stays a separate axis
 * ({@code type} / {@code FileSetTarget}, default {@code :jar}), so no {@code .pom} leaks onto the classpath.
 *
 * <p>Fixture: {@code agg-consumer} --type=pom--> {@code agg} (pom-only) --> {@code lib-a}, {@code lib-b};
 * {@code lib-a} --> {@code lib-c}.
 */
public class DependenciesTaskAggregatorTraversalTest extends TaskRunner implements ProcessNotificationListener {

	@Override
	protected String filesystemRoot() {
		return "dependencies.aggregator";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput("aggregator.tree.definition.yaml");
	}

	@Override
	protected void additionalTasks() {
		TestUtils.copy(new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy(new File(input, "pom.xml"), new File(output, "pom.xml"));
	}

	@Override
	protected void preProcess() {
	}

	@Override
	protected void postProcess() {
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println(msg);
	}

	@Test
	public void aggregatorEdgeIsTraversedByDefault_andPrunedWithJarFilter() {
		process(new File(output, "build.xml"), "dependencies");

		List<String> cpDefault = loadNamesFromFilesetDump(new File(output, "default.classpath.txt"));
		List<String> pomsDefault = loadNamesFromFilesetDump(new File(output, "default.poms.txt"));
		List<String> cpJarFilter = loadNamesFromFilesetDump(new File(output, "jarfilter.classpath.txt"));

		Validator validator = new Validator();

		// default (no traversal filter): the pom aggregator edge is walked, so its transitive jars are on the classpath
		validator.assertTrue("default classpath must contain lib-a-1.0.1.jar", cpDefault.contains("lib-a-1.0.1.jar"));
		validator.assertTrue("default classpath must contain lib-b-1.0.1.jar", cpDefault.contains("lib-b-1.0.1.jar"));
		validator.assertTrue("default classpath must contain transitive lib-c-1.0.1.jar", cpDefault.contains("lib-c-1.0.1.jar"));

		// projection is a separate axis: the classpath carries only jars, never pom parts
		validator.assertTrue("default classpath must not contain any .pom part",
				cpDefault.stream().noneMatch(name -> name.endsWith(".pom")));

		// the aggregator pom itself WAS resolved - it just belongs in the :pom fileset, not on the classpath
		validator.assertTrue("aggregator pom must appear in the :pom fileset", pomsDefault.contains("agg-1.0.1.pom"));

		// explicit typeFilter="jar" reproduces the old broken behavior: the pom edge (and its subtree) is pruned
		validator.assertTrue("jar-filter classpath must NOT contain lib-a-1.0.1.jar (pruned pom edge)",
				!cpJarFilter.contains("lib-a-1.0.1.jar"));
		validator.assertTrue("jar-filter classpath must NOT contain lib-b-1.0.1.jar (pruned pom edge)",
				!cpJarFilter.contains("lib-b-1.0.1.jar"));

		validator.assertResults();
	}
}
