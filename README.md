# jenkins-atom-ant

The [ant](https://plugins.jenkins.io/ant/) plugin (≈200k installations) as an **atom**: one Java file,
no plugin.

Runs `ant <goals> <options>` in the job workspace and reports the exit code. The plugin's value was never the
wrapping — it was the tool, the log streaming and the fail-on-error rule, all of which are a few lines here.

* `goals` / `options` — `${ENV}` is expanded, so `-Dbuild=${BUILD_NUMBER}` works.
* `home` — a `ant` install directory; empty means PATH, then `$ANT_HOME`.
* `failOnError` — default on; off keeps the exit code as an output and lets the build continue.

## Why an atom and not the plugin

The plugin variants each re-implement tool discovery, environment setup and result mapping, and they drift. This atom
is one file you can read in a minute, it appears in the atom center with a form, and a pipeline calls it with
`runAtom(atom: 'ant', version: '1.0.0', inputs: [goals: 'clean build'])`.

## Build and test

```bash
mvn verify             # → target/dist/ant/{ant.jar,task.json}
```

`src/test/java/atom/AntIT.java` starts the packaged jar the way the runner does, against a fake `ant` on PATH: the
arguments and the workspace the tool sees, a non-zero exit both failing and being tolerated, and the PATH fallback.

## Publish

```bash
tools/publish-atom.sh --center <center-dir> --atom-dir target/dist/ant --version 1.0.0 \
    --key key.pem --key-id <keyId>     # tools/ 来自 jenkins-atom-plugin 仓库
```
