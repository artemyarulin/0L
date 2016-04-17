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
This concept allow to do a lot of interesting and powerful stuff.

#### 2016-04-17 Sun

- If we would provide automatic solution, then control would be lost, which means it wouldn't work in every case (most likely otherwise - it **would work** with just a few cases). General solution would be to provide a language where every case could be described. This is our goal
- Example application idea: Counter + save number on disk, where IO takes 1 second. Can we express requirements like:
  - IO is expensive and we should minimize it (throttling)
  - IO is free, can we achieve latency zero? Pre-save
- Example application idea: Order management system. Login + Order dashboard + OrderView + CustomerView. Example case: Get order #12113 -> Customer -> Phone.
- Example application idea: Graph of mouse pointer velocity:
```` F#
let speed x y =
  let dx = x - prev x in
  let dy = y - prev y in
  dx * dx + dy * dy in
speed
```
- We should split atom into three: `data`, `ui`, `io` for the performance reasons. We don't want to create whole `data` atom copy when user has scrolled and changed `ui` state
- No state, but rather **states**:
![states](images/states.png)
- Reason why we've described state of the world in general (and `io` in particular) as data because we can process it. We should use data for the rules as well, in case we would like to introduce some self-inspection (e.g. rules analyze other rules). It's an only way how to achieve prediction. We have state history so we can go back, and we have to have rules as data so we can go into feature.

##### IO prediction
As IO is a `f(constraints,required-data)` there are two types of prediction: Satisfied prediction and predicted data
![prediction_types](images/prediction_types.png)

Consider following typical IO cases:
![io-cases](images/io_cases.png)

We can cover all of them using this kind of API:
![prediction_io_api.png](images/prediction_io_api.png)
