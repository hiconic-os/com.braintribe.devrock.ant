// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks.extractor.fileset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.braintribe.devrock.mc.core.declared.commons.HashComparators;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.essential.PartIdentification;

/**
 * helper class to deal with {@link PartIdentification} and {@link Collection}s thereof<br/>
 * parses the {@link PartTuple} from a string, formatted as follows:
 * [<classifier>]:[<extension>],<symbol>[,expression] <br/>
 *	ie.<br/>
 *		sources:jar,jar,:war,:xml,md5 <br/>
 * values from {@link PartType} are directly supported.
 * 
 * @author Pit
 *
 */
public class RelevantPartTupleProcessor {
	private static Logger log = Logger.getLogger(RelevantPartTupleProcessor.class);
	
	/**
	 * parses a string and extracts {@link PartIdentification} from it
	 * @param type - the string as in the format described in the class doc 
	 * @return - a {@link List} of {@link PartIdentification}
	 */
	public static List<PartIdentification> parseRelevantPartTuplesFromString( String type) {
		if (type == null)
			return null;
		List<PartIdentification> result = new ArrayList<PartIdentification>();
		String [] tokens = type.split(",");
		for (String token : tokens) {
			PartIdentification partIdentification = PartIdentification.parse(token);
			result.add(partIdentification);
		}
		
		return result;
	}
	
	/**
	 * merges two {@link Collection}s into a new list of {@link PartIdentification}, only unique {@link PartIdentification} are returned
	 * @param configured - the {@link Collection} of {@link PartIdentification} to merge into
	 * @param specified - the {@link Collection} to merge from 
	 * @return - a new {@link List} that contains the unique {@link PartIdentification} from both lists
	 */
	public static List<PartIdentification> mergeRelevantPartTuplesForEnricher( Collection<PartIdentification> configured, Collection<PartIdentification> specified){
		List<PartIdentification> result = new ArrayList<>( configured);
		if (specified == null) {
			return result;
		}
		
		for (PartIdentification specifiedTuple : specified) {
			boolean toAdd = true;
			for (PartIdentification configuredTuple : result) {
				if (HashComparators.partIdentification.compare(specifiedTuple, configuredTuple)) {
					toAdd = false;
					break;
				}
			}
			if (toAdd) {
				result.add( specifiedTuple);
			}
		}
		return result;
	}

	
	private static final List<String> excludedJarClassifiersForCp = Arrays.asList("sources", "javadoc");
	
	/**
	 * checks whether a collection of {@link PartTuple} contains a specified {@link PartTuple},
	 * it does this be iterating over the collection and calling the {@link PartTupleProcessor} to check.
	 * @param partTuples - the {@link Collection} of {@link PartTuple} to scan 
	 * @param suspect - the {@link PartTuple} to look for 
	 * @return - true if it is contained, false otherwise 
	 */
	public static boolean contains( Collection<PartIdentification> partTuples, PartIdentification suspect) {
		for (PartIdentification tuple: partTuples) {
			if (HashComparators.partIdentification.compare(tuple, FilesetConstants.classpathPartIdentification)) {
				if ("jar".equals(suspect.getType()) && !excludedJarClassifiersForCp.contains(suspect.getClassifier()))
					return true;
			}
			else {
				if (HashComparators.partIdentification.compare(tuple, suspect))
					return true;
			}
		}
		return false;
	}
}
