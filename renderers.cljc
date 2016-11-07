(ns )

(def app (class {:render (fn[] (c/text nil "HELLO"))}))


(js/ReactDOM.render
 (.createElement js/React "div" nil "Hello")
 (.getElementById js/document "main"))


(
(def renderer-console (fn[state event!]
                        (println "Current:" state)
                        (when (< (:b state) 10)
                          (future
                            (Thread/sleep 1000)
                            (println "Want more!")
                            (event! [:a] [:a] inc)))))

(core/register-component

(def renderer-react
  (fn[state event!]
    (js/ReactDOM.render
     (.createElement react "Text" nil "Hello")
     (js/document.getElementById "main"))))

(def renderer-react-native
    (js/ReactDOM.render
     (js/React.createElement "input" #js {:type "button"
                                          :value (:b state)
                                          :onClick #(event! [:a] [:a] inc)} nil)
     (js/document.getElementById "main"))))

(def engine (z/engine [[[:a][:b] inc]]
                      {:a 1}
                      {}
                      renderer-react))

(z/start-engine! engine)

(z/apply-event! engine [:a] [:a] inc)
