# change version task

This task is used to change the version of an artifact's source. It is an extracted feature of the complex  'publishing' task. It aims to split this task into smaller chunks that can be glued together with the build script. This finer granularity allows for an easier flow control and diminished roll-back requirements in case of an issue.


## call sequence 
        <bt:changeVersion version="1.0.2">
	        <pom file="variables.pom.xml"/>
	    </bt:changeVersion>    	    	

or

        <bt:pom id="pom" file="pom.xml" />

        ... 

        <bt:changeVersion version="1.0.2">
	        <pom refid="pom"/>
	    </bt:changeVersion>    	    	

## procedure

The task will patch the pom passed, and update the version withhin. Furthermore, it will also update any json package file if any is found in the same directory the pom resides.

### pom
The task is scanning the 'version' tag of the pom. If it finds no variables in the existing value, it will simply overwrite what is there with the actual version. 

Keep in mind that the task doesn't understand any specific logic with the use of variables in the pom file. So it needs to use an abbreviated procedure : 

If it finds at least one 

    '${' 

character sequence in the 'version' tag, it is assuming that it's a standard construct like 

    '${major}.${minor}.${revision}' 

and it will *NOT* update the version tag. 

In all cases, it will search and try to update the properties, while searching for 'major', 'minor' and 'revision' respectively. 

    <properties>
    	<major>1</major>
    	<minor>0</minor>
    	<revision>1</revision>
    </properties>    

If the version tag is deemed to be variable based, and it finds one of the three standard properties linked to version is missing, it will however complain and throw an BuildException.

### json package
The task is also checking for the existence of a json package file, 'package.json'. It then scans for a line with the version entry. If found, it will update the version there. If no such entry is found or if there are multiple entries in the file, it will fail.


