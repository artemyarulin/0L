(ns zerol.engine)


(defn rule
  "Rule name following by query that rule depends on and a function which will be executed with query result. Rule should return a new data that will be placed back in state"
  [name query f] [rule name query f query]
  [name query f return-query])

(defn expect
  "Starting with initial state and set of state waits untill expected-state is achived or fail after timeout"
  [state rules expected-state & {:keys [timeout] :or {timeout 5}}]
  [state rules expected-state timeout])
