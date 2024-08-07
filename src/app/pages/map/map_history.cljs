(ns app.pages.map.map-history
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [app.model :as model]
            [app.pages.map.map-data :refer [layers ar-layers base-satellite]]))


(defn text
  [details ar-details arabic?]
  (get
   {:en {:description (merge {:panel "Description"
                              :title-header "Title"
                              :scale-header "Scale"
                              :notes-header "Notes"
                              :source-header "Source"
                              :issuer-header "Issuer"
                              :submitter-header "Submitted by"} details)
         :buttons {:switch-mode {:transparency "Transparency Mode"
                                 :split "Split Mode"}
                   :description "Description"}}
    :ar {:description (merge {:panel  "تفاصيل"
                              :title-header "العنوان"
                              :scale-header "مقياس"
                              :notes-header "ملاحظات"
                              :source-header  "المصدر"
                              :issuer-header "الناشر"
                              :submitter-header " مساهمة"} ar-details)
         :buttons {:switch-mode {:transparency "شفاف"
                                 :split "ابو قسمين"}
                   :description  "تفاصيل"}}} (if arabic? :ar :en)))


(defn modal-description
  [state* arabic?]
  (let [details (get-in layers (:selected @state*))
        ar-details (merge details (get-in ar-layers (:selected @state*)))
        txt (:description (text details ar-details arabic?))]
    [:div.modal {:id "modal-description" :lang (if arabic? "ar" "en") :dir (if arabic? "rtl" "ltr")}
     [:div.modal-content
      [:p.panel-block [:strong (:title-header txt)] ": "
       (:title txt)]
      [:div.panel-block [:strong (:scale-header txt)] ": " (:scale txt)]
      (when (:description txt)
        [:p.panel-block.description-text
         (:description txt)])
      [:p.panel-block.description-text
       [:strong (:notes-header txt)] ": " (:notes txt)]
      [:a {:href (:source-link details) :style {:color "#DA291C"}}
       [:div.panel-block {:style {:color "#DA291C"}} [:strong (:source-header txt)] ": " (:source txt) " - "
        [:span.icon.home [:i.fas.fa-download]] "(georectified)"]]
      (when (:issuer txt)
        [:a {:href (:issuer-link txt)}
         [:div.panel-block {:style {:color "#DA291C" :padding-bottom "10px"}}
          [:strong (:issuer-header txt)] ": " (str " " (:issuer txt)) " - "
          [:span.icon.home [:i.fas.fa-download]] "(original)"]])
      (when (:submitted-by txt)
        [:div.panel-block {:style {:padding-bottom "10px"}} [:strong (:submitter-header txt)] ": "
         (if (:submitted-by-url details)
           [:a {:href (:submitted-by-url txt)}
            (str " " (:submitted-by txt))]
           (str " " (:submitted-by txt)))])
      [:button.modal-close.is-large.is-danger {:aria-label "close"
                                               :style (merge {} (if arabic? {:left "0"} {:right "0"}))
                                               :on-click (fn [e] (swap! state* update :show-description? not)
                                                           (-> js/document (.getElementById "modal-description") .-classList (.toggle "is-active")))}]]]))

(defn map-container
  []
  [:div#mapid {:style {:height (str "calc(" js/window.screen.availHeight "px - 10rem)")}}])

(defn get-layer
  [state*]
  (if-let [layers (:layers @state*)]
    (get-in layers (:selected  @state*))))


(defn update-transparency
  [layer v]
  (-> layer (.setOpacity (/ v 100))))


(defn transparency-slider
  [state* arabic?]
  (if-let [layer (get-layer state*)]
    (if-let [transparency (-> layer (aget "options") (aget "opacity"))]
      [:input {:title "Adjust Transparency" :style (merge {:position :absolute :background "transparent" :opacity 0.6 :bottom "35px"
                                                           :width "120px"
                                                           :z-index 998}
                                                          (if arabic? {:left "12px"} {:right "12px"}))
               :on-change (fn [e v] (update-transparency layer (.. e -target -value)))
               :class (str "slider " (:selected @state*)) :step 1 :min 0 :max 100 :default-value (* transparency 100) :type "range"}])))


(defn pan-map
  [lat long zoom map]
  (when (and zoom lat long) (.flyTo map (-> js/L (.latLng lat long)) zoom #js {:animate false})))


(defn base-layer-change
  [map state*]
  (.on map "overlayadd" (fn [layer]
                          (let [gname (-> layer js->clj (get "group") (get "name"))
                                lname (-> layer js->clj (get "name"))]
                            (when (and (not= lname "Terrain") (not= lname "Satellite"))
                              (swap! state* assoc :selected [gname lname])))))
  (.on map "baselayerchange" (fn [layer]
                               (let [lname (-> layer js->clj (get "name"))]
                                 (when (or (= lname "Terrain") (= lname "Satellite"))
                                   (swap! state* assoc :base (get (js->clj layer) "name"))))))
  nil)


(defn download-button
  [state*]
  (let [map (:map @state*)]
  ;; Add option to save map
    [:button.button.is-success.is-small.is-light.is-outlined {:style {:position :absolute :height "30px" :border-radius "2px" :font-size "0.6rem" :top "180px" :left "12px" :z-index 997}
                                                    :on-click (fn [] (-> (new js/LeafletExporter. map 4.0) .Export))}
     [:i.fas.fa-download]]))


(defn init-map
  [state*]
  (let [map (:map @state*)]
    (when map (do (.off map) (.remove map)
                  (-> js/document (.getElementById "mapid") (aset "innerHTML" "")))))
  (let [base-layers (->> base-satellite
                         (mapv (fn [[k selected-layer]]
                                 [k (-> js/L (.tileLayer (:url selected-layer) (-> selected-layer :opts clj->js)))]))
                         (into {}))
        process-layers (fn [layers] (->> layers (mapv (fn [[k selected-layer]] [k (-> js/L (.tileLayer (:url selected-layer) (-> selected-layer :opts clj->js)))]))
                                         (sort-by first)
                                         (into (sorted-map))))
        overlay-layers (->> layers (map (fn [[k v]] [k (process-layers v)])) (into (sorted-map)))
        {:keys [zoom lat long]} @state*
        map (-> js/L (.map "mapid" (clj->js {:maxBounds (-> js/L (.latLngBounds (-> js/L (.latLng 25.5 50))
                                                                                (-> js/L (.latLng 26.5 51))))}))

                (.setView #js [26.05 50.4849414] 10.3))
        base (-> base-layers (get (:base @state*)))
        selected (get-in overlay-layers (:selected @state*))]
    ;; Add all layers in control
    (-> js/L .-control (.groupedLayers (clj->js base-layers)
                                       (clj->js overlay-layers)
                                       (clj->js {"exclusiveGroups" (keys overlay-layers)
                                                 "allExclusive" true
                                                 "groupCheckboxes" false
                                                 "groupsCollapsible" true}))
        (.addTo map))

    ;; Add option to locate user
    (-> js/L .-control (.locate (clj->js {:keepCurrentZoomLevel true
                                          :locateOptions (clj->js {:enableHighAccuracy true})
                                          :followMarkerStyle (clj->js {:radius 5 :fillColor "rgb(241, 70, 104, 0.7)" :weight 5})
                                          :markerStyle (clj->js {:radius 5 :fillColor "rgb(241, 70, 104, 0.7)" :weight 5})})) (.addTo map));

     ;; Register handlers to update state with new layer so other components can pull the data
    (when map (base-layer-change map state*))

    ;; Zoom to location
    (pan-map lat long zoom map)

    ;; Add Base
    (when map (-> map (.addLayer base)))
    ;; Add pre-selected default map
    (when map (-> map (.addLayer selected)))
    ;; Store state
    (swap! state* assoc :layers overlay-layers :map map)

    {:base-layers base-layers
     :overlay-layers overlay-layers
     :zoom zoom :lat lat :long long
     :map map
     :base base
     :selected selected}))


(defn transparency-init-map
  [state*]
  (init-map state*))


(defn sbs-init-map
  [state*]
  (let [_  (swap! state* assoc :transparency 1.0)
        {:keys [map base selected]} (init-map state*)]
    ;; Create side-by-side mode
    (-> js/L .-control (.sideBySide base selected) (.addTo map))
    (.setOpacity selected 1.0)))

(defn switch-mode
  [state* arabic?]
  (let [mode (:mode @state*)
        map (:map @state*)
        txt (get-in (text nil nil arabic?) [:buttons :switch-mode])]
    [:button.button.is-danger.is-small.is-rounded
     {:style {:position :absolute :top "65px" :left "60px" :z-index 997 :font-size (when arabic? "105%")}
      :on-click (fn []
                  (let [zoom-level (.getZoom map)
                        {:keys [lat lng]} (js->clj (.getCenter map) :keywordize-keys true)]
                    (swap! state* assoc :zoom zoom-level :lat lat :long lng)
                    (if (= mode "transparency")
                      (do (swap! state* assoc :mode "side-by-side" :transparency 1.0)
                          (sbs-init-map state*))
                      (do (swap! state* assoc :mode "transparency")
                          (transparency-init-map state*)))))}
     (if (= mode "transparency") (:split txt) (:transparency txt))]))

(defn modal-button
  [state* arabic?]
  [:button.button.is-light
   {:style (merge (if arabic? {:right "12px"} {:left "12px"}) {:position :absolute :bottom "23px"  :z-index 997 :font-size (when arabic? "105%")})
    :on-click (fn [e] (swap! state* update :show-description? not)
                (-> js/document (.getElementById "modal-description") .-classList (.toggle "is-active")))}

   [:i.fa.fa-list {:style {:margin-right "1rem"}}]
   (get-in (text nil nil arabic?) [:buttons :description])])


(defn historical-map []
  (let [state* (reagent/atom {:selected ["Bahrain" "1956 - Bahrain"]
                              :mode "transparency"
                              :base "Satellite"
                              :show-description? false
                              :transparency 0.65})
        language* (rf/subscribe [::model/language])]
    (reagent/create-class
     {:component-did-mount
      (fn [] ;; Setup Map
        (if (= "transparency" (:mode @state*))
          (transparency-init-map state*)
          (sbs-init-map state*)))
      :render
      (fn []
        (let [arabic? (= :ar @language*)]
          [:div#map-history {:style {:overflow-y :none}}
           [map-container]
           [modal-button state* arabic?]
           [modal-description state* arabic?]
           [switch-mode state* arabic?]
           (when (= "transparency" (:mode @state*)) [download-button state*])
           (when (= "transparency" (:mode @state*)) [transparency-slider state* arabic?])]))})))
