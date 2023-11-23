# DevRock ant tasks

Ant extension for tribefire/GM development.


### How to install

##### First you need Apache Ant

Download apache ant from https://ant.apache.org/

Extract the apache ant to some folder, say `tools/apache-ant-1.10.5`

Configure environement variable `ANT_HOME` with an absolute path to this folder, say `${absolute path to tools folder}/apache-ant-1.10.5`

##### Installing with jinni

In case you have jinni up and running, simply run

> jinni update-devrock-ant-tasks


##### Installing manually (without jinni)

Download `devrock-ant-tasks.zip` from https://artifactory.bt.com/ui/native/devrock/com/braintribe/devrock/ant/devrock-ant-tasks/

Extract the zip (containing jar file) into your `${ANT_HOME}/lib` folder.


### How to use

##### Building the entire group

> ant -Drange=.

##### Building one specific artifact

> ant -Drange=[my-artifact]

##### Building one specific artifact and all its dependencies in this group

> ant -Drange=my-artifact

##### Building with multiple threads

> ant -Drange=. -Dthreads=4


### Tips and Tricks

#### Color output in the console

##### Permanently

Set environment variable `BT__ANT_COLORS=true`.

##### Just for the build

> ant -Drange=. -Dcolors=true

#### Debug

See HowToDebug.txt
