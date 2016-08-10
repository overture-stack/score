package org.icgc.dcc.storage.audit.cli;

import org.icgc.dcc.storage.audit.model.report.ReportType;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class ReportConverter implements IStringConverter<ReportType> {

  @Override
  public ReportType convert(String value) {
      ReportType convertedValue = ReportType.fromString(value);

      if(convertedValue == null) {
          throw new ParameterException("Did not recognize " + value + " as a Report type.");
      }
      return convertedValue;
  }

}
