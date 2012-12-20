;; ## PN Indexer
;; The indexer is responsible for generating most of the Papyrological Navigator.
;; It loads data into the Solr indexes (for data, bibliography, and lemmas), and generates
;; HTML and text representations of the data. When compiled into a JAR, it exposes some
;; static methods which allow its functions to be called from Java code.
;;
;; Most of the work in PN Indexer is done by queueing up (via a query on the Numbers Server) 
;; first the DDbDP data with any matching HGV and APIS records, then HGV data with any matching 
;; APIS records but without any matching DDbDP record, then APIS without any DDbDP or HGV 
;; matches. This queue of URIs is then processed. URIs are resolved to filenames, and those XML
;; files are transformed using XSLT into Solr add documents, HTML files, and text files.
;;
;; ### Usage
;; (from Leiningen)
;;
;; * run without arguments — builds the PN index (pn-search) and HTML/txt pages for texts and data
;; * run with a list of files — indexes and generates HTML/txt for just the files provided
;; * `biblio` — builds the PN index for bibliography (biblio-search)
;; * `load-lemmas` — loads the morphological data from the Perseus lemma db into the lemma index (morph-search)
;;
;; (from Java)
;; 
;; * `info.papyri.indexer.index(List<String> files)` (files can be null)
;; * `info.papyri.indexer.loadBiblio()`
;; * `info.papyri.indexer.loadLemmas()`

(ns info.papyri.indexer
  (:gen-class
   :name info.papyri.indexer
   :methods [#^{:static true} [index [java.util.List] void]
             #^{:static true} [loadBiblio [] void]
             #^{:static true} [loadLemmas [] void]])
  (:import
    (clojure.lang ISeq)
    (com.hp.hpl.jena.rdf.model Model ModelFactory Resource ResourceFactory)
    (java.io File FileInputStream FileOutputStream FileReader StringWriter FileWriter)
    (java.net URI URL URLEncoder URLDecoder)
    (java.nio.charset Charset)
    (java.text Normalizer Normalizer$Form)
    (java.util ArrayList TreeMap)
    (java.util.concurrent Executors ConcurrentLinkedQueue ConcurrentSkipListSet)
    (javax.xml.parsers SAXParserFactory)
    (javax.xml.transform Result )
    (javax.xml.transform.sax SAXResult)
    (javax.xml.transform.stream StreamSource StreamResult)
    (net.sf.saxon Configuration FeatureKeys PreparedStylesheet StandardErrorListener StandardURIResolver TransformerFactoryImpl)
    (net.sf.saxon.trans CompilerInfo XPathException)
    (org.apache.solr.client.solrj SolrServer SolrQuery)
    (org.apache.solr.client.solrj.impl CommonsHttpSolrServer StreamingUpdateSolrServer BinaryRequestWriter)
    (org.apache.solr.client.solrj.request RequestWriter)
    (org.apache.solr.common SolrInputDocument)
    (com.hp.hpl.jena.query QueryExecutionFactory)
    (org.xml.sax InputSource)
    (org.xml.sax.helpers DefaultHandler)))
      
;; NOTE: hard-coded paths and addresses
(def filepath "/data/papyri.info/idp.data")
(def xsltpath "/data/papyri.info/git/navigator/pn-xslt")
(def htpath "/data/papyri.info/pn/idp.html")
(def solrurl "http://localhost:8083/solr/")
(def numbersurl "http://localhost:8090/pi/query?query=")
(def nthreads (.availableProcessors (Runtime/getRuntime)))
(def server "http://localhost:8090/pi")
(def nserver "localhost")
(def collections (ref (ConcurrentLinkedQueue.)))
(def htmltemplates (ref nil))
(def html (ref (ConcurrentLinkedQueue.)))
(def solrtemplates (ref nil))
(def text (ref (ConcurrentLinkedQueue.)))
(def texttemplates (ref nil))
(def bibsolrtemplates (ref nil))
(def bibhtmltemplates (ref nil))
(def links (ref (ConcurrentLinkedQueue.)))
(def words (ref (ConcurrentSkipListSet.)))
(def solr (ref nil))
(def solrbiblio (ref nil))
(def ^:dynamic *current*)
(def ^:dynamic *doc*)
(def ^:dynamic *index*)

(defn add-words 
  "Adds the list of words provided to the set in the ref `words`."
  [words]
  (let [word-arr (.split words "\\s+")]
    (for [word word-arr]
      (.add @words word))))

(defn dochandler 
  "A document handler for Solr that handles the conversion of a SOLR XML
  document into a Java object representing an add document and then when 
  the document has been read to its end, adds that document to the 
  `StreamingUpdateSolrServer` stored in the @solr ref."
  []
  (SAXResult. 
    (let [current (StringBuilder.)
          chars  (StringBuilder.)
          solrdoc (SolrInputDocument.)]
      (proxy [DefaultHandler] []
        (startElement [uri local qname atts]
          (when (= local "field")
            (doto current (.append (.getValue atts "name")))
            (when (> (.length chars) 0)
              (doto chars (.delete 0 (.length chars))))))
        (characters [ch start length]
          (doto chars (.append ch start length)))
        (endElement [uri local qname]
          (when (> (.length current) 0)
            (.addField solrdoc (.toString current) (.toString chars))
            (doto current (.delete 0 (.length current)))))
        (endDocument []
          (when (not (nil? (.getField solrdoc "transcription")))
            (add-words (.getFieldValue solrdoc "transcription")))
          (.add @solr solrdoc))))))

(defn bibliodochandler 
  "A document handler that behaves much like `dochandler` above, but
  works for bibliographic records."
  []
     (SAXResult. 
      (let [current (StringBuilder.)
	    chars  (StringBuilder.)
	    solrdoc (SolrInputDocument.)]
	(proxy [DefaultHandler] []
	  (startElement [uri local qname atts]
			(when (= local "field")
			  (doto current (.append (.getValue atts "name")))
			  (when (> (.length chars) 0)
			    (doto chars (.delete 0 (.length chars))))))
	  (characters [ch start length]
		      (doto chars (.append ch start length)))
	  (endElement [uri local qname]
		      (when (> (.length current) 0)
			(.addField solrdoc (.toString current) (.toString chars))
			(doto current (.delete 0 (.length current)))))
	  (endDocument []
		       (.add @solrbiblio solrdoc))))))

(defn copy
  "Performs a file copy from the source to the destination, making directories if necessary."
  [in out]
  (try
    (let [outfile (File. out)]
      (.mkdirs (.getParentFile outfile))
      (.createNewFile outfile))
    (let [buffer (byte-array 1024)
    from (FileInputStream. in)
    to (FileOutputStream. out)]
      (loop []
  (let [size (.read from buffer)]
    (when (pos? size)
      (.write to buffer 0 size)
      (recur))))
      (.close from)
      (.close to))
    (catch Exception e
      (println (str (.getMessage e) " copying " in " to " out ".")))))

(defn init-templates
  "Initializes the XSLT template pool."
    [xslt, nthreads, pool]
  (dosync (ref-set (load-string pool) (ConcurrentLinkedQueue.) ))
  (dotimes [n nthreads]
    (let [xsl-src (StreamSource. (FileInputStream. xslt))
            configuration (Configuration.)
            compiler-info (CompilerInfo.)]
          (doto xsl-src 
            (.setSystemId xslt))
    (doto configuration
      (.setXIncludeAware true))
          (doto compiler-info
            (.setErrorListener (StandardErrorListener.))
            (.setURIResolver (StandardURIResolver. configuration)))
          (dosync (.add (load-string (str "@" pool)) (PreparedStylesheet/compile xsl-src configuration compiler-info))))))
    
;; ## Utility functions

(defn substring-after
  "Returns the part of string1 that comes after the first occurrence of string2, or 
  nil if string1 does not contain string2."
  [string1 string2]
  (when (.contains string1 string2) (.substring string1 (+ (.indexOf string1 string2) (.length string2)))))

(defn substring-before
  "Returns the part of string1 that comes before the first occurrence of string2, or
  nil if string1 does not contain string2."
  [string1 string2]
  (when (.contains string1 string2) (.substring string1 0 (.indexOf string1 string2))))

(defn ceil
  [num]
  (substring-before (str (Math/ceil num)) "."))

(defn encode-url
  "Wrapper for the `java.net.URLEncoder.encode()` method."
  [url]
  (URLEncoder/encode url "UTF-8"))

(defn decode-url
  "Wrapper for the `java.net.URLEncoder.decode()` method."
  [url]
  (URLDecoder/decode url "UTF-8"))

(defn get-filename
  "Resolves the filename of the local XML file associated with the given URL."
  [url]
  (if (.contains url "ddbdp/")
    (let [identifier (.split (substring-before (substring-after url "http://papyri.info/ddbdp/") "/source") ";")]
      (if (= (second identifier) "")
        (str filepath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "."
       (.replace (.replace (.replace (.replace (last identifier) "," "-") "/" "_") "%2F" "_") "%2C" "-") ".xml")
        (str filepath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "." (second identifier) 
       "/" (first identifier) "." (second identifier) "."
       (.replace (.replace (.replace (.replace (last identifier) "," "-") "/" "_") "%2F" "_") "%2C" "-") ".xml")))
    (if (.contains url "hgv/")
      (let [identifier (substring-before (substring-after url "http://papyri.info/hgv/") "/source")
            id-int (Integer/parseInt (.replaceAll identifier "[a-z]" ""))]
        (str filepath "/HGV_meta_EpiDoc/HGV" (ceil (/ id-int 1000)) "/" identifier ".xml"))
      (when (.contains url "apis/")
        (let [identifier (.split (substring-before (substring-after url "http://papyri.info/apis/") "/source") "\\.")]
          (str filepath "/APIS/" (first identifier) "/xml/" (first identifier) "." (second identifier) "." (last identifier) ".xml"))))))

(defn get-txt-filename
  "Resolves the filename of the local text file associated with the given URL."
  [url]
  (try (if (.startsWith url "file:")
    (.replace (str htpath (substring-before (substring-after url (str "file:" filepath)) ".xml") ".txt") "/xml/" "/")
    (if (.contains url "ddbdp")
      (let [url (decode-url url)]
  (when (.endsWith url "/source")
    (let [identifier (.split (substring-before (substring-after url "http://papyri.info/ddbdp/") "/source") ";")]
      (if (= (second identifier) "")
        (str htpath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "."
       (.replace (.replace (last identifier) "," "-") "/" "_") ".txt")
        (str htpath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "." (second identifier) 
       "/" (first identifier) "." (second identifier) "."
       (.replace (.replace (last identifier) "," "-") "/" "_") ".txt")))))
      (if (.contains url "hgv")
        (when (.endsWith url "/source")
          (let [identifier (substring-before (substring-after url "http://papyri.info/hgv/") "/source")
                id-int (Integer/parseInt (.replaceAll identifier "[a-z]" ""))]
            (str htpath "/HGV_meta_EpiDoc/HGV" (ceil (/ id-int 1000)) "/" identifier ".txt")))
        (when (.contains url "/apis")
          (if (.endsWith url "/source")
            (let [identifier (.split (substring-before (substring-after url "http://papyri.info/apis/") "/source") "\\.")]
              (str htpath "/APIS/" (first identifier) "/" (first identifier) "." (second identifier) "." (last identifier) ".txt")))))))
       (catch Exception e
   (println (str (.getMessage e) " processing " url ".")))))
          
(defn get-html-filename
  "Resolves the filename of the local HTML file associated with the given URL."
  [url]
  (try (if (.startsWith url "file:")
    (.replace (str htpath (substring-before (substring-after url (str "file:" filepath)) ".xml") ".html") "/xml/" "/")
    (if (.contains url "ddbdp")
      (let [url (decode-url url)]
  (if (.endsWith url "/source")
        (let [identifier (.split (substring-before (substring-after url "http://papyri.info/ddbdp/") "/source") ";")]
          (if (= (second identifier) "")
            (str htpath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "."
     (.replace (.replace (last identifier) "," "-") "/" "_") ".html")
            (str htpath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "." (second identifier) 
     "/" (first identifier) "." (second identifier) "."
     (.replace (.replace (last identifier) "," "-") "/" "_") ".html")))
        (if (= url "http://papyri.info/ddbdp")
          (str htpath "/DDB_EpiDoc_XML/index.html")
    (if (.contains url ";")
      (let [identifier (.split (substring-after url "http://papyri.info/ddbdp/") ";")]
        (str htpath "/DDB_EpiDoc_XML/" (first identifier) "/" (first identifier) "." (second identifier) "/index.html"))
      (str htpath "/DDB_EpiDoc_XML/" (substring-after url "http://papyri.info/ddbdp/") "/index.html")))))
      (if (.contains url "hgv")
        (if (.endsWith url "/source")
          (let [identifier (substring-before (substring-after url "http://papyri.info/hgv/") "/source")
                id-int (Integer/parseInt (.replaceAll identifier "[a-z]" ""))]
            (str htpath "/HGV_meta_EpiDoc/HGV" (ceil (/ id-int 1000)) "/" identifier ".html"))
          (if (= url "http://papyri.info/hgv")
            (str htpath "/HGV_meta_EpiDoc/index.html")
            (str htpath "/HGV_meta_EpiDoc/" (substring-after url "http://papyri.info/hgv/") "/index.html")))
        (when (.contains url "/apis")
          (if (.endsWith url "/source")
            (let [identifier (.split (substring-before (substring-after url "http://papyri.info/apis/") "/source") "\\.")]
              (str htpath "/APIS/" (first identifier) "/" (first identifier) "." (second identifier) "." (last identifier) ".html"))
            (if (= url "http://papyri.info/apis")
              (str htpath "/APIS/index.html")
              (str htpath "/APIS/" (substring-after url "http://papyri.info/apis/") "/index.html")))))))
       (catch Exception e
   (println (str (.getMessage e) " processing " url ".")))))

(defn transform
  "Runs an XSLT transform on the `java.io.File` in the first parameter, 
  using a list of key/value parameter pairs, and feeds the result of the transform into
  a `javax.xml.transform.Result`."
  [url, params, #^Result out, pool]
    (let [xslt (.poll pool)
    transformer (.newTransformer xslt)]
      (try
        (when (not (== 0 (count params)))
          (doseq [param params] (doto transformer
            (.setParameter (first param) (second param)))))
        (.transform transformer (StreamSource. (.openStream (URL. url))) out)
        (catch Exception e
          (println (str (.getMessage e) " transforming " url "."))
          (.printStackTrace e))
        (finally
          (.add pool xslt)))))
    
;; ## SPARQL Queries
;; Each of the following functions formats a SPARQL query.

(defn has-part-query
  "Constructs a set of triples where A `<dc:hasPart>` B."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a 
            filter not exists {?a dc:isReplacedBy ?b }}" url ))
            
(defn is-part-of-query
  "Returns a flattened list of parent, child, grandchild URIs."
	[url]
	(format "prefix dc: <http://purl.org/dc/terms/>
			select ?p ?gp ?ggp
			from <http://papyri.info/graph>
			where{ <%s> dc:isPartOf ?p .
				   ?p dc:isPartOf ?gp .
				   optional { ?gp dc:isPartOf ?ggp }
			}" url))
            
(defn batch-relation-query
  "Retrieves a set of triples where A `<dc:relation>` B when A is a child of the given URI."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?b
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:relation ?b
                    filter(!regex(str(?b),'/images$'))}" url))

(defn relation-query
  "Returns URIs that are the object of `<dc:relation>`s where the given URI is the subject."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:relation ?a 
            filter(!regex(str(?a),'/images$'))}" url))

(defn batch-replaces-query
  "Gets the set of triples where A `<dc:replaces>` B for a given collection."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?b
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:replaces ?b }" url))

(defn batch-hgv-source-query
  "Gets the set of triples where A `<dc:source` B for a given collection."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?b
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:source ?b }" url))
        
(defn hgv-source-query
  "Returns A where the given URI `<dc:source>` A."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:source ?a  }" url))        
        
(defn batch-other-source-query
  "Gets `dc:source`s for items in a given collection where there are 
  related HGV docs with sources."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?b
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:relation ?hgv .
                    ?hgv dc:source ?b }" url))
                    
(defn other-source-query
  "Gets `dc:source`s for the given URI using related HGV docs."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:relation ?hgv .
                    ?hgv dc:source ?a }" url))        
        		                                    
        		                
(defn batch-hgv-citation-query
  "Gets `dc:bibliographicCitation`s for items in a given collection where 
  there are related sources with bibliography."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?c
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:source ?b .
                    ?b dc:bibliographicCitation ?c }" url))  
                    
(defn hgv-citation-query
  "Gets a bibliographic citation for the given URI, using the related source."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:source ?b .
                    ?b dc:bibliographicCitation ?a }" url)) 
                    
(defn batch-other-citation-query
  "Gets bibliographic citations for items in a given collection, via their
  relationship with HGV records."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?c
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:relation ?hgv .
                    ?hgv dc:source ?b .
                    ?b dc:bibliographicCitation ?c }" url))  
                    
(defn other-citation-query
  "Gets bibliographic citations for a given item via its relationship
  to HGV records."
	[url]
    (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:relation ?hgv .
                    ?hgv dc:source ?b .
                    ?b dc:bibliographicCitation ?a }" url)) 

(defn batch-cited-by-query
  [url]
  (format "prefix cito: <http://purl.org/spar/cito/>
          prefix dc: <http://purl.org/dc/terms/> 
          select ?a ?c
          from <http://papyri.info/graph>
          where {<%s> dc:hasPart ?a .
                 ?a dc:source ?b .
                 ?b cito:isCitedBy ?c }" url))

(defn cited-by-query
  "Looks for Cito citations coming from biblio"
  [url]
  (let [uri (.replace url "/source" "/work")]
    (format "prefix cito: <http://purl.org/spar/cito/>
             select ?a
            from <http://papyri.info/graph>
            where {<%s> cito:isCitedBy ?a }" uri)))
            
(defn replaces-query
  "Finds items that the given item replaces."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:replaces ?a }" url))

(defn batch-is-replaced-by-query
  "Finds items in a given collection that are replaced by other items."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a ?b
            from <http://papyri.info/graph>
            where { <%s> dc:hasPart ?a .
                    ?a dc:isReplacedBy ?b }" url))

(defn is-replaced-by-query
  "Finds any item that replaces the given item."
  [url]
  (format  "prefix dc: <http://purl.org/dc/terms/> 
            select ?a
            from <http://papyri.info/graph>
            where { <%s> dc:isReplacedBy ?a }" url))
            
(defn batch-images-query
  [url]
  (format "prefix dc: <http://purl.org/dc/terms/>
           prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
           select ?a ?image
           from <http://papyri.info/graph>
           where { <%s> dc:hasPart ?a .
                   ?a dc:relation ?i .
                   ?i rdf:type rdf:Seq .
                   ?i ?p ?image .
                      filter (?a != ?image)
                      filter (?image != rdf:Seq) 
                      filter (?p != dc:relation)}
           order by ?a ?p" url))
            
(defn images-query
  "Finds images related to the given url"
  [url]
  (let [uri (.replace url "/source" "/images")]
    (format "prefix dc: <http://purl.org/dc/terms/>
             prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             select ?image
             from <http://papyri.info/graph>
             where { <%1$s> ?p ?image .
                     <%1$s> rdf:type rdf:Seq
                       minus { <%1$s> rdf:type rdf:Seq } }
             order by ?p" uri )))

;; ## Jena functions

(defn collect-row  
  "Builds a row of results from Jena into a vector."
  [row rvars]
  (let [*row* (transient [])]
    (doseq [svar rvars]
      (when (not (nil? (.get row svar))) (conj! *row* (.toString (.get row svar)))))
    (persistent! *row*)))
                    
(defn execute-query
  "Executes the query provided and returns a vector of vectors containing the results"
  [query]
  (try
    (with-open [exec (QueryExecutionFactory/sparqlService (str server "/query") query)]
      (let [answer (.execSelect exec)]
        (loop [result []]
          (if (.hasNext answer)
            (recur (conj result
                         (collect-row (.next answer) (.getResultVars answer))))
            result))))
    (catch Exception e 
      (println (.getMessage e))
      (println query))))


;; ## Data queueing functions

(defn queue-item
  "Adds the given URL to the @html queue for processing, along with associated data."
  [url]
  (println (get-filename url))
  (let [relations (execute-query (relation-query url))
        replaces (execute-query (replaces-query url))
        is-replaced-by (execute-query (is-replaced-by-query url))
        is-part-of (execute-query (is-part-of-query url))
        source (if (empty? (re-seq #"/hgv/" url))
       		   	  (execute-query (other-source-query url))
       		   	  (execute-query (hgv-source-query url)))
        citation(if (empty? (re-seq #"/hgv" url))
       			  (execute-query (other-citation-query url))
       			  (execute-query (hgv-citation-query url)))
        biblio (execute-query (cited-by-query url))
        images (flatten (conj '() (execute-query (images-query url)) 
                     (filter 
                       (fn [x] (> (count x) 0))
                       (for [r relations] (execute-query (images-query (first r)))))))
       ]
    (when (not (first is-replaced-by))
    (.add @html (list (str "file:" (get-filename url))
                      (list "collection" (substring-before (substring-after url "http://papyri.info/") "/"))
                      (list "related" (apply str (interpose " " (for [x relations] (first x)))))
                      (list "replaces" (apply str (interpose " " (for [x replaces] (first x))))) 
                      (list "isPartOf" (apply str (interpose " " (first is-part-of))))
                      (list "sources" (apply str (interpose " " (for [x source](first x)))))
                      (list "images" (apply str (interpose " " images)))
                      (list "citationForm" (apply str (interpose " " (for [x citation](first x)))))
                      (list "biblio" (apply str (interpose " " (for [x biblio] (first x)))))
                      (list "selfUrl" (substring-before url "/source"))
                      (list "server" nserver))))))

(defn queue-items
  "Adds children of the given collection or volume to the @html queue for processing,
  along with a set of parameters."
  [url exclude prev-urls]
  (let [all-urls (cons url prev-urls)
        items (execute-query (has-part-query url))
        relations (execute-query (batch-relation-query url))
        replaces (execute-query (batch-replaces-query url))
        is-replaced-by (execute-query (batch-is-replaced-by-query url))
        all-sources (if (empty? (re-seq #"/hgv/" url))
                      (execute-query (batch-other-source-query url))
        			        (execute-query (batch-hgv-source-query url)))
        all-citations (if (empty? (re-seq #"/hgv/" url))
                        (execute-query (batch-other-citation-query url))
                        (execute-query (batch-hgv-citation-query url)))
        all-biblio (execute-query (batch-cited-by-query url))
        all-images (execute-query (batch-images-query url))]	
    (doseq [item items]
      (let  [related (if (empty? relations) ()
                       (filter (fn [x] (= (first x) (last item))) relations))
             reprint-from (if (empty? replaces) ()
                            (filter (fn [x] (= (first x) (last item))) replaces))
             sources (if (empty? all-sources) ()
                       (filter (fn [x] (= (first x) (last item))) all-sources))
             citations (if (empty? all-citations) ()
                         (filter (fn [x] (= (first x) (last item))) all-citations))
             biblio (if (empty? all-biblio) ()
                      (filter (fn [x] (= (first x) (last item))) all-biblio))
             reprint-in (if (empty? is-replaced-by) ()
                          (filter (fn [x] (= (first x) (last item))) is-replaced-by))
             exclusion (some (set (for [x (filter 
                                            (fn [s] (and (.startsWith (last s) "http://papyri.info") 
                                                         (not (.contains (.toString (last s)) "/images/")))) 
                                            related)] 
                                    (substring-before (substring-after (last x) "http://papyri.info/") "/"))) 
                             exclude)
             images (if (empty? all-images) () (filter (fn [x] (= (first x) (last item))) all-images))
            ]
        (if (nil? exclusion)
          ( .add @html (list (str "file:" (get-filename (last item)))
                             (list "collection" (substring-before (substring-after (last item) "http://papyri.info/") "/"))
                             (list "related" (apply str (interpose " " (for [x related] (last x)))))
                             (list "replaces" (apply str (interpose " " (for [x reprint-from] (last x))))) 
                             (list "isPartOf" (apply str (interpose " " all-urls)))   
                             (list "sources" (apply str (interpose " " (for [x sources] (last x)))))  
                             (list "images" (apply str (interpose " " (for [x images] (last x)))))
                             (list "citationForm" (apply str (interpose "" (for [x citations] (last x))))) 
                             (list "biblio" (apply str (interpose " " (for [x biblio] (last x))))) 
                             (list "selfUrl" (substring-before (last item) "/source"))     
                             (list "server" nserver)))
          (do (.add @links (list (get-html-filename 
                                   (.toString 
                                     (last 
                                       (reduce (fn [x y] 
                                                 (if (.contains (last x) exclusion) x y)) 
                                               related))))
                                 (get-html-filename (.toString (last item)))))
            (.add @links (list (get-txt-filename 
                                 (.toString 
                                   (last 
                                     (reduce (fn [x y] 
                                               (if (.contains (last x) exclusion) x y)) 
                                             related))))
                               (get-txt-filename (last item))))))))))	
                  
(defn queue-collections
  "Adds URLs to the HTML transform and indexing queues for processing.  Takes a URL, like 
  `http://papyri.info/ddbdp`, a set of collections to exclude and recurses down to the item level."
  [url exclude prev-urls]
  ;; TODO: generate symlinks for relations
  ;; queue for HTML generation
   (let [all-urls (cons url prev-urls)
         items (execute-query (has-part-query url))]
    (when (> (count items) 0)
      (if (.endsWith (last (first items)) "/source")
        (queue-items url exclude prev-urls)
        (doseq [item items]
          (queue-collections (last item) exclude all-urls))))))

;; ## File generation and indexing functions

(defn delete-html
 "Get rid of any old/stale versions of related files"
 [files]
 (when (> (count files) 0)
   (doseq [file (.split files "\\s")]
     (let [fname (get-html-filename file)] 
     (when-not (nil? fname)
       (let [f (File. fname)]
         (when (.exists f)
           (.delete f))))))))
         
(defn delete-text
  "Get rid of any old/stale versions of related files"
  [files]
  (when (> (count files) 0)
    (doseq [file (.split files "\\s")]
      (let [fname (get-txt-filename file)]
      (when-not (nil? fname)
        (let [f (File. fname)]
          (when (.exists f)
            (.delete f))))))))

(defn generate-html
  "Builds the HTML files for the PN."
  []
    (let [pool (Executors/newFixedThreadPool nthreads)
    tasks (map (fn [x]
         (fn []
           (try (.mkdirs (.getParentFile (File. (get-html-filename (first x)))))
            ;(println "Transforming " (first x) " to " (get-html-filename (first x)))
              (delete-html (last (nth x 2)))
              (delete-html (last (nth x 3)))
              (transform (if (.startsWith (first x) "http")
                (str (.replace (first x) "papyri.info" nserver) "/rdf")
                (first x))
                (list (second x) (nth x 2) (nth x 3) (nth x 4) (nth x 5) (nth x 6) (nth x 7) (nth x 8) (nth x 9))
                (StreamResult. (File. (get-html-filename (first x)))) @htmltemplates)
             (catch Exception e
               (.printStackTrace e)
               (println (str "Error converting file " (first x) " to " (get-html-filename (first x))))))))
       @html)]
    (doseq [future (.invokeAll pool tasks)]
      (.get future))
    (doto pool
      (.shutdown)))
    (dosync (ref-set text @html)
      (ref-set htmltemplates nil)))

(defn generate-text
  "Builds the text files for the PN."
  []
    (let [pool (Executors/newFixedThreadPool nthreads)
        tasks (map (fn [x]
         (fn []
           (when (not (.startsWith (first x) "http"))
      (try (.mkdirs (.getParentFile (File. (get-html-filename (first x)))))
        (delete-text (last (nth x 2)))
        (delete-text (last (nth x 3)))
        (transform (if (.startsWith (first x) "http")
        							   ((println "File is " + (first x))
                                       (str (.replace (first x) "papyri.info" nserver) "/rdf"))
                                       (first x))
        (list (second x) (nth x 2) (nth x 3) (nth x 4))
        (StreamResult. (File. (get-txt-filename (first x)))) @texttemplates)
        (catch Exception e
          (.printStackTrace e)
          (println (str "Error converting file " (first x) " to " (get-txt-filename (first x)))))))))
       @text)]
    (doseq [future (.invokeAll pool tasks)]
      (.get future))
    (doto pool
      (.shutdown)))
    (dosync (ref-set texttemplates nil)))

(defn print-words 
  "Dumps accumulated word lists into a file."
  []
     (let [out (FileWriter. (File. "/data/papyri.info/words.txt"))]
       (for [word @words]
   (.write out (str word "\n")))))

(defn load-morphs 
  "Loads morphological data from the given file into the morph-search Solr index."
 [file]
 (let [value (StringBuilder.)
       handler (proxy [DefaultHandler] []
		 (startElement [uri local qname atts]
			       (set! *current* qname)
			       (when (= qname "analysis")
				 (set! *doc* (SolrInputDocument.))
				 (set! *index* (+ *index* 1))
				 (.addField *doc* "id" *index*)))
		 (characters [ch start length]
			     (when-not (.startsWith *current* "analys")
			       (.append value ch start length)))
		 (endElement [uri local qname]
			     (if (not (.startsWith qname "analys"))
			       (.addField *doc* *current* (.toString value))
			       (.add @solr *doc*))
			     (set! *current* "analysis")
			     (.delete value 0 (.length value))))]
   (.. SAXParserFactory newInstance newSAXParser
       (parse (InputSource. (FileInputStream. file)) 
	      handler))))
         
;; ## Java static methods

(defn -loadLemmas 
  "Calls the morphological data-loading procedure on the Greek and Latin XML files from Perseus.
  NOTE: hard code file paths."
  []
  (binding [*current* nil
      *doc* nil
	    *index* 0]
    (dosync (ref-set solr (StreamingUpdateSolrServer. (str solrurl "morph-search/") 5000 5))
	    (.setRequestWriter @solr (BinaryRequestWriter.)))
    (load-morphs "/data/papyri.info/git/navigator/pn-lemmas/greek.morph.unicode.xml")
    (load-morphs "/data/papyri.info/git/navigator/pn-lemmas/latin.morph.xml")
    (let [solr (CommonsHttpSolrServer. (str solrurl "morph-search/"))]
      (doto solr
	(.commit)
	(.optimize)))))

(defn -loadBiblio 
  "Loads bibliographic data into the biblio-search Solr index."
  []
  (init-templates (str xsltpath "/Biblio2Solr.xsl") nthreads "info.papyri.indexer/bibsolrtemplates")
  (init-templates (str xsltpath "/Biblio2HTML.xsl") nthreads "info.papyri.indexer/bibhtmltemplates")
  (dosync (ref-set solrbiblio (StreamingUpdateSolrServer. (str solrurl "biblio-search/") 1000 5))
          (.setRequestWriter @solrbiblio (BinaryRequestWriter.)))

  ;; Generate and Index bibliography
  (println "Generating and indexing bibliography...")
  (let [pool (Executors/newFixedThreadPool nthreads)
        htmlpath (str htpath "/biblio/")
        files (file-seq (File. (str filepath "/Biblio")))
        tasks (map (fn [x]
                     (fn []
                       (transform (str "file://" (.getAbsolutePath x)) () (bibliodochandler) @bibsolrtemplates)
                       (transform (str "file://" (.getAbsolutePath x)) ()
                                  (StreamResult. (File. (str htmlpath (.getName (.getParentFile x)) "/" (.replace (.getName x) ".xml" ".html"))))
                                  @bibhtmltemplates)))
                   (filter #(.endsWith (.getName %) ".xml") files))]
    (doseq [future (.invokeAll pool tasks)]
      (.get future))
    (doto pool
      (.shutdown)))

  (println "Optimizing index...")
  (doto @solrbiblio
    (.commit)
    (.optimize)))

   
(defn -index 
  "Runs the main PN indexing process."
  [& args]
  (init-templates (str xsltpath "/RDF2HTML.xsl") nthreads "info.papyri.indexer/htmltemplates")
  (init-templates (str xsltpath "/RDF2Solr.xsl") nthreads "info.papyri.indexer/solrtemplates")
  (init-templates (str xsltpath "/MakeText.xsl") nthreads "info.papyri.indexer/texttemplates")
  (dosync (ref-set html (ConcurrentLinkedQueue.)))
  
  (if (nil? (first args))
    (do
      (println "Queueing DDbDP...")
      (queue-collections "http://papyri.info/ddbdp" () ())
      (println (str "Queued " (count @html) " documents."))
      (println "Queueing HGV...")
      (queue-collections "http://papyri.info/hgv" '("ddbdp") ())
      (println (str "Queued " (count @html) " documents."))
      (println "Queueing APIS...")
      (queue-collections "http://papyri.info/apis" '("ddbdp", "hgv") ())
      (println (str "Queued " (count @html) " documents.")))
    (doseq [arg (first args)] (queue-item arg)))

  (dosync (ref-set text @html))
   
  ;; Generate HTML
  (println "Generating HTML...")
  (generate-html)

  ;; Generate text
  (println "Generating text...")
  (generate-text)

  (dosync (ref-set solr (StreamingUpdateSolrServer. (str solrurl "pn-search/") 500 5))
	  (.setRequestWriter @solr (BinaryRequestWriter.)))
  
  ;; Index docs queued in @text
  (println "Indexing text...")
  (let [pool (Executors/newFixedThreadPool nthreads)
        tasks
   	(map (fn [x]
  	       (fn []
  		 (when (not (.startsWith (first x) "http"))
  		   (transform (first x)
  			      (list (second x) (nth x 2))
  			      (dochandler) @solrtemplates)))) @text)]
    (doseq [future (.invokeAll pool tasks)]
      (.get future))
    (doto pool
      (.shutdown)))

  (println "Optimizing index...")
  (doto @solr
    (.commit)
    (.optimize))

  (dosync (ref-set html nil)
    (ref-set text nil)
    (ref-set solrtemplates nil))
  
  ;;(print-words)
  )


(defn -main [& args]
  (if (> (count args) 0)
    (case (first args) 
      "load-lemmas" (-loadLemmas)
      "biblio" (-loadBiblio)
      (-index args))
    (-index)))
