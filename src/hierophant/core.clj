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
  [
   ; ["-b" "--base" "Base directory path"
   ;  :default "."]
   ; ["-e" "--extension EXT" "File extension"
   ;  :multi true
   ;  :update-fn conj
   ;  :default []]
   ["-c" "--caption CAPTION" "Caption for system tray icon"
    :default "Hierophant"]
   ["-a" "--action ACTION" "Template for action(see below)"
    :default ""]
   ["-h" "--help" "Show usage"]])

(defn- show-usage!
  [options-summary]
  (println "Hierophant barrier can detect any change in the file system,")
  (println "then fire arbitrary command right away.")
  (println "Usage: hierophant [OPTIONS] DIR...")
  (println "Ex:    hierophant /var/log \"/var/tmp/*\" --action=\"cp {1}/{2} /tmp\"")
  (println "         ...trailing '*' of DIR indicates recursive")
  (println "       hierophant '\".\\src\\*\"' --action=\"cmd /c copy {1}\\{2} c:\\temp\"")
  (println "         ...on PowerShell, note the way to quote")
  (println)
  (println "Options:")
  (println options-summary)
  (println)
  (println "Action template:")
  (println "  {0}: Kind of changes. create, modify, or delete")
  (println "  {1}: The directory path specified by DIR argument")
  (println "  {2}: The file name")
  )

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
          (dorun (for [[path kind dir file] (collapse-events dir watch-key)]
                   (when-not (u/dir? path)
                     (handler kind dir file))))
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
        {:keys [action help caption]} options
        err-options? (when errors
                       (println (str/join \newline errors))
                       true)]
    (cond
     (or help err-options?) (show-usage! summary)
     :else (do
            (when (u/os-win?)
              (gui/show-tasktray-icon caption))
            (watch (watch-dirs arguments)
                   (fn [kind dir file]
                     ; (println "kind:" kind)
                     ; (println "dir:" dir)
                     ; (println "file:" file)
                     ; (println "action:" action)
                     (u/shell-run! (u/message-format action [kind dir file]))
                     ))))))

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

