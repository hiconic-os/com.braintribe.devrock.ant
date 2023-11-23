# build ranges

Build ranges are a concept that the Transitive Dependency Resolver understands and can be used to 'extract' some paths within a dependency tree. 
This text here explains what they do while using the features the build system introduces. 

Still, the functionality is the same and therefore this following text is - albeit older - still relevant. 

The expressions below are however fully supported by the CLI (command line interface) of the build system.

## the basic tree

Consider this build sequence of the terminal

```
    com.braintribe.terminal:Terminal#1.0
```


its full build sequence is 

```
    com.braintribe.terminal:TerminalParent#1.0.1
    com.braintribe.terminal:A#1.0.1
    com.braintribe.grpBase:GrpBaseParent#1.0.1
    com.braintribe.grpBase:BaseDependency#1.0.1
    com.braintribe.grpOne:GrpOneParent#1.0.1
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.grpOne:A#1.0.1
    com.braintribe.grpOne.subOne:GrpOneSubOneParent#1.0.1
    com.braintribe.grpOne.subOne:B#1.0.1
    com.braintribe.grpOne.subOne:A#1.0.1
    com.braintribe.terminal:Terminal#1.0.1
```

As a picture, it looks like this : 

![example tree](../pics/build-ranges.jpg)


Note the following terms :

- dependency  : a standard dependency
    
- parent dependency : rather a reference to the parent
    
- virtual dependency : a dependency that is declared within the parent, which is not a 'real' dependency of the parent, but is a dependency that is injected into any child of the parent.
    
- injected dependency : a dependency of the artifact that is not declared in the artifact itself, but introduced by means of the parent.
    

## Applying ranges 

For each artifact a prefixed and suffixed exclusion/inclusion symbol can be added

```
    [<symbol>]<artifact declaration>[<symbol>][+[<symbol>]<artifact declaration>[<symbol>]+..]
```

### Lower bounds
The lower bounds describe the interval towards the dependencies of an artifact. If an artifact has a lower bound prefix, it acts as a boundary, i.e. nothing below is taken into account. The kind of prefix describes how the boundary is to be treated.

- [ : the interval is closed, i.e. the boundary itself is included. 
- ] : the interval is open, i.e. the boundary itself is not included.


### Upper bounds
The upper bounds describe the interval towards the dependers (aka requesting artifacts) of an artifact. If an artifact has a upper bound suffix, it acts as a boundary, i.e. nothing above is taken into account. The kind of suffix describes how the boundary is to be treated.


## Examples

#### upper bounds only 

##### closed upper bounds / nothing  specified 
```
    com.braintribe.terminal:Terminal#1.0
```
this is actually interpreted as closed upper bounds

```
    com.braintribe.terminal:Terminal#1.0]
```
and yields

```
    com.braintribe.terminal:TerminalParent#1.0.1
    com.braintribe.terminal:A#1.0.1
    com.braintribe.grpBase:GrpBaseParent#1.0.1
    com.braintribe.grpBase:BaseDependency#1.0.1
    com.braintribe.grpOne:GrpOneParent#1.0.1
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.grpOne:A#1.0.1
    com.braintribe.grpOne.subOne:GrpOneSubOneParent#1.0.1
    com.braintribe.grpOne.subOne:B#1.0.1
    com.braintribe.grpOne.subOne:A#1.0.1
    com.braintribe.terminal:Terminal#1.0.1
```

this would be the old target "install-all"

##### open upper bounds

```
    com.braintribe.terminal:Terminal#1.0[
```

yields

```
    com.braintribe.terminal:TerminalParent#1.0.1
    com.braintribe.terminal:A#1.0.1
    com.braintribe.grpBase:GrpBaseParent#1.0.1
    com.braintribe.grpBase:BaseDependency#1.0.1
    com.braintribe.grpOne:GrpOneParent#1.0.1
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.grpOne:A#1.0.1
    com.braintribe.grpOne.subOne:GrpOneSubOneParent#1.0.1
    com.braintribe.grpOne.subOne:B#1.0.1
    com.braintribe.grpOne.subOne:A#1.0.1
```
this would be the old target "install-deps"


####  lower bounds only 
```
    ]com.braintribe.terminal:Terminal#1.0
```

yields

```
    <nothing, rien de rien, niente, nada, nüscht>
```
nothing - no upper bounds, aka no terminal


#### lower and upper bounds

```
    [com.braintribe.terminal:Terminal#1.0]
```

yields

```
    com.braintribe.terminal:Terminal#1.0.1
```

this would be the old target "install"


```
    ]com.braintribe.terminal:Terminal#1.0[
```
yields

```
    <nothing, rien de rien, niente, nada, nüscht>
```

nothing - open interval, i.e. it takes itself out of the equation

```
    [com.braintribe.grpOne:C#1.0+com.braintribe.terminal:Terminal#1.0]
```
yields

```
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.grpOne:A#1.0.1
    com.braintribe.grpOne.subOne:A#1.0.1
    com.braintribe.terminal:Terminal#1.0.1
```
read this as everything in the branches from com.braintribe.grpOne:C#1.0.1 up to com.braintribe.terminal:Terminal#1.0.1 is installed.

```
    [com.braintribe.grpOne:C#1.0]+[com.braintribe.terminal:Terminal#1.0]
```

yields

```
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.terminal:Terminal#1.0.1
```
 
and

```
    [com.braintribe.grpOne.subOne:B#1.0+[com.braintribe.grpOne:C#1.0+com.braintribe.terminal:Terminal#1.0]
```

yields

```
    com.braintribe.grpOne.subOne:B#1.0.1
    com.braintribe.grpOne:C#1.0.1
    com.braintribe.grpOne:A#1.0.1
    com.braintribe.grpOne.subOne:A#1.0.1
    com.braintribe.terminal:Terminal#1.0.1
```
