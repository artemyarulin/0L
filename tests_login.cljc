(ns )

;; Use case login
;;
;; 1. Load accounts file
;; 2. If doesn't exist:
;;      - Show create account screen
;;      - else load previous data

;; If rule returns set or rules - it has to return a function which returns it
;;




[
 ;; If data/accounts is nil - make IO and try to read accounts file. If there is any error - return empty array
 (rules/io-init [:data :accounts] {:type :read :path "accounts"})
 ;; If accounts array is empty - load CreateAccount UI
 (rules/if-empty [:data :accounts] [:ui :create-account])
