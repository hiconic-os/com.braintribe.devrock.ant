// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.util.Map;
import java.util.function.Function;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.template.Template;

/**
 * @author dirk.scheffler
 */
public class ReasonedTemplating {

	public static Maybe<String> merge(String text, Function<String, String> resolver) {
		Template template = Template.parse(text);
		
		try {
			var result = template.evaluate(n -> {
				var value = resolver.apply(n);
				
				if (value == null)
					throw new UnsatisfiedMaybeTunneling(Reasons.build(NotFound.T).text("unresolved variable: " + n).toMaybe());
					
				return value;
			});
			
			return Maybe.complete(result);
		}
		catch (UnsatisfiedMaybeTunneling e) {
			return e.getMaybe();
		}
	}
	
	public static Maybe<String> merge(String text, Map<String, String> vars) {
		return merge(text, vars::get);
	}
}
