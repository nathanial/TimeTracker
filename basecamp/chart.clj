(ns basecamp.chart
  (:use basecamp.gutil)
  (:import (org.jfree.chart ChartFactory ChartUtilities JFreeChart
			    ChartPanel)
	   (javax.swing JFrame)
	   (java.awt Dimension Dialog Toolkit)
	   (org.jfree.data.general DefaultPieDataset)
	   (java.io File)))

(defn show-time-pie-chart [person]
  (let [dataset (DefaultPieDataset. )
	chart (ChartFactory/createPieChart 
	       (str (:firstname person) " " (:lastname person))
	       dataset true true false)
	panel (ChartPanel. chart)
	projects (unique (map :project-name (:times person)))
	totals (for [project projects]
		 (let [times (filter #(= project (:project-name %)) (:times person))]
		   [project (apply + (map #(Double/valueOf (:hours %)) times))]))]
    (doseq [total totals]
      (.setValue dataset (first total) (second total)))
    (doto (JFrame.)
      (.setSize (Dimension. 500 400))
      (.setContentPane panel)
      (move-to-center)
      (.setVisible true))))