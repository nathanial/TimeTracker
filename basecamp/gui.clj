(ns basecamp.gui
  (:import (javax.swing JFrame JPanel JLabel JButton
			JSplitPane UIManager JList DefaultListModel
			JScrollPane JTable JOptionPane ListSelectionModel
			SortOrder)
	   (javax.swing.table DefaultTableModel)
	   (javax.swing.event ListSelectionListener TableModelEvent)
	   (java.awt Dimension)
	   (org.jdesktop.swingx JXTable)
	   (net.miginfocom.swing MigLayout)))

(UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(defmacro swing [& body]
  `(javax.swing.SwingUtilities/invokeLater (fn []
					     ~@body)))


(defn run-with [people]
  (let [frame (JFrame. )
	left-panel (JPanel. (MigLayout.))
	right-panel (JPanel. (MigLayout.))
	pane (JSplitPane. )
	person-list (JList. (DefaultListModel.))
	person-scroll-pane (JScrollPane. person-list)
	time-table (JXTable. (DefaultTableModel.))
	time-scroll-pane (JScrollPane. time-table)
	show-times-for (fn [person]
			 (let [times (:times person)
			       model (.getModel time-table)]
			   (loop []
			     (when (not= 0 (.getRowCount model))
			       (.removeRow model 0)
			       (recur)))
			   (doseq [time times]
			     (.addRow model (to-array [(:date time) (:hours time) (:description time)])))
			   (.revalidate time-table)))]
    (doto left-panel
      (.add (JLabel. "People") "wrap")
      (.add person-scroll-pane "grow,push"))
    (doto right-panel
      (.add (JLabel. "Times") "wrap")
      (.add time-scroll-pane "grow,push"))
    (doseq [person people]
      (let [model (.getModel person-list)]
	(.addElement model (str (:firstname person) " " (:lastname person)))))
    (doto (.getModel time-table)
      (.addColumn "Date")
      (.addColumn "Hours")
      (.addColumn "Description"))
    (.. time-table (getColumnModel) (getColumn 0) (setMaxWidth 75))
    (.. time-table (getColumnModel) (getColumn 1) (setMaxWidth 50))
    
    (doto time-table
      (.setSortOrder "Date" SortOrder/DESCENDING)
      (.setAutoResizeMode JTable/AUTO_RESIZE_LAST_COLUMN))
    (doto person-list
      (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
      (.addListSelectionListener
       (proxy [ListSelectionListener] []
	 (valueChanged [e]
		       (when (not (.getValueIsAdjusting e))
			 (try
			  (show-times-for (nth people (.getSelectedIndex person-list)))
			  (catch Exception e
			    (JOptionPane/showMessageDialog time-table e))))))))
    (doto pane
      (.setLeftComponent left-panel)
      (.setRightComponent right-panel)
      (.setDividerLocation 150))
    (doto frame
      (.. (getContentPane) (add pane))
      (.setSize 400 400)
      (.setVisible true))))