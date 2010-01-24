(ns basecamp.person
  (:use clojure.xml)
  (:import 
   (org.apache.http.client ResponseHandler HttpClient)
   (org.apache.http.client.methods HttpGet)
   (org.apache.http.impl.client BasicResponseHandler DefaultHttpClient)
   (org.apache.http.auth UsernamePasswordCredentials AuthScope)
   (java.sql Connection Statement DriverManager PreparedStatement 
	     ResultSet)))

(defstruct Person :firstname :lastname :email :id)

(defn get-content [person tag]
  (loop [attributes (:content person)]
    (when (not (empty? attributes))
      (let [attribute (first attributes)]
	(if (= tag (:tag attribute))
	  (first (:content attribute))
	  (recur (rest attributes)))))))

(defn first-name [person]
  (get-content person :first-name))

(defn last-name [person]
  (get-content person :last-name))

(defn email [person]
  (get-content person :email-address))

(defn id [person]
  (get-content person :id))

(defn extract [people-xml]
  (for [person (:content people-xml)]
    (struct-map Person 
      :firstname (first-name person)
      :lastname (last-name person)
      :email (email person)
      :id (id person))))

(defn persist [people]
  (let [conn (DriverManager/getConnection "jdbc:sqlite:basecamp.db")
	stat (.createStatement conn)]
    (doto stat
      (.executeUpdate "drop table if exists people;")
      (.executeUpdate "create table people (firstname, lastname, email, id)"))
    (let [prep (.prepareStatement conn "insert into people values (?, ?, ?, ?);")]
      (doseq [person people]
	(doto prep
	  (.setString 1 (:firstname person))
	  (.setString 2 (:lastname person))
	  (.setString 3 (:email person))
	  (.setString 4 (:id person))
	  (.addBatch)))
      (.setAutoCommit conn false)
      (.executeBatch prep)
      (.setAutoCommit conn true)
      
      (.close conn))))
