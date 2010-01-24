(ns basecamp.get
  (:use clojure.xml)
  (:import 
   (org.apache.http.client ResponseHandler HttpClient)
   (org.apache.http.client.methods HttpGet)
   (org.apache.http.impl.client BasicResponseHandler DefaultHttpClient)
   (org.apache.http.auth UsernamePasswordCredentials AuthScope)
   (java.sql Connection Statement DriverManager PreparedStatement 
	     ResultSet)))

(def base-url "http://erdosmiller.basecamphq.com/")
(def username "nathanial")
(def password "ribbit22")

(defn fetch-people []
  (let [client (DefaultHttpClient.)
	get (HttpGet. (str base-url "people.xml"))
	response-handler (BasicResponseHandler.)]
    (.. client (getCredentialsProvider) 
	(setCredentials AuthScope/ANY 
			(UsernamePasswordCredentials. username password)))
    (doto get
      (.addHeader "Accept" "application/xml")
      (.addHeader "Content-Type" "application/xml"))
    (let [response-body (.execute client get response-handler)
	  result (parse (java.io.ByteArrayInputStream. (.getBytes response-body)))]
      (.. client (getConnectionManager) (shutdown))
      result)))

(defn fetch-times [person-uuid]
  (let [client (DefaultHttpClient.)
	get (HttpGet. (str base-url 
			   "time_entries/report.xml?"
			   "subject_id=" person-uuid "&"
			   "from=20090801&"
			   "to=20100201"))
	response-handler (BasicResponseHandler.)]
    (.. client (getCredentialsProvider)
	(setCredentials AuthScope/ANY
			(UsernamePasswordCredentials. username password)))
    (doto get
      (.addHeader "Accept" "application/xml")
      (.addHeader "Content-Type" "application/xml"))
    (let [response (.execute client get)
	  response-body (.. response (getEntity) (getContent))
	  result (parse response-body)]
      (.. client (getConnectionManager) (shutdown))
      result)))