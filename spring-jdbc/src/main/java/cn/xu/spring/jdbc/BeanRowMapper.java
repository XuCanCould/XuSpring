package cn.xu.spring.jdbc;

import cn.xu.spring.exception.DataAccessException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * created by Xu on 2024/5/24 16:38.
 */
public class BeanRowMapper<T> implements RowMapper<T> {

    Logger logger =  LoggerFactory.getLogger(getClass());

    Class<T> clazz;

    Constructor<T> constructor;

    Map<String, Field> fields = new HashMap<>();

    Map<String, Method> methods = new HashMap<>();

    public BeanRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        try {
            this.constructor = clazz.getDeclaredConstructor();
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("No public default constructor found for class %s when build BeanRowMapper",
                    clazz.getName(), e));
        }
        // 处理属性和set方法
        for (Field f : clazz.getFields()) {
            String name = f.getName();
            this.fields.put(name, f);
            logger.atDebug().log("Add row mapping: {} to field {}", name, name);
        }
        for (Method m : clazz.getMethods()) {
            Parameter[] parameters = m.getParameters();
            if (parameters.length == 1) {
                String name = m.getName();
                if (name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    methods.put(prop, m);
                    logger.atDebug().log("Add row mapping: {} to method {}({})", prop, name, parameters[0].getType().getSimpleName());
                }
            }
        }
    }

    @Nullable
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean;
        try {
            bean = this.constructor.newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            for (int i = 1; i <= columns; i++) {
                String label = meta.getColumnLabel(i);
                Method method = methods.get(label);
                if (method != null) {
                    method.invoke(bean, rs.getObject(label));
                } else {
                    Field field = fields.get(label);
                    if (field != null) {
                        field.set(bean, rs.getObject(label));
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("Could not map result set to class %s", this.clazz.getName()), e);
        }
        return bean;
    }
}
