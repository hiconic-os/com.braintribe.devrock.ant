# RX application packaged resources

RX application assembly materializes every resource referenced by an artifact's
`META-INF/classpath-index.txt` into a lossless, artifact-scoped filesystem mirror:

```text
application/packaged-resources/
  <artifact-file-name-without-jar>/
    HICONIC-CONF/...
    MY-RESOURCES/...
    ...
  index.properties             # deterministic runtime index
  index.json
```

The resources are copied byte-for-byte at their canonical classpath-relative
paths. Artifact provenance lives in the central indexes instead of adding
technical `META-INF` files to every slot. `index.properties` is the compact
runtime inventory; `index.json` additionally records digests and packaging
diagnostics.

Applications carrying
`hiconic.platform.reflex:configuration-assembly-processing` additionally opt
into build-time configuration closure:

```text
application/effective-conf/
  compiled/
    <configuration-key>.yaml
    properties.yaml
  <artifact-slot>/
    <unconsumed-configuration-resource>

application/configuration-compilation.yaml
```

The application classpath supplies the same model reflection and merger
implementation used at runtime. Modeled configuration and properties are
merged into `compiled/`; properties available at assembly time are resolved,
while explicitly declared deployment imports remain symbolic. Configuration
resources which no compiler owns remain byte-identical in their artifact slot.
Consequently every raw `HICONIC-CONF` contribution is either represented by a
compiled result or retained as a residual input. Undeclared unresolved
properties, conflicts, invalid modeled configuration, and output collisions
fail the application build. `configuration-compilation.yaml` records the
assembly result outside the consumable configuration slots.

Script-based launches set `reflex.packaged.resources.dir`. If `effective-conf`
exists, the platform reads general resources directly from `packaged-resources`,
suppresses only its raw `HICONIC-CONF/` entries, and exposes the direct
`effective-conf` slots under the canonical logical `HICONIC-CONF/` prefix.
There is therefore no duplicate configuration consumption, while icons,
templates, and all other indexed resources continue to come from the complete
mirror. `application/conf` remains the later deployment override layer.

Older `classpath-resources` and `packaged-conf` layouts remain readable during
the transition. IDE launches without a filesystem resource property continue
to use the real classpath.

Classpath-resource mirroring is enabled by default. Bootstrap-oriented CLI applications which do
not consume indexed runtime configuration may explicitly disable it in their POM:

```xml
<mirrorClasspathResources>false</mirrorClasspathResources>
```

This opt-out also keeps such terminals buildable by an older SDK while a newly published
`assembleClasspathResources` task is being integrated into the next SDK generation.

## Pure indexed resource artifacts

An artifact containing only indexed resources is marked with:

```text
META-INF/classpath-resource-only
```

Which currently contains:

```properties
formatVersion=1
```

It is written by the `index-classpath-resources` target of `common-ant-script`. It writes the marker only when the artifact's `src`
contains nothing besides `META-INF/classpath-resources.txt` and the entries that file declares. The marker is generated into the build output and never enters the source tree.

See the [classpath resources](../../hiconic-documentation/generic-model/classpath-resources.md) documentation for
the declaration format and for how the same declaration serves an IDE launch.

`assemble` retains all runtime JARs for a mixed local/IDE environment. `assemble-image` additionally
removes marked artifacts from `application/lib` after all indexed resources have been materialized
in `packaged-resources`. Pruning happens before the launch JAR manifest is generated, so its
`Class-Path` only references retained JARs.

`packaged-solutions.txt` remains a complete dependency provenance list. The mirror's `index.json`
records whether each indexed artifact is `MIRRORED_AND_CLASSPATH` or `MIRRORED_ONLY` and includes a
SHA-256 digest for every materialized resource.

When the RX platform-reflection diagnostic package is available, its configuration archive includes
`packaged-resources/`, `effective-conf/`, and `configuration-compilation.yaml`. This exposes the
lossless inputs, effective runtime view, and compilation result without inspecting application
JARs.

## Application images

Image-stable properties belong to the application POM:

```xml
<properties>
    <dockerImage>example/server</dockerImage>
    <dockerTag>dev</dockerTag>
    <dockerBaseImage>ghcr.io/example/server-base:latest</dockerBaseImage>
    <dockerExposedPorts>8080 8443</dockerExposedPorts>
    <dockerEnvironment>NAME=value;OTHER=value</dockerEnvironment>
</properties>
```

`assemble-image` produces the image-oriented application layout, including the classpath-resource
mirror and removal of marked pure-resource JARs. `build-image` packages an already assembled
application. This is useful for pipelines which add deployment overlays or derive final tags after
resolution. `build-docker` performs both steps.

Registry names and the publishing mode are invocation properties rather than application
properties:

```text
docker.image.names
docker.image.push
docker.image.pull
docker.image.noCache
```

Local application artifacts may additionally set `localDockerInstall` to `true`. This declares that
they support installing a local image, but does not add a Docker side effect to an ordinary build.
Outside CI, callers explicitly activate the image installation with:

```text
hc -Dinstall.local.docker=true install
```

The opt-in controls only this local side effect; all actual image construction still uses the same
`build-image` target and `buildApplicationImage` task as a pipeline-synthesized app.
