#!/usr/bin/env bb

(require '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[hiccup2.core :as h])

;;; List of environment variables used by this script:
;;;
;;; - PN__AUTHOR_STYLE
;;; - PN__PROJECT_TRIM_REGEX
;;;
;;; - TEAMS_WEBHOOK_URL
;;;
;;; - CI_COMMIT_AUTHOR
;;; - CI_COMMIT_BRANCH
;;; - CI_COMMIT_MESSAGE
;;; - CI_COMMIT_SHA
;;; - CI_COMMIT_SHORT_SHA
;;;
;;; - CI_PIPELINE_ID
;;; - CI_PIPELINE_PASSED
;;; - CI_PIPELINE_URL
;;;
;;; - CI_PROJECT_ID
;;; - CI_PROJECT_PATH
;;; - CI_PROJECT_URL
;;;
;;; - GITLAB_TOKEN
;;; - GITLAB_USER_LOGIN

(defn env [v]
  (System/getenv (str/upper-case (name v))))

(def MESSAGE_PRE
  (-> (str "https://gitlab.com/api/v4/projects/" (env :CI_PROJECT_ID) "/repository/commits/" (env :CI_COMMIT_SHA))
      (http/get {:headers {"PRIVATE-TOKEN" (env :GITLAB_TOKEN)}})
      (:body)
      (json/parse-string)
      (get "message")
      (str/trimr)))

(def html-notification-string
  (let [author (case (env :PIPELINE_NOTIFIER_AUTHOR_STYLE)
                 "email"      (env :GITLAB_USER_EMAIL)
                 "name"       (env :GITLAB_USER_NAME)
                 "name_email" (env :CI_COMMIT_AUTHOR)
                 "username"   (env :GITLAB_USER_LOGIN)
                 (env :CI_COMMIT_AUTHOR))
        author (case (env :PN__AUTHOR_STYLE)
                 "email"      (env :GITLAB_USER_EMAIL)
                 "name"       (env :GITLAB_USER_NAME)
                 "name_email" (env :CI_COMMIT_AUTHOR)
                 "username"   (env :GITLAB_USER_LOGIN)
                 author)
        callout (if (env :CI_PIPELINE_PASSED)
                  [:span {:style "background-color: green; color: white; padding: 4px; font-weight: bold"} "PASSED:"]
                  [:span {:style "background-color: red;   color: white; padding: 4px; font-weight: bold"} "FAILED:"])

        project-trim-regex (env :PN__PROJECT_TRIM_REGEX)
        project-path (if project-trim-regex
                       (str/replace-first (env :CI_PROJECT_PATH) (re-pattern project-trim-regex) "")
                       (env :CI_PROJECT_PATH))]
    (str
     (h/html
      [:h1 callout
       " "
       [:a {:href (str (env :CI_PROJECT_URL) "/-/commits/" (env :CI_COMMIT_BRANCH))}
        [:span {:style "background-color: blue; color: white"}
         [:em (env :CI_COMMIT_BRANCH)]]]
       " "
       [:a {:href (env :CI_PROJECT_URL)}
        [:strong project-path]]]
      " by "
      [:a {:href (str "https://gitlab.com/" (env :GITLAB_USER_LOGIN))}
       [:strong author]]
      ": commit "
      [:a {:href (str (env :CI_PROJECT_URL) "/-/commit/" (env :CI_COMMIT_SHA))} (env :CI_COMMIT_SHORT_SHA)]
      [:br]
      " pipeline "
      [:a {:href (env :CI_PIPELINE_URL)} (env :CI_PIPELINE_ID)]
      [:br]
      [:br]
      [:pre MESSAGE_PRE]))))

(http/post (env :TEAMS_WEBHOOK_URL)
           {:headers {"Content-Type" "application/json"}
            :body (json/generate-string {:text html-notification-string})})
