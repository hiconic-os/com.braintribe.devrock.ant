# the import task

The import task allows you to reference an artifact that has a specific ant build file. It needs to be the first statement in your build file, but you can nest imports, i.e. the imported build file can have an import on its own etc.

```xml
    <bt:import artifact="com.braintribe.devrock.ant:myscript#1.0.1" />
```
The artifact - here "com.braintribe.devrock.ant:myscript#1.0.1" needs to be resolvable of course. 

The file that is actually imported is 
```xml
    <artifactId>-<version>-import.xml
```

