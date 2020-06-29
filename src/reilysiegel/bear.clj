(ns reilysiegel.bear
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(set! *warn-on-reflection* true)

(defn- parse-int
  "Utility to parse Integer."
  [s]
  (Integer/parseInt s))

(defn- battery-capacity
  "Returns the capacity of the battery.
  Uses data from `/sys/class/power_supply/BAT{battery}/capacity"
  ([] (battery-capacity 0))
  ([battery]
   (->> battery
        (format "/sys/class/power_supply/BAT%s/capacity")
        slurp
        str/trim-newline
        parse-int)))

(defn- battery-charging?
  "Returns the capacity of the battery.
  Uses data from `/sys/class/power_supply/BAT{battery}/status
  
  Optionally takes `charging-delay`, which can be used to delay the status
  check. This may be useful for udev events in systems where a `DISCHARGE` event
  is fired as soon as the charger is plugged in, and the status is updated some
  time later."
  ([] (battery-charging? 0 0))
  ([battery] (battery-charging? battery 0))
  ([battery charging-delay]
   (when (pos? charging-delay)
     (Thread/sleep (* 1000 charging-delay)))
   (->> battery
        (format "/sys/class/power_supply/BAT%s/status")
        slurp
        str/trim-newline
        #{"Charging"}
        boolean)))

(defn- battery-exists?
  "Checks for the existence of a battery."
  [battery]
  (.exists (io/file (format "/sys/class/power_supply/BAT%s"
                    battery))))

(def cli-opts
  [["-d" "--daemon" "Daemon mode"]
   ["-a" "--action ACTION" "Low battery action"
    :default :none
    :parse-fn keyword
    :validate [#{:hibernate :shutdown :suspend :none}
               "Must be one of {suspend|hibernate|shutdown|none}."]]
   ["-b" "--battery BATTERY" "Battery to track"
    :default 0
    :parse-fn parse-int
    :validate [battery-exists? "Battery does not exist."]]
   ["-l" "--limit LIMIT" "Battery limit, inclusive"
    :default 10
    :parse-fn parse-int
    :validate [(set (range 101)) "Must be 0-100"]]
   ["-p" "--poll-rate RATE" "Poll frequency (daemon mode only)"
    :default 10
    :parse-fn parse-int
    :validate [pos-int? "Poll rate must be a positive int."]]
   ["-c" "--charging-delay DELAY" "Charging check delay"
    :default 0
    :parse-fn parse-int
    :validate [(complement neg-int?) "Delay must be a positive int or zero."]]
   ["-h" "--help" "Show help"]])

(defn- take-action?
  [capacity limit charging?]
  (and (<= capacity limit)
       (not charging?)))

(defn- take-action! [action]
  (case action
    :shutdown (sh/sh "systemctl" "poweroff")
    :hibernate (sh/sh "systemctl" "hibernate")
    :suspend (sh/sh "systemctl" "suspend")
    :none nil))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-opts)
        {:keys [battery limit daemon action charging-delay poll-rate help]}
        options]
    (when help
      (println "Bear: a simple utility for easy sleep.\n\n")
      (println summary))
    (when errors
      (binding [*out* *err*]
        (run! println errors))
      (System/exit 1))
    (loop []
      (when (take-action? (battery-capacity battery)
                          limit
                          (battery-charging? battery charging-delay))
        (println "Limit exceeded, taking action" action)
        (take-action! action)
        (when-not daemon
          (System/exit 4)))
      (when-not daemon
        (System/exit 0))
      (Thread/sleep (* 1000 poll-rate))
      (recur))))
