(ns hierophant.core
  (:require
   [clojure.tools.cli :as cli]
   [clojure.string :as str]
   [hierophant.gui :as gui]
   [hierophant.util :as u])
  (:gen-class))

(def ^:private ^:dynamic *debug* false)

(def ^:private cli-options
  [["-b" "--base" "Base directory path"
    :default "."]
   ["-e" "--extension EXT" "File extension"
    :multi true
    :update-fn conj
    :default []]
   ["-h" "--help" "Show usage"]])

(defn- show-usage!
  [options-summary]
  (println "Hierophant barrier can detect any change in the file system,")
  (println "then fire arbitrary command right away.")
  (println "Usage: hierophant [OPTIONS] DIR...")
  (println "Ex:    hierophant /var/log /var/tmp")
  (println "       hierophant --extension log --base /var log tmp")
  (println)
  (println "Options:")
  (println options-summary))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)
        {:keys [area-list help]} options
        err-options? (when errors
                       (println (str/join \newline errors))
                       true)]
    (cond
     (or help err-options?) (show-usage! summary)
     :else (gui/show-tasktray-icon)
     )))

(comment

 (-main "--help")
 (cli/parse-opts ["--extension" "log" "--extension" "js" "/var/log"] cli-options)

 (import
  '[java.nio.file FileSystems Paths WatchEvent StandardWatchEventKinds WatchKey])
 (let [fs (FileSystems/getDefault)
       watcher (.newWatchService fs)
       path (.getPath fs "./build" (into-array String []))
       _ (.register path watcher (into-array [StandardWatchEventKinds/ENTRY_CREATE]))
       watchKey (.take watcher)]
   (for [event (.pollEvents watchKey)]
     [(.kind event)
      (let [f (.toFile (.context event))]
        (.isDirectory f))
      (.watchable watchKey)]))

 (binding [*debug* true]
   )

 )

