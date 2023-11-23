# Solution hasher task

The solution hasher task is used to detect changes in the *transitive dependencies* of artifacts in a group. 

You run it one or more groups in your source directories. For each artifact found, it will determine the transitive dependencies and build a hash for it. 

```xml
	<target name="solutionsHash">
		<property name="knownHashes" value=""/>
		<bt:ensureRange input="${range}" outputProperty="ensuredRange" root="${basedir}" expand="true"/>
		<fail unless="ensuredRange"/>
		<bt:hasher range="${ensuredRange}" targetDirectory="${basedir}" knownHashes="${knownHashes}" />
	</target>
```

## input

### range
As you can see in the example above, the task requires a range - which could of course be added manually, or as shown here, using the [ensureRange](./transitivebuild.md) - which contains the name of the artifacts to get the hashes of. 

The range will typically be a list of qualfied artifact names, delimited by a plus sign.

- Mandatory 

### targetDirectory
The target directory is a full qualified file path that points the directory where the artifacts are expected below and where the generated files will be created. 

- Mandatory 

### useScope
The use scope is used to influence how the dependencies are resolved.

- Optional, default is 'compile'

### tag rule
The tag rule can be used to filter top-level dependencies based on the 'tag' in the pom. Tags are represented as 'Processing Instructions'.

```xml
	<dependency>
		<groupId>com.braintribe.devrock.test</groupId>
		<artifactId>my-model</artifactId>
		<version>1.0.1</version>
		<? tag asset?>
	</dependency>	
```
- Optional, default is no filtering

### type rule
The type rule can be used the filter dependencies (on any level) based on their type.

- Optional. default is no filtering

### details
If set to true, the task will - for each artifact it processes - leave a yaml file containing the resolution the hashing used. The file will be named as the artifact (the ':' between groupId and artifactId replaced by a '.') and reside in the targetDirectory.

```
com.braintribe.devrock.test.t#1.0.1.resolution.yaml
```

- Optional, default is true

### algorithm
The algorithm to be used for hashing can also be passed.

- Optional, default is 'MD5'

### exclusion dependency
The exclusion dependency points to an artifact that needs to contain a part of the type 'exclusions'.

```
exlusion-artifact-1.0.1.exclusions
```

The file is required to be a new-line delimited list of artifact identifications, where '#' acts as a full line comment.

```
# exclusion for log4j issue
org.apache:log4j

```

The exclusions are applied to the terminal, so they are active during the traversion of the full tree.

- Optional, default is no exclusions (other than declared in the respective poms).

### known hashes


### missing solutions file
?? not clear what this is intended for. There seems to be no code in the task dealing with this?? 

### suffix
Suffix is the extension of the files to be created. All the files containing the hashes will have this suffix appended to their name, where the main has file will be called exactly as the suffix.

- Optional, default is 'hash.txt' 

## output 
The task will generate at least three files in the target directory: 

- hash.txt : A simple one-liner text file containing a hash over the groups.hash.txt file

```
12c654ef8188c4382284c8e6ea046512
```

- groups.hash.txt : Contains a hash for each of the groups found, one per line.
```
com.braintribe.gm 12c654ef8188c4382d284c8e6ea04608
```

- 'name of the group'.hash.txt : Contains a hash for each of the artifacts within the group. 
```
com.braintribe.gm:absence-information-model#1.0.20 7644435bd2e7150c711c6895babe7061
com.braintribe.gm:access-api-commons#1.0.10 4983b1f9a2afab6fa93d06f23955e59f
com.braintribe.gm:access-api-model#1.0.24 12c654ef8188c4382284c8e6ea040228
```

- 'name of the artifact'.resolution.yaml
Contains a YAML representation of the resolution that was used to calculate the hashes for the artifact.

```
com.braintribe.gm.absence-information-model#1.0.20.resolution.yaml
```

The resolution can be viewed in AC's resolution viewer for instance.



