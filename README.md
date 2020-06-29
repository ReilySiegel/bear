# bear

A simple utility for easy sleep.

## Motivation

I use Arch btw, and when using Arch we have to do lots of things ourselves, like
making sure a laptop shuts off before the battery dies. The Great Arch Wiki
presents the following solution:

```
# Suspend the system when battery level drops to 5% or lower
SUBSYSTEM=="power_supply", ATTR{status}=="Discharging", ATTR{capacity}=="[0-5]", RUN+="/usr/bin/systemctl hibernate"
```

This solution relies on udev rules, which can be unreliable on some systems. On
my laptop, plugging in the charging cord causes a `Discharging` event, followed
by a `Charging` event several seconds later. Because of this, the above solution
can cause my laptop to hibernate *after* plugging in the power. On some systems,
udev does not provide *any* battery events.

The main goal of bear is to provide a unified utility for low-battery checks,
that can be used with any scheduling system, including udev, cron, systemd
timers, or even as a systemd service with bear's built in daemon mode.

By default, bear takes no action if the battery drops below the limit, returning
a status code of 4 if action should be taken, and zero if no action should be
taken. However, various actions (shutdown, hibernate, suspend) can be provided
with `--action` or `-a`.

## Usage

Bear requires [Clojure CLI Tools](https://clojure.org/guides/getting_started)
and optionally [GraalVM Native
Image](https://www.graalvm.org/docs/reference-manual/native-image/) to compile
the Clojure code to a native binary, instead of running on the JVM.

To compile a native binary, run:

```bash
clojure -A:native-image
```

Bear can then be called by running:

```bash
./bear [options]
```

If you chose to avoid GraalVM and run the Clojure code directly on the JVM, you
can run

```bash
clojure -m reilysiegel.bear [options]
```

Note that when running on the JVM, bear has a significantly longer start-up
time (on the order of seconds) than the native-image.

## Examples

Return exit code 4 if the battery is lower than 5%

```bash
./bear --limit 5
```

Hibernate if the battery is lower than 10%
```bash
./bear --limit 10 --action hibernate
```

Shutdown if the battery is lower than 5%
```bash
./bear --limit 5 --action shutdown
```
 
Run in daemon mode, with a poll-rate of 30 seconds, and suspend when the battery
is lower than 15%
```bash
./bear --daemon --poll-rate 30 --limit 15 --action suspend
```

Other options not covered here can bee seen with `./bear --help`
## License

Copyright © 2020 Reily Siegel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
