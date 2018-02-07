
(ns app.updater
  (:require [app.schema :as schema]
            [respo.cursor :refer [mutate]]
            [bisection-key.core :refer [bisect max-id min-id]]))

(defn add-after [store task-id op-id]
  (let [base-task (get-in store [:tasks task-id])
        base-sort-id (:sort-id base-task)
        all-sort-ids (->> (:tasks store) (vals) (map :sort-id) (sort))
        sort-id-after (first (filter (fn [x] (> x base-sort-id)) all-sort-ids))
        new-sort-id (bisect base-sort-id (or sort-id-after max-id))
        new-task (merge schema/task {:id op-id, :sort-id new-sort-id})]
    (-> store (assoc-in [:tasks op-id] new-task) (update :pointer inc))))

(defn add-before [store task-id op-id]
  (let [base-task (get-in store [:tasks task-id])
        base-sort-id (:sort-id base-task)
        all-sort-ids (->> (:tasks store) (vals) (map :sort-id) (sort))
        sort-id-before (last (filter (fn [x] (< x base-sort-id)) all-sort-ids))
        new-sort-id (bisect (or sort-id-before min-id) base-sort-id)
        new-task (merge schema/task {:id op-id, :sort-id new-sort-id})]
    (-> store (assoc-in [:tasks op-id] new-task))))

(defn delete-task [store op-data]
  (let [[task-id idx] op-data]
    (if (= 1 (count (:tasks store)))
      store
      (-> store
          (update :tasks (fn [tasks] (dissoc tasks task-id)))
          (update :pointer (fn [pointer] (if (zero? idx) 0 (dec pointer))))))))

(defn move-task [store op-data]
  (let [[from-id to-id before?] op-data
        tasks (:tasks store)
        from-task (get tasks from-id)
        to-task (get tasks to-id)
        base-sort-id (:sort-id to-task)
        all-sort-ids (->> (:tasks store) (vals) (map :sort-id))
        new-sort-id (if before?
                      (bisect
                       (or (last (filter (fn [x] (< x base-sort-id)) all-sort-ids)) min-id)
                       base-sort-id)
                      (bisect
                       base-sort-id
                       (or (first (filter (fn [x] (> x base-sort-id)) all-sort-ids)) max-id)))
        new-pointer (.indexOf
                     (sort
                      (conj (disj (set all-sort-ids) (:sort-id from-task)) new-sort-id))
                     new-sort-id)]
    (-> store (assoc-in [:tasks from-id :sort-id] new-sort-id) (assoc :pointer new-pointer))))

(defn shorten-tasks [store]
  (-> store
      (update
       :tasks
       (fn [tasks]
         (let [next-tasks (->> tasks
                               (filter (fn [[task-id task]] (not (:done? task))))
                               (into {}))]
           (if (empty? next-tasks) (:tasks schema/store) next-tasks))))
      (assoc :pointer 0)))

(defn updater [store op op-data op-id op-time]
  (case op
    :states (update store :states (mutate op-data))
    :task/add-before (add-before store op-data op-id)
    :task/add-after (add-after store op-data op-id)
    :task/edit (let [[task-id text] op-data] (assoc-in store [:tasks task-id :text] text))
    :task/toggle (update-in store [:tasks op-data :done?] not)
    :task/clear schema/store
    :task/shorten (shorten-tasks store)
    :task/delete (delete-task store op-data)
    :task/move (move-task store op-data)
    :pointer/touch (assoc store :pointer op-data)
    :pointer/before (if (zero? (:pointer store)) store (update store :pointer dec))
    :pointer/after
      (if (= (:pointer store) (dec (count (:tasks store)))) store (update store :pointer inc))
    store))