(ns basecamp.gui
  (:require [basecamp.person :as person] [basecamp.get :as get]
	    [basecamp.time :as time]
	    [basecamp.project :as project])
  (:import (javax.swing JFrame JPanel JLabel JButton
			JSplitPane UIManager JList DefaultListModel
			JScrollPane JTable JOptionPane ListSelectionModel
			SortOrder JMenuBar JMenu JMenuItem)
	   (javax.swing.table DefaultTableModel)
	   (javax.swing.event ListSelectionListener TableModelEvent)
	   (java.awt Dimension)
	   (java.awt.event ActionListener)
	   (org.jdesktop.swingx JXTable)
	   (net.miginfocom.swing MigLayout)))

(UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(defmacro swing [& body]
  `(javax.swing.SwingUtilities/invokeLater (fn []
					     ~@body)))

(defstruct GUI 
  :all-people 
  :frame 
  :left-panel 
  :right-panel 
  :pane 
  :person-list 
  :person-scroll-pane 
  :time-table 
  :time-scroll-pane 
  :menu-bar 
  :file-menu)

(def interface (ref nil))

(defn refresh-people []
  (let [projects (project/extract (get/fetch-projects))
	people (let [people (person/extract (get/fetch-people))] 
		 (for [person people]
		   (let [times (time/extract (get/fetch-times (:id person)))
			 times (for [time times]
				 (assoc time :project-name 
					(:name (project/find-with-id projects (:project-id time)))))]
		     (assoc person :times times))))]
    (dosync 
     (ref-set all-people people))))

(defn show-times-for [person]
  (let [times (:times person)
	time-table (:time-table @interface)
	model (.getModel time-table)]
    (loop []
      (when (not= 0 (.getRowCount model))
	(.removeRow model 0)
	(recur)))
    (doseq [time times]
      (.addRow model (to-array [(:date time) (:hours time) 
				(:project-name time) (:description time)])))
    (.revalidate time-table)))

(defn setup-file-menu [_interface]
  (doto (:file-menu _interface)
    (.add (doto (JMenuItem. "Refresh")
	    (.addActionListener 
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(JOptionPane/showMessageDialog (:frame _interface) 
							       "you pressed refresh!"))))))
    (.addSeparator)
    (.add (doto (JMenuItem. "Export...")
	    (.addActionListener
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(JOptionPane/showMessageDialog (:frame _interface)
							       "you pressed export!"))))))
    (.add (doto (JMenuItem. "Notify...")
	    (.addActionListener
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(JOptionPane/showMessageDialog (:frame _interface)
							       "you pressed notify!"))))))))

(defn setup-time-table [_interface]
  (let [time-table (:time-table _interface)]
    (doto (.getModel time-table)
      (.addColumn "Date")
      (.addColumn "Hours")
      (.addColumn "Project")
      (.addColumn "Description"))
    (.. time-table (getColumnModel) (getColumn 0) (setMaxWidth 75))
    (.. time-table (getColumnModel) (getColumn 1) (setMaxWidth 50))    
    (.. time-table (getColumnModel) (getColumn 2) (setMaxWidth 200))
    (doto time-table
      (.setSortOrder "Date" SortOrder/DESCENDING))))

(defn setup-person-list [_interface]
  (let [person-list (:person-list _interface)
	people @all-people]
    (doto (:person-list _interface)
      (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
      (.addListSelectionListener
       (proxy [ListSelectionListener] []
	 (valueChanged [e]
		       (when (not (.getValueIsAdjusting e))
			 (try
			  (show-times-for (nth people (.getSelectedIndex person-list)))
			  (catch Exception e
			    (JOptionPane/showMessageDialog (:time-table _interface) e))))))))))

(defn run []
  (let [people @all-people
	_interface
	(struct-map GUI
	  :frame (JFrame. )
	  :left-panel (JPanel. (MigLayout.))
	  :right-panel (JPanel. (MigLayout.))
	  :pane (JSplitPane. )
	  :person-list (JList. (DefaultListModel.))
	  :person-scroll-pane (JScrollPane.)
	  :time-table (JXTable. (DefaultTableModel.))
	  :time-scroll-pane (JScrollPane.)
	  :menu-bar (JMenuBar.)
	  :file-menu (JMenu. "File"))]

    (doto (:person-scroll-pane _interface)
      (.setViewportView (:person-list _interface)))
    (doto (:time-scroll-pane _interface)
      (.setViewportView (:time-table _interface)))
    (doto (:menu-bar _interface)
      (.add (:file-menu _interface)))
    (setup-file-menu _interface)
    (doto (:left-panel _interface)
      (.add (JLabel. "People") "wrap")
      (.add (:person-scroll-pane _interface) "grow,push"))
    (doto (:right-panel _interface)
      (.add (JLabel. "Times") "wrap")
      (.add (:time-scroll-pane _interface) "grow,push"))
    (doseq [person people]
      (let [model (.getModel (:person-list _interface))]
	(.addElement model (str (:firstname person) " " (:lastname person)))))
    (setup-time-table _interface)
    (setup-person-list _interface)
    (doto (:pane _interface)
      (.setLeftComponent (:left-panel _interface))
      (.setRightComponent (:right-panel _interface))
      (.setDividerLocation 150))
    (doto (:frame _interface)
      (.setJMenuBar (:menu-bar _interface))
      (.. (getContentPane) (add (:pane _interface)))
      (.setSize 750 500)
      (.setVisible true))
    (dosync (ref-set interface _interface))))