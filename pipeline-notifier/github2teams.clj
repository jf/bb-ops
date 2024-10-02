#!/usr/bin/env bb

(require '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[hiccup2.core :as h])

(defn env [v]
  (System/getenv (str/upper-case (name v))))

#_(def MESSAGE_PRE
  (if-let [access-token (env :PN__GITLAB_ACCESS_TOKEN)]
    (-> (str "https://gitlab.com/api/v4/projects/" (env :CI_PROJECT_ID) "/repository/commits/" (env :CI_COMMIT_SHA))
        (http/get {:headers {"PRIVATE-TOKEN" access-token}})
        (:body)
        (json/parse-string)
        (get "message")
        (str/trimr))))

(def html-notification-string
  (let [triggering_actor (env :GITHUB_TRIGGERING_ACTOR)
        author (env :GITHUB_ACTOR)
        callout (if (env :PN__PIPELINE_PASSED)
                  [:span {:style "background-color: green; color: white; padding: 4px; font-weight: bold"} "PASSED:"]
                  [:span {:style "background-color: red;   color: white; padding: 4px; font-weight: bold"} "FAILED:"])

        project-trim-chars (env :PN__PROJECT_TRIM_CHARS)
        project-name (if project-trim-chars
                       (str/replace-first (env :GITHUB_REPOSITORY) (re-pattern project-trim-chars) "")
                       (env :GITHUB_REPOSITORY))
        repo-url (str "https://" (env :GITHUB_REPOSITORY))]
    (str
     (h/html
      [:h1 callout
       " "
       [:a {:href (str repo-url "/tree/" (env :GITHUB_REF_NAME))}
        [:span {:style "background-color: blue; color: white"}
         [:em (env :GITHUB_REF_NAME)]]]
       " "
       [:a {:href repo-url}
        [:strong project-name]]]
      " by "
      [:a {:href (str "https://github.com/" author)}
       [:strong author]]
      ": commit "
      [:a {:href (str repo-url "/commit/" (env :GITHUB_SHA))} (env :GITHUB_SHA)]
      [:br]
      " pipeline "
      [:a {:href (str repo-url "/actions/runs/" (env :GITHUB_RUN_ID)} (env :GITHUB_RUN_ID)]
      [:br]
      [:br]
      #_[:pre MESSAGE_PRE]))))

(let [webhook-url (env :PN__TEAMS_WEBHOOK_URL)]
  (http/post webhook-url
             {:headers {"Content-Type" "application/json"}
              :body (json/generate-string {:text html-notification-string})}))
