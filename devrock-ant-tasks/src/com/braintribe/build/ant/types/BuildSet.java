// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.ProjectComponent;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.devrock.mc.api.transitive.RangedTerminals;
import com.braintribe.devrock.mc.core.resolver.transitive.RangedTerminalParser;
import com.braintribe.devrock.model.repository.filters.ArtifactFilter;
import com.braintribe.devrock.model.repository.filters.DisjunctionArtifactFilter;
import com.braintribe.devrock.model.repository.filters.GroupsArtifactFilter;
import com.braintribe.devrock.model.repository.filters.NoneMatchingArtifactFilter;
import com.braintribe.devrock.model.repository.filters.QualifiedArtifactFilter;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.version.Version;
import com.braintribe.model.version.VersionExpression;
import com.braintribe.model.version.VersionRange;

public class BuildSet extends ProjectComponent {
	private static com.braintribe.model.version.VersionRange fullRange;
	
	static {
		fullRange = VersionRange.T.create();
		fullRange.setLowerBoundExclusive(true);
		fullRange.setUpperBoundExclusive(true);
	}
	
	private String buildRangeAsStr;
	private File codebaseRoot;
	private String codebasePattern = "${groupId}/${version}/${artifactId}";
	private String defaultGroup;
	private String defaultVersion;
	private String groups;
	private RangedTerminals rangedTerminals;
	private File groupsFolder;
	
	/**
	 * Configures the root folder of the codebase on which the pattern configured with {@link #setCodebasePattern(String)} is applied upon. 
	 */
	@Required
	public void setCodebaseRoot(File codebaseRoot) {
		this.codebaseRoot = codebaseRoot;
	}

	/**
	 * This configures the buildRange with a string that is parsed. It contains upper and lower boundaries for the build walk.
	 * The string is a '+' delimited list of boundaries using '(', ')', '[', ']':
	 */
	@Required
	public void setBuildRange(String buildRangeAsStr) {
		this.buildRangeAsStr = buildRangeAsStr;
	}

	/**
	 * Optionally configures the pattern that describes the filesystem structure of the codebase as a deduction from artifact identification properties.
	 * The default is "${groupId}/${version}/${artifactId}"
	 */
	@Configurable
	public void setCodebasePattern(String codebasePattern) {
		this.codebasePattern = codebasePattern;
	}
	
	/**
	 * Optionally configures the string that will be parsed to feed the artifact filter. The syntax of the parsed string is:
	 * groupName1+groupName2+groupName3#maj.min+.....
	 * If used this configuration property will overrule any configuration given by {@link #setGroupsFolder(File)}.
	 */
	@Configurable
	public void setGroups(String groups) {
		this.groups = groups;
	}
	
	/**
	 * Optionally configures the folder that will be enumerated to feed the artifact filter with full version ranged groups.
	 * This configuration property will not be taken into account when {@link #setGroups(String)} was also used for configuration.
	 */
	@Configurable
	public void setGroupsFolder(File groupsFolder) {
		this.groupsFolder = groupsFolder;
	}
	
	
	/**
	 * Optionally configures the group to be used if a given artifact has not been qualified with a group
	 */
	@Configurable
	public void setDefaultGroup(String defaultGroup) {
		this.defaultGroup = defaultGroup;
	}
	
	/**
	 * Optionally configures the version to be used if a given artifact has not been qualified with a version 
	 */
	@Configurable
	public void setDefaultVersion(String defaultVersion) {
		this.defaultVersion = defaultVersion;
	}
	
	public String getCodebasePattern() {
		return codebasePattern;
	}

	private static VersionExpression hotfixRange(Version version) {

		if (version.getMinor() == null) {
			return VersionRange.from(version, true, Version.create(version.getMajor() + 1), false);
		}
		else if (version.getRevision() == null) { 
			return VersionRange.from(version, true, Version.create(version.getMajor(), version.getMinor() + 1), false);
		}
		else {
			return version;
		}
	}
	
	public ArtifactFilter getArtifactFilter() {
		
		if (groups != null) {
			List<ArtifactFilter> filters = new ArrayList<>();
			
			String groupIdentifications[] = groups.split("\\+");
			
			for (String groupIdentification: groupIdentifications) {
				String groupIdentificationParts[] = explodeGroupIdentification(groupIdentification);
				
				VersionExpression versionRange = fullRange;
				String groupId = groupIdentification;
				String version = null;
				
				if (groupIdentificationParts != null) {
					groupId = groupIdentificationParts[0];
					String versionAsStr = groupIdentificationParts[1]; //
					
					version = hotfixRange(Version.parse(versionAsStr)).asString();
				}
				
				QualifiedArtifactFilter filter = QualifiedArtifactFilter.T.create();
				filter.setGroupId(groupId);
				filter.setVersion(version);
				filters.add(filter);
			}
			return combineFilters(filters);
		}
		else if (groupsFolder != null) {
			GroupsArtifactFilter filter = GroupsArtifactFilter.T.create();
			for (File file: groupsFolder.listFiles()) {
				if (file.isDirectory()) {
					String groupId = file.getName();
					filter.getGroups().add(groupId);
				}
			}
			
			return filter;
		}
		else {
			List<ArtifactFilter> filters = new ArrayList<>();
			
			RangedTerminals rangedTerminals = getRangedTerminals();
			
			for (CompiledTerminal terminal: rangedTerminals.terminals()) {
				final VersionExpression versionExpression;

				if (terminal instanceof CompiledDependencyIdentification)
					versionExpression = ((CompiledDependencyIdentification) terminal).getVersion();
				else
					versionExpression = ((CompiledArtifact) terminal).getVersion();
				
				String groupId = terminal.getGroupId();
				String version = versionExpression.asString();
				
				QualifiedArtifactFilter filter = QualifiedArtifactFilter.T.create();
				filter.setGroupId(groupId);
				filter.setVersion(version);
				filters.add(filter);
			}
			return combineFilters(filters);
		}
	}

	private ArtifactFilter combineFilters(List<ArtifactFilter> filters) {
		switch (filters.size()) {
		case 0:
			return NoneMatchingArtifactFilter.T.create();
		case 1:
			return filters.get(0);
		default:
			DisjunctionArtifactFilter disjunction = DisjunctionArtifactFilter.T.create();
			disjunction.setOperands(filters);
			return disjunction;
		}
	}
	
	private static Pattern groupsPattern = Pattern.compile("(.*)#(.*)");
	
	private static String[] explodeGroupIdentification(String name) {
		Matcher matcher = groupsPattern.matcher(name);
		
		if (matcher.matches()) {
			return new String[] {
					matcher.group(1),
					matcher.group(2),
			};
		}
		
		return null;
	}
	
	public File getCodebaseRoot() {
		return codebaseRoot;
	}
	
	public RangedTerminals getRangedTerminals() {
		if (rangedTerminals == null) {
			rangedTerminals = RangedTerminalParser.parseFromConcatString(buildRangeAsStr, defaultGroup, g -> g.equals(defaultGroup)? defaultVersion: null);
		}

		return rangedTerminals;
	}

	public String getDefaultVersion() {
		return defaultVersion;
	}
	
}
