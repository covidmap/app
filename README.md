
## COVID Impact Map  [![Build status](https://badge.buildkite.com/f3cfe4096b4a4624c50ef31a9075fba4898ea34c6bc6ff731d.svg)](https://buildkite.com/bloomworks/covid-impact-map)

*Coming soon.*


### Development process

First, clone the repo. Then, make sure you have [Bazelisk](https://github.com/bazelbuild/bazelisk) and [make](https://www.gnu.org/software/make/) working in your environment. Once you've done that, you should basically be set to go!

You can run `make help` to get a list of local dev commands. That should output something like the following:

```
COVID Impact Map:

build                          Build the app.
image                          Build a container image for the app, locally.
push                           Build and publish a container image for the app, tagged with the current source tree hash.
clean                          Clean ephemeral build targets.
distclean                      Clean ephemeral targets and dependencies.
forceclean                     DANGEROUS: Wipe all local changes and force-reset codebase.
help                           Show this help text.
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

