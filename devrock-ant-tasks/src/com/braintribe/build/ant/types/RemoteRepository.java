// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.types;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.model.repository.HasCredentials;
import com.braintribe.devrock.model.repository.MavenFileSystemRepository;
import com.braintribe.devrock.model.repository.MavenHttpRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.StandardCloningContext;

/**
 * 
 * mimics a repository like 
 * 
 * 		<artifact:remoteRepository id="remoteRepository" url="dav:http://archiva.bt.com/repository/standalone/">
 *			<authentication username="builder" password="operating2005"/>
 *		</artifact:remoteRepository>
 * 		
 * 
 * <artifact:deploy file="dist/lib/${versionedName}.jar">
 *		<remoteRepository refId="remoteRepository">
 *			 <authentication username="builder" password="operating2005"/>
 *		</remoteRepository>
 *		<pom refid="maven.project"/>
 *      <attach file="${dist}/${versionedName}-sources.jar" classifier="sources"/>
 *	</artifact:deploy>
 *		
 * or
 * 
 * <bt:deploy file="dist/lib/${versionedName}.jar" useCase="DEVROCK">
 *		<remoteRepository refId="remoteRepository"/> *			
 *		<pom refid="maven.project"/>
 *      <attach file="${dist}/${versionedName}-sources.jar" classifier="sources"/>
 *	</bt:deploy>
 * 
 * or even
 * 
 * <bt:deploy file="dist/lib/${versionedName}.jar" useCase="DEVROCK">			
 *		<pom refid="maven.project"/>
 *      <attach file="${dist}/${versionedName}-sources.jar" classifier="sources"/>
 *	</bt:deploy>
 *  
 * 
 * @author pit
 *
 */
public class RemoteRepository extends ProjectComponent {

	private static Logger log = Logger.getLogger(RemoteRepository.class);
	
	private static final String DAV_PREFIX = "dav:";
	private static final String PROFILE_USECASE = "PROFILE_USECASE";

	private String id;
	private String refid;
	private String url;
	private Authentication authentification;
	
	private String useCase = System.getenv(PROFILE_USECASE);
	private Repository repository;
	
	public RemoteRepository() {
		
	}
		

	@Configurable
	public void setUseCase(String useCase) {
		this.useCase = useCase;
	}
	
	public String getId() {
		return id;
	}
	
	@Configurable
	public void setId(String id) {
		this.id = id;
		Project project = getProject();
		if (project == null)
			return;
		project.addReference( id, this);		
	}

	@Configurable
	public void setUrl(String url) {
		if (url.startsWith( DAV_PREFIX))
			this.url = url.substring( DAV_PREFIX.length());
		else
			this.url = url;
	}
	
	@Configurable
	public void addAuthentication( Authentication authentification) {
		this.authentification = authentification; 
	}

	
	public String getRefid() {
		return refid;
	}
	
	public void setRefid(String refid) {
		this.refid = refid;
	}
	
	
	public Repository getRepository() {
		if (repository == null) {
			repository = buildRepository();
		}
		
		return repository;
	}
	
	private Repository buildRepository() {
		Repository repository = buildBasicRepository();
		configureRepository(repository);
		return repository;
	}
	
	private void configureRepository(Repository repository) {
		if (authentification != null && repository instanceof HasCredentials) {
			HasCredentials hasCredentials = (HasCredentials)repository;
			hasCredentials.setUser(authentification.getUsername());
			hasCredentials.setUser(authentification.getPassword());
		}
	}

	private Repository buildBasicRepository() {
		if (url != null) {
			return buildRepositoryFromProperties();
		}
		else if (refid != null) {
			return buildRepositoryFromProjectOrConfiguration();
		}
		else {
			return buildDefaultRepository();
		}
	}
	
	private Repository buildRepositoryFromProperties() {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new BuildException("Invalid RemoteRepository url: " + url);
		}
		
		String scheme = Optional.ofNullable(uri.getScheme()).orElse("");
		
		switch (scheme) {
		case "http":
		case "https":
			return buildMavenHttpRepository();
		case "file":
			return buildMavenFileSystemRepository(new File(uri).getAbsolutePath());
		default:
			throw new BuildException("Unsupported protocol for RemoteRepository in url: " + url);
		}
	}

	private Repository buildMavenFileSystemRepository(String rootPath) {
		MavenFileSystemRepository fsRepository = MavenFileSystemRepository.T.create();
		fsRepository.setName(id);
		fsRepository.setRootPath(rootPath);
		return fsRepository;
	}

	private Repository buildMavenHttpRepository() {
		MavenHttpRepository httpRepository = MavenHttpRepository.T.create();
		httpRepository.setName(id);
		httpRepository.setUrl(url);
		
		return httpRepository;
	}

	private Repository buildRepositoryFromProjectOrConfiguration() {
		Object referenceValue = getProject().getReference(refid);
		
		if (referenceValue == null) {
			return copyRepository(getRepositoryFromConfiguration());
		}
		else if (referenceValue instanceof RemoteRepository) {
			RemoteRepository other = (RemoteRepository)referenceValue;
			return copyRepository(other.getRepository());
		}
		else {
			throw new BuildException("Project had an object with id [" + refid + "] but it was no RemoteRepository");
		}
	}

	private Repository getRepositoryFromConfiguration() {
		Repository repository = Bridges.getInstance(getProject(), useCase).getRepository(refid);
		
		if (repository == null)
			throw new BuildException("No Repository with id [" + refid + "] found in RepositoryConfiguration");
		
		return repository;
	}

	private Repository buildDefaultRepository() {
		Repository defaultUploadRepository = Bridges.getInstance(getProject(), useCase).getDefaultUploadRepository();
		
		if (defaultUploadRepository == null)
			throw new BuildException("No default upload repository configured in RepositoryConfiguration");
		
		return copyRepository(defaultUploadRepository);
	}

	private Repository copyRepository(Repository defaultUploadRepository) {
		return defaultUploadRepository.clone(new StandardCloningContext());
	}

	
	
}
