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
- Prediction - what user going to do/request next? What is going to happen after it? What user **want** when he runs the application

Another important goal that we would like to achieve is to minimize code complexity: it's very difficult and error prone to implement all those rules manually. Artificial intelligence is a best candidate which can have solution for our problem.

## Tools and environment

- [ClojureScript](https://github.com/clojure/clojurescript) ~~because it's a LISP, LISP means Mccarthy, Mccarthy means AI~~ - because of good support of representing logic as data
- [React](https://facebook.github.io/react/) and [React Native](https://facebook.github.io/react-native/) as a render engine - as it has a good support of declarative UI
- [Rum](https://github.com/tonsky/rum) - as a thin and very flexible wrapper around React

## Goal

As this concept has no value on small examples, we are going to implement fully-featured mail client iOS application. The end result of this research should be:
- Proof (or not) that we can minimize overall latency by leaving behind event based paradigm
- Set of libraries that we can reuse in other projects
- Example application which uses the library

## References

Important papers:
- [Applying Goal-Oriented Action Planning to Games](http://alumni.media.mit.edu/~jorkin/GOAP_draft_AIWisdom2_2003.pdf)
- [Experience with Rules-Based Programming for Distributed, Concurrent, Fault-Tolerant Code](http://web.stanford.edu/~ouster/cgi-bin/papers/rules-atc15)
