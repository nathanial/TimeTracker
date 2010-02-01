(ns basecamp.main
  (:require [basecamp.person :as person] [basecamp.get :as get]
	    [basecamp.gui :as gui] [basecamp.time :as time]
	    [basecamp.project :as project]))

(Class/forName "org.sqlite.JDBC")

(let [credentials (gui/login)]
  (dosync 
   (ref-set get/credentials credentials)))
(gui/refresh-people)
(gui/run)