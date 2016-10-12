package wzr.com.localpictureloader.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 用于跟踪一次事件,事件可以由其他子事件组成，最终组成一颗树
 * Created by wuzr on 2016/10/9.
 */
public class EventLog {
    private static HashMap<String, Event> sRootEventByKey = new HashMap<>();

    public static class Event {
        String eventKey;
        String message;
        ArrayList<Event> children = new ArrayList<>();
        Event parent;
    }

    public interface IPrinter {
        void print(Event e);
    }

    public static String createEvent(String key) {
        return createEvent(key, null);
    }

    public static String createEvent(String key, Event parent) {
        if (key == null || key.equals("")) {
            throw new IllegalArgumentException("key不能为空");
        }
        Event e = sRootEventByKey.get(key);
        if (e != null) {
            throw new IllegalArgumentException("key" + key + "已被使用");
        }
        e = new Event();
        e.eventKey = key;
        if (parent != null) {
            parent.children.add(e);
            e.parent = parent;
        } else {
            sRootEventByKey.put(key, e);
        }
        return key;
    }

    public static void addMarker(String key, String msg) {
        Event e = findEvent(key);
        if (e == null) {
            throw new IllegalArgumentException("event不存在");
        }
        if (e.message == null || e.message.equals("")) {
            e.message = msg;
        } else {
            e.message = e.message + "->" + msg;
        }
    }

    private static Event findEvent(String key) {
        Event e = sRootEventByKey.get(key);
        if (e == null) {
            for (String k : sRootEventByKey.keySet()) {
                for (Event child : sRootEventByKey.get(k).children) {
                    if (child.eventKey.equals(key)) {
                        return child;
                    }
                }
            }
        }
        return e;
    }

    public static void print(String key, boolean isRemove) {
        print(key, isRemove, null);
    }

    public static void print(String key, boolean isRemove, IPrinter printer) {
        Event e = findEvent(key);
        if (printer != null) {
            printer.print(e);
            return;
        }

        Log.d(key, e.message);
        defaultPrint(key, e);
        if (isRemove) {
            if (sRootEventByKey.containsKey(key)) {
                sRootEventByKey.remove(key);
            } else {
                if (e.parent != null) {
                    e.parent.children.remove(e);
                }
            }
        }
    }

    private static void defaultPrint(String key, Event e) {
        if (e == null) {
            throw new IllegalArgumentException("event不存在");
        }
        if (e.children.size() == 0) {
            return;
        }
        for (Event child : e.children) {
            if (child.parent != null) {
                Log.d(key, child.parent.eventKey + "::" + child.eventKey + ":" + child.message);
            } else {
                Log.d(key, child.message);
            }
            defaultPrint(key, child);
        }
    }

    public static void print(String key) {
        print(key, true);
    }
}
