# validate pom files
Pom files can be wrong in several ways. Repositories like artifactory will validate a pom at upload, but there are reasons to be able to validate such poms before the upload - especially as repositories may not have the understanding about all the different aspects when it comes to pom validity. 

devrock-ant-task have two distinct tasks to validate poms.

## syntactically
This validation concentrates on formal aspects. 

First, structural test is done by validating the pom against the official XSD file (maven_4.0.0.xsd). This will also test 'wellformedness' of the pom, then the correct layering of tags, duplicates and so on. 

    The XSD is rather weak : a content like <project/> is valid in that respect.

As the XSD validation doesn't suffice, a higher-level validation takes place.

It tests whether the 'maven coordinates' (groupId, artifactId and version) are declared or at least deriveable. 

- no parent reference : groupId, artifactId and version must be specifically declared in the pom.

- parent reference :  only the artifactId must be declared specifically, the parent reference must be fully declared. The version of the pom may only be omitted if the parent reference's version is not a ranged version.

If any of these checks fail, the task will throw a 'BuildException' with the reasons for the failure contained within the message. 

Also, the text content of the reasons are also exposed to a project property, default name is *pomValidationResult*.

Useage :
```xml
	 <bt:validatePomFormat pomFile="pom.xml" />


	 <bt:validatePomFormat pomFile="pom.xml" exposureProperty="reasons"/>
```
## semantically 

This validation concentrates on the *direct* dependencies of the pom, both standard dependencies, but also parent references and import statements.
```
    Obviously, it relies on the wellformendness of the pom, so best if the formal validation is run first. 
```
After the pom has been compiled into a CompiledArtifact, all of the references are checked for existence, i.e. whether they can be resolved. 
```
    Note that only the direct references are checked - no check happens for any 'transitive dependencies'.
```

As with the formal test, any issues deemed failures will be reported withhin the buildException, and the text content of the reasoning is exposed to a project property, default name is *pomValidationResult*.


Useage :
```xml
	 <bt:validatePomContent pomFile="pom.xml" />


	 <bt:validatePomContent pomFile="pom.xml" exposureProperty="reasons"/>
```