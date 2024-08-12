## ready-made notifiers with excellent DX (Developer Experience) for your devops pipelines

### gitlab2teams.clj

GitLab pipeline -> Teams notifier _([Workflows-ready](https://aka.ms/O365ConnectorDeprecation)!)_

`pipeline-notifier` is available as a [docker image](https://hub.docker.com/r/jeffreyjflim/pipeline-notifier) for use in GitLab pipelines.

You will need to provide the following environment variables ("prefix-namedspaced" under `PN__`) to the container:
- `PN__TEAMS_WEBHOOK_URL`: your webhook URL for posting notifications to Teams. For your Workflows action's `Message` parameter, use `@{triggerBody()?['text']}`. More detailed setup instructions forthcoming, but for now, see [this GitLab issues comment](https://gitlab.com/gitlab-org/gitlab/-/issues/471344#note_2022899536).
- `PN__GITLAB_ACCESS_TOKEN`: GitLab Access Token (needed to get the actual _raw_, un-expanded commit message). For a least-privilege setup, scope `read_api` is all you need; and if you're using a project- or group-based access token, use `Reporter` for the role.
- `PN__PIPELINE_PASSED`: define this (any value) in your "notify-success" job to indicate to `gitlab2teams.clj` that the pipeline has passed. _Otherwise, it is considered to have failed._ See the sample snippet below if you want to see how to do this.
- `PN__AUTHOR_STYLE` (optional): desired "author mention style". Can be one of `name`, `name_email`, `email`, or `username`. Defaults to `name_email`.
- `PN__PROJECT_TRIM_CHARS` (optional): *string* or *regex* for removing characters from project name
  - The expected use for this would be to remove a prefix (although sure, you could also remove characters from in the middle if you wish!) so that your project name is shorter and more succinct.
    You may have a nested project name, for instance `company-name/project-name/subproject-name/backend/core`, but for your own purposes, you only care to show `backend/core` (the prefix / other context is unnecessary) in your notifications. In this case, setting `PN__PROJECT_TRIM_CHARS` to `company-name/project-name/subproject-name/` would work, as would something more generic like `^[^/]*/[^/]*/[^/]*/`.

Sample YAML snippet:
```
notify-success:
  stage: .post
  variables:
    PN__PIPELINE_PASSED: boo
    PN__AUTHOR_STYLE: name_email

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - script is ignored but must be provided as per GitLab requirements

notify-failure:
  stage: .post
  when:  on_failure

  variables:
    PN__AUTHOR_STYLE: username

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - script is ignored but must be provided as per GitLab requirements
```

### License

pipeline-notifier is released under the [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.html); [babashka](https://github.com/babashka/babashka) is released under its own [license](https://github.com/babashka/babashka?tab=readme-ov-file#license).
