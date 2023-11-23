# FilesetTarget

The FilesetTarget type is used to configure the org.apache.tools.ant.types.FileSet as returned by the [dependencies task](../tasks/dependencies.md).

It is used as this:
```xml
    <bt:dependencies pomFile="pom.xml">
        <FileSetTarget id="classpathFileSet" type=":jar" path="classpath"/>
        <FileSetTarget id="sources" type="sources:jar"/>
        <FileSetTarget id="javadoc" type="javadoc:jar"/>
    </bt:dependencies>
```    

Basically, what you declare is the name (or id) of the org.apache.tools.ant.types.FileSet returned, a org.apache.tools.ant.types.Path type if you want, and of course, the types you want to have in your Fileset or Path.


- id : the name of the org.apache.tools.ant.types.FileSet that should be produced
- path : the name of org.apache.tools.ant.types.Path that sould be produced
- type : a comma-delimited list of PartIdentifications

You can specify things like 
```xml
    <FileSetTarget id="pom" type=":pom"/>
    <FileSetTarget id="man" type="asset:man"/>
    <FileSetTarget id="snapshotters" type=":war,:ear"/>
```
    
