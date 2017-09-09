(ns lcmap.chipmunk.util
  "Miscellaneous support functions."
  (:require [clojure.tools.logging :as log]
            [mount.core]))


(defn add-shutdown-hook
  ""
  []
  (log/debug "register shutdown handler")
  (.addShutdownHook (java.lang.Runtime/getRuntime)
                    (Thread. #(mount.core/stop) "shutdown-handler")))


(defn add-usr-path
  ""
  [& paths]
  (let [field (.getDeclaredField ClassLoader "usr_paths")]
    (try (.setAccessible field true)
         (let [original (vec (.get field nil))
               updated  (distinct (concat original paths))]
           (.set field nil (into-array updated)))
         (finally
           (.setAccessible field false)))))


(defn get-usr-path
  ""
  [& paths]
  (let [field (.getDeclaredField ClassLoader "usr_paths")]
    (try (.setAccessible field true)
         (vec (.get field nil))
         (finally
           (.setAccessible field false)))))


(defn amend-usr-path
  ""
  [more-paths]
  (apply add-usr-path more-paths))


(defmulti numberize
  "Converts a string to a number or nil.  If the string contains a mix of
   number and character data, returns "
  (fn [n] (type n)))


(defmethod numberize :default [n]
  nil)


(defmethod numberize Number [number]
  number)


(defmethod numberize String [string]
  (let [number-format (java.text.NumberFormat/getInstance)]
    (try
      (.parse number-format string)
      (catch java.text.ParseException ex nil))))
