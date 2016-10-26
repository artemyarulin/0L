(ns zerol.proto)

(def states [])

(defn start []
  {:io {:type :disk
        :op :read
        :path "user.txt"}})

(def workflow [:start ])

;; Let's write down some use cases

;; 1 User login
;; First run - login screen
;; Second run - hello user info

;; If state is empty - try to load user.txt
;; If :user object exists - show hello user info
;; If loggin of user.txt succeed - parse it into :user object
;; If user requested authentication - start it
;; If authentication failed - show error
;; If authentication succeed - save it into user.txt and into :user object
;; If loging of user.txt failed - show login screen


[
 "01.01.02 12:23:23.1234" {:ui nil :io nil :data nil}
 "01.01.02 12:23:23.1237" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :pending}] :data nil}
 "01.01.02 12:23:23.1240" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :progress}] :data nil}
 "01.01.02 12:23:23.1250" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :succeed :result "{user:\"John\"}"}] :data nil}
 "01.01.02 12:23:23.1259" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :succeed :result "{user:\"John\"}"}] :data {:user "John"}}
 "01.01.02 12:23:23.1264" {:ui {:hello {:name "John"}} :io [] :data {:user "John"}}
 ]

(defn in-case-io-suceed [v])
(defn in-case-io-suceed-with-result [res ret])
(defn in-case-io-errors [ret])

(defn io [name io-fn on-succeed on-err]
  (hash-map (keyword (str "io-" name)) io-fn
            (keyword (str "io-" name "-succeed")) on-succeed ;;in-case-io-succeed
            (keyword (str "io-" name "-failed")) on-err))    ;;in-case-io-errored




[(merge (io "user"
            (fn[ui io data](when (nil? data) {:io (io-user)}))
            #(hash-map :user (data-user %))
            {:ui {:login {}}}
        (io "auth"
            (fn[ui io data](when-let [{:keys [user pass mode-clicked] (-> ui :login)}] (when mode-clicked {:io (io-auth user pass)})))
            #(when (= % "OK") {:user (-> ui :login :user)})
            {:ui {:login {:mode-auth-failed true}}}
        {:ui-user (fn[ui io data](when-let [user (-> data :user)] {:ui (ui-hello user)}))


(defn io-user []
  {:type :disk
   :id :user
   :op {:method :read
        :path (get-absolute-path "user.txt")}})

(defn io-auth [user pwd]
  {:type :http
   :id :auth
   :op {:method "GET"
        :address "http://example.com"
        :headers {:user-agent "IE"}}})

(defn data-user [res]
  (.parse js/JSON res))

(defn ui-hello [name]
  {:hello {:name name}})

[:io-user
 :data-user [:io [
 :ui-hello [:data :user] #(hash-map :ui {:hello {:name %}})


[
 "01.01.02 12:23:23.1234" {:ui nil :io nil :data nil}
 "01.01.02 12:23:23.1237" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :pending}] :data nil}
 "01.01.02 12:23:23.1240" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :progress}] :data nil}
 "01.01.02 12:23:23.1250" {:ui nil :io [{:type :disk :op :read :path "user.txt" :status :error {:type :no-file :p1 :v1 :p2 :v2}}] :data nil}
 "01.01.02 12:23:23.1264" {:ui {:login nil} :io [{:type :disk :op :read :path "user.txt" :status :error {:type :no-file :p1 :v1 :p2 :v2}}] :data nil}
 "01.01.02 12:23:23.1264" {:ui {:login {:user "John" :pass "John" :mode-clicked true} :io [{:type :disk :op :read :path "user.txt" :status :error {:type :no-file :p1 :v1 :p2 :v2}}] :data nil}
                           "01.01.02 12:23:23.1264" {:ui {:login {:user "John" :pass "John" :mode-clicked true}}
                                                     :io [{:type :disk :op {:path "user.txt"} :status :error :value {:type :no-file :p1 :v1 :p2 :v2}}
                                                          {:type :http :op {:method :GET :url "http://example.com"} :status :pending}]
                                                     :data nil}
                           "01.01.02 12:23:23.1264" {:ui {:login {:user "John" :pass "John" :mode-clicked true}}
                                                     :io [{:type :disk :op {:path "user.txt"} :status :error :value {:type :no-file :p1 :v1 :p2 :v2}}
                                                          {:type :http :op {:method :GET :url "http://example.com"} :status :progress}]
                                                     :data nil}
                           "01.01.02 12:23:23.1264" {:ui {:login {:user "John" :pass "John" :mode-clicked true}}
                                                     :io [{:type :disk :op {:path "user.txt"} :status :error :value {:type :no-file :p1 :v1 :p2 :v2}}
                                                          {:type :http :op {:method :GET :url "http://example.com"} :status :succeed :value {:code 200
                                                                                                                                             :body ""
                                                                                                                                             :headers {}}}]
                                                     :data nil}
                           "01.01.02 12:23:23.1264" {:ui {:login {:user "John" :pass "John" :mode-clicked true}}
                                                     :io [{:type :disk :op {:path "user.txt"} :status :error :value {:type :no-file :p1 :v1 :p2 :v2}}
                                                          {:type :http :op {:method :GET :url "http://example.com"} :status :succeed :value {:code 200
                                                                                                                                             :body ""
                                                                                                                                             :headers {}}}]
                                                     :data {:user "John"}}
                           "01.01.02 12:23:23.1264" {:ui {:hello {:name "John"}}
                                                     :io [{:type :disk :op {:path "user.txt"} :status :error :value {:type :no-file :p1 :v1 :p2 :v2}}
                                                          {:type :http :op {:method :GET :url "http://example.com"} :status :succeed :value {:code 200
                                                                                                                                             :body ""
                                                                                                                                             :headers {}}}]
                                                     :data {:user "John"}}
 ]


 ;; Kapteko Start

 [(merge (io "prev-session"
             (on-init {:io (load-prev-session)})
             parse-session
             {:ui {:login {}}})
         (io "auth"
             (fn[ui io data](let [{:keys [user pass mode-clicked] (-> ui :login)}] (when mode-clicked {:io (io-auth user pass)})))
             #(when (= % "OK") {:data {:user (-> ui :login :user)}})
             {:ui {:login {:mode-auth-failed true}}}))]


 (def state [{:data nil :ui nil :io nil}])
 (def zerol [rules io])

 (def rules [])
 (def io {:disk (fn[{:keys [path op value]} cb]
                  (cb {:status :ok :value nil}))})

 (def app (zerol rules io))

 (defn expect [state app query val])


 (deftest first-run-opens-account-screen
   (expect nil rules io
           [:ui :login] {:user "" :pass ""}))

 (deftest run-with-account-opens-mail-screen
   (let [profile {:id "a"}]
     (expect nil rules {:disk (constantly (str profile))}
             [:ui :mail :messages] []
             [:data :accounts] [profile])))

 (def ews-acc {:id 1
               :email "support@kapteko.com"
               :url "https://outlook.office365.com/EWS/Exchange.asmx"
               :pass "1832719381"
               :provider :ews})

 (deftest check-email
   (let [state {:data {:accounts [ews-acc]}}]
     (expect state rules io
             [:ui :mail :messages] #(and (< 0 (count %)) (->> % first (every-pred :from :subject))))))

 (deftest new-message
   (let [token (str "new-message" (rand-int 100000))
         state {:data {:accounts [ews-acc]}
                :ui {:mail {:new-message {:to (:email ews-acc)
                                          :subject token
                                          :body token
                                          :mode-send true}}}}]
     (expect state rules io
             [:ui :mail :messages] (fn[msgs](->> msgs first :subject (#(= % token)))))))

 {:ui react-native-ui
  :io react-native-io}
