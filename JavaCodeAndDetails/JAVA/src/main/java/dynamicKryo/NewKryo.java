package dynamicKryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

public class NewKryo extends Kryo {

    private DynaKryoRegAssist assist;

    public NewKryo(DynaKryoRegAssist assist) {
        this.assist = assist;
        // this is a bad pattern from concurrency view - See Joshua Bloch's book - concurrency
        assist.addKryo(this);
    }

    public void registerDynamic(Class type) {
        assist.addClass(type);
    }
}
