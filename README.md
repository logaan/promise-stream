# promise-list

A promise list serves the same purpose as a blocking lazy sequence. Javascript
code may not block and so an asynchronous alternative is required.

## Example

Using promise-lists we can avoid callback hell and create functional code that
elegantly expresses our data flow, regardless of whether that data is gathered
asyncronously.

```clojure
(let [changes   (event-list ($ :#query) "change")
      keyups    (event-list ($ :#query) "keyup")
      events    (concat* changes keyups)
      queries   (mapd* (comp :value summarise) events)
      responses (map*  perform-search queries)
      groups    (mapd* group-names responses)]
  (mapd* set-query-title!  queries)
  (mapd* set-results-list! groups))
```

Here we are merging two lists that represent all change and keyup events that
will ever occur on the query input. Once merged we pull the value out of the
raw event and use it to search for groups on flickr. The responses are
transformed into lists of names which are then rendered on the page.

## Project structure

Utility scripts can be found in the `script` directory. Here you can start the
repl, or a watcher that will automatically build.

## Roadmap

* Add to the quick search example rate limits on typed characters and order
  limits on rendering of results.
* Figure out how failures propogate.
* Replace calls to jQuery deferred objects with protocol methods instead.
* Make sure that deferred objects aren't leaked. Instead expose promise
  objects. Only keep the original deferred object for open plsits.

## License

Copyright © 2013 Logan Campbell

Distributed under the Eclipse Public License, the same as Clojure.
