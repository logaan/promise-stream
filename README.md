# promise-list

A promise list serves the same purpose as a blocking lazy sequence. Javascript
code may not block and so an asynchronous alternative is required.

## Usage

Accessing the second item from a pre-populated dlist.

```clojure
(jq/done (dc/first (dc/rest (dlist 1 2 3))) (fn [f]
  (test 2 f)))
```

Using productive-dlist we can write to the tail of a dlist. Code that operates
on a value can be registered before that value has been added to the sequence.

```clojure
(let [writer (productive-dlist)
      reader (deref writer)]
  (jq/done (dc/first reader) (fn [v] (test 1 v)))
  (produce writer 1))
```

## License

Copyright © 2013 Logan Campbell

Distributed under the Eclipse Public License, the same as Clojure.
