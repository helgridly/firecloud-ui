# firecloud-ui

FireCloud user interface for web browsers.

https://firecloud.dsde-dev.broadinstitute.org/

https://firecloud.dsde-staging.broadinstitute.org/

## Technologies

[ClojureScript](https://github.com/clojure/clojurescript) is used for the UI.

We use the [Leiningen](http://leiningen.org/) build tool to manage ClojureScript dependencies.

The code incorporates usage of [react-cljs](https://github.com/dmohs/react-cljs) which is 
a ClojureScript wrapper for [React](https://facebook.github.io/react/).

Figwheel replaces the running JavaScript within the page so changes are visible without a browser reload. More information [here](https://github.com/bhauman/lein-figwheel). [This video](https://www.youtube.com/watch?v=j-kj2qwJa_E) gives some insight into the productivity gains available when using this technology (up to about 15:00 is all that is necessary).

## Getting Started

Start with a [docker](https://www.docker.com/) environment.

You will need to create a Google web application client ID from here:

https://console.developers.google.com/

This should include your docker host as an authorized JavaScript origin. By convention, we use `dhost` as the hostname, which requires an addition to your /etc/hosts file, e.g.,

```
192.168.99.100 dhost
```

The IP address is returned by `docker-machine ip default`.

Set your client ID in your environment:

```
export GOOGLE_CLIENT_ID='...'
```

Start the server in docker:

```
./script/dev/start-server.sh
```

Build the code:

```
docker exec -it firecloud-ui ./script/dev/build-once.sh

```

## Build Options

Build once:
```
docker exec -it firecloud-ui ./script/dev/build-once.sh
```

Watch files and rebuild whenever a file is saved:
```
docker exec -it firecloud-ui ./script/dev/start-auto-build.sh
```

Watch files, rebuild, and reload changes into the running browser window:
```
docker exec -it firecloud-ui ./script/dev/start-hot-reloader.sh
```

This can take around 20 seconds to completely start. When ready, it will display the following message:
```
Prompt will show when figwheel connects to your application
```

To connect, reload the browser window (see the Running section below).
