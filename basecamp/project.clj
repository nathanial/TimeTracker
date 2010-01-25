(ns basecamp.project)

(defstruct Project :id :name)

(defn get-content [project tag]
  (loop [attributes (:content project)]
    (when (not (empty? attributes))
      (let [attribute (first attributes)]
	(if (= tag (:tag attribute))
	  (first (:content attribute))
	  (recur (rest attributes)))))))

(defn id [project]
  (get-content project :id))

(defn _name [project]
  (get-content project :name))

(defn extract [projects-xml]
  (for [project (:content projects-xml)]
    (struct-map Project
      :id (id project)
      :name (_name project))))

(defn find-with-id [projects pid]
  (first (filter #(= pid (:id %)) projects)))