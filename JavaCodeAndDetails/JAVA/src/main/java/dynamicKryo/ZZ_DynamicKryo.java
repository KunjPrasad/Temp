package dynamicKryo;

import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class ZZ_DynamicKryo {

    private static final KryoPool kryoPool;

    static {
        final DynaKryoRegAssist assist = new DynaKryoRegAssist(new ArrayList<Class>(),
                new WeakHashMap<NewKryo, Object>());
        KryoFactory factory = new KryoFactory() {
            public Kryo create() {
                System.out.println("CREATING");
                Kryo kryo = new NewKryo(assist);
                // configure kryo instance, customize settings
                return kryo;
            }
        };
        // Build pool "with SoftReferences" enabled (optional)
        kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }

    public static void main(String[] args) throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 3; i++) {
            final int id = i;
            exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    NewKryo k = (NewKryo) kryoPool.borrow();
                    while (true) {
                        System.out.println(id + " -- " + k.getNextRegistrationId());
                        Thread.sleep(500);
                    }
                }
            });
        }

        NewKryo k;

        Thread.sleep(1000);
        k = (NewKryo) kryoPool.borrow();
        System.out.println("[MAIN-1] -- " + k.getNextRegistrationId());
        k.registerDynamic(ClassA.class);
        System.out.println("[MAIN-1] -- " + k.getNextRegistrationId());

        Thread.sleep(1000);
        k = (NewKryo) kryoPool.borrow();
        System.out.println("[MAIN-2] -- " + k.getNextRegistrationId());
        k.registerDynamic(ClassA.class);
        System.out.println("[MAIN-2] -- " + k.getNextRegistrationId());

        Thread.sleep(1000);
        k = (NewKryo) kryoPool.borrow();
        System.out.println("[MAIN-3] -- " + k.getNextRegistrationId());
        k.registerDynamic(ClassB.class);
        System.out.println("[MAIN-3] -- " + k.getNextRegistrationId());
    }

    public static class ClassA {
        int a;
    }

    public static class ClassB {
        int b;
    }
}
