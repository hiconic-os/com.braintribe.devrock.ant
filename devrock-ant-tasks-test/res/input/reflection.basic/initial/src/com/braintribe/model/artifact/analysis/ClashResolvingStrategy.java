package com.braintribe.model.artifact.analysis;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * defines the two possible strategies to resolve clashes:
 * {@link #firstOccurrence} means resolving by dependency order of the clashing dependencies,
 * {@link #highestVersion} means resolving by the highest version of the clashing dependencies
 *  
 * @author pit
 *
 */
public enum ClashResolvingStrategy implements EnumBase {
		
	firstOccurrence, highestVersion;
	
	public static EnumType T = EnumTypes.T(ClashResolvingStrategy.class);

	@Override
	public EnumType type() {
		return T;
	}

	
}

