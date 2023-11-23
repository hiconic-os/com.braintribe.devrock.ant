# publish task

The publish tasks is a variant of the [publish direct task](../tasks/publish-direct.md). In BT it actually - in most cases - superseeds the deploy tasks, so as a rule of thumb: If you think you need that task, you're probably wrong and should use this task.

```xml
    <target name="publish" depends="common">        
        <property name="versionedName" value="${pom.project.artifactId}-${pom.project.version}" />
        
        <bt:publish increaseVersion="true" skipGit="true">
            <pom refid="pom.project"/>
        </bt:publish>       
    </target>
```    
    
While its call looks quite innocent, it does some important things.

## prerequisites in the current build file

the 'publish' task requires that a target 'install' exists in the project as it's going to be called by the task. A typical target would look like this:
```xml
    <target name="install" depends="compile">                   
       <property name="versionedName" value="${pom.project.artifactId}-${pom.project.version}" />           
        <bt:install file=".">
            <pom refid="pom.project"/>
            <attach file="${versionedName}.jar" type="jar"/>
            <attach file="${versionedName}-sources.jar" type="sources:jar"/>
            <attach file="${versionedName}-javadoc.jar" type="javadoc:jar"/>
            <attach file="${versionedName}.data.zip" skipIfNoFile="true" type="data:zip"/>
            <attach file="package.json" skipIfNoFile="true"/>
        </bt:install>       
    </target>
```

## basic procedure

The 'publish' task actually does several things. 

 - it changes the revision of the passed artifact by removing the 'pc' suffix
 - it installs the artifact by calling 'install' (which most likely calls a compilation step)
 - it deploys what 'install' installed
 - it changes the revision of the passed artifact's source by increasing the revision and adding the 'pc' suffix
 - it writes these changes back to git (if not switched off)


As you see, the tasks is a rather complex one and it's granularity is quite coarse. Therefore, we're planning on splitting the tasks. Only thing missing is the revision touching and the git push - the other tasks can be combined via the build script as their finer granularity (of course) requires the glue code in the build script. 



