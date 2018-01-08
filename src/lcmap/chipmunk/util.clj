(ns lcmap.chipmunk.util
  "Miscellaneous support functions."
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
            [org.httpkit.client :as http]
            [mount.core])
  (:import  [org.joda.time DateTime Interval DateTimeZone]
            [java.util TimeZone]))

(set! *warn-on-reflection* true)

;; The time zone is set explicitly for both the JVM and Joda to avoid
;; conditions where Joda uses previously cached values; setting the
;; JVM time zone should be sufficient, but there are cases where it
;; may not be properly set implicitly.

(TimeZone/setDefault (TimeZone/getTimeZone "GMT"))

(DateTimeZone/setDefault (DateTimeZone/forTimeZone (TimeZone/getTimeZone "GMT")))


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
      (catch java.text.ParseException ex :clojure.spec.alpha/invalid))))


(def numberizer (spec/conformer numberize))


(defmulti intervalize
  ""
  (fn [n] (type n)))


(defmethod intervalize java.lang.String
  [interval]
  (try
    (Interval/parse interval)
    (catch java.lang.IllegalArgumentException ex :clojure.spec.alpha/invalid)))


(defmethod intervalize org.joda.time.Interval
  [interval]
  interval)


(defn interval? [d]
  (try
    (intervalize d)
    true
    (catch java.lang.IllegalArgumentException ex nil)))


(def intervalizer (spec/conformer intervalize))


(defmulti instantize
  ""
  (fn [i] (type i)))


(defmethod instantize java.lang.String
  [instant]
  (try
    (->> instant
         (re-matches #"([0-9]{4})([0-9]{2})([0-9]{2})")
         (rest)
         (clojure.string/join "-")
         (DateTime/parse)
         (.toDate))
    (catch java.lang.IllegalArgumentException ex :clojure.spec.alpha/invalid)))


(defmethod instantize org.joda.time.DateTime
  [^org.joda.time.DateTime instant]
  (.toDate instant))


(defmethod instantize java.util.Date
  [^java.util.Date instant]
  instant)


(defn re-grouper
  [^java.util.regex.Matcher matcher keys]
  (if (.matches matcher)
    (into {} (map (fn [k] [k (.group matcher (name k))])) keys)))


(defn re-mapper [re ks s]
  (re-grouper (re-matcher re s) ks))


(defn check!
  ""
  [spec params]
  (or (some->> (spec/explain-data spec params)
               (ex-info "validation error")
               (throw))
      (spec/conform spec params)))


(defn byte-buffer-copy
  ""
  [^java.nio.ByteBuffer byte-buffer]
  (let [size (.capacity byte-buffer)
        copy (byte-array size)]
    (.get byte-buffer copy)
    (.rewind byte-buffer)
    copy))
