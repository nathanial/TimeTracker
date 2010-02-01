(ns basecamp.gui
  (:use basecamp.gutil)
  (:require [basecamp.person :as person] [basecamp.get :as get]
	    [basecamp.time :as time]
	    [basecamp.project :as project]
	    [basecamp.chart :as chart])
  (:import (javax.swing JFrame JPanel JLabel JButton JDialog JPasswordField
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
	   (net.miginfocom.swing MigLayout)))

(UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(defmacro swing [& body]
  `(javax.swing.SwingUtilities/invokeLater (fn []
					     ~@body)))

(defstruct GUI 
  :frame 
  :left-panel 
  :right-panel 
  :pane 
  :person-list 
  :person-scroll-pane 
  :time-table 
  :time-scroll-pane 
  :menu-bar 
  :file-menu
  :view-menu
  :show-all-users)

(def all-people (ref []))

(def interface (ref nil))

(def refresh-dialog (agent nil))

(def selected-person (ref nil))

(declare refresh-person-list show-times-for)

(defn show-refresh-progress []
  (swing
    (let [dialog (JDialog. )
	  panel (JPanel. (MigLayout. ))
	  progress (JProgressBar. )]
      (doto progress
	(.setIndeterminate true))
      (doto panel
	(.add (JLabel. "Refreshing Data") "wrap, align 50%")
	(.add progress "push,grow"))
      (.. dialog (getContentPane) (add panel))
      (send refresh-dialog (fn [_] dialog))
      (doto dialog
	(.setSize (Dimension. 200 100))
	(move-to-center)
	(.show)))))

(defn refresh-people []
  (show-refresh-progress)
  (let [projects (project/extract (get/fetch-projects))
	people (let [people (person/extract (get/fetch-people))
		     times (time/extract (get/fetch-times))] 
		 (for [person people]
		   (let [times (for [time times]
				 (assoc time :project-name 
					(:name (project/find-with-id projects (:project-id time)))))]
		     (assoc person :times 
			    (filter #(= (:person-id %) (:id person)) times)))))]
    (dosync 
     (ref-set all-people people))
    (when (not= nil @interface)
      (swing
	(refresh-person-list)
	(let [p @selected-person]
	  (show-times-for (person/find-by-name (:firstname p) (:lastname p) @all-people)))))
    (send refresh-dialog  #(swing (.hide %)))))

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

(defn show-export-dialog []
  (let [dialog (JDialog. )
	panel (JPanel. (MigLayout.))]
    (.. dialog (getContentPane) (add panel))
    (doto panel
      (.add (JLabel. "Export")))
    (doto dialog
      (.setSize (Dimension. 100 100))
      (move-to-center)
      (.show))))

(defn show-refresh-dialog []
  (let [dialog (JDialog. )
	panel (JPanel. (MigLayout.))]
    (.. dialog (getContentPane) (add panel))
    (doto panel
      (.add (JLabel. "Refresh")))
    (.setSize dialog (Dimension. 100 100))
    (.show dialog)))

(defn create-person-table [people]
  (let [model (proxy [DefaultTableModel] []
		(isCellEditable [row col] (= col 2)))
	table (proxy [JXTable] [model]
		(getColumnClass [col]
				(if (= 2 col)
				  (Class/forName "java.lang.Boolean")
				  (Class/forName "java.lang.Object"))))]
    (.addTableModelListener model 
			    (proxy [TableModelListener] []
			      (tableChanged [e]
					    (let [col (.getColumn e)
						  first-row (.getFirstRow e)
						  last-row (.getLastRow e)]
					      (when (> first-row 0) nil)
))))
    (doto model
      (.addColumn "Name" (into-array (map (fn [p] (str (:firstname p) " " (:lastname p))) people)))
      (.addColumn "Email" (into-array (map :email people)))
      (.addColumn "Notify" (into-array (map (fn [x] false) people))))
    (.. table (getColumnModel) (getColumn 2) (setMaxWidth 75))
    (.revalidate table)
    table))

(defn show-email-dialog []
  (let [people @all-people
	dialog (JDialog. (:frame @interface))
	panel (JPanel. (MigLayout. ))
	person-table (create-person-table people)
	person-scroll-pane (JScrollPane. person-table)]
    (.. dialog (getContentPane) (add panel))
    (doto panel
      (.add person-scroll-pane "grow,push,wrap")
      (.add (JButton. "Notify...") "align right"))
    (.revalidate person-table)
    (doto dialog
      (.setSize (Dimension. 400 300))
      (move-to-center)
      (.show))))

(defn setup-file-menu [_interface]
  (doto (:file-menu _interface)
    (.add (doto (JMenuItem. "Refresh")
	    (.addActionListener 
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(doto (Thread. (fn [] (refresh-people)))
				  (.start)))))))
    (.addSeparator)
    (.add (doto (JMenuItem. "Export...")
	    (.addActionListener
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(show-export-dialog))))))
    (.add (doto (JMenuItem. "Email...")
	    (.addActionListener
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(show-email-dialog))))))))

(defn toggle-show-all-users []
  (dosync 
   (let [value (:show-all-users @interface)]
     (alter interface assoc :show-all-users (not value))))
  (refresh-person-list))

(defn show-time-pie-chart []
  (let [person @selected-person]
    (if (not= nil person)
      (chart/show-time-pie-chart @selected-person)
      (JOptionPane/showMessageDialog (:frame @interface) "You must select a person over there in the person list before you can graph anything"))))

(defn setup-view-menu [_interface]
  (doto (:view-menu _interface)
    (.add (doto (JCheckBoxMenuItem. "Show All Users")
	    (.addActionListener 
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(toggle-show-all-users))))))
    (.addSeparator)
    (.add (doto (JMenuItem. "Graph...")
	    (.addActionListener
	     (proxy [ActionListener] []
	       (actionPerformed [e]
				(show-time-pie-chart))))))))

(defn setup-time-table [_interface]
  (let [time-table (:time-table _interface)
	model (proxy [DefaultTableModel] []
		(isCellEditable [row col] false))]
    (.setModel time-table model)
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

(defn refresh-person-list []
  (let [_interface @interface
	person-list (:person-list _interface)
	people @all-people
	show-all-users (:show-all-users _interface)
	model (.getModel person-list)]
    (.clear model)
    (doseq [person people]
      (when (or show-all-users (not= 0 (count (:times person))))
	(.addElement model (str (:firstname person) " " (:lastname person)))))))

(defn setup-person-list [_interface]
  (let [person-list (:person-list _interface)
	people @all-people
	show-all-users (:show-all-users _interface)
	model (.getModel person-list)]
    (doseq [person people]
      (when (or show-all-users (not= 0 (count (:times person))))
	(.addElement model (str (:firstname person) " " (:lastname person)))))
    (doto (:person-list _interface)
      (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
      (.addListSelectionListener
       (proxy [ListSelectionListener] []
	 (valueChanged [e]
		       (when (not (.getValueIsAdjusting e))
			 (let [people @all-people
			       selected (.getSelectedIndex person-list)]
			   (when (and (<= 0 selected) (< selected (count people)))
			     (let [[firstname lastname] (.split (.getSelectedValue person-list) " ")
				   person (person/find-by-name firstname lastname people)]
			       (dosync 
				(ref-set selected-person person))
			       (show-times-for person)))))))))))

(defn login []
  (let [dialog (JDialog. )
	panel (JPanel. (MigLayout. "ins 10 10 0 10"))
	username (JTextField.)
	password (JPasswordField.)
	sign-in (doto (JButton. "Sign in")
		  (.addActionListener (proxy [ActionListener] []
					(actionPerformed [e]
							 (.hide dialog)))))]
    (doto panel
      (.add (JLabel. "Username") "wrap")
      (.add username  "wrap,growx,pushx")
      (.add (JLabel. "Password") "wrap")
      (.add password "wrap,growx,pushx")
      (.add sign-in "spanx 2, wrap"))
    (.. dialog (getContentPane) (add panel))
    (doto dialog
      (.setModal true)
      (.setTitle "Basecamp Login")
      (.setSize (Dimension. 250 200))
      (move-to-center)
      (.show))
    [(.getText username) (.getText password)]))

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
	  :file-menu (JMenu. "File")
	  :view-menu (JMenu. "View")
	  :show-all-users false)]

    (doto (:person-scroll-pane _interface)
      (.setViewportView (:person-list _interface)))
    (doto (:time-scroll-pane _interface)
      (.setViewportView (:time-table _interface)))
    (doto (:menu-bar _interface)
      (.add (:file-menu _interface))
      (.add (:view-menu _interface)))
    (setup-file-menu _interface)
    (setup-view-menu _interface)
    (doto (:left-panel _interface)
      (.add (JLabel. "People") "wrap")
      (.add (:person-scroll-pane _interface) "grow,push"))
    (doto (:right-panel _interface)
      (.add (JLabel. "Times") "wrap")
      (.add (:time-scroll-pane _interface) "grow,push"))
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
      (move-to-center)
      (.setVisible true))
    (dosync (ref-set interface _interface))
    nil))