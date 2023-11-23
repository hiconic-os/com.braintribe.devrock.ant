# the publish-direct task

the publish-direct task is used to upload an installed artifact to a Maven compatible repository. See [install](./install.md) task for details about installing.

In the BT environment, it's is the very most cases not recommended to use this task, as we are running the 'publishing' scheme. Therefore, please make sure that you really need this task and have good reasons not to use the [publishing](./publish.md) task.



Basically it needs to know where to deploy the files to and what user credentials to use.
```xml
    <target name="deploy" >
        <bt:publish-direct>
            <remoteRepository url="remote repo">
                <authentication username="user" password="pwd"/>        
            </remoteRepository>
            <pom file="${path_in_local_repository}/pom.xml" />            
        </bt:publish-direct>                
    </target>
```
If you do not have a pom file handy, you can simply pass the artifact to be uploaded via its string representation

```xml
    <target name="deploy" >
        <bt:publish-direct artifact="com.braintribe.devrock.test:t#1.0.1">
            <remoteRepository url="remote repo">
                <authentication username="user" password="pwd"/>        
            </remoteRepository>
        </bt:publish-direct>                
    </target>
```
The [remoteRepository](../types/remote.repository.md) is used to specify the target repository, and the [authentication](../types/authentication.md) provides the credentials for it. 


NOTE: 

The task actually looks into the directory of the LOCALLY INSTALLED artifact and simply uploads all files it finds in the directory. Each and every file gets its three hashes file (.md5, .sha1, .sha256). The version of the artifact is automatically added to maven-metadata.xml on the remote server. 



