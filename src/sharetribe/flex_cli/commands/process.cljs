(ns sharetribe.flex-cli.commands.process
  (:require [sharetribe.flex-cli.io-util :as io-util]
            [sharetribe.flex-cli.exception :as exception]
            [sharetribe.flex-cli.commands.process.list :as process.list]
            [sharetribe.flex-cli.commands.process.pull :as process.pull]
            [sharetribe.flex-cli.commands.process.push :as process.push]
            [sharetribe.flex-cli.commands.process.create :as process.create]
            [sharetribe.flex-cli.commands.process.create-or-push-and-create-or-update-alias :as process.create-or-push-and-create-or-update-alias]
            [sharetribe.flex-cli.commands.process.create-alias :as process.create-alias]
            [sharetribe.flex-cli.commands.process.update-alias :as process.update-alias]
            [sharetribe.flex-cli.commands.process.delete-alias :as process.delete-alias]
            [sharetribe.tempelhof.tx-process :as tx-process]))

(declare describe-process)

(def cmd {:name "process"
          :no-api-key? true
          :no-marketplace? true
          :handler #'describe-process
          :desc "describe a process file"
          :opts [{:id :path
                  :long-opt "--path"
                  :desc "path to the directory where the process.edn file is"
                  :required "PROCESS_DIR"}

                 ;; Commented out. These are not yet implemented.
                 #_{:id :process-name
                  :long-opt "--process"
                  :required "[WIP] PROCESS_NAME"}
                 #_{:id :version
                  :long-opt "--version"
                  :required "[WIP] VERSION_NUM"}
                 #_{:id :alias
                  :long-opt "--alias"
                  :required "[WIP] PROCESS_ALIAS"}
                 {:id :transition-name
                  :long-opt "--transition"
                  :desc "transition name, e.g. transition/request to get more details of it"
                  :required "TRANSITION_NAME"}

                 ;; TODO I don't know what's the plan for getting the
                 ;; marketplace ident into commands yet. Is it
                 ;; declared per command? Who implements the the logic
                 ;; of pulling marketplace ident from state when it's
                 ;; not explicitly given to the command?
                 ]
          :sub-cmds [process.list/cmd
                     process.pull/cmd
                     process.push/cmd
                     process.create/cmd
                     process.create-or-push-and-create-or-update-alias/cmd
                     process.create-alias/cmd
                     process.update-alias/cmd
                     process.delete-alias/cmd]})

(defn- load-tx-process-from-path
  "Load process from given path (directory). Encapsulates the idea of
  understanding process format on disk.

  TODO I don't belong to this namespace in the long term."
  [path]
  (let [path-to-process-file (str path "/process.edn")]
    (-> (io-util/load-file path-to-process-file)
        (tx-process/parse-tx-process-string))))

(defn- format-state [state]
  (-> state
      (update :name io-util/namespaced-str)
      (update :in #(apply str (interpose
                               ", "
                               (map io-util/namespaced-str %))))
      (update :out #(apply str (interpose
                               ", "
                               (map io-util/namespaced-str %))))))

(defn print-states [states]
  (println (io-util/section-title "States"))
  (let [ks [:name :in :out]]
    (io-util/print-table ks (map format-state states)))
  (println) (println))

(defn- format-actor [actor privileged?]
  (let [actor-role (io-util/kw->title actor)]
    (if privileged?
      (str actor-role " (privileged transition)")
      actor-role)))

(defn- format-transition [{:keys [privileged?] :as transition}]
  (-> transition
      (update :actor format-actor privileged?)
      (update :name io-util/namespaced-str)
      (update :from io-util/namespaced-str)
      (update :to io-util/namespaced-str)
      (update :at io-util/format-code)))

(defn print-transitions [transitions]
  (println (io-util/section-title "Transitions"))
  (let [ks [:name :from :to :actor]]
    (io-util/print-table ks (map format-transition transitions)))
  (println) (println))

(defn- format-notification [notification]
  (-> notification
      (update :to io-util/kw->title)
      (update :name io-util/namespaced-str)
      (update :on io-util/namespaced-str)
      (update :template io-util/namespaced-str)
      (update :at io-util/format-code)))

(defn print-notifications [notifications]
  (println (io-util/section-title "Notifications"))
  (let [ks [:name :on :to :template]]
    (io-util/print-table ks (map format-notification notifications))))

(defn- describe-full-process [tx-process]
  (print-states (tx-process/states tx-process))
  (print-transitions (tx-process/transitions tx-process))
  (print-notifications (tx-process/notifications tx-process)))

(defn- describe-transition [tx-process tr-name]
  (if-let [transition (tx-process/transition tx-process tr-name)]
    (let [formatted-tr (format-transition transition)
          formatted-nts (->> (tx-process/notifications-after-transition
                              tx-process
                              tr-name)
                             (map format-notification))]
      (println
       (io-util/definition-list [:name :from :to :actor :at] formatted-tr))

      (println (io-util/section-title "Actions"))
      (io-util/print-table [:name :config] (:actions formatted-tr))
      (println)

      (println (io-util/section-title "Notifications") "\n")
      (if (seq formatted-nts)
        (println (apply str
                        (interpose
                         "\n--\n\n"
                         (map #(io-util/definition-list
                                 [:name :on :to :template :at] %)
                              formatted-nts))))
        (println "-"))
      (println))

    (exception/throw! :command/invalid-args
                      {:command :process
                       :errors [(str "No transition for name " (io-util/namespaced-str tr-name) " found.")]})))

(defn describe-process
  "Describe a process or a process transition if --transition is
  given.

  The process is loaded either from disk (when --path is given) or
  from a live backend (with coordinates --process (--version ||
  --alias) and --marketplace)."
  [{:keys [path process-name version alias marketplace transition-name] :as opts} ctx]
  (if (empty? path)
    (exception/throw! :command/invalid-args
                      {:command :process
                       :errors ["Currently only --path is supported and must be specified."]})
    (let [tx-process (load-tx-process-from-path path)]
      (if (seq transition-name)
        (describe-transition tx-process (keyword transition-name))
        (describe-full-process tx-process)))))

