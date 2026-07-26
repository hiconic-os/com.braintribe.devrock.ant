# RX application classpath resources

RX application assembly materializes every resource referenced by an artifact's
`META-INF/classpath-index.txt` into an artifact-scoped filesystem mirror:

```text
application/classpath-resources/
  <artifact-file-name-without-jar>/
    META-INF/classpath-index.txt
    META-INF/classpath-origin.properties
    HICONIC-CONF/...
  index.json
```

The resources are copied byte-for-byte. Placeholders are intentionally not resolved and modeled
configuration is not merged during assembly. This mirror is the lossless source/provenance view.
An effective, merged configuration is a separate reflection concern.

Script-based launches set `reflex.classpath.resources.dir` and therefore read indexed resources
exclusively from this mirror. IDE launches without that system property continue to use the real
classpath.

## Pure indexed resource artifacts

The producer build marks an artifact containing only indexed resources with:

```text
META-INF/classpath-resource-only
```

The marker currently contains:

```properties
formatVersion=1
```

It may only be generated after checking that the artifact has no Java classes, service-provider
declarations, native libraries, or nested archives. The shared classpath-index build target
`generate-pure-classpath-index` performs this validation. After compilation,
`mark-pure-classpath-resource` writes the marker into the build output; generated markers never
enter the source tree.

`assemble` retains all runtime JARs for a mixed local/IDE environment. `assemble-image` additionally
removes marked artifacts from `application/lib` after their resources have been mirrored. Pruning
happens before the launch JAR manifest is generated, so its `Class-Path` only references retained
JARs.

`packaged-solutions.txt` remains a complete dependency provenance list. The mirror's `index.json`
records whether each indexed artifact is `MIRRORED_AND_CLASSPATH` or `MIRRORED_ONLY` and includes a
SHA-256 digest for every materialized resource.

When the RX platform-reflection diagnostic package is available, its configuration archive also
contains this complete raw mirror under `classpath-resources/`. This exposes both runtime
configuration and classpath-origin configuration without inspecting application JARs.

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
