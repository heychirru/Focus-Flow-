package com.focusflowai.model;

public class Tag {
    public Long id;
    public String name;
    public String color;

    @Override
    public String toString() {
        return name;
    }
}
