package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.utils.IOTools;

/**
 * tests the import task: a) does the resolving properly work, b) is the target properly exported, c) can the calling build.xml use the imported targets. 
 * 
 * @author pit
 *
 */
public class ImportTaskTest extends TaskRunner {
	private String message;

	@Override
	protected String filesystemRoot() {		
		return "import";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( new File( output, "import.definition.yaml"));
	}
	
	
	@Override
	protected File settings() {	
		return new File( input, "settings.xml");
	}
	
	@Override
	protected void additionalTasks() {
		Map<String,String> variables = new HashMap<>();
		variables.put("${output}", output.getAbsolutePath().replace("\\", "/"));
		copyAndTouchTextFile( new File(input, "import.definition.yaml"), new File(output, "import.definition.yaml"), variables);							 
	}

	@Override
	protected void preProcess() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy( new File(input, "import-task-1.0.1-import.xml"), new File(output, "import-task-1.0.1-import.xml"));
	}

	@Override
	protected void postProcess() {
		File messageFile = new File( output, "imported.txt");
		if (messageFile.exists()) {
			try {
				message = IOTools.slurp(messageFile, "UTF-8");
			} catch (IOException e) {
			}
		}

	}

	@Test
	public void test() {
		process( new File( output, "build.xml"), "test");
		Assert.assertTrue( "no result of imported task found", message != null);
		Assert.assertTrue("message is unexpectedly [" + message + "]", message.equals("this is the imported task"));
	}
}
