package com.powsybl.dataframe.network;

public class ExtensionInformation {
    private final String id;
    private final String description;
    private final String attributes;

    public ExtensionInformation(String id, String description, String attributes) {
        this.id = id;
        this.description = description;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getAttributes() {
        return attributes;
    }
}
