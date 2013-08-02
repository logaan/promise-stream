#!/usr/bin/env node

var http = require('http'),
    fs   = require('fs'),
    url  = require('url'),
    qs   = require('querystring'),
    exec = require('child_process').exec;

http.createServer(function (req, res) {

  switch(url.parse(req.url, true).pathname) {
    case "/":
      res.writeHead(200, {'Content-Type': 'text/html'});
      fs.readFile('resources/public/index.html', function(err, data) {
        if (err) throw err;
        res.end(data);
      });
      break;

    case "/slow":
      setTimeout(function() {
        res.writeHead(200, {'Content-Type': 'text/plain'});
        res.end('slow');
      }, 1000);
      break;

    case "/fast":
      res.writeHead(200, {'Content-Type': 'text/plain'});
      res.end('fast');
      break;

    case "/dictionary":
      var query = url.parse(req.url, true).query.search;
      var wordsFile = "/usr/share/dict/words";

      exec("grep '" + query + "' " + wordsFile, function (error, stdout) {
        res.writeHead(200, {'Content-Type': 'text/plain'});
        res.end(stdout);
      });

      break;

    default:
      fs.readFile('resources/public' + req.url, function(err, data) {
        if (err) console.log(err);
        res.writeHead(200, {'Content-Type': 'application/javascript'});
        res.end(data);
      });
      break;

  }
}).listen(1337, '127.0.0.1');

console.log('Server running at http://127.0.0.1:1337/');
