package com.fahlberg.model;

import java.util.AbstractMap;

public class StringPair extends AbstractMap.SimpleEntry<String, String> {
    public StringPair(String key, String value) {
        super(key, value);
    }
}