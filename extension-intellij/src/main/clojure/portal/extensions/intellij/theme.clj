(ns portal.extensions.intellij.theme
  (:import
   (com.intellij.openapi.editor DefaultLanguageHighlighterColors)
   (com.intellij.openapi.editor.colors TextAttributesKey EditorColorsManager ColorKey)
   (java.awt Color)))

(def ^:private language-colors
  {:INLINE_PARAMETER_HINT               DefaultLanguageHighlighterColors/INLINE_PARAMETER_HINT
   :INLINE_REFACTORING_SETTINGS_DEFAULT DefaultLanguageHighlighterColors/INLINE_REFACTORING_SETTINGS_DEFAULT
   :LABEL                               DefaultLanguageHighlighterColors/LABEL
   :REASSIGNED_PARAMETER                DefaultLanguageHighlighterColors/REASSIGNED_PARAMETER
   :GLOBAL_VARIABLE                     DefaultLanguageHighlighterColors/GLOBAL_VARIABLE
   :MARKUP_ENTITY                       DefaultLanguageHighlighterColors/MARKUP_ENTITY
   :DOC_COMMENT_LINK                    DefaultLanguageHighlighterColors/DOC_COMMENT_LINK
   :INLAY_TEXT_WITHOUT_BACKGROUND       DefaultLanguageHighlighterColors/INLAY_TEXT_WITHOUT_BACKGROUND
   :INLINE_PARAMETER_HINT_HIGHLIGHTED   DefaultLanguageHighlighterColors/INLINE_PARAMETER_HINT_HIGHLIGHTED
   :PARENTHESES                         DefaultLanguageHighlighterColors/PARENTHESES
   :TEMPLATE_LANGUAGE_COLOR             DefaultLanguageHighlighterColors/TEMPLATE_LANGUAGE_COLOR
   :FUNCTION_DECLARATION                DefaultLanguageHighlighterColors/FUNCTION_DECLARATION
   :REASSIGNED_LOCAL_VARIABLE           DefaultLanguageHighlighterColors/REASSIGNED_LOCAL_VARIABLE
   :FUNCTION_CALL                       DefaultLanguageHighlighterColors/FUNCTION_CALL
   :CONSTANT                            DefaultLanguageHighlighterColors/CONSTANT
   :MARKUP_TAG                          DefaultLanguageHighlighterColors/MARKUP_TAG
   :VALID_STRING_ESCAPE                 DefaultLanguageHighlighterColors/VALID_STRING_ESCAPE
   :HIGHLIGHTED_REFERENCE               DefaultLanguageHighlighterColors/HIGHLIGHTED_REFERENCE
   :DOT                                 DefaultLanguageHighlighterColors/DOT
   :IDENTIFIER                          DefaultLanguageHighlighterColors/IDENTIFIER
   :DOC_COMMENT_MARKUP                  DefaultLanguageHighlighterColors/DOC_COMMENT_MARKUP
   :STATIC_FIELD                        DefaultLanguageHighlighterColors/STATIC_FIELD
   :DOC_COMMENT_TAG                     DefaultLanguageHighlighterColors/DOC_COMMENT_TAG
   :INLAY_DEFAULT                       DefaultLanguageHighlighterColors/INLAY_DEFAULT
   :COMMA                               DefaultLanguageHighlighterColors/COMMA
   :MARKUP_ATTRIBUTE                    DefaultLanguageHighlighterColors/MARKUP_ATTRIBUTE
   :INSTANCE_METHOD                     DefaultLanguageHighlighterColors/INSTANCE_METHOD
   :INLINE_REFACTORING_SETTINGS_FOCUSED DefaultLanguageHighlighterColors/INLINE_REFACTORING_SETTINGS_FOCUSED
   :DOC_COMMENT                         DefaultLanguageHighlighterColors/DOC_COMMENT
   :NUMBER                              DefaultLanguageHighlighterColors/NUMBER
   :BRACKETS                            DefaultLanguageHighlighterColors/BRACKETS
   :INLINE_PARAMETER_HINT_CURRENT       DefaultLanguageHighlighterColors/INLINE_PARAMETER_HINT_CURRENT
   :INLINE_REFACTORING_SETTINGS_HOVERED DefaultLanguageHighlighterColors/INLINE_REFACTORING_SETTINGS_HOVERED
   :INTERFACE_NAME                      DefaultLanguageHighlighterColors/INTERFACE_NAME
   :BRACES                              DefaultLanguageHighlighterColors/BRACES
   :INSTANCE_FIELD                      DefaultLanguageHighlighterColors/INSTANCE_FIELD
   :KEYWORD                             DefaultLanguageHighlighterColors/KEYWORD
   :CLASS_NAME                          DefaultLanguageHighlighterColors/CLASS_NAME
   :OPERATION_SIGN                      DefaultLanguageHighlighterColors/OPERATION_SIGN
   :DOC_COMMENT_GUIDE                   DefaultLanguageHighlighterColors/DOC_COMMENT_GUIDE
   :LOCAL_VARIABLE                      DefaultLanguageHighlighterColors/LOCAL_VARIABLE
   :LINE_COMMENT                        DefaultLanguageHighlighterColors/LINE_COMMENT
   :METADATA                            DefaultLanguageHighlighterColors/METADATA
   :SEMICOLON                           DefaultLanguageHighlighterColors/SEMICOLON
   :CLASS_REFERENCE                     DefaultLanguageHighlighterColors/CLASS_REFERENCE
   :PARAMETER                           DefaultLanguageHighlighterColors/PARAMETER
   :STRING                              DefaultLanguageHighlighterColors/STRING
   :STATIC_METHOD                       DefaultLanguageHighlighterColors/STATIC_METHOD
   :PREDEFINED_SYMBOL                   DefaultLanguageHighlighterColors/PREDEFINED_SYMBOL
   :DOC_COMMENT_TAG_VALUE               DefaultLanguageHighlighterColors/DOC_COMMENT_TAG_VALUE
   :BLOCK_COMMENT                       DefaultLanguageHighlighterColors/BLOCK_COMMENT
   :INVALID_STRING_ESCAPE               DefaultLanguageHighlighterColors/INVALID_STRING_ESCAPE})

(defn- format-color [^Color color]
  (str "rgb(" (.getRed color) "," (.getGreen color) "," (.getBlue color) ")"))

(defn- get-color
  ([key]
   (get-color (.getGlobalScheme (EditorColorsManager/getInstance)) key))
  ([^EditorColorsManager theme key]
   (some->
    (if-let [attrs (language-colors key)]
      (cond
        (instance? ColorKey attrs)
        (.getColor theme ^ColorKey attrs)
        (instance? TextAttributesKey attrs)
        (some-> theme (.getAttributes ^TextAttributesKey attrs) .getForegroundColor))
      (.getColor theme (ColorKey/createColorKey (name key))))
    format-color)))

(defn resolve-theme [m]
  (reduce-kv
   (fn [out k v]
     (assoc out k (get-color v)))
   {}
   m))

(def theme-mapping
  {:portal.colors/background  :CONSOLE_BACKGROUND_KEY
   :portal.colors/background2 :CARET_ROW_COLOR
   :portal.colors/boolean     :LABEL
   :portal.colors/border      :VISUAL_INDENT_GUIDE
   :portal.colors/diff-add    :ADDED_LINES_COLOR
   :portal.colors/diff-remove :DELETED_LINES_COLOR
   :portal.colors/exception   :FILESTATUS_UNKNOWN
   :portal.colors/keyword     :LABEL
   :portal.colors/namespace   :INTERFACE_NAME
   :portal.colors/number      :NUMBER
   :portal.colors/package     :INSTANCE_METHOD
   :portal.colors/string      :STRING
   :portal.colors/symbol      :IDENTIFIER
   :portal.colors/tag         :VALID_STRING_ESCAPE
   :portal.colors/text        :CONSTANT
   :portal.colors/uri         :METADATA})

(defn get-theme [] (resolve-theme theme-mapping))

(defn- get-keys
  ([]
   (get-keys (.getGlobalScheme (EditorColorsManager/getInstance))))
  ([^EditorColorsManager theme]
   (concat
    (keys language-colors)
    (for [key (.getColorKeys theme)]
      (keyword (.getExternalName key))))))

(comment
  (tap> (get-theme))
  (tap> (get-keys))
  (tap> (get-color :CONSOLE_BACKGROUND_KEY))
  (tap> (into {} (map (juxt identity get-color)) (get-keys))))
