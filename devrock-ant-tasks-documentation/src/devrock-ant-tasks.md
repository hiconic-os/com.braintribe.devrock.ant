# the devrock-ant-tasks

This contains all add-ons we made for our venerable ant in order to get the building done.

While this documentation lists most of the available features, it's by no means complete - if you're looking for documentation that doesn't exist here, feel free to research and add what you've learned to this documentation.

## tasks

The [pom task](./tasks/pom.md) is used to read a pom. i.e. to read the content of a pom file into the tasks realm. It also gives access to its coordinates and properties.

The two pom validation tasks, [validate pom](./tasks/validate-pom.md) and [validate pom content](./tasks/validate-pom.md) validate poms, the former 'syntactically' (pom makes sense) and the latter 'semantically' (content is referenceable).

The [import task](./tasks/import.md) is used to import an additional ant build file that is addressed as an artifact and is resolved as one.

The [dependencies task](./tasks/dependencies.md) is used to calculate class-paths for an artifact, i.e. they transitively traverse a dependency tree and return all required artifacts.

The [install task](./tasks/install.md) is used to install a locally built artifact into the local repository.

The [direct-publish task](./tasks/publish-direct.md) is used to directly publish an artifact existing in the local repository into the remote repository.

The [publish task](./tasks/publish.md) is used to publish a locally built artifact into the remote repository - also updating the revision number in the artifact's version.

The [change version task](./tasks/changeVersion.md) is used to change the version of an artifact's source. 

The [commit to git task](./tasks/commitToGit.md) is used to commit a changed artifact to git (mostly used after changeVersion, in a fine-granual publish process).

The [transitive build task](./tasks/transitivebuild.md) is used to automatically run a specified task over a whole set of artifacts. 

The [repository extract task](./tasks/repositoryextract.md) is used to retrieve all artifacts (and their parts) of a dependency tree.

The [solution hasher task](./tasks/solutionhasher.md) is used to create hash files from a group and all artifacts within a group to determine if anything has changed in the dependencies

The [processing data insight folder](./tasks/processing-data-insight-folder.md) can be used to export the currently defined folder for the various analysis data collected during some tasks and some data related to issues that led to failure of certain tasks.

 ## types
 
 While the [pom task](./tasks/pom.md) is actually a task (it needs to be able to run), it's semantically rather a type, i.e. it represents the pom.
 
 A [fileset target](./types/filesettarget.md) represents a collection of files within ant. 
 
 A [remote repository](./types/remote.repository.md) is used to declare targets for the publishing tasks, as they represent a 'target' remote repository.
 
 An [authentication](./types/authentication.md) is used to attach user credentials to a [remote repository](./types/remote.repository.md).



## notes

>Some of the tasks do use mc-ng via their bridge to do some resolving. This functionality in most cases relies on a repository-configuration, i.e. it needs to know what remote repositories are to be used and where the local repository lies. Furthermore, filters may have been defined (pc-bias for instance), and other factors may influence the resolution. 
If such internal mc-ng tasks fail, they no only throw an exception to stop ANT processing, but they also dump the currently active repository-configuration as a YAML file. It is written into the current directory of the ANT task, named 

```
    repository-configuration'-<timestamp>.yaml
```

>It not only contains all of the configuration, but also the 'origination' of it, i.e. what was the compiling agent, what environment variables were followed and what was their value etc. 
