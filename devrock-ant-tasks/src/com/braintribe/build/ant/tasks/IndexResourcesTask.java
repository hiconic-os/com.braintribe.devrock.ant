package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newTreeMap;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.tools.ant.Task;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.utils.resource.ResourceMetaDataTools;

/**
 * For given directory computes a {@code Map<String, Resource>} and writes it as YAML.
 * <p>
 * The map contains one entry for each file within given directory.
 * <p>
 * Keys of the resulting map are paths relative to the directory, always written with forward slashes (e.g. "sub-dir/file.txt") and values are
 * {@link Resource} instances with as many properties set as possible. This includes {@link Resource#getMimeType()} and
 * {@link Resource#getSpecification()}, but {@link Resource#getResourceSource()} is left <tt>null</tt>.
 * <p>
 * NOTE that if an error occurs while trying to detect the mime type or specification, this only issues a warning, but doesn't prevent the build from
 * being successful. The reason is
 * <p>
 * Input:<br>
 * {@link #setDir(File)}
 * <p>
 * Output:<br>
 * {@link #setDestfile(File)}
 */
public class IndexResourcesTask extends Task {

	// ################################################
	// ## . . . . . . . Configuration . . . . . . . .##
	// ################################################

	private File dir;
	private Path rootPath;
	private Pattern excludePattern;
	private File destfile;

	/**
	 * Directory to be indexed.
	 * <p>
	 * If it doesn't exist, it's treated as empty and an empty file will be written.
	 * 
	 * @throws IllegalArgumentException
	 *             if given dir exists, but is a file, not a directory.
	 */
	@Required
	public void setDir(File dir) {
		this.dir = dir;
		this.rootPath = dir.toPath();
	}

	/**
	 * Files whose relative path match this regex will be ignored.
	 */
	@Configurable
	public void setExclude(String excludeRegex) {
		if (excludeRegex == null)
			return;

		excludePattern = Pattern.compile(excludeRegex);
	}

	/**
	 * File where the map is written as YAML.
	 * 
	 * @throws IllegalArgumentException
	 *             if given file already exists
	 */
	@Required
	public void setDestfile(File destfile) {
		this.destfile = destfile;
	}

	// ################################################
	// ## . . . . . . . Implementation . . . . . . . ##
	// ################################################

	private final Map<String, Resource> result = newTreeMap();

	@Override
	public void execute() {
		validateInput();
		computeResultingMap();
		writeYml();
	}

	private void validateInput() {
		NullSafe.nonNull(dir, "dir");
		NullSafe.nonNull(destfile, "destfile");

		if (dir.exists() && !dir.isDirectory())
			throw new IllegalArgumentException("Input parameter 'dir' is not a directory but a file: " + dir.getAbsolutePath());

		if (destfile.exists())
			throw new IllegalArgumentException("Input parameter 'destfile' cannot be an existing file: " + destfile.getAbsolutePath());
	}

	private void writeYml() {
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults() //
				.setOutputPrettiness(OutputPrettiness.high) //
				.build();

		FileTools.write(destfile) //
				.usingOutputStream(os -> new YamlMarshaller().marshall(os, result, options));
	}

	private void computeResultingMap() {
		if (dir.exists())
			indexDir(dir);
	}

	private void indexDir(File directory) {
		for (File file : directory.listFiles())
			if (file.isDirectory())
				indexDir(file);
			else
				indexFile(file);
	}

	private void indexFile(File file) {
		String relativePath = toRelativePathWithForwardSlashes(file);

		if (!isExcluded(relativePath))
			result.put(relativePath, ResourceMetaDataTools.fileToResource("devrock", file));
	}

	private boolean isExcluded(String relativePath) {
		return excludePattern != null && excludePattern.matcher(relativePath).matches();
	}

	private String toRelativePathWithForwardSlashes(File file) {
		return rootPath.relativize(file.toPath()).toString().replace("\\", "/");
	}

}
