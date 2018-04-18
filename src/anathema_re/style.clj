(ns anathema-re.style
  (:require [garden.core :as g]
            [garden.def :as gd]
            [garden.media :as gm]
            [garden.color :as gc]
            [garden.arithmetic :as ga]
            [garden.selectors :as gs]
            [clojure.string :as str]
            [clojure.set :as set]
            [garden.stylesheet :as gss :refer [at-media]]
            [garden.types :as gt]))


(gd/defcssfn url)
(gd/defcssfn blur)
(gd/defcssfn calc)
(gd/defcssfn src)
(gd/defcssfn linear-gradient)

(defn supports [support-statement garden-seq]
  (fn [previous-css] (str previous-css "\n\n\n" "@Supports (" support-statement ") {\n     " (g/css garden-seq) "}")))

(defn grid-area-strings [& stringers]
  (reduce str
          (map
            (fn [a] (str "\n\"" a "\""))
            stringers)))

(defn name-or-string [thing]
  (try
    (name thing)
    (catch Exception e
      (str thing))))

(defn name-if-not-symbol [thing]
  (if (symbol? thing) thing (name-or-string thing)))

(def opmap {- " - " + " + " * " * " / " / "})

(defn replace-operators [possible-op]
  (if-let [oper (get opmap possible-op)]
    oper
    possible-op))

(defn calchelper [& args]
  (let [calcstring (->> args (map replace-operators) (map name-or-string) (map #(str % " ")) (reduce str) str/trim)]
    (calc calcstring)))

(defn unit-fn-for [unittype]
  (fn [n] (str (double n) unittype)))

(def -px (unit-fn-for "px"))
(def -em (unit-fn-for "em"))
(def -% (unit-fn-for "%"))

(defn quoth [n]
  (str "/'" n "/'"))

(def colors
  {:titlebar-back (gc/rgba 0, 0, 0, 0.64)
   :titlebar-text (gc/rgba 1, 1, 1, 1)})

(def color-p-main (gc/hex->rgb "#795548"))
(def color-p-light (gc/hex->rgb "#a98274"))
(def color-p-lighter (gc/lighten color-p-light 10))
(def color-p-dark (gc/hex->rgb "#4b2c20"))
(def color-p-darker (gc/darken color-p-dark 0.5))
(def color-brightest (gc/from-name :white))
(def color-text-bright (gc/hex->rgb "#e0e0e0"))
(def color-darkest (gc/from-name :black))
(def color-off-dark (gc/lighten color-darkest 0.2))

(def brown {:main               "#795578"
            :accent             "#FFC7B3"
            :element-active     "#A67563"
            :element-lighter    "#8C6354"
            :element-darker     "#66483D"
            :background-lighter "#4D362E"
            :background-darker  "#33241F"})

(def primary-color (gc/from-name "goldenrod"))
(def compliment-color (gc/hex->rgb "#075d97"))

(def gbb-opac 0.5)
(def title-bg-color (assoc (gc/lighten primary-color 0) :alpha gbb-opac))

(def radial-gold-gradient "radial-gradient(ellipse farthest-corner at bottom, #FEDB37 0%, #FDB931 8%, #9f7928 30%, #8A6E2F 40%, transparent 80%),\n
radial-gradient(ellipse farthest-corner at top, #FFFFFF 0%, #FFFFAC 8%, #D1B464 25%, #5d4a1f 62.5%, #5d4a1f 100%)")
(def gold-gradient-bottom (linear-gradient "to top"
                                           [(gc/hex->rgb "#FEDB37") "0%"]
                                           [(gc/hex->rgb "#FDB931") "8%"]
                                           [(gc/hex->rgb "#9f7928") "30%"]
                                           [(gc/hex->rgb "#8A6E2F") "40%"]
                                           ["transparent" "80%"]))
(def gold-gradient-top (linear-gradient "to bottom"
                                        [(gc/hex->rgb "#FFFFFF") "0%"]
                                        [(gc/hex->rgb "#FFFFAC") "8%"]
                                        [(gc/hex->rgb "#D1B464") "25%"]
                                        [(gc/hex->rgb "#5d4a1f") "62.5%"]
                                        [(gc/hex->rgb "#5d4a1f") "100%"]))
(def depth-gradient (linear-gradient
                      (assoc (gc/lighten primary-color 40) :alpha gbb-opac)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc title-bg-color :alpha 0.3)
                      (assoc (gc/darken primary-color 40) :alpha gbb-opac)))

(def title-background-image [(url "../img/brushed_metal.png")
                             depth-gradient
                             gold-gradient-bottom
                             gold-gradient-top])
(def section-title-gradient [(url "../img/brushed_metal.png")
                             depth-gradient
                             gold-gradient-bottom
                             gold-gradient-top])

title-bg-color

(def menu-background-image
  (into [(linear-gradient
           (assoc (gc/lighten primary-color 25) :alpha 0.5)
           (assoc (gc/darken primary-color 20) :alpha 0.5))]
        title-background-image))

(def input-background [(linear-gradient
                         (assoc (gc/lighten primary-color 20) :alpha gbb-opac)
                         (assoc (gc/lighten primary-color 20) :alpha gbb-opac)
                         (assoc (gc/lighten primary-color 30) :alpha gbb-opac)
                         (assoc (gc/lighten primary-color 40) :alpha gbb-opac))
                       (url "../img/brushed_metal.png")])

(def button-bar-background [(linear-gradient
                              (assoc (gc/darken primary-color 20) :alpha 0.3)
                              (assoc (gc/darken primary-color 20) :alpha 0.5)
                              (assoc (gc/darken primary-color 15) :alpha 0.7)
                              (assoc (gc/darken primary-color 15) :alpha 0.6)
                              (assoc (gc/darken primary-color 20) :alpha 0.5)
                              (assoc (gc/darken primary-color 50) :alpha 0.3))
                            (url "../img/brushed_metal.png")])



(def title-color (gc/lighten primary-color 10))


(defn prefix-it [stylekey stylerule]
  (let [prefixes ["moz" "webkit" "o"]
        stylekeys (map (fn [a] (keyword (str "-" a "-" (name stylekey))))
                       prefixes)]
    (into {} (map #(vector % stylerule) stylekeys))))

(def title-bar-height "3em")
(def navshadow "0 0 15px black")
(def elementshadow (str "0 0 6px darkgray")) ;#6d6d6d
(def minor-button-shadow (str "0 0 4px " (gc/as-hex (gc/desaturate (gc/darken (gc/mix (gc/complement primary-color) primary-color) 20) 20)))) ;#6d6d6d
(def inputshadow (str "inset " elementshadow))
(def buttonshadow (str "0 -2px 10px" (gc/as-hex color-p-dark)))
(def focusshadow (str "0 -3px 5px" (gc/as-hex color-p-dark)))
(def focusshadowtext (str "0 -5px 10px" (gc/as-hex color-p-dark)))
(def section-inner-shadow (str "inset " navshadow))
(def section-title-shadow "0 0 5px black")
(def menu-shadow (str "inset " section-title-shadow))
(def title-text-shadow (str "0 0 10px " (gc/as-hex (gc/darken primary-color 10))))



(def page-content-margin-scalar 7)
(def page-content-margin (keyword (str page-content-margin-scalar "px")))
(def standard-field-width (calchelper :100% - :80px - :10px - :2em - page-content-margin - page-content-margin))

(gs/defselector td "td")
(gs/defselector tr "tr")


(defn add-generated-statement [csser]
  (str "/*This file is automatically generated. Any changes made will be overwritten by dev/gardener.clj*/\n\n\n" csser))


(def main-style
  [[:* {:margin 0
        :padding 0
        :font-family "opendyslexic, sans-serif"
        :font-weight :normal
        :font-size :13px}]
   [:#goog-user {:z-index 500
                     :position :fixed
                     :top :5px
                     :right :10px
                     :boder :solid
                     :border-radius :10px}
    [:.user-button
     [:img {:width :40px
            :height :40px
            :border-radius :40px}]]
    [:.abcRioButton {:border-radius :10px}]]
   [:html {:height  (calchelper :100% - :20px)
           :background-image (url "../img/solar_bg.jpg")
           :background-repeat :no-repeat
           :background-attachment :fixed
           :background-position :right
           :background-size :cover}]
   [:body {:height :100%
           :width :100%
           :position :relative}]
   [:h1 :h2 :h3 :h4 :h5 :h6
    {:font-size   :25px
     :font-family "Envision, serif"
     :font-weight :bold
     :color       (gc/darken compliment-color 45)}
    [:p {:font-weight :normal}]]
   [:#app {:width :100%}]
   [:#app-frame {:top          0
                 :left         0
                 :height       :100%
                 :margin-right 0
                 ;:margin-bottom :-40px
                 :width        :100% ;(calchelper :100% - :10px)
                 :position     :relative}
    [:.page-title {:position         :fixed
                   :top              0
                   :padding-top      :0px
                   :padding-bottom   :10px
                   :width            :100%
                   :height           :40px
                   :font-size        :37px
                   :background-image (url "/img/light_blue_back.jpg")
                   :background-position [:center, :bottom]
                   :background-size :cover
                   ;:text-shadow      title-text-shadow
                   :text-align       :center
                   :box-shadow elementshadow
                   ;:border-bottom    :ridge
                   ;:border-width     :3px
                   ;:border-color     primary-color
                   :z-index          110}
     [:h1 {:font-size :inherit}]]
    [:#menu {:margin "0 auto"
             :text-align :justify-all
             :color :white
             ;:text-shadow navshadow
             :display :block
             :position :fixed
             :height :36px
             :width :100%
             ;:top :45px
             :background-image (url "/img/blue_back.jpg")
             :background-size :cover
             :background-position [:center :top]
             :box-shadow elementshadow
             :bottom :0px
             :left :0px
             :transition [:left :0.5s]
             :z-index 50}
     [:&:hover {:left :0%
                :bottom :0px}]
     [:ul {:position :relative
           :overflow :hide
           :height :100%
           :padding-left :15px}
      [:li {:display       :inline-block
            ;:float :right
            ;:border-bottom-style :groove
            :border-bottom-color (-> (gc/as-hsl compliment-color)
                                     (assoc :saturation 20)
                                     (assoc :lightness 30))
            ;:border-bottom-width :1px
            :font-size     :20px
            ;:width (calchelper :100% - :30px)
            :text-align :center
            :width :17%
            :padding       :4px}
       [:&:last {:border :none}]
       [:&:before {:content "\"\""}]
       [:span.label {:display :none}]
       [:.menu-icon {:font-size :27px
                     :text-align :center
                     :vertical-align :middle}]]]]

    [:#content {:position :relative}
     ;:top      :0px
     ;:margin-top :45px
     ;:left     :40px
     ;:width    :100%}
     [:.page {:position :relative
              :margin-top :60px
              :margin-bottom :40px}
      [:.section {;:background-image (url "../img/canvas_paper.png")
                  ;:border-width     :1px
                  :border-color     primary-color
                  :position         :relative
                  :width            :100% ;(calchelper :100% - :20px)
                  ;:box-shadow       elementshadow
                  :z-index          10
                  :padding-top      0
                  :margin-bottom    :10px}
       [:h3 {:background-image    (url "/img/gold_back.jpg")
             :font-size           :27px
             :background-position [:bottom :center]
             :background-size     :cover
             ;:position         :sticky
             :top                 :50px
             :padding-top         :2px
             :margin-right        :10px
             :margin-left         :10px
             :box-shadow          elementshadow
             ;:width (calchelper :100% - :20px)
             ;:height (calchelper :100% + :10px)
             :padding             :5px}]
       [:.interior {:padding :10px
                    :padding-top :3px}
        [:img {:width :100%}]
        [:ul
         [:li {:list-style :none} ;:border :solid}
          [:* {:display :block
               :width :100%}]]]]]]]]
   [:input :select :textarea {:background-color :transparent ;(gc/rgba 255 255 255 0.0)
                              :background-image (url "/img/blue_text_back.png")
                              :background-position :center
                              :background-size :cover
                              ;:border-radius :9px
                              :box-shadow       inputshadow
                              ;:border-bottom    :solid
                              ;:border-style :double
                              :border           :none
                              :border-width     :1px
                              :border-color     :grey
                              :border-left      :none
                              :border-right     :none
                              :border-top       :none
                              :height           (-% 75)
                              :margin-bottom    (-px 3)
                              :margin-left      (-px -5)
                              :padding          :5px
                              :padding-right    0
                              :vertical-align   :bottom}
    ;:margin-left (-px 3)}
    [:&:focus {:outline          :none
               :box-shadow       elementshadow
               :border-width     :3px
               :border-bottom    :double
               :margin-bottom    (-px 1)
               :background-color color-text-bright}]]])

(defn compile-style []
  (add-generated-statement (g/css main-style)))