(ns basecamp.gutil
  (:import (java.awt Dimension Dialog Toolkit)))

(defn move-to-center [window]
  (let [dim (.. Toolkit (getDefaultToolkit) (getScreenSize))
	w (.. window (getSize) width)
	h (.. window (getSize) height)
	x (/ (- (. dim width) w) 2)
	y (/ (- (. dim height) h) 3)]
    (.setLocation window x y)))

(defn unique [xs]
  (loop [xs xs
	 acc []]
    (let [x (first xs)]
      (if (not x)
	acc
	(let [others (filter #(not= x %) xs)]
	  (recur others (conj acc x)))))))