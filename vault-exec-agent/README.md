## a better Vault exec agent, with ready-made agent-assisted images for common Docker images

_**better**_ = smaller and more lightweight than `vault`<sup>1</sup>, _with support for KV path-based key-value inheritance, and support for [GitLab-style file-type variables](https://docs.gitlab.com/ee/ci/variables/#use-file-type-cicd-variables)<sup>2</sup>._

(<sup>1.</sup> `bb` + `vault-exec-agent.clj` = 79M as opposed to 409M for `vault` as tested on arm64 as of August 30 2024)

You will need to provide the following environment variables to the agent (`vault-exec-agent.clj`):
- `VAULT_ADDR`
- `VAULT_KV_MOUNT_PATH` (optional): KV secrets engine mount path; defaults to `kv-v2`.
- `VAULT_KV_PATH`: KV secret key path (e.g. `dev/pipeline/service_name`)
- `VAULT_TOKEN`: this token must have the permissions to read all of the KV secrets along the path to `VAULT_KV_PATH` (so with the example of `dev/pipeline/service_name` above, this token would be able to read `dev`, `dev/pipeline`, and `dev/pipeline/service_name`).
_**Secrets do NOT need to be defined at all stops along VAULT_KV_PATH**; the token just needs to be able to read them if available._

<sup>2.</sup> **Special handling for "\*_FILE" key-values in secret data:** when `vault-exec-agent.clj` encounters a "\*_FILE" key (e.g. `AWS_CONFIG_FILE`) in the data of a KV secret, it treats its value as the contents of a file, and automatically writes the content out into a file.
If you include a `#bb-ops: ` header as the first line (`#bb-ops: /path/to/write/contents/to`), `vault-exec-agent.clj` will write the contents out to the given filename / path.
After "\*_FILE" key-value processing, `vault-exec-agent.clj` updates the value to the filename / path, for reading / usage by your executable(s).

### bb-ops.kaniko

[`bb-ops.kaniko`](https://hub.docker.com/r/jeffreyjflim/bb-ops.kaniko) is `vault-exec-agent.clj`-assisted `gcr.io/kaniko-project/executor:debug`; available as a [docker image](https://hub.docker.com/r/jeffreyjflim/bb-ops.kaniko) for use in GitLab pipelines.

Sample YAML snippet:
```
build-and-push:
  stage: build

  image:
    name: jeffreyjflim/bb-ops.kaniko

  script:
    - /kaniko/executor ...
```

### bb-ops.aws-cli

[`bb-ops.aws-cli`](https://hub.docker.com/r/jeffreyjflim/bb-ops.aws-cli) is `vault-exec-agent.clj`-assisted `public.ecr.aws/aws-cli/aws-cli`; available as a [docker image](https://hub.docker.com/r/jeffreyjflim/bb-ops.aws-cli) for use in GitLab pipelines.

Sample YAML snippet:
```
build-job:
  stage: build

  image:
    name: jeffreyjflim/bb-ops.aws-cli
    entrypoint: ['/usr/local/bin/vault-exec-agent.clj', '/usr/bin/env']

  script:
    - envsubst < .pipeline_files/task_definition_template.json > task_definition.json
    - aws ecs ...
```
