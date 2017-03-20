package org.icgc.dcc.storage.report.model;

public enum ReportType {
	BY_DATE,
  BY_USER,
  BY_OBJECT,
  BY_OBJECT_BY_USER,
  TEST,
  BYPASS;

  public static ReportType fromString(String str) {
      for(ReportType report : ReportType.values()) {
          if(report.toString().equalsIgnoreCase(str)) {
              return report;
          }
      }
      return null;
  }
}
