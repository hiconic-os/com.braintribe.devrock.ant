# repository extract task

The repository extract tasks is used to pull all artifacts required for the build process of the passed terminal from the local repository into one extract. The idea is that once you re-install the extracted artifact into a new repository, you can build the terminal from that repository. 

The call will do a 'build style' resolving (i.e. no clash resolving) and for each artifact reached download all parts that the artifact's original repository contains. All these files will be added to the result.

## calling 

Simplest way without any exclusions looks like this 
```xml
     <bt:repositoryExtract pomFile="pom.xml" filesetId="extracted"/>
```
If you do want to have some artifacts excluded, you can give an external file with exclusion patterns.
```xml     
     <bt:repositoryExtract pomFile="pom.xml" filesetId="extracted" globalExclusionsFile="exclusions.txt" />       
```
## parameter

It does need the two following parameters

 - pomFile : this is simply the pom of the terminal, i.e. of the artifact you want to retrieve all related files of. 
 - filesetId : this is the id of the fileset that will contain all the related files. 
 

## exclusion pattern

You can also additionally specify a set of exclusions via the global exclusion file.

- globalExclusionsFile : a simple line-delimited list of string expression compilable to (Java regular expression) patterns. 


### format 
```
    [#][pattern]</n>
```

The # at first place of a line makes the line to a comment, which is of course ignored. The same applies to empty lines.

The exclusions are turned into an artifact filter which the underlying mc-core internal processor will use. 
