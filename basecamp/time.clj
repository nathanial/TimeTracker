(ns basecamp.time
  (:use clojure.xml)
  (:import 
   (org.apache.http.client ResponseHandler HttpClient)
   (org.apache.http.client.methods HttpGet)
   (org.apache.http.impl.client BasicResponseHandler DefaultHttpClient)
   (org.apache.http.auth UsernamePasswordCredentials AuthScope)
   (java.sql Connection Statement DriverManager PreparedStatement 
	     ResultSet)))

(defstruct Time :date :description :hours :person-id :project-id)

(defn get-content [time tag]
  (loop [attributes (:content time)]
    (when (not (empty? attributes))
      (let [attribute (first attributes)]
	(if (= tag (:tag attribute))
	  (first (:content attribute))
	  (recur (rest attributes)))))))

(defn date [time]
  (get-content time :date))

(defn description [time]
  (get-content time :description))

(defn hours [time]
  (get-content time :hours))

(defn person-id [time]
  (get-content time :person-id))

(defn project-id [time]
  (get-content time :project-id))

(defn extract [time-xml]
  (for [time (:content time-xml)]
    (struct-map Time
      :date (date time)
      :description (description time)
      :hours (hours time)
      :person-id (person-id time)
      :project-id (project-id time))))

(defn persist [times]
  (let [conn (DriverManager/getConnection "jdbc:sqlite:basecamp.db")
	stat (.createStatement conn)]
    (doto stat
      (.executeUpdate "drop table if exists time;")
      (.executeUpdate "create table time (date, description, hours, person-id, project-id)"))
    (let [prep (.prepareStatement conn "insert into people values (?, ?, ?, ?, ?);")]
      (doseq [time times]
	(doto prep
	  (.setString 1 (:date time))
	  (.setString 2 (:description time))
	  (.setString 3 (:hours time))
	  (.setString 4 (:person-id time))
	  (.setString 5 (:project-id time))
	  (.addBatch)))
      (.setAutoCommit conn false)
      (.executeBatch prep)
      (.setAutoCommit conn true)
      
      (.close conn))))


