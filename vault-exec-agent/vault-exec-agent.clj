#!/usr/bin/env bb

(require '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[babashka.process :refer [exec]]
         '[babashka.fs :as fs])

(defn env [v]
  (System/getenv (str/upper-case (name v))))

(defn get-kv-values-at [path]
  (-> (str (env :VAULT_ADDR) "/v1/" (or (env :VAULT_KV_MOUNT_PATH) "kv-v2") "/data/" path)
      (http/get {:headers {"X-Vault-Token" (env :VAULT_TOKEN)}})
      (:body)
      (json/parse-string)
      (get-in ["data" "data"])))

(def merged-secret-values
  (loop [path-components (-> (env :VAULT_KV_KEY) (str/split #"/"))
         current (first path-components)
         env {}]
    (if (empty? path-components)
      env
      (recur (rest path-components)
             (str current "/" (nth path-components 1 nil))
             (merge env (get-kv-values-at current))))))

;; special handling for kaniko (and other situations where there is no available translation for uid->username)
(defn expand-home [s]
  (if (and (= "?" (System/getProperty "user.home"))
           (= "0" (-> (babashka.process/sh "id" "-u") :out str/trimr)))
    (-> s
        (str/replace-first #"^~" (env :HOME))
        fs/path)
    (fs/expand-home s)))

;; given the contents of a file (as a string), write it out to a file, and return path to file
(defn env-var-2-file [content-string]
  (fs/unixify
   (if-let [bbo-header (re-find #"^#bb-ops: [^\r?\n]+" content-string)]
     (let [bbo-path (-> bbo-header
                        (subs 9)
                        str/trim
                        expand-home)
           bbo-content-string (-> (str/split content-string #"\r?\n" 2)
                                  (get 1)
                                  str)]
       (fs/create-dirs (fs/parent bbo-path))
       (-> bbo-path
           fs/create-file
           (fs/write-lines [bbo-content-string]))
       bbo-path)
     (let [_ (fs/create-dirs (fs/temp-dir))
           fs-tempfile (fs/file (fs/create-temp-file))]
       (spit fs-tempfile content-string)
       fs-tempfile))))

;; search for "*_FILE"-named env vars, write their values out to files, and replace each env var value with path to said file
(def fileified-secret-values
  (reduce-kv (fn [m k v] (assoc m k (if (re-find #".+_FILE" k)
                                      (env-var-2-file v)
                                      v)))
             {}
             merged-secret-values))

;; exec program with args with supplied; otherwise the practical result is to simply fileify out the *_FILE env vars
(if *command-line-args*
  (apply exec {:extra-env fileified-secret-values} *command-line-args*)
  "")
