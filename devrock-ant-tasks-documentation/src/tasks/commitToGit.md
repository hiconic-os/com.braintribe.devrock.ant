# commit to git task

This task is used to commit changes that were done to update the version of an artifact's source. It is an extracted feature of the complex 'publishing' task. It aims to split this task into smaller chunks that can be glued together with the build script. This finer granularity allows for an easier flow control and diminished roll-back requirements in case of an issue.


# call

     <bt:pushToGit message="commit-message">
            <pom file="pom.xml"/>
     </bt:pushToGit>    	    	
	
or
    <bt:pom file="pom.xml" id="pom" />

     <bt:pushToGit message="commit-message">
            <pom refid="pom"
     </bt:pushToGit>    	    	


# procedure 
The task will call two subsequent git commands when activated

First it will commit the changes happened to the pom file.

    git commit -m "<message>" pom.xml

The pom file being the 'pom.xml' that is been added to pom.

If that fails, a BuildException is thrown. 


In the next step, a push command is issued

    git push

Again, if that fails, a BuildException is thrown. 

