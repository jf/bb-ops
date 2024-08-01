## ready-made notifiers with excellent DX (Developer Experience) for your devops pipelines

### gitlab2teams.clj

GitLab pipeline -> Teams notifier _(Workflows-ready!)_

`pipeline-notifier` is currently available as a docker image on Docker Hub at https://github.com/jf/pipeline-notifier.

To use this in your GitLab pipeline, you will need to provide following environment variables to the container:
- `TEAMS_WEBHOOK_URL`: your webhook URL for posting the notification to Teams. For your Workflows action's `Message` parameter, use `@{triggerBody()?['text']}`. More detailed setup instructions forthcoming, but for now, see [this GitLab issues comment](https://gitlab.com/gitlab-org/gitlab/-/issues/471344#note_2022899536).
- `GITLAB_TOKEN`: GitLab API token (needed to get the actual raw commit message)
- `AUTHOR_STYLE`: desired "author mention style". Can be one of `name`, `name_email`, `email`, or `username`.

Sample YAML snippet for usage in GitLab:
```
notify-success:
  stage: .post
  variables:
    CI_PIPELINE_PASSED: define this (any value) to indicate that the pipeline has passed. Else it has failed
    AUTHOR_STYLE: name_email

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - # script is ignored but must be provided as per GitLab requirements

notify-failure:
  stage: .post
  when:  on_failure

  variables:
    AUTHOR_STYLE: username

  image:
    name: jeffreyjflim/pipeline-notifier
    entrypoint: ['/scripts/gitlab2teams.clj']

  script:
    - # script is ignored but must be provided as per GitLab requirements
```

### License

pipeline-notifier is released under the [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.html); [babashka](https://github.com/babashka/babashka) is released under its own [license](https://github.com/babashka/babashka?tab=readme-ov-file#license).
