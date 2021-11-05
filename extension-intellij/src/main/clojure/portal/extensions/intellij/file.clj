(ns portal.extensions.intellij.file
  (:import
   (com.intellij.openapi.application ApplicationManager)
   (com.intellij.openapi.fileEditor OpenFileDescriptor)
   (com.intellij.openapi.project Project)
   (com.intellij.openapi.vfs VirtualFileManager)))

(defn- ->descriptor ^OpenFileDescriptor
  [^Project project {:keys [file line column]}]
  (OpenFileDescriptor.
   project
   (.findFileByUrl
    (VirtualFileManager/getInstance)
    (str "file://" file))
   (if line (dec line) 0)
   (if column (dec column) 0)))

(defn open [^Project project info]
  (.invokeLater
   (ApplicationManager/getApplication)
   (fn []
     (.runReadAction
      (ApplicationManager/getApplication)
      ^Runnable
      (fn []
        (.navigate (->descriptor project info) true))))))
