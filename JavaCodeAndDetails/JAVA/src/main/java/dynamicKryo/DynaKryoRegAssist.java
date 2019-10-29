package dynamicKryo;

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.esotericsoftware.kryo.Kryo;

/**
 * Thi class contains structures that help in dynamic registration
 * 
 * @author Kunj
 *
 */
public class DynaKryoRegAssist {

    private List<Class> regList;
    private WeakHashMap<NewKryo, Object> listenerMap;
    private Object obj;

    public DynaKryoRegAssist(List<Class> regList, WeakHashMap<NewKryo, Object> listenerMap) {
        this.regList = regList;
        this.listenerMap = listenerMap;
        this.obj = new Object();
    }

    public void addClass(Class cls) {
        synchronized (obj) {
            regList.add(cls);
            for (NewKryo kryo : listenerMap.keySet()) {
                kryo.register(cls);
            }
        }
    }

    public void addKryo(NewKryo kryo) {
        synchronized (obj) {
            listenerMap.put(kryo, null);
            for (Class cls : regList) {
                kryo.register(cls);
            }
        }
    }
}
