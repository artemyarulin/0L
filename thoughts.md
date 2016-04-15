#### 2016-04-15 Fri

- We have to represent state of the world as data, so program can process it. Good candidate could be global atom with:
``` clojure
{:data {} ;; Normalized data
 :ui {}   ;; Current UI tree
 :io {}   ;; All requested/in-progress/finished IO
 }
 ```

 - Idea of representing all IO as a data is a good one. Batching/Throttling/Priorities/Retry could be easily represented in a declarative style
 - Stateless UI components which can change only their own state. Framework would subscribe to state change and propagate it to the engine (https://awelonblue.wordpress.com/2012/07/01/why-not-events/) Meaning we have a component with a button:

``` clojure
;; t1
{:component {:button {:pressed false}}}
;; t2
{:component {:button {:pressed true}}}
;; t3
{:component {:button {:pressed false}}}

;; Rule using core.match
[{:component {:button {:pressed true}}}] 'login
```
Problem that we will cause a reaction when button is pressed, not when it was released. Solution (which would solve many other things) is to provide previous states as well. Rule is:
```
ButtonPressedState has changed from false to true
```
This concept allow to do a lot of interesting and powerful staff.
