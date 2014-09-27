(ns ac-view.server
  (:require [ring.middleware.resource :as resources]
            [ring.util.response :as response]))

;;; THIS IS DUMMY SERVER for frontend

(defn- handler [req]
  (cond
    (re-find #"/$" (:uri req)) (response/redirect (str (:uri req) "index.html"))
    :else nil))

(defn init []
  nil)

(def app
  (-> handler
    (resources/wrap-resource "public")))

(defn app [req]
  ;; Kill cache for debug
  (let [res ((-> handler (resources/wrap-resource "public")) req)
        res (assoc res :headers (assoc (:headers res)
                                       "Cache-Control" "no-cache"
                                       "Pragma" "no-cache"))]
    ;; IE must needs content-type for css files !!!
    (if (and
          (= java.io.File (type (:body res)))
          (re-find #"\.css$" (.getName (:body res))))
      (assoc res :headers (assoc (:headers res) "Content-Type" "text/css"))
      res)))
