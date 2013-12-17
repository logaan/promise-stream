# promise-stream

_"What happens when an unblockable force meets an immutable object?" - Lyndon
Maydwell_

A promise stream serves the same purpose as a blocking lazy sequence. Javascript
code may not block and so an asynchronous alternative is required.

## Example

Using promise-streams we can avoid callback hell and create functional code that
elegantly expresses our data flow, regardless of whether that data is gathered
asyncronously.

```clojure
(let [changes   (event-stream ($ :#query) "change")
      keyups    (event-stream ($ :#query) "keyup")
      events    (concat* changes keyups)
      queries   (mapd* (comp :value summarise) events)
      responses (map*  perform-search queries)
      groups    (mapd* group-names responses)]
  (mapd* set-query-title!  queries)
  (mapd* set-results-stream! groups))
```

Here we are merging two streams that represent all change and keyup events that
will ever occur on the query input. Once merged we pull the value out of the
raw event and use it to search for groups on flickr. The responses are
transformed into streams of names which are then rendered on the page.

## Project structure

Utility scripts can be found in the `script` directory. Here you can start the
repl, or a watcher that will automatically build.

## Roadmap

* Wrap pcells in some protocols and have everything hit them through those.
* Switch from jayq deferreds to google closure deferreds.
* Setup propper failure propogation.
* Look into making the sequences more lazy with some consumer producer
  coordination.

## License

Copyright © 2013 Logan Campbell

Distributed under the Eclipse Public License, the same as Clojure.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/logaan/promise-stream/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

