# the install task
The install tasks copies a locally built artifact (compiled and packaged) into your local repository. It's the base for all [install](./install.md), deploy and [publish](./publish.md) tasks.
```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach file="${basedir}/dist/BtAntTasks-1.9-sources.jar" classifier=�sources� type=�jar�/>
        </bt:install>               
    </target>
```

I cannot gauge what Maven does with both the classifier and the type parameters, but they�re gobbled up in the Maven-project�s internals.  

## MC style 
### Lenient maven style
If you pass the file, we simply ignore the two additional arguments, so this is perfectly fine
```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach file="${basedir}/dist/BtAntTasks-1.9-sources.jar"/>
        </bt:install>               
    </target>
```

### Enhanced style
As we have a smarter system based on the classifier and type scheme, you can also specify the classifier and type and let the task sort out what files should be installed.
```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach classifier=�sources� type=�jar/>
        </bt:install>               
    </target>
```

In the example above, the task will look for the sources file in the current directory. You can also specify a directory, and if you do, the task will look there. 
```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach file="${basedir}/dist" classifier=�sources� type=�jar/>
        </bt:install>               
    </target>
```
 
If you only specify the type, then this is interpreted as a symbolic value, so this would be equivalent to the example above:

```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach file="${basedir}/dist" type=�sources/>
        </bt:install>               
    </target>
```

The power of this enhancement becomes obvious once you remember how the symbolic values work: 

Both enhanced examples will automatically scan for possible candidates - in our case BtAntTask-1.9.sources.jar amongst them of course. As we should stick to the pattern of naming our packaged sources like this, you may argue that the feature is not really necessary.  

But we could imagine that the javadoc is present in several languages, say in english, french, german and hindi, each named like BtAntTasks-1.9-<locale>-javadoc.jar.

So this example here would detect all these javadoc files and install them 
```xml
    <target name="installTest" depends="init">
        <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <pom refid="pom" />
            <attach file="${basedir}/dist" type=�javadoc/>
        </bt:install>               
    </target>
```

More on the symbolic values you�ll find in the documentation linked above. 


### normalization
It has shown that Maven cannot deal with variable statements in the version properties of the artifact itself and of an eventual parent reference. Therefore, these tasks do normalize the pom in such a manner that the variables inside the version tags are resolved and the string contains the 'real' version expression.


### validation
The task also validates the pom (see [pom validation](./validate-pom.md)) before it is installed, deployed or published.  It is active by default (other than in the [pom task](./pom.md)) and can be deactivated by setting 'validatePom' to false.
```xml
    <bt:install file="${basedir}/dist/lib/BtAntTasks-1.9.jar" validatePom="false"           
    ..
    </bt:install>
```