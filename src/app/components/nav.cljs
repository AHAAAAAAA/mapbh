(ns app.components.nav
  (:require [app.routes :as routes]
            [app.events :as events]
            [re-frame.core :as rf]))

(defn top-en
  []
  [:nav.navbar.is-fixed-top
   [:div.navbar-brand
    [:a.nav-logo.is-vcentered {:href (routes/url-for :home)}
     [:p.column.logo {:style {:color :black
                              :z-index 2}} "mapBH"]]]
   [:div.navbar-menu.is-active {:style {:font-size "0.8em"}}
    [:div.navbar-start.is-vcentered
     [:a.navbar-item {:href (routes/url-for :about)} "About"]
     [:a.navbar-item {:href (routes/url-for :article-index)} "Articles"]
     [:a.navbar-item {:href (routes/url-for :contribute)} "Contribute"]
     [:a.navbar-item {:href (routes/url-for :map)} "Map"]
     [:a.navbar-item {:style {:font-family "Amiri, Scheherazade, serif" :display :flex :align-items :center}
                      :on-click #(rf/dispatch [::events/set-language "ar"])} "العربية"]]]])


(defn top-ar
  []
  [:nav.navbar.is-fixed-top {:lang "ar" :direction "rtl"}
   [:div.navbar-brand
    [:a.nav-logo.is-vcentered {:href (routes/url-for :home)}
     [:p.column.logo {:style {:color :black
                              :font-family "Comfortaa"
                              :z-index 2}} "mapBH"]]]
   [:div.navbar-menu.is-active {:style {:font-size "1.1em"}}
    [:div.navbar-end
     [:a.navbar-item {:style {:font-family "Roboto, Helvetica, san serif" :font-size "0.8em" :display :flex :align-items :center}
                      :on-click #(rf/dispatch [::events/set-language "en"])} "English"]
     [:a.navbar-item {:href (routes/url-for :about)} "نبذة"]
     [:a.navbar-item {:href (routes/url-for :article-index)} "مقالات"]
     [:a.navbar-item {:href (routes/url-for :contribute)} "ساهم"]
     [:a.navbar-item {:href (routes/url-for :map)} "الخارطة"]]]])

(defn top
  [language]
  (condp = language
    :ar [top-ar]
    :en [top-en]
    [top-ar]))

(defn footer-en
  []
  [:footer.footer
    [:div.content.has-text-centered
     [:span.icon [:a {:style {:color :black}
                      :href "https://twitter.com/map_bh"} [:i.fab.fa-twitter]]]
     [:span.icon [:a {:style {:color :black}
                      :href "https://github.com/AHAAAAAAA/mapbh"} [:i.fab.fa-github]]]
     [:span.icon [:a {:style {:color :black}
                      :href "mailto:mapbh.org@gmail.com"} [:i.fas.fa-envelope]]]
     [:span.icon [:a {:style {:color :black}
                      :href "https://instagram.com/map_bh"} [:i.fab.fa-instagram]]]]])

(defn footer-ar
  []
  [:footer.footer {:lang "ar" :direction "rtl"}
    [:div.content.has-text-centered
     [:span.icon [:a {:style {:color :black}
                      :href "https://twitter.com/map_bh"} [:i.fab.fa-twitter]]]
     [:span.icon [:a {:style {:color :black}
                      :href "https://github.com/AHAAAAAAA/mapbh"} [:i.fab.fa-github]]]
     [:span.icon [:a {:style {:color :black}
                      :href "mailto:mapbh.org@gmail.com"} [:i.fas.fa-envelope]]]
     [:span.icon [:a {:style {:color :black}
                      :href "https://instagram.com/map_bh"} [:i.fab.fa-instagram]]]]])

(defn footer
  [language]
  (condp = language
    :ar [footer-ar]
    :en [footer-en]
    [footer-ar]))
