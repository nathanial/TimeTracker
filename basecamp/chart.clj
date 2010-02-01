(ns basecamp.chart
  (:use basecamp.gutil)
  (:import (org.jfree.chart ChartFactory ChartUtilities JFreeChart
			    ChartPanel)
	   (org.jfree.data.general DefaultPieDataset)
	   (org.jfree.data.xy XYDataset XYSeries XYSeriesCollection)
	   (org.jfree.data.time TimeSeriesCollection TimeSeries Minute)
	   (org.jfree.chart.axis NumberAxis)
	   (org.jfree.chart.plot PlotOrientation XYPlot)
	   (org.jfree.chart.renderer.xy XYLineAndShapeRenderer)
	   (javax.swing JFrame JPanel JLabel JButton JDialog JPasswordField
			JSplitPane UIManager JList DefaultListModel
			JScrollPane JTable JOptionPane ListSelectionModel
			SortOrder JMenuBar JMenu JMenuItem JTabbedPane
			JCheckBox DefaultCellEditor JTextField
			JCheckBoxMenuItem JProgressBar)
	   (javax.swing.table DefaultTableModel TableCellRenderer)
	   (javax.swing.event ListSelectionListener TableModelEvent TableModelListener)
	   (java.awt Dimension Dialog Toolkit)
	   (java.awt.event ActionListener)
	   (org.jdesktop.swingx JXTable)
	   (net.miginfocom.swing MigLayout)
	   (java.awt Dimension Dialog Toolkit)
	   (java.text SimpleDateFormat)
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

(defn show-hours-line-chart [person]
  (let [dataset (TimeSeriesCollection.)
	chart (ChartFactory/createTimeSeriesChart
	       (str (:firstname person) " " (:lastname person))
	       "Date" "Value" dataset true true false)
	renderer (XYLineAndShapeRenderer. true true)
	panel (ChartPanel. chart)
	projects (unique (map :project-name (:times person)))
	format (SimpleDateFormat. "yyyy-MM-dd")]
    (doto (.getPlot chart)
      (.setRenderer renderer))
    (doseq [project projects]
      (let [series (TimeSeries. project, Minute)
	    times (filter #(= project (:project-name %)) (:times person))]
	(doseq [time times]
	  (.addOrUpdate series (Minute. (.parse format (:date time))) (Double/valueOf (:hours time))))
	(.addSeries dataset series)))
    (doto (JFrame.)
      (.setSize (Dimension. 500 400))
      (.setContentPane panel)
      (move-to-center)
      (.setVisible true))))