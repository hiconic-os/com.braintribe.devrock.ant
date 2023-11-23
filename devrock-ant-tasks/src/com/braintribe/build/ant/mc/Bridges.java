package com.braintribe.build.ant.mc;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.Project;

import com.braintribe.utils.lcd.LazyInitialized;

public abstract class Bridges {
	private static Map<BridgeKey, DirectMcBridge> bridges = new ConcurrentHashMap<>();
	private static Map<File, LazyInitialized<File>> devEnvFolderCache = new ConcurrentHashMap<>();

	public static McBridge getAntInstance(Project project, String profileUseCase) {
		File devEnvFolder = detectDevEnvFolder(project);
		return getInstance(devEnvFolder, profileUseCase, true);
	}
	
	public static McBridge getInstance(Project project, String profileUseCase) {
		File devEnvFolder = detectDevEnvFolder(project);
		return getInstance(devEnvFolder, profileUseCase);
	}
	
	public static McBridge getInstance(Project project, File codebaseRoot, String codebasePattern, String defaultVersion) {
		File devEnvFolder = detectDevEnvFolder(project);
		return getInstance(devEnvFolder, codebaseRoot, codebasePattern, defaultVersion);
	}
	
	public static McBridge getInstance(Project project) {
		File devEnvFolder = detectDevEnvFolder(project);
		return getInstance(devEnvFolder);
	}
	
	private static File detectDevEnvFolder(Project project) {
		File baseDir = project.getBaseDir();
		
		return detectDevEnvFolder(baseDir);
	}

	private static File detectDevEnvFolder(File dir) {
		return devEnvFolderCache.computeIfAbsent(dir, d -> new LazyInitialized<File>(() -> _detectDevEnvFolder(d))).get();
	}
	
	private static File _detectDevEnvFolder(File dir) {
		boolean isDevEnvRoot = isDevEnvRootFolder(dir);
		
		if (isDevEnvRoot)
			return dir;
		
		File parent = dir.getParentFile();
		
		if (parent == null)
			return null;
		
		return detectDevEnvFolder(parent);
	}

	private static boolean isDevEnvRootFolder(File dir) {
		File markerFile = new File(dir, "dev-environment.yaml");
		return markerFile.exists();
	}

	public static McBridge getInstance(File devEnvFolder, String profileUseCase) {
		return getInstance(devEnvFolder, profileUseCase, false);
	}
	
	public static McBridge getInstance(File devEnvFolder, String profileUseCase, boolean antInstance) {
		if (profileUseCase != null) 
			return bridges.computeIfAbsent(new BridgeKey(profileUseCase, devEnvFolder, antInstance), p -> new DirectMcBridge(devEnvFolder, profileUseCase, antInstance));
		else 
			return getInstance(devEnvFolder);
	}
	
	public static McBridge getInstance(File devEnvFolder, File codebaseRoot, String codebasePattern, String defaultVersion) {
		return new DirectMcBridge(devEnvFolder, codebaseRoot, codebasePattern);
	}
	
	public static McBridge getInstance(File devEnvFolder) {
		return getInstance(devEnvFolder, "");
	}
	
	private static class BridgeKey {
		String profileUseCase;
		File devEnvFolder;
		boolean ant;
		
		public BridgeKey(String profileUseCase, File devEnvFolder, boolean ant) {
			super();
			this.profileUseCase = profileUseCase;
			this.devEnvFolder = devEnvFolder;
			this.ant = ant;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (ant ? 1231 : 1237);
			result = prime * result + ((devEnvFolder == null) ? 0 : devEnvFolder.hashCode());
			result = prime * result + ((profileUseCase == null) ? 0 : profileUseCase.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BridgeKey other = (BridgeKey) obj;
			if (ant != other.ant)
				return false;
			if (devEnvFolder == null) {
				if (other.devEnvFolder != null)
					return false;
			} else if (!devEnvFolder.equals(other.devEnvFolder))
				return false;
			if (profileUseCase == null) {
				if (other.profileUseCase != null)
					return false;
			} else if (!profileUseCase.equals(other.profileUseCase))
				return false;
			return true;
		}
		
		
		
		
	}
}
