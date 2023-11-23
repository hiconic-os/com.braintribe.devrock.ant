package com.braintribe.build.ant.tasks.extractor.fileset;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;

public class StaticResourceCollection extends FileSet implements ResourceCollection {
	private List<Resource> resources = new ArrayList<>();

	public void add(File file) {
		resources.add(new FileResource(file));
	}
			
	@Override
	public Iterator<Resource> iterator() {
		return resources.iterator();
	}

	@Override
	public int size() {
		return resources.size();
	}
	
	@Override
	public StaticResourceCollection clone() {
		StaticResourceCollection fs = new StaticResourceCollection();
		fs.resources.addAll(this.resources);
		return fs;
	}
	
	@Override
	public boolean isEmpty() {
		return resources.isEmpty(); 
	}

	@Override
	public boolean isFilesystemOnly() {
		return true;
	}
	
	@Override
	public String toString() {
		return this.resources.stream().map(Resource::toString).collect(Collectors.joining(";"));
	}
}
