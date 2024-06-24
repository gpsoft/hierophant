(ns hierophant.gui
  (:import
   [java.awt SystemTray TrayIcon PopupMenu MenuItem]
   [java.awt.event ActionListener]
   [javax.imageio ImageIO]))

(defn show-tasktray-icon
  ([]
   (show-tasktray-icon "Hierophant"))
  ([caption]
   (let [tray (SystemTray/getSystemTray)
         icon-image (-> (Thread/currentThread)
                        (.getContextClassLoader)
                        (.getResourceAsStream "icon.png")
                        (ImageIO/read))
         menu (PopupMenu.)
         exit-cmd (MenuItem. "Exit")
         exit-listener (reify ActionListener
                         (actionPerformed [_ _] (System/exit 0)))
         _ (.addActionListener exit-cmd exit-listener)
         _ (.add menu exit-cmd)
         icon (TrayIcon. icon-image caption menu)]
     (.add tray icon))))

(comment



 )

