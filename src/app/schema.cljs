
(ns app.schema )

(def task {:id nil, :text "", :done? false})

(def store {:tasks [(merge task {:id 0, :text ""})], :pointer 0, :shift 0, :states {}})