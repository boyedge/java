package com.pubnub.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncedObject {
    private Pubnub pubnub;
    private SyncedObjectManager syncedObjectManager;

    private String path;
    private String objectID;
    private String location;
    private AtomicBoolean isReady;

    public final static String TYPE_LIST = "List";
    public final static String TYPE_OBJECT = "Object";
    public final static String TYPE_INTEGER = "Integer";
    public final static String TYPE_STRING = "String";
    public final static String TYPE_BOOLEAN = "Boolean";

    public SyncedObject(SyncedObjectManager manager, String objectID) {
        this(manager, objectID, "");
    }

    public SyncedObject(SyncedObjectManager manager, String objectID, String path) {
        this.syncedObjectManager = manager;
        this.pubnub = manager.getPubnub();
        this.objectID = objectID;
        this.path = path;
        this.location = SyncedObject.glue(objectID, path);
        this.isReady = new AtomicBoolean(false);
    }

    public String getPath() {
        return path;
    }

    public String getObjectID() {
        return objectID;
    }

    public String getLocation() {
        return location;
    }

    public Boolean getIsReady() {
        return isReady.get();
    }

    public void setIsReady(Boolean isReady) {
        this.isReady.set(isReady);
    }

    /**
     * Getter for string endpoint value
     *
     * @param relativePath to value
     * @return value as string
     */
    public String getString(String relativePath) {
        try {
            return getValue(relativePath).toString();
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Getter for integer endpoint value
     *
     * @param relativePath to value
     * @return value as int
     */
    public Integer getInteger(String relativePath) {
        try {
            return new Integer(Integer.parseInt(getValue(relativePath).toString()));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Getter for boolean endpoint value
     *
     * @param relativePath to value
     * @return value as boolean
     */
    public Boolean getBoolean(String relativePath) {
        try {
            return (Boolean) getValue(relativePath);
        } catch (JSONException e) {
            return null;
        }
    }

    public ArrayList getList() {
        return getList(null);
    }

    public ArrayList getList(String relativePath) {
        try {
            return (ArrayList) getValue(relativePath);
        } catch (JSONException e) {
            return null;
        }
    }

    public HashMap getMap() {
        return getMap(null);
    }

    public HashMap getMap(String relativePath) {
        try {
            return (HashMap) getValue(relativePath);
        } catch (JSONException e) {
            return null;
        }
    }

    private Object getValue(String relativePath) throws JSONException {
        return syncedObjectManager.getValue(glue(objectID, glue(path, relativePath)));
    }

    public Object pop() {
        try {
            String lastKey = syncedObjectManager.lastListKey(location);
            Object result = syncedObjectManager.getValue(glue(location, lastKey));
            remove(lastKey);
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    public Object shift() {
        try {
            String firstKey = syncedObjectManager.firstListKey(location);
            Object result = syncedObjectManager.getValue(glue(location, firstKey));
            remove(firstKey);
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    public Object removeByIndex(Integer index) {
        String key = getKeyByIndex(index);

        if (getKeyByIndex(index) != null) {
            try {
                Object result = syncedObjectManager.getValue(glue(location, key));
                remove(key);
                return result;
            } catch (JSONException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public Object replaceByIndex(Integer index, Object data) {
        String key = getKeyByIndex(index);

        if (getKeyByIndex(index) != null) {
            try {
                Object result = syncedObjectManager.getValue(glue(location, key));
                replace(key, data);
                return result;
            } catch (JSONException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public Object getByIndex(Integer index) {
        String key = getKeyByIndex(index);

        if (getKeyByIndex(index) != null) {
            try {
                return syncedObjectManager.getValue(glue(location, key));
            } catch (JSONException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getKeyByIndex(Integer index) {
        try {
            JSONObject value = syncedObjectManager.getRawValue(location);
            if (isPnList(value)) {
                Integer i = 0;
                Iterator valueIterator = value.sortedKeys();
                while (valueIterator.hasNext()) {
                    if (index.equals(i)) {
                        return (String) valueIterator.next();
                    } else {
                        valueIterator.next();
                        i++;
                    }
                }
                return null;
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    public Object removeByKey(String key) {
        try {
            Object value = syncedObjectManager.getValue(glue(location, key));
            remove(key);
            return value;
        } catch (JSONException e) {
            return null;
        }
    }

    public Object replaceByKey(String key, Object data) {
        try {
            Object value = syncedObjectManager.getValue(glue(location, key));
            replace(key, data);
            return value;
        } catch (JSONException e) {
            return null;
        }
    }

    public Object removeByValue(Object searchValue) {
        String key = getKeyByValue(searchValue);

        if (key != null) {
            Object result;
            try {
                result = syncedObjectManager.getValue(glue(location, key));
            } catch (JSONException e) {
                return null;
            }
            remove(key);
            return result;
        } else {
            return null;
        }
    }

    public Object replaceByValue(Object searchValue, Object data) {
        String key = getKeyByValue(searchValue);

        if (key != null) {
            Object result;
            try {
                result = syncedObjectManager.getValue(glue(location, key));
            } catch (JSONException e) {
                return null;
            }
            replace(key, data);
            return result;
        } else {
            return null;
        }
    }

    public String getKeyByValue(Object searchValue) {
        if (searchValue == null) return null;

        try {
            JSONObject value = syncedObjectManager.getRawValue(location);
            Iterator valueIterator = value.keys();
            JSONObject currentObject;
            String currentKey;
            String resultKey = null;

            while (valueIterator.hasNext()) {
                currentKey = (String) valueIterator.next();
                currentObject = value.getJSONObject(currentKey);
                if (currentObject.has("pn_val") && currentObject.get("pn_val").equals(searchValue)) {
                    resultKey = currentKey;
                    break;
                }
            }

            return resultKey;
        } catch (JSONException e) {
            return null;
        }
    }

    public String getType() {
        return getType("");
    }

    public String getType(String path) {
        try {
            JSONObject value = syncedObjectManager.getRawValue(glue(location, path));

            if (value.has("pn_val")) {
                Object rawValue = value.get("pn_val");
                if (rawValue instanceof String) {
                    return TYPE_STRING;
                } else if (rawValue instanceof Integer) {
                    return TYPE_INTEGER;
                } else if (rawValue instanceof Boolean) {
                    return TYPE_BOOLEAN;
                } else {
                    return null;
                }
            } else if (SyncedObject.isPnList(value)) {
                return TYPE_LIST;
            } else {
                return TYPE_OBJECT;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    public Integer size() {
        return size("");
    }

    public Integer size(String path) {
        try {
            JSONObject value = syncedObjectManager.getRawValue(glue(location, path));
            if (!value.has("pn_val")) {
                return value.length();
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Return child synced object.
     *
     * @param relativePath - relative path
     * @param callback     - callback
     * @return child object
     */
    public SyncedObject child(String relativePath, DataSyncCallback callback) {
        return syncedObjectManager.add(objectID, glue(path, relativePath), callback);
    }

    public SyncedObject child(String relativePath) {
        return child(relativePath, null);
    }

    public void merge(String path, Object data) {
        merge(path, data, new Callback() {
        });
    }

    public void merge(String path, Object data, final Callback callback) {
        Hashtable args = new Hashtable();


        args.put("location", glue(location, path));
        args.put("data", data);

        pubnub.merge(args, callback);
    }

    public void replace(String path, Object data) {
        replace(path, data, new Callback() {
        });
    }

    public void replace(String path, Object data, Callback callback) {
        Hashtable args = new Hashtable();

        args.put("location", glue(location, path));
        args.put("data", data);

        pubnub.replace(args, callback);
    }

    public void push(String path, Object data) {
        push(path, data, new Callback() {
        });
    }

    public void push(String path, Object data, Callback callback) {
        Hashtable args = new Hashtable();

        args.put("location", glue(location, path));
        args.put("data", data);

        pubnub.push(args, callback);
    }

    public void push(String path, Object data, String key) {
        push(path, data, key, new Callback() {
        });
    }

    public void push(String path, Object data, String key, Callback callback) {
        Hashtable args = new Hashtable();

        args.put("location", glue(location, path));
        args.put("data", data);
        args.put("sort_key", key);

        pubnub.push(args, callback);
    }

    public void remove(String path) {
        remove(path, new Callback() {
        });
    }

    public void remove(String path, Callback callback) {
        Hashtable args = new Hashtable();

        args.put("location", glue(location, path));

        pubnub.remove(args, callback);
    }

    public void unsubscribe() {
        this.syncedObjectManager.unsubscribe(location);
    }

    public static String getURLizedObjectPath(String location) {
        String[] path = PubnubUtil.splitString(location, ".");

        for (int i = 0; i < path.length - 1; i++) {
            path[i] = PubnubUtil.urlEncode(path[i]);
        }

        return PubnubUtil.joinString(path, "/");
    }

    public static String glue(String first, String second) {
        if (PubnubUtil.isPresent(first) && PubnubUtil.isPresent(second)) {
            return first + "." + second;
        } else if (PubnubUtil.isPresent(first) && PubnubUtil.isBlank(second)) {
            return first;
        } else if (PubnubUtil.isBlank(first) && PubnubUtil.isPresent(second)) {
            return second;
        } else {
            return "";
        }
    }

    public static boolean isPnList(JSONObject item) {
        if (item == null) {
            return false;
        }

        JSONArray itemNames = item.names();

        for (int i = 0; i < itemNames.length(); i++) {
            try {
                if (itemNames.getString(i).indexOf("-") == 0 && itemNames.getString(i).indexOf("!") > 0)
                    return true;
            } catch (JSONException e) {
                return false;
            }
        }

        return false;
    }
}