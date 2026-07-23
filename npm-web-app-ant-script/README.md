# npm web application Ant script

This Ant script builds a browser application with the toolchain declared by
its `package.json` and publishes the result as the Devrock part
`web-app:zip`.

The consuming artifact must contain:

- `pom.xml` for its hiconic/Devrock identity and dependencies
- `build.xml` importing `npm-web-app-ant-script`
- `package.json` with a required `build` script
- `package-lock.json`

The default lifecycle executes:

```text
npm ci --no-audit --no-fund
npm run --if-present typecheck
npm run build
```

The build script must write its deployable static files to `build/web`.
Another directory can be selected with the Ant property
`web.app.build.dir`.

The regular `test` target executes `npm run --if-present test`.

An RX setup can mount the published part through
`HICONIC-CONF/webapp-dependencies.properties`, for example:

```properties
hiconic.platform.reflex:log-reflection-webapp#[1.0,1.1)/web-app:zip=/log-reflection;welcome=index.html
```
