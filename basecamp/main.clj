(ns basecamp.main
  (:require [basecamp.person :as person] [basecamp.get :as get]
	    [basecamp.gui :as gui] [basecamp.time :as time]))

(Class/forName "org.sqlite.JDBC")

(def people 
     (let [people (person/extract (get/fetch-people))] 
       (for [person people]
	 (let [times (time/extract (get/fetch-times (:id person)))]
	   (assoc person :times times)))))

(gui/run-with people)