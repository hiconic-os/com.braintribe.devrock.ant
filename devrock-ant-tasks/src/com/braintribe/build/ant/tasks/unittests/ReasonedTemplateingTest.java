package com.braintribe.build.ant.tasks.unittests;

import java.util.Map;

import com.braintribe.build.ant.tasks.ReasonedTemplating;
import com.braintribe.gm.model.reason.Maybe;

public class ReasonedTemplateingTest {

	public static void main(String[] args) {
		String text = "Hello ${groupId}:${artifactId}#${version}";
		Maybe<String> maybe = ReasonedTemplating.merge(text, Map.of("groupIds", "foo.bar", "artifactId", "fix-fox", "version", "1.0"));
		System.out.println(maybe.get());
	}

}
