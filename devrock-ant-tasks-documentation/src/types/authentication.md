## Authentication type
This type is the container for the user credentials
```xml
    <authentication username="user" password="pwd"/>
```

Again, you can use the type as any other type, you can defined it somewhere different in the project where you use it, as it both supports the id and the refid property. 

 - user :  the user for the repository access

 - password : the password for the repository access
 
 - id : the id to be used to export the type into the project
 
 - refid : the id of the instance to be referenced. 
 

### Example:
 ```xml
    <authentication id="myAuth" username="user" password="pwd"/>
    
    ... 
    
     <bt:remoteRepository refId="my-repo">
        <authentication refId="myAuth" />
    </bt:remoteRepository>  
```