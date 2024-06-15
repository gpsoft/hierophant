(ns hierophant.core
  (:require
   [clojure.tools.cli :as cli]
   [clojure.string :as str]
   [clojure.java.shell :refer [sh]]
   [hierophant.gui :as gui]
   [hierophant.util :as u])
  (:import
   [java.nio.file FileSystems Paths WatchEvent StandardWatchEventKinds WatchKey])
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
  (println "Ex:    hierophant /var/log /var/tmp/*")
  (println "         ...trailing '*' indicates recursive")
  (println "       hierophant --extension log --base /var log tmp/*")
  (println)
  (println "Options:")
  (println options-summary))

(defn- kind-str
  [kind]
  (cond
    (.equals kind StandardWatchEventKinds/ENTRY_CREATE) "create"
    (.equals kind StandardWatchEventKinds/ENTRY_MODIFY) "modify"
    (.equals kind StandardWatchEventKinds/ENTRY_DELETE) "delete"
    :default "unknown"))

(defn- event-str
  [dir event]
  (let [kind (kind-str (.kind event))
        file (.toString (.context event))]
    [(u/resolve-path dir file) kind dir file]))

(defn- collapse-events
  [dir watch-key]
  (let [events (map #(event-str dir %)
                    (.pollEvents watch-key))
        g (group-by first events)]
    (->> g
         (vals)
         (map last)
         (filter #(not= "delete" (second %))))))

(defn- watch
  [dirs handler]
  (let [fs (FileSystems/getDefault)
        service (.newWatchService fs)
        kinds [StandardWatchEventKinds/ENTRY_CREATE
                StandardWatchEventKinds/ENTRY_MODIFY
                StandardWatchEventKinds/ENTRY_DELETE]
        _ (dorun (for [dir-path dirs]
                    (-> (.getPath fs dir-path (into-array String []))
                        (.register service (into-array kinds)))))]
    (loop [watch-key (.take service)]
      (Thread/sleep 100)  ;; ...
      (when watch-key
        (let [dir (.toString (.watchable watch-key))]
          (dorun (for [[_ kind dir file] (collapse-events dir watch-key)]
                   (handler kind dir file)))
          (flush)
          (.reset watch-key)
          (recur (.take service)))))))

(defn- watch-dirs
  [args]
  (mapcat (fn [dir]
            (if (str/ends-with? dir "*")
              (u/dirs (str/replace dir #"\*$" ""))
              [dir]))
          args))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)
        {:keys [area-list help]} options
        err-options? (when errors
                       (println (str/join \newline errors))
                       true)]
    (cond
     (or help err-options?) (show-usage! summary)
     :else (do
            (gui/show-tasktray-icon)
            (watch (watch-dirs arguments)
                   (fn [kind dir file]
                     (sh "cmd" "/c" "copy" (u/resolve-path dir file) "c:\\tmp")))))))

(comment

 (-main "--help")
 (cli/parse-opts ["--extension" "log" "--extension" "js" "/var/log" "/var/tmp"] cli-options)

 (watch ["./work"]
        (fn [kind dir file]
          (prn kind)
          (prn dir)
          (prn file)))

 (watch-dirs ["./work" "./src/*"])
 (watch-dirs ["./*"])
 (str/replace "./src/*" #"\*$" "")

 (binding [*debug* true]
   )

 )

