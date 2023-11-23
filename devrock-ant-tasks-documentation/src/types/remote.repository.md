# Remote repository type
The remote repository type declares what repository to use for the deploy process. Basically, it just contains the URL and the user credentials.
```xml
    <bt:remoteRepository id="remoteRepository" url="protocol:url">
        <authentication username="user" password="pwd"/>
    </bt:remoteRepository>
```

## attributes

The attributes you can add are as follows:

 - url : basically a URL, but it may - depending on the upload protocol - contain more than that, as in maven. 
 
 The actual format is 
 ```
    [<protocol>:]<URL>
 ```   
 like this for example:  
 ```
    dav:http://archiva.bt.com/repository/standalone/
 ```
 - id : the id to be used to export the type into the project
 
 - refId : the id of the instance you want to use. It can either be the id of a [remote-repository](../types/remote.repository.md) declared within the build file OR the name of a repository from the current repository configuration, see [below](#referencing-repositories).
 

## child elements 
 
 - authentication : the user credentials in form of an instance of the authentication type.the id of the instance to be referenced. See [authentication](../types/authentication.md) for details.



Don't ask me what the rationale for that is, but maven seems to work like that in most cases - at least it does in quite a few build files I looked at. It does work with our deploy task, but the standard way of only using reference to a remote repository without overloading the authentication seems to be norm. 



## referencing repositories
As mentioned, you can reference a remote-repository that has been declared within the build file - as standard.
```xml
    <target name="init">
        <bt:pom file="${basedir}/pom.xml" id="pom"/>
        <bt:remoteRepository id="remoteRepository" url="remote repo/">
            <authentication username="user" password="pwd"/>
        </bt:remoteRepository>      
    </target>
```
Declared here, but used in another target :     
```xml
    <target name="deploy" depends="init">
        <bt:deploy file="${basedir}/dist/lib/BtAntTasks-1.9.jar">
            <remoteRepository refId="remoteRepository" >
                <authentication username="other user" password="other pwd"/>
            </remoteRepository>
            <pom refid="pom"/>
            <attach file="${basedir}/dist" type="sources"/>
        </bt:deploy>                
    </target>
```

There is however a further way to reference a remote-repository. Keep in mind that a remote-repository actually reflects a 'real life' repository. Therefore, you can reference a remote-repository that isn't simply declared with the build file - it can be one of the actual configuration. 

Consider this settings.xml:
```xml
    <?xml version="1.0"?>
    <settings>
      ...
      <profiles>
        <profile>
            <id>core</id>
            <repositories>
                <repository>
                    <id>my-repo</id>
                    <layout>default</layout>
                    <url>https://my-repo-url/</url>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <releases>
                        <enabled>true</enabled>
                        <updatePolicy>daily</updatePolicy>
                    </releases>
                </repository>              
                ...             
            </repositories>
            ... 
        </profile>
        ...
      </profiles>
      ...
    </settings>
```

And this repository-configuration
```yml
    !com.braintribe.devrock.model.repository.RepositoryConfiguration {                                            
        localRepository: 'f:/myrepo',
        repositories: [
            !com.braintribe.devrock.model.repository.MavenHttpRepository {
               name: 'my-repo',
               url: 'https://my-repo-url',
              ....                               
            },                          
            ....
        ]
    }        
```
So no matter which approach you decide to use - settings.xml or direct repository-configuration - you can reference a repository declared either way by referencing it via its 'name' or 'id'. 

So in both cases, the repository called 'my-repo' will be used:
```xml
     <bt:remoteRepository refId="my-repo">
        <authentication username="user" password="pwd"/>
    </bt:remoteRepository>  
```
    
However: even if you can reference the repository, you cannot reference its authentication data - you'll need to add the ['authentication'](../types/authentication.md) type. 
