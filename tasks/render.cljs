
(ns ssr-stages.boot
  (:require
    [respo.alias :refer [html head title script style meta' div link body]]
    [respo.render.html :refer [make-html make-string]]
    [client.comp.container :refer [comp-container]]))

(defn html-dsl [data html-content ssr-stages]
  (make-html
    (html {}
      (head {}
        (title {:attrs {:innerHTML "Pudica Schedule"}})
        (link {:attrs {:rel "icon" :type "image/png" :href "http://logo.mvc-works.org/mvc.png"}})
        (link {:attrs {:rel "stylesheet" :type "text/css" :href "style.css"}})
        (link (:attrs {:rel "manifest" :href "manifest.json"}))
        (meta' {:attrs {:charset "utf-8"}})
        (meta' {:attrs {:name "viewport" :content "width=device-width, initial-scale=1"}})
        (meta' {:attrs {:id "ssr-stages" :content (pr-str ssr-stages)}})
        (style {:attrs {:innerHTML "body {margin: 0;}"}})
        (style {:attrs {:innerHTML "body * {box-sizing: border-box;}"}})
        (style {:attrs {:innerHTML "::-webkit-input-placeholder {color: hsla(0,0%,100%,0.4);}"}})
        (script {:attrs {:id "config" :type "text/edn" :innerHTML (pr-str data)}}))
      (body {}
        (div {:attrs {:id "app" :innerHTML html-content}})
        (script {:attrs {:src "main.js"}})))))

(defn generate-html []
  (let [ tree (comp-container {} #{:shell})
         html-content (make-string tree)]
    (html-dsl {:build? true} html-content #{:shell})))

(defn generate-empty-html []
  (html-dsl {:build? true} "" {}))

(defn spit [file-name content]
  (let [fs (js/require "fs")]
    (.writeFileSync fs file-name content)))

(defn -main []
  (if (= js/process.env.env "dev")
    (spit "target/dev.html" (generate-empty-html))
    (spit "target/index.html" (generate-html))))

(-main)
