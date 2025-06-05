package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    // For global
    Environment() {
        enclosing = null;
    }

    //For local
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // If the variable isn't found in the envi, we try in the closing one.
        if(enclosing != null) return enclosing.get(name);

        throw new RunTimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // Works similar to get()
        if(enclosing != null){
            enclosing.assign(name,value);
            return;
        }

        throw new RunTimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }
}
