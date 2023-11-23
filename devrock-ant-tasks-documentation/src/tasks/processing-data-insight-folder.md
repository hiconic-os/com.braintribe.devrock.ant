# determineProcessingDataInsightFolder task

This task exports the 'processing-data-insight' folder. The folder is used to store additional information during the builds, for instance if a classpath resolution via the [dependencies task](./dependencies.md) fails, both the resolution and the current configuration are written to the folder.


## location of the processing-data-insight folder

Where the folder is located depends on whether you are using a 'dev-env' or a 'unstructured' (aka traditional) setup. The task (actually the bridge that supplies the data) will automatically find out whether the directory the task runs in is inside a 'dev-env' or not. 

> Remember that the dev-env determination is recursive : it will - starting from the current directory - walk upwards to detect dev-envs. The consequence is that you might not even be aware that the current folder is within a 'dev-env', as any dev-env folder encountered on the way to the root directory of the current drive will be taken. 


### dev-env 
In the dev-env setup, the folder is located here : 

```
<dev-env-root>
	artifacts
		processing-data-insight
```

### unstructured 
In the unstructured setup, the folder is located here :

``` 
<user.home>
	.devrock
		processing-data-insight
```

This setup also supports the environment variable 'DEVROCK_HOME'. If the variable is present, then it is used for the location of *parent* folder of the 'processing-data-insight-folder'.

``` 
${DEVROCK_HOME}
	processing-data-insight	
```

## usage 
The task itself is of no big importance, but if you want to use the folder inside your own build scripts, you can retrieve the currently active folder location like this: 

``` 
  	<target name="processing-data-insight-folder">
        <bt:determineProcessingDataInsightFolder propertyName="folder"/>    	    	
    	<echo message="current processing-data-insight-folder is: ${folder}" />
	</target>
```