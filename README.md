
## COVID Impact Map  [![Build status](https://badge.buildkite.com/f3cfe4096b4a4624c50ef31a9075fba4898ea34c6bc6ff731d.svg)](https://buildkite.com/bloomworks/covid-impact-map)

With the threat of COVID-19 looming large across the global healthcare system, many ragtag software teams have sprung up to build tools that might help. Most, however, focus on _tracking the virus itself_, not the _virus' local impact_ on the healthcare system that must respond to it - which is a _big problem_, for all of us.

Because this data really doesn't exist, we're creating it with a few inputs, and then this piece of software turns that into a useful app. We are going to be accepting crowd-sourced inputs for certain report types, and others will be available only for healthcare staff.

At the end of the day, this repo is made by concerned citizens/volunteers, and we're trying our best. If the app doesn't work, we're very sorry, we are probably working on it right now (as long as the world is still under quarantine). If it doesn't accomplish what you need, we're all ears. In both cases - file a bug, or PRs are certainly welcome.

_-COVID Impact Map Team_


### Slack / [Productboard](https://portal.productboard.com/covidmap/1-covid-impact-map)

If you'd like to contribute, or join our Slack channel (to help us or keep us company!) fill out the form link below. For Productboard access, do the same, and in both cases make sure the email you list is the one where you want to receive your invite.

Productboard lets you suggest features, vote on features, and see the roadmap in realtime.


### Development process

First, clone the repo. Then, make sure you have [Bazelisk](https://github.com/bazelbuild/bazelisk) and [make](https://www.gnu.org/software/make/) working in your environment. If you want to use the auto-reload/auto-rebuild functionality, you'll also need to install [`ibazel`](https://github.com/bazelbuild/bazel-watcher). Once you've done that, you should basically be set to go!

You can run `make help` to get a list of local dev commands. That should output something like the following:

```
COVID Impact Map:

build                          Build the app.
clean                          Clean ephemeral build targets.
deploy                         Deploy the app to production.
dev                            Build the app, start it up, and auto-reload with changes.
distclean                      Clean ephemeral targets and dependencies.
forceclean                     DANGEROUS: Wipe all local changes and force-reset codebase.
help                           Show this help text.
image                          Build a container image for the app, locally.
push                           Build and publish a container image for the app, tagged with the current source tree hash.
run                            Run the app locally.
test                           Run any testsuites.
```

So you can just run `make build` to build the app, or `make run` to run it locally. If you run it locally, the app will be available at [`http://localhost:8080`](http://localhost:8080) by default.


### Simulating production

To simulate the full production stack, the app makes use of [Skaffold](https://skaffold.dev/). The tool is intelligent enough to push the entire app to your local Kubernetes cluster, and keep it up to date as you develop. In this operating mode, you should do the following additional steps to get it fully working:

1. Add `127.0.0.1 beta.covidmap.link` to your `/etc/hosts` (or equivalent on your OS)
2. Visit [`https://beta.covidmap.link:8443`](https://beta.covidmap.link:8443) in your favorite browser



### Team behind this app

*Coming soon.*


### Licensing

This app will be released under the MIT license. For other licensing details, see [GUST](https://github.com/sgammon/gust), the framework this app is written with.

