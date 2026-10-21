package com.mx.cbtis.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * Prueba las clases del paquete com.mx.cbtis.modelo (constructores, setters,
 * getters, toString, hashCode y equals) sin depender de los nombres de los campos.
 */
class ModeloReflectionTest {

    private static List<Class<?>> clasesDelModelo() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        List<Class<?>> clases = new ArrayList<Class<?>>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.mx.cbtis.modelo")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            if (!c.isInterface() && !c.isEnum() && !c.isAnnotation() && !Modifier.isAbstract(c.getModifiers())) {
                clases.add(c);
            }
        }
        return clases;
    }

    private static Object valor(Class<?> t) {
        if (t == String.class) return "x";
        if (t == Integer.class || t == int.class) return 1;
        if (t == Long.class || t == long.class) return 1L;
        if (t == Double.class || t == double.class) return 1.0;
        if (t == Float.class || t == float.class) return 1f;
        if (t == Short.class || t == short.class) return (short) 1;
        if (t == Byte.class || t == byte.class) return (byte) 1;
        if (t == Boolean.class || t == boolean.class) return true;
        if (t == Character.class || t == char.class) return 'x';
        if (t == BigDecimal.class) return BigDecimal.ONE;
        return null;
    }

    private static Object[] argumentos(Class<?>[] tipos) {
        Object[] args = new Object[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            Object v = valor(tipos[i]);
            if (v == null && tipos[i].isPrimitive()) {
                return null;
            }
            args[i] = v;
        }
        return args;
    }

    @Test
    @DisplayName("UT-34 Modelo: constructores, getters, setters, toString, hashCode y equals")
    void modelo_constructoresGettersYSetters() throws Exception {
        List<Class<?>> clases = clasesDelModelo();
        assertFalse(clases.isEmpty());

        for (Class<?> clase : clases) {
            // Constructores publicos (con y sin parametros)
            for (Constructor<?> ctor : clase.getConstructors()) {
                Object[] args = argumentos(ctor.getParameterTypes());
                if (args != null) {
                    ctor.newInstance(args);
                }
            }

            Object a = clase.getDeclaredConstructor().newInstance();
            Object b = clase.getDeclaredConstructor().newInstance();

            for (Method m : clase.getMethods()) {
                if (m.getDeclaringClass() == Object.class) {
                    continue;
                }
                // Setters: se invocan y se comprueba el getter correspondiente
                if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
                    Object v = valor(m.getParameterTypes()[0]);
                    if (v == null && m.getParameterTypes()[0].isPrimitive()) {
                        continue;
                    }
                    m.invoke(a, v);
                    String sufijo = m.getName().substring(3);
                    Method getter = null;
                    try {
                        getter = clase.getMethod("get" + sufijo);
                    } catch (NoSuchMethodException e1) {
                        try {
                            getter = clase.getMethod("is" + sufijo);
                        } catch (NoSuchMethodException e2) {
                            getter = null;
                        }
                    }
                    if (getter != null && v != null && getter.getReturnType() == m.getParameterTypes()[0]) {
                        assertEquals(v, getter.invoke(a));
                    }
                }
            }
            // Getters restantes y metodos de Object
            for (Method m : clase.getMethods()) {
                if (m.getDeclaringClass() != Object.class && m.getParameterCount() == 0
                        && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                    m.invoke(a);
                }
            }
            a.toString();
            a.hashCode();
            a.equals(b);
            a.equals(a);
            a.equals(null);
        }
    }
}
