# transitive build
The transitive build task is a rather complex task. While it basically only calls a specified target sequentially for all artifacts passed, it looks kinda strange in ant.

This tasks uses [build ranges](./buildranges.md) to specify what needs to be built.

This is how a target would look like:
```xml
    <bt:ensureRange input="." outputProperty="ensuredRange" root="${basedir}" expand="false" ignoreProperty="ignoreRange"/>    
    <property name="target" value="install"/>
    <xmlproperty file="parent/pom.xml" />
    <property name="version" value="${project.properties.major}.${project.properties.minor}.${project.properties.revision}"/>
    <property name="groupId" value="${project.groupId}"/>
    <property name="list-range-output-file" value="./list-range.txt"/>
    <bt:buildSet id="buildSet"
            buildRange="${ensuredRange}"
            codebaseRoot="${basedir}"
            codebasePattern="$${artifactId}"
            groups="${groupId}#${version}"
            defaultGroup="${groupId}"
            defaultVersion="${version}"
            />          
    <bt:transitive-build mode="individual" target="list-range" buildSetRefId="buildSet" ignore="${ignoreRange}"/>       
```

## parts 
In the end, three tasks are used for this concert. 

#ensureRange

This task actually takes the user input and (if required) builds a valid range for the further tasks which it returns as string based property. 

```xml
    <bt:ensureRange input="${expression}" outputProperty="ensuredRange" root="${basedir}" expand="false" ignoreProperty="ignoreRange"/>
```

## parameters

  - input : either directly a 
  - outputProperty : the name of the property where the result of the files to be processed is to be exported to ant
  - ignoreProperty : the name of the property where the result of the files to be ignored is to be exported to ant
  - root : specifies the root directory for the ranging.
  - expand : true if fully qualified artifact names are to be returned, false if the artifactId suffices. 


#buildSet
The buildSet gets the ensured range (which is a string of artifact names) and turns the into a set of projects 

```xml
    <bt:buildSet id="buildSet"
            buildRange="${ensuredRange}"
            codebaseRoot="${basedir}"
            codebasePattern="$${artifactId}"
            groups="${groupId}#${version}"
            defaultGroup="${groupId}"
            defaultVersion="${version}"
    />          
```

## parameters

- buildRange : a string containing the range (generated via ensureRange or directly entered), see [here](./buildranges.md)
- codebaseRoot : the root directory of the codebase (i.e. either the group directory or the main directory of the repo)
- codebasePattern : the pattern that describes the organization of the codebase (see mc-core-documentation about that)
- groups: <don't have that one yet in detail.. pending>
- defaultGroup : the default group (if no group specified in artifact)
- defaultVersion : the default version (if no version is specified in the artifact)
    
#transitiveBuild
Finally, the transitiveBuild will iterate over the build-set and execute the specified target on each of the projects within the build-set

```xml
    <bt:transitive-build mode="individual" target="list-range" buildSetRefId="buildSet" ignore="${ignoreRange}"/>  
```

## parameters

  - mode
        the mode can either be 'individual' or 'shared'. It is specifying which build file is called. 'individual' accesses the standard 'build.xml' file which each artifact has. 'shared' however references the 'shared-build.xml' which is used to build across groups.
                
  - target : the target which is to be called for every artifact in the set. Obviously, it needs to exist. 
  - buildSetRefId : the id of the build-set produced by the buildSet task.
  - ignore : a string containing the artifacts that are to be ignored as produced by the ensureRange task.


