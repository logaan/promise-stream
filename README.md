# promise-list

A promise list serves the same purpose as a blocking lazy sequence. Javascript
code may not block and so an asynchronous alternative is required.

## Usage

Accessing the second item from a pre-populated dlist.

```clojure
(jayq.core/done (first (rest (dlist 1 2 3)))
  (fn [v] (assert (= 2 v)))
```

Using productive-dlist we can write to the tail of a dlist. Code that operates
on a value can be registered before that value has been added to the sequence.

```clojure
(let [writer (productive-dlist)
      reader (deref writer)]
  (jayq.core/done (first reader) (fn [v] (assert (= 1 v))))
  (produce writer 1))
```

## License

Copyright © 2013 Logan Campbell

Distributed under the Eclipse Public License, the same as Clojure.
