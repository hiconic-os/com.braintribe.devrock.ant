# dependencies
 
 The *dependencies* task is used to calculate a classpath.  
  
 The tasks follows Maven's ancient ant integration, so all features it supported are supported by this. Plus some more of course.

Basically, you tell the task what terminal you want, for what purpose you need the classpath and what it should return to ant.


##  basic call
```xml
    <bt:dependencies pomFile="pom.xml" pathId="classpath" />
```

This will use the artifact defined in the file 'pom.xml' passed and will return the class path as a org.apache.tools.ant.types.Path type named 'classpath'


Alternatively, this construct can also be used    
 ```xml
    <bt:dependencies pathId="classpath" >
        <pom file="pom.xml"/>
    </bt:dependencies>
```    
and if the pom file has already been read, you can reference it as well

```xml
    <bt:dependencies pathId="classpath" >
        <pom refId="pom.project"/>
    </bt:dependencies>
```

see the text about the [pom task](./pom.md)

### specifying the terminal 

There are two ways to declare a terminal with the task.

First style is to use an existing artifact that you have access to its pom file.

```xml
    <bt:dependencies pathId="classpath" >
        <pom file="pom.xml"/>
    </bt:dependencies>
```

in this way, the artifact that is defined within the pom file is *not* part of the classpath. 

Second style is to use an existing artifact 

```xml
    <bt:dependency artifact="com.braintribe.mygroup:myArtifact#1.0" pathId="classpath"/>
```

The tasks will resolve the artifact and take it from there. Again, the artifact will not be part of the classpath 

Third style is to use a dependency 

```xml
     <bt:dependencies pathId="classpath" >
        <dependency artifactId="myArtifact" groupId="com.braintribe.mygroup" scope="runtime" version="1.0"/>
    </bt:dependencies>
```
You can of course use as many dependencies as you want, actually declaring a 'virtual artifact' 

```xml
    <bt:dependencies pathId="classpath" >
        <dependency artifactId="myFirstArtifact" groupId="com.braintribe.mygroup" scope="runtime" version="1.0"/>
        <dependency artifactId="mySecondArtifact" groupId="com.braintribe.mygroup" scope="runtime" version="1.0"/>
    </bt:dependencies>
```

If you use dependencies, the respective solutions (the artifact that the dependency pointed to) are part of the classpath.

### specifying the scope

You can specifiy the 'magick scope' that you want to use. There are three of these that we support:

- runtime : produces a classpath that can be used to run the terminal's content.
- compile : produces a classpath that can be used to compile the terminal
- test    : produces a classpath that can be used to run the - incorporated into it - terminal's test routines.


Note on test: We do not use that feature as in our philosophy the test features of an artifact are better placed into a separate artifact, so that the actual artifact can concentrate on doing its thing only.

If you want to know more about 'magick scope' and what they mean in detail, consult the documentation of 'mc-core' 

Default scope is the 'runtime' scope, so you needn't specify it, but you can of course. Basically, it is done via the 'useScope' property.

```xml
      <bt:dependencies pomFile="pom.xml" pathId="classpath" useScope="compile" />
```

### return values 

The task can return several values or rather expose them to the ant project other than the intrinsic org.apache.tools.ant.types.Path instance. Most prominent are of course the file-sets. 


Maven thought of three distinct org.apache.tools.ant.types.FileSet that their dependencies task should return. The ids are passed as parameter and depending on the name of the parameter, the different FileSet are produced.

- filesetId : the classpath as a fileset 
- sourcesFilesetId  : the source files as a fileset (*-sources.jar)
- javadocFilesetId  : the javadoc files as a fileset (*-javadoc.jar)

You would use it like this :

```xml
    <bt:dependencies 
            pathId="classpath"      
            filesetId="classpathFileSet" 
            sourcesFilesetId="sourceFileSet" 
            javadocFilesetId="javadocFileSet"
        >
        <pom file="pom.xml"/>
    </bt:dependencies>
```

#### the classpath

What is regarded as relevant to the classpath is also configurable. Basically of course, its the 'jar' types, depending on the result, it also may contain files like 'classes:jar'. But you might want to add still other files.

In that case, you can use the 'type' parameter

```xml
    <bt:dependencies 
            pathId="classpath"      
            type="additional:jar"
        >
        <pom file="pom.xml"/>
    </bt:dependencies>
```

Parameterized in that manner, dependencies would also add files like '*-additional.jar' to the classpath.


### the file set target type

We also added the [FileSetTarget](../types/filesettarget.md) task that is more generic and can collect whatever files you want

This sequence below is equivalent to the one above

```xml
    <bt:dependencies pomFile="pom.xml">
        <FileSetTarget id="classpathFileSet" type=":jar" path="classpath"/>
        <FileSetTarget id="sources" type="sources:jar,src:jar"/>
        <FileSetTarget id="javadoc" type="javadoc:jar,javadoc.zip,:javadoc,:jdar"/>
    </bt:dependencies>
```    

You can of course also specify things like 

```xml
    <FileSetTarget id="pom" type=":pom"/>
    <FileSetTarget id="man" type="asset:man"/>
    <FileSetTarget id="snapshotters" type=":war,:ear"/>
```

## options
There are more things the task can produce other than the Fileset.

### solutionListFile
The solutionListFile is an XML dump of the classpath expressed in entities of the following model

```xml
    com.braintribe.devrock:artifact-model
```

While it was native with mc-legacy, it's only retained in mc-ng for the sake of backward compatibility. 

You simply add the property to the tasks parameters

```xml
    <bt:dependencies pathId="classpath" solutionListFile="dump.xml">
        <pom file="pom.xml"/>
    </bt:dependencies>
```

Note that the property 'addSelf' has been de-continued as it's no longer required. If you want the terminal to appear in the solution list, use the approach with the dependency as explained [above](#specifying-the-terminal)

### resolutionList
The resolution list has been introduced in bt-ant-tasks-ng and - for all means and purposes - is thought the replace the solutionList. So if you can, please switch your build scripts to use this feature.

The format and the content can be configured. Depending on the types, you'll find the entities in 
```
    com.braintribe.devrock:consumable-artifact-model
```    
or
```
    com.braintribe.devrock:analysis-artifact-model
```    

TODO : add the description of with/without 'origin', 'Artifact', 'LinkedArtifact', 'AnalysisArtifact', 'AnalyiseArtifactResolution'


### solutionListProperty

A specified solutionListProperty triggers the tasks to generate a String-type property with the content of the classpath, formatted as a comma-delimited list of the qualified (condensed) names of the artifacts included.
```xml
    <bt:dependencies pathId="classpath" solutionListProperty="solutions">
        <pom file="pom.xml"/>
    </bt:dependencies>
```

As this list is sorted, it can be used to generate hashes and check by comparing the hash to earlier generated hashes. 

## error handling

If the resolving fails - i.e. the resolution is flagged as failed - then two files are written to the 'processing-data-insight' folder. See [processing-data-insight tasks](./problem-analysis-insight.md). 

The files both show the timestamp in their name, so that every resolution's files aren't overwritten. 

The timestamp has the following format 

```
yyyy-MM-dd-HH-mm-ss
```

The resolution file looks like this : 

```
artifact-resolution-dump-<timestamp>.yaml 
artifact-resolution-dump-2022-06-22-14-04-34.yaml
```

The repository file looks like this : 

```
repository-configuration-dump<timestampe>.yaml 
repository-configuration-dump-2022-06-22-14-04-34.yaml
```

You can analyze the content of the resolution and the configuration using the Devrock Eclipse plugins. 


## notes