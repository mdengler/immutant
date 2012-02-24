;; Copyright 2008-2012 Red Hat, Inc, and individual contributors.
;;
;; This is free software; you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License as
;; published by the Free Software Foundation; either version 2.1 of
;; the License, or (at your option) any later version.
;;
;; This software is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
;; Lesser General Public License for more details.
;;
;; You should have received a copy of the GNU Lesser General Public
;; License along with this software; if not, write to the Free
;; Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
;; 02110-1301 USA, or see the FSF site: http://www.fsf.org.

(ns immutant.jobs.core
  (:use immutant.utilities
        immutant.mbean)
  (:require [immutant.registry :as registry]
            [clojure.tools.logging :as log])
  (:import [org.immutant.jobs ClojureJob ScheduledJobMBean]
           [org.immutant.jobs.as JobsServices]
           [org.projectodd.polyglot.jobs BaseScheduledJob]))

(defn job-schedulizer  []
  (registry/fetch "job-schedulizer"))

(defn create-scheduler
  "Creates a scheduler for the current application.
A singleton scheduler will participate in a cluster, and will only execute its jobs on one node."
  [singleton]
  (log/info "Creating job scheduler for"  (app-name) "singleton:" singleton)
  (.createScheduler (job-schedulizer) singleton))

(defn scheduler
  "Retrieves the appropriate scheduler, creating it if necessary"
  [singleton]
  (let [name (str (if singleton "singleton-" "") "job-scheduler")]
    (if-let [scheduler (registry/fetch name)]
      scheduler
      (registry/put name (create-scheduler singleton)))))

(defn wait-for-scheduler
  "Waits for the scheduler to start before invoking f"
  ([scheduler f]
     (wait-for-scheduler scheduler f 30))
  ([scheduler f attempts]
     (cond
      (.isStarted scheduler) (f)
      (< attempts 0)         (throw (IllegalStateException.
                                     (str "Gave up waiting for " (.getName scheduler) " to start")))
      :default               (do
                               (log/debug "Waiting for scheduler" (.getName scheduler) "to start")
                               (Thread/sleep 1000)
                               (recur scheduler f (dec attempts))))))

(defn create-job
  "Instantiates and starts a job"
  [f name spec singleton]
  (let [scheduler (scheduler singleton)]
    (doto
        (proxy [BaseScheduledJob ScheduledJobMBean]
            [ClojureJob (app-name) name "" spec singleton]
          (start []
            (wait-for-scheduler
             scheduler
             #(.addJob scheduler name (ClojureJob. f)))
            (proxy-super start))
          (getScheduler []
            (.getScheduler scheduler)))
      .start)))

(defn stop-job
  "Stops (unschedules) a job, removing it from the scheduler."
  [job]
  (if-not (.isShutdown (.getScheduler job))
    (.stop job)))

;; we don't currently register an mbean for each job, since we can't
;; yet easily unregister the mbean
(defn register-job-mbean [name job]
  (register-mbean
   "immutant.jobs"
   (.append JobsServices/JOBS
            (doto (make-array String 2)
              (aset 0 (app-name))
              (aset 1 name)))
   job
   (job-schedulizer)))
