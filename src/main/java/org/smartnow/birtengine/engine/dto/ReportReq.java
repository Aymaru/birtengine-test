package org.smartnow.birtengine.engine.dto;


import lombok.Data;
import lombok.Lombok;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReportReq {
	private String xml_report;
    private String name;

    public ReportReq(String xml_report, String name) {
        this.xml_report = xml_report;
        this.name = name;
    }

}