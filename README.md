# 0L (zero latency)

```

IBM Pollyanna Principle

(axiom) "machines should work; people should think."
	e.g. machines should do all the hard work, freeing people to think
	e.g. most of the world's major problems result from machines that fail to work, and people who fail to think.
```

Research project about bringing certain AI principles to UI and application behavior

## Rationale

We tend to think about handling UI as a `f(event) -> reaction`. Because computation is delayed until the actual event - we have a latency which may be big (IO like HTTP call), or may be small, but still noticeable (new UI window creation).

The aim of this research project is to provide framework which could be used in order to minimize latency by:

- Pre-fetch - we cannot do anything about speed of light and latency NY <> Sydney would never be less than ~70ms. But we can fetch the content **before** user has requested it
- Pre-render - it takes time to create UI elements, especially when complex forms are involved. But we can prepare as much as we could **before** user can see it
- Partial load - only load what user can see at first, load the rest later on
- Prediction - what user going to do/request next? What is going to happen after it? What user **want** when he runs the application

Another important goal that we would like to achieve is to minimize code complexity: it's very difficult and error prone to implement all those rules manually. Artificial intelligence is a best candidate which can have solution for our problem.

It's impossible to solve all those problems automatically, but what we can do is to **provide a language which can be used for actions and constraints description**


## Tools and environment

- [ClojureScript](https://github.com/clojure/clojurescript) ~~because it's a LISP, LISP means Mccarthy, Mccarthy means AI~~ - because of good support of representing logic as data
- [React](https://facebook.github.io/react/) and [React Native](https://facebook.github.io/react-native/) as a render engine - as it has a good support of declarative UI
- [Rum](https://github.com/tonsky/rum) - as a thin and very flexible wrapper around React

## Goal

As this concept has no value on small examples, we are going to implement fully-featured mail client iOS application. The end result of this research should be:
- Proof (or not) that we can minimize overall latency by leaving behind event based paradigm
- Set of libraries that we can reuse in other projects
- Example application which uses the library

## Plan

- [ ] Preparation/study
  - [ ] Common solution and approaches in AI field
    - [x] Rule based approach and expert system looks interesting
    - [x] Goal oriented action planning - can we express domain as a set of goals/actions and constraints?
    - [x] [Coeffects](http://tomasp.net/coeffects/). Looks interesting
      - [x] Dynamic scoping - doesn't make any sense for us
      - [x] `prev` keyword and the concept itself is very interesting
    - [ ] Logic programming and [core.logic](https://github.com/clojure/core.logic) looks interesting. But overall this is just depth-first search? [Prolog Programming for Artificial Intelligence](http://www.amazon.com/dp/0201403757/) probably has an answer
- [ ] Implementation
  - [ ] UI-less engine
  - [ ] UI rendering and receiving signals from it
- [ ] Proof
  - [ ] Benchmarks

## Current status

Brainstorming, you can read all previous and current investigated ideas in [thoughts](thoughts.md)

## References

Important papers:
- [Applying Goal-Oriented Action Planning to Games](http://alumni.media.mit.edu/~jorkin/GOAP_draft_AIWisdom2_2003.pdf)
- [Experience with Rules-Based Programming for Distributed, Concurrent, Fault-Tolerant Code](http://web.stanford.edu/~ouster/cgi-bin/papers/rules-atc15)
- [Computer Architecture Lecture 24: Prefetching](http://www.ece.cmu.edu/~ece740/f11/lib/exe/fetch.php%3Fmedia%3Dwiki:lectures:onur-740-fall11-lecture24-prefetching-afterlecture.pdf)

Articles:
- [Why not events](https://awelonblue.wordpress.com/2012/07/01/why-not-events/)

Books:
- [Paradigms of Artificial Intelligence Programming: Case Studies in Common Lisp](http://www.amazon.com/Paradigms-Artificial-Intelligence-Programming-Studies/dp/1558601910)

Projects:
- [Haskell](https://www.haskell.org) - for teaching me that IO has to be handled carefully
- [om-next](https://github.com/omcljs/om) - components queries is great idea
- [redux](https://github.com/reactjs/redux) - almost perfect solution, used as a starting point
- [om-next-ios-pure](https://github.com/artemyarulin/om-next-ios-pure) - previous attempt to solve the issue without actually thinking about it
