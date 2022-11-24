package org.iushu.market.component;

import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public class FastJsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    @Override
    protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
        return null;
    }

    @Override
    protected void writeInternal(Object object, Type type, Writer writer) throws Exception {

    }
}
