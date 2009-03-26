
dojo.provide("dojo.docs");
dojo.require("dojo.io.*");
dojo.require("dojo.event.topic");
dojo.require("dojo.rpc.JotService");
dojo.require("dojo.dom");
dojo.require("dojo.uri.Uri");
dojo.require("dojo.Deferred");
dojo.require("dojo.DeferredList");
dojo.docs = new function() {this._url = dojo.uri.dojoUri("docscripts");
this._rpc = new dojo.rpc.JotService;
this._rpc.serviceUrl = dojo.uri.dojoUri("docscripts/jsonrpc.php");};
dojo.lang.mixin(dojo.docs, {_count: 0,
_callbacks: {function_names: []},
_cache: {},
require: function( require,  sync) {dojo.debug("require(): " + require);
var parts = require.split("/");
var size = parts.length;
var deferred = new dojo.Deferred;
var args = {mimetype: "text/json",
load: function(type, data){dojo.debug("require(): loaded for " + require);
if(parts[0] != "function_names") {for(var i = 0, part; part = parts[i]; i++){data = data[part];}}
deferred.callback(data);},
error: function(){deferred.errback();}};
if(location.protocol == "file:"){if(size){if(parts[parts.length - 1] == "documentation"){parts[parts.length - 1] = "meta";}
if(parts[0] == "function_names"){args.url = [this._url, "local_json", "function_names"].join("/");}else{var dirs = parts[0].split(".");
args.url = [this._url, "local_json", dirs[0]].join("/");
if(dirs.length > 1){args.url = [args.url, dirs[1]].join(".");}}}}
dojo.io.bind(args);
return deferred;},
getFunctionNames: function(){return this.require("function_names");},
unFormat: function( string){var fString = string;
if(string.charAt(string.length - 1) == "_"){fString = [string.substring(0, string.length - 1), "*"].join("");}
return fString;},
getMeta: function( pkg,  name,  callback,  id){if(typeof name == "function"){id = callback;
callback = name;
name = pkg;
pkg = null;
dojo.debug("getMeta(" + name + ")");}else{dojo.debug("getMeta(" + pkg + "/" + name + ")");}
if(!id){id = "_";}},
_withPkg: function( type,  data,  evt,  input,  newType){dojo.debug("_withPkg(" + evt.name + ") has package: " + data[0]);
evt.pkg = data[0];
if("load" == type && evt.pkg){evt.type = newType;}else{if(evt.callbacks && evt.callbacks.length){evt.callbacks.shift()("error", {}, evt, evt.input);}}},
_gotMeta: function( type,  data,  evt){dojo.debug("_gotMeta(" + evt.name + ")");
var cached = dojo.docs._getCache(evt.pkg, evt.name, "meta", "functions", evt.id);
if(cached.summary){data.summary = cached.summary;}
if(evt.callbacks && evt.callbacks.length){evt.callbacks.shift()(type, data, evt, evt.input);}},
getSrc: function( name,  callback,  id){dojo.debug("getSrc(" + name + ")");
if(!id){id = "_";}},
getDoc: function( name,  callback,  id){dojo.debug("getDoc(" + name  + ")");
if(!id){id = "_";}
var input = {};
input.type = "doc";
input.name = name;
input.callbacks = [callback];},
_gotDoc: function( type,  data,  evt,  input){dojo.debug("_gotDoc(" + evt.type + ")");
evt[evt.type] = data;
if(evt.expects && evt.expects.doc){for(var i = 0, expect; expect = evt.expects.doc[i]; i++){if(!(expect in evt)){dojo.debug("_gotDoc() waiting for more data");
return;}}}
var cache = dojo.docs._getCache(evt.pkg, "meta", "functions", evt.name, evt.id, "meta");
var description = evt.fn.description;
cache.description = description;
data = {returns: evt.fn.returns,
id: evt.id,
variables: []}
if(!cache.parameters){cache.parameters = {};}
for(var i = 0, param; param = evt.param[i]; i++){var fName = param["DocParamForm/name"];
if(!cache.parameters[fName]){cache.parameters[fName] = {};}
cache.parameters[fName].description = param["DocParamForm/desc"]}
data.description = cache.description;
data.parameters = cache.parameters;
evt.type = "doc";
if(evt.callbacks && evt.callbacks.length){evt.callbacks.shift()("load", data, evt, input);}},
getPkgDoc: function( name,  callback){dojo.debug("getPkgDoc(" + name + ")");
var input = {};},
getPkgInfo: function( name,  callback){dojo.debug("getPkgInfo(" + name + ")");
var input = {expects: {pkginfo: ["pkgmeta", "pkgdoc"]},
callback: callback};
dojo.docs.getPkgMeta(input, name, dojo.docs._getPkgInfo);
dojo.docs.getPkgDoc(input, name, dojo.docs._getPkgInfo);},
_getPkgInfo: function( type,  data,  evt){dojo.debug("_getPkgInfo() for " + evt.type);
var input = {};
var results = {};
if(typeof key == "object"){input = key;
input[evt.type] = data;
if(input.expects && input.expects.pkginfo){for(var i = 0, expect; expect = input.expects.pkginfo[i]; i++){if(!(expect in input)){dojo.debug("_getPkgInfo() waiting for more data");
return;}}}
results = input.pkgmeta;
results.description = input.pkgdoc;}
if(input.callback){input.callback("load", results, evt);}},
getInfo: function( name,  callback){dojo.debug("getInfo(" + name + ")");
var input = {expects: {"info": ["meta", "doc"]},
callback: callback}
dojo.docs.getMeta(input, name, dojo.docs._getInfo);
dojo.docs.getDoc(input, name, dojo.docs._getInfo);},
_getInfo: function( type,  data,  evt,  input){dojo.debug("_getInfo(" + evt.type + ")");
if(input && input.expects && input.expects.info){input[evt.type] = data;
for(var i = 0, expect; expect = input.expects.info[i]; i++){if(!(expect in input)){dojo.debug("_getInfo() waiting for more data");
return;}}}
if(input.callback){input.callback("load", dojo.docs._getCache(evt.pkg, "meta", "functions", evt.name, evt.id, "meta"), evt, input);}},
_getMainText: function( text){dojo.debug("_getMainText()");
return text.replace(/^<html[^<]*>/, "").replace(/<\/html>$/, "").replace(/<\w+\s*\/>/g, "");},
getPackageMeta: function( input){dojo.debug("getPackageMeta(): " + input.package);
return this.require(input.package + "/meta", input.sync);},
getFunctionMeta: function( input){var package = input.package || "";
var name = input.name;
var id = input.id || "_";
dojo.debug("getFunctionMeta(): " + name);
if(!name) return;
if(package){return this.require(package + "/meta/functions/" + name + "/" + id + "/meta");}else{this.getFunctionNames();}},
getFunctionDocumentation: function( input){var package = input.package || "";
var name = input.name;
var id = input.id || "_";
dojo.debug("getFunctionDocumentation(): " + name);
if(!name) return;
if(package){return this.require(package + "/meta/functions/" + name + "/" + id + "/documentation");}},
_onDocSearch: function( input){var _this = this;
var name = input.name.toLowerCase();
if(!name) return;
this.getFunctionNames().addCallback(function(data){dojo.debug("_onDocSearch(): function names loaded for " + name);
var output = [];
var list = [];
var closure = function(pkg, fn) {return function(data){dojo.debug("_onDocSearch(): package meta loaded for: " + pkg);
if(data.functions){var functions = data.functions;
for(var key in functions){if(fn == key){var ids = functions[key];
for(var id in ids){var fnMeta = ids[id];
output.push({package: pkg,
name: fn,
id: id,
summary: fnMeta.summary});}}}}
return output;}}
pkgLoop:
for(var pkg in data){if(pkg.toLowerCase() == name){name = pkg;
dojo.debug("_onDocSearch found a package");
return;}
for(var i = 0, fn; fn = data[pkg][i]; i++){if(fn.toLowerCase().indexOf(name) != -1){dojo.debug("_onDocSearch(): Search matched " + fn);
var meta = _this.getPackageMeta({package: pkg});
meta.addCallback(closure(pkg, fn));
list.push(meta);
continue pkgLoop;}}}
list = new dojo.DeferredList(list);
list.addCallback(function(results){dojo.debug("_onDocSearch(): All packages loaded");
_this._printFunctionResults(results[0][1]);});});},
_onDocSearchFn: function( type,  data,  evt){dojo.debug("_onDocSearchFn(" + evt.name + ")");
var name = evt.name || evt.pkg;
dojo.debug("_onDocSearchFn found a function");
evt.pkgs = packages;
evt.pkg = name;
evt.loaded = 0;
for(var i = 0, pkg; pkg = packages[i]; i++){dojo.docs.getPkgMeta(evt, pkg, dojo.docs._onDocResults);}},
_onPkgResults: function( type,  data,  evt,  input){dojo.debug("_onPkgResults(" + evt.type + ")");
var description = "";
var path = "";
var methods = {};
var requires = {};
if(input){input[evt.type] = data;
if(input.expects && input.expects.pkgresults){for(var i = 0, expect; expect = input.expects.pkgresults[i]; i++){if(!(expect in input)){dojo.debug("_onPkgResults() waiting for more data");
return;}}}
path = input.pkgdoc.path;
description = input.pkgdoc.description;
methods = input.pkgmeta.methods;
requires = input.pkgmeta.requires;}
var pkg = evt.name.replace("_", "*");
var results = {path: path,
description: description,
size: 0,
methods: [],
pkg: pkg,
requires: requires}
var rePrivate = /_[^.]+$/;
for(var method in methods){if(!rePrivate.test(method)){for(var pId in methods[method]){results.methods.push({pkg: pkg,
name: method,
id: pId,
summary: methods[method][pId].summary})}}}
results.size = results.methods.length;
dojo.docs._printPkgResult(results);},
_onDocResults: function( type,  data,  evt,  input){dojo.debug("_onDocResults(" + evt.name + "/" + input.pkg + ") " + type);
++input.loaded;
if(input.loaded == input.pkgs.length){var pkgs = input.pkgs;
var name = input.pkg;
var results = {methods: []};
var rePrivate = /_[^.]+$/;
data = dojo.docs._cache;
for(var i = 0, pkg; pkg = pkgs[i]; i++){var methods = dojo.docs._getCache(pkg, "meta", "methods");
for(var fn in methods){if(fn.toLowerCase().indexOf(name) == -1){continue;}
if(fn != "requires" && !rePrivate.test(fn)){for(var pId in methods[fn]){var result = {pkg: pkg,
name: fn,
id: "_",
summary: ""}
if(methods[fn][pId].summary){result.summary = methods[fn][pId].summary;}
results.methods.push(result);}}}}
dojo.debug("Publishing docResults");
dojo.docs._printFnResults(results);}},
_printFunctionResults: function(results){dojo.debug("_printFnResults(): called");},
_printPkgResult: function(results){dojo.debug("_printPkgResult(): called");},
_onDocSelectFunction: function( input){var name = input.name;
var package = input.package || "";
var id = input.id || "_";
dojo.debug("_onDocSelectFunction(" + name + ")");
if(!name || !package) return false;
var pkgMeta = this.getPackageMeta({package: package});
var meta = this.getFunctionMeta({package: package, name: name, id: id});
var doc = this.getFunctionDocumentation({package: package, name: name, id: id});
var list = new dojo.DeferredList([pkgMeta, meta, doc]);
list.addCallback(function(results){dojo.debug("_onDocSelectFunction() loaded");
for(var i = 0, result; result = results[i]; i++){dojo.debugShallow(result[1]);}});
return list;},
_onDocSelectPackage: function( input){dojo.debug("_onDocSelectPackage(" + input.name + ")")
input.expects = {"pkgresults": ["pkgmeta", "pkgdoc"]};
dojo.docs.getPkgMeta(input, input.name, dojo.docs._onPkgResults);
dojo.docs.getPkgDoc(input, input.name, dojo.docs._onPkgResults);},
_onDocSelectResults: function( type,  data,  evt,  input){dojo.debug("_onDocSelectResults(" + evt.type + ", " + evt.name + ")");
if(evt.type == "meta"){dojo.docs.getPkgMeta(input, evt.pkg, dojo.docs._onDocSelectResults);}
if(input){input[evt.type] = data;
if(input.expects && input.expects.docresults){for(var i = 0, expect; expect = input.expects.docresults[i]; i++){if(!(expect in input)){dojo.debug("_onDocSelectResults() waiting for more data");
return;}}}}
dojo.docs._printFunctionDetail(input);},
_printFunctionDetail: function(results) {},
selectFunction: function( name,  id){},
savePackage: function( callbackObject,  callback,  parameters){dojo.event.kwConnect({srcObj: dojo.docs,
srcFunc: "_savedPkgRpc",
targetObj: callbackObject,
targetFunc: callback,
once: true});
var props = {};
var cache = dojo.docs._getCache(parameters.pkg, "meta");
var i = 1;
if(!cache.path){var path = "id";
props[["pname", i].join("")] = "DocPkgForm/require";
props[["pvalue", i++].join("")] = parameters.pkg;}else{var path = cache.path;}
props.form = "//DocPkgForm";
props.path = ["/WikiHome/DojoDotDoc/", path].join("");
if(parameters.description){props[["pname", i].join("")] = "main/text";
props[["pvalue", i++].join("")] = parameters.description;}
dojo.docs._rpc.callRemote("saveForm",	props).addCallbacks(dojo.docs._pkgRpc, dojo.docs._pkgRpc);},
_pkgRpc: function(data){if(data.name){dojo.docs._getCache(data["DocPkgForm/require"], "meta").path = data.name;
dojo.docs._savedPkgRpc("load");}else{dojo.docs._savedPkgRpc("error");}},
_savedPkgRpc: function(type){},
functionPackages: function( name,  callback,  input){dojo.debug("functionPackages() name: " + name);
if(!input){input = {};}
if(!input.callbacks){input.callbacks = [];}
input.type = "function_names";
input.name = name;
input.callbacks.unshift(callback);
input.callbacks.unshift(dojo.docs._functionPackages);},
_functionPackages: function( type,  data,  evt){dojo.debug("_functionPackages() name: " + evt.name);
evt.pkg = '';
var results = [];
var data = dojo.docs._cache['function_names'];
for(var key in data){if(dojo.lang.inArray(data[key], evt.name)){dojo.debug("_functionPackages() package: " + key);
results.push(key);}}
if(evt.callbacks && evt.callbacks.length){evt.callbacks.shift()(type, results, evt, evt.input);}},
setUserName: function( name){dojo.docs._userName = name;
if(name && dojo.docs._password){dojo.docs._logIn();}},
setPassword: function( password){dojo.docs._password = password;
if(password && dojo.docs._userName){dojo.docs._logIn();}},
_logIn: function(){dojo.io.bind({url: dojo.docs._rpc.serviceUrl.toString(),
method: "post",
mimetype: "text/json",
content: {username: dojo.docs._userName,
password: dojo.docs._password},
load: function(type, data){if(data.error){dojo.docs.logInSuccess();}else{dojo.docs.logInFailure();}},
error: function(){dojo.docs.logInFailure();}});},
logInSuccess: function(){},
logInFailure: function(){},
_set: function( base,  keys,  value){var args = [];
for(var i = 0, arg; arg = arguments[i]; i++){args.push(arg);}
if(args.length < 3) return;
base = args.shift();
value = args.pop();
var key = args.pop();
for(var i = 0, arg; arg = args[i]; i++){if(typeof base[arg] != "object"){base[arg] = {};}
base = base[arg];}
base[key] = value;},
_getCache: function( keys){var obj = dojo.docs._cache;
for(var i = 0; i < arguments.length; i++){var arg = arguments[i];
if(!obj[arg]){obj[arg] = {};}
obj = obj[arg];}
return obj;}});
dojo.event.topic.subscribe("/docs/search", dojo.docs, "_onDocSearch");
dojo.event.topic.subscribe("/docs/function/select", dojo.docs, "_onDocSelectFunction");
dojo.event.topic.subscribe("/docs/package/select", dojo.docs, "_onDocSelectPackage");
dojo.event.topic.registerPublisher("/docs/function/results", dojo.docs, "_printFunctionResults");
dojo.event.topic.registerPublisher("/docs/function/detail", dojo.docs, "_printFunctionDetail");
dojo.event.topic.registerPublisher("/docs/package/detail", dojo.docs, "_printPkgResult");