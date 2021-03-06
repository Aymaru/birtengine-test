package org.smartnow.birtengine.engine.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Report {
    private String title;
    private String name;
    private List<Parameter> parameters;

    public Report(String title, String name) {
        this.title = title;
        this.name = name;
        this.parameters = new ArrayList<Report.Parameter>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String title;
        private String name;
        private ParameterType type;

    }

    public enum ParameterType {
        INT, STRING
    }
}