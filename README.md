## ready-made notifiers with excellent DX (Developer Experience) for your devops pipelines

### gitlab2teams.clj

GitLab pipeline -> Teams notifier _([Workflows-ready](https://aka.ms/O365ConnectorDeprecation)!)_

`pipeline-notifier` is available as a [docker image](https://hub.docker.com/r/jeffreyjflim/pipeline-notifier) for use in GitLab pipelines.

You will need to provide the following environment variables to the container:
- `TEAMS_WEBHOOK_URL`: your webhook URL for posting notifications to Teams. For your Workflows action's `Message` parameter, use `@{triggerBody()?['text']}`. More detailed setup instructions forthcoming, but for now, see [this GitLab issues comment](https://gitlab.com/gitlab-org/gitlab/-/issues/471344#note_2022899536).
- `GITLAB_TOKEN`: GitLab Access Token (needed to get the actual _raw_, un-expanded commit message). For a least-privilege setup, scope `read_api` is all you need; and if you're using a project- or group-based access token, use `Reporter` for the role.
- `PIPELINE_NOTIFIER_AUTHOR_STYLE`: desired "author mention style". Can be one of `name`, `name_email`, `email`, or `username`.

Sample YAML snippet for usage in GitLab:
```
notify-success:
  stage: .post
  variables:
    CI_PIPELINE_PASSED: define this (any value) to indicate that the pipeline has passed. Else it has failed
    PIPELINE_NOTIFIER_AUTHOR_STYLE: name_email

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - script is ignored but must be provided as per GitLab requirements

notify-failure:
  stage: .post
  when:  on_failure

  variables:
    PIPELINE_NOTIFIER_AUTHOR_STYLE: username

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - script is ignored but must be provided as per GitLab requirements
```

### License

pipeline-notifier is released under the [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.html); [babashka](https://github.com/babashka/babashka) is released under its own [license](https://github.com/babashka/babashka?tab=readme-ov-file#license).
