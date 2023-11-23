# pom

The pom task is used to read and hold the pom related artifact. It follows the pom task of Maven's ancient ant adaption.

# basic call
```xml
    <bt:pom file="pom.xml" id="pom"/>
```    
This sets up the pom task, lets it read the file 'pom.xml' and adds the result to the ant project with the id 'pom'.

Once declared in that way, you can always reference the pom in the following manner
```xml
    <bt:pom refId="pom" />
```    

# usage

## direct use 
Typical uses are like this 
```xml
    <target name="common-init">
        <bt:pom id="pom.project" file="pom.xml" />
    </target>


    <target name="download-deps" depends="common-init">
        <bt:dependencies pathId="compile.classpath" filesetId="compile.fileset" sourcesFilesetId="compile.sources.fileset" useScope="compile">
            <pom refid="pom.project" />
        </bt:dependencies>
        
    </target>
```

## exposed properties

The pom task also exposes the internal values of the pom read. Some are straightforward exposures of the internal values (such as the identification of the pom), but also deeper ones like the dependencies, exclusions and properties defined within the pom.

### artifact identification 

So if you want to know what pom you read you can use a construct like this 
```xml
    <bt:pom id="pom.project" file="pom.xml" />
    <property name="artifact" value="${pom.project.groupId}:${pom.project.artifactId}#${pom.project.version}"/>
```            

### properties 

Any property declared within the properties section of the pom are also exposed. 

Consider this pom file 
```xml
    <project >
        <groupId>com.braintribe.devrock.ant</groupId>
        <artifactId>bt-ant-tasks</artifactId>
        <version>${major}.${minor}.${revision}</version>
        <properties>
            <major>1</major>
            <minor>0</minor>
            <nextMinor>1</nextMinor>
            <revision>769-pc</revision>
        </properties>
    </project>
```
and this target 
```xml
    <target ... >
        <bt:pom id="pom.project" file="pom.xml" />
        <property name="version" value="${pom.project.major}.${pom.project.minor}.${pom.project.revision}"/>        
    </target>
```
note : in mc-ng, you can access also the properties of the parents referenced within the pom, as its PomReader does incorporate all properties declared within the parent reference chain.

### validation
The task can validate the pom, both syntactically (wellformedness, XSD adherence, coordinates present) and semantically (all references - parent, imports, dependencies - can be resolved). See [pom validation](./validate-pom.md) for details.

The validation is deactivated per default, but you can activate it by setting the 'validatePom' property to true
```xml
        <bt:pom id="pom.project" file="pom.xml" validatePom="true" />
```

### dependencies
You can also get the dependencies declared within the pom. 

TODO: create example


### exclusions
And also, a list of the strings with the global exclusions declared within the pom can be returned. 
            
TODO: create example            