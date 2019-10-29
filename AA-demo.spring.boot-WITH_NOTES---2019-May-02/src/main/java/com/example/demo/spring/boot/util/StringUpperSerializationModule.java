package com.example.demo.spring.boot.util;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Expose this class as bean to influence generic serialization behavior
 * 
 * @author KunjPrasad
 *
 */
// either activate annotation for special case (currently done);
// ..Or expose a "module" bean for general case (currently disabled)
public class StringUpperSerializationModule extends SimpleModule {

    private static final long serialVersionUID = 8355921487099557490L;

    public StringUpperSerializationModule() {
        this.addSerializer(String.class, new StringUpperSerializer());
    }
}
