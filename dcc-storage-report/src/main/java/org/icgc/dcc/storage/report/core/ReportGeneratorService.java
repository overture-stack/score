package org.icgc.dcc.storage.report.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.report.model.ReportRow;
import org.icgc.dcc.storage.report.model.ReportType;
import org.icgc.dcc.storage.report.model.log.AuditEvent;
import org.icgc.dcc.storage.report.model.reference.ObjectEntry;
import org.icgc.dcc.storage.report.model.reference.TokenEntry;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


@Slf4j
public class ReportGeneratorService {

	public String _outputDir;
	public boolean _consoleFlag;
	
	private static DateTimeFormatter _fmt = DateTimeFormat.forPattern("MM-dd-yyyy");

	private static String _runDate = _fmt.print(new DateTime().getMillis());
	private static String _reportFileExt = ".txt";
	
	public Map<String, TokenEntry> _tokenDigests = new HashMap<String, TokenEntry>();
	public Map<String, ObjectEntry> _objectData = new HashMap<String, ObjectEntry>();
	
	public AuditReportDAO _dao = new AuditReportDAO();

	public ReportGeneratorService(@NonNull String outputDir, boolean consoleFlag) {
		_outputDir = outputDir;
		_consoleFlag = consoleFlag;
	}
	
	@SneakyThrows
	public void init(String schemaFile) {
		_dao.init(schemaFile);
	}

	public void loadTokenDigests(@NonNull List<TokenEntry> tokens) {
		_dao.insertTokens(tokens);
	}
	
	public void loadObjectData(@NonNull List<ObjectEntry> objectEntries) {
		_dao.insertObjects(objectEntries);		
	}
	
	public void getObjectTypeCounts() {
		val objCounts = _dao.getObjectTypes();
		for (val k : objCounts.keySet()) {
			System.out.println(String.format("%s: %d", k, objCounts.get(k)));	
		}
	}
	
	public void loadEvents(@NonNull List<AuditEvent> events) {
		val start = System.currentTimeMillis();
		_dao.insertEvents(events);
		val end = System.currentTimeMillis();
		System.out.println(String.format("Insert time: %d ms", (end - start)));

		if (_consoleFlag) {
			_dao.verify();
		}
	}

	public String generateOutputFileName(@NonNull String reportType) {
		val fnameBuilder = new StringBuilder(reportType.toLowerCase()).append("_").append(_runDate).append(_reportFileExt);
		return fnameBuilder.toString();
	}
	
	private File getOutputFile(@NonNull String outputFileName) {
		return new File(_outputDir, outputFileName);
	}

	public void generateReports(List<ReportType> reportTypes) {
	  
		for (val report : reportTypes) {
			switch (report) {
			case BY_DATE:
				reportCountByDate(generateOutputFileName(report.toString()));
				break;
				
			case BY_OBJECT:
				reportCountByObject(generateOutputFileName(report.toString()));
				break;
				
			case BY_OBJECT_BY_USER:
				reportCountByObjectByUser(generateOutputFileName(report.toString()));
				break;
				
			case BY_USER:
				reportCountByUser(generateOutputFileName(report.toString()));
				break;
				
			case TEST:
				reportCount();
				break;
				
				default:
					//System.out.println("Bypassing all");
					log.info("Bypassing all");
					break;	
			}
		}
	}
	
	/*
	 * Report methods
	 */
	private void reportCountByObject(String outputFileName) {
		val result = _dao.countByObject();
		result.sort(new Comparator<ReportRow>() {

			public int compare(ReportRow o1, ReportRow o2) {
				return o1.getEventDate().compareTo(o2.getEventDate());
			}

		});

		BufferedWriter writer = null;
		try {
			val rptFile = getOutputFile(outputFileName);
			log.info("Writing output to: " + rptFile.getCanonicalPath());

			writer = new BufferedWriter(new FileWriter(rptFile));

			val heading = String.format("Date\tType\tObjectId\tFileFormat\tCount\n");
			writer.write(heading);
			for (val row : result) {
				val line = String.format("%s\t%s\t%s\t%s\t%s\n", _fmt.print(row.getEventDate()), row.getEventType(),
						row.getObjectId(), row.getFileFormat(), row.getCount());
				writer.write(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	private void reportCountByObjectByUser(@NonNull String outputFileName) {
		val result = _dao.countByObjectByUser();
		result.sort(new Comparator<ReportRow>() {

			public int compare(ReportRow o1, ReportRow o2) {
				return o1.getObjectId().compareTo(o2.getObjectId());
			}

		});

		BufferedWriter writer = null;
		try {
			val rptFile = getOutputFile(outputFileName);
			log.info("Writing output to: " + rptFile.getCanonicalPath());

			writer = new BufferedWriter(new FileWriter(rptFile));

			val heading = String.format("ObjectId\tFileFormat\tUser\tType\tCount\n");
			writer.write(heading);
			for (val row : result) {
				val line = String.format("%s\t%s\t%s\t%s\t%s\n", row.getObjectId(), row.getFileFormat(), row.getUserName(), row.getEventType(), row.getCount());
				writer.write(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	private void reportCountByDate(@NonNull String outputFileName) {
		val result = _dao.countByDate();
		result.sort(new Comparator<ReportRow>() {

			public int compare(ReportRow o1, ReportRow o2) {
				return o1.getEventDate().compareTo(o2.getEventDate());
			}

		});

		// write output to text file
		BufferedWriter writer = null;
		try {
			val rptFile = getOutputFile(outputFileName);
			log.info("Writing output to: " + rptFile.getCanonicalPath());

			writer = new BufferedWriter(new FileWriter(rptFile));

			val heading = String.format("Date\tType\tUser\t(Deleted)\tCount\n");
			writer.write(heading);
			for (val row : result) {
				val line = String.format("%s\t%s\t%s\t(%b)\t%s\n", _fmt.print(row.getEventDate()), row.getEventType(),
						row.getUserName(), row.isDeleted(), row.getCount());
				writer.write(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	private void reportCountByUser(@NonNull String outputFileName) {
		val result = _dao.countByDateByTypeByUser();
		result.sort(new Comparator<ReportRow>() {

			public int compare(ReportRow o1, ReportRow o2) {
				return o1.getUserName().compareTo(o2.getUserName());
			}

		});

		BufferedWriter writer = null;
		try {
			val rptFile = getOutputFile(outputFileName);
			log.info("Writing output to: " + rptFile.getCanonicalPath());

			writer = new BufferedWriter(new FileWriter(rptFile));

			val heading = String.format("User\tType\tDate\tCount\n");
			writer.write(heading);
			for (val row : result) {
				val line = String.format("%s\t%s\t%s\t%s\n", row.getUserName(), row.getEventType(),
						_fmt.print(row.getEventDate()), row.getCount());
				writer.write(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	private void reportCount() {
		int count = _dao.count();
		System.out.println("Quick count: " + count);
	}
	
	public void shutdownDb() {
		_dao.shutdown();
	}
	
	public void dumpTokenDigests() {
		for (val k : _tokenDigests.keySet()) {
			val entry = _tokenDigests.get(k);
			val digest = String.format("%s = %s, %s", k, entry.getTokenMD5(), entry.getUserName());
			System.out.println(digest);
		}
	}
}