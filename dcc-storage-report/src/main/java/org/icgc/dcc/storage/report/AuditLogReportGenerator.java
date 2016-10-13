package org.icgc.dcc.storage.report;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.thisptr.jackson.jq.JsonQuery;

import org.apache.commons.beanutils.PropertyUtils;
import org.icgc.dcc.storage.report.cli.ReportConverter;
import org.icgc.dcc.storage.report.core.BasicAuthRestTemplate;
import org.icgc.dcc.storage.report.core.ReportGeneratorService;
import org.icgc.dcc.storage.report.model.ReportType;
import org.icgc.dcc.storage.report.model.log.AuditEvent;
import org.icgc.dcc.storage.report.model.log.EventType;
import org.icgc.dcc.storage.report.model.reference.Digests;
import org.icgc.dcc.storage.report.model.reference.ObjectEntry;
import org.icgc.dcc.storage.report.model.reference.TokenEntry;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class AuditLogReportGenerator {

	@Parameter(names = { "-d", "--logdir" }, required = true, description = "Path containing audit log files named *.audit.[YYYY-MM-DD].log")
	public String _logDirectory = "/tmp";
	@Parameter(names = { "-f", "--logfile" }, description = "Name of a single log file to process")
	public String _logFile;
	@Parameter(names = { "-o", "--output-dir" }, description = "Name of output directory")
	public String _outputDir = "/tmp";
	@Parameter(names = { "-j", "--digests" }, description = "Path of JSON file containing MD5-hashed access tokens to use instead of querying auth server directly")
	public String _digestFile;
	@Parameter(names = { "-c", "--console" }, description = "Show H2 db console in browser to verify load")
	public boolean _consoleFlag = false;
	@Parameter(names = { "-r", "--report" }, description = "type of report to generate: BY_DATE, BY_USER, BY_OBJECT, BY_OBJECT_BY_USER. Multiple options can be "
			+ "specified (separated by spaces)", variableArity = true, converter = ReportConverter.class)
	public List<ReportType> _reportTypes = Lists.newArrayList(ReportType.BY_USER);
	@Parameter(names = { "-h", "-?", "--help" }, help = true)
	private boolean help = false;

	private static ObjectMapper _mapper = new ObjectMapper();
	
	// could turn this into a parameter?
	private String _schemaFile = "schema.h2.sql";
	
	public static String OBJECT_ID_SIG = "object id ";
	public static String TOKEN_SIG = "access token ";
	public static String SIZE_OF_SIG = "size of ";
	public static String SOURCE_IP_SIG = " from ";
	public static String CLIENT_VERSION_SIG = " using client version ";

	public static String PORTAL_OBJECT_QUERY = "https://dcc.icgc.org/api/v1/repository/files?filters=%s&include=fields&size=%d&from=%d";
	public static String PORTAL_OBJECT_QUERY_FILTERS = "{\"file\":{\"repoName\":{\"is\":[\"AWS - Virginia\"]}}}";

	private ReportGeneratorService _service = null;

	public AuditLogReportGenerator() {
	}

	public static void main(String[] args) {
		AuditLogReportGenerator app = new AuditLogReportGenerator();

		try {
			app.init(args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
 
		app.loadReferenceData();
		app.loadAuditEvents();
		
		app.generateReports();
		 
		app.shutdown();
		
		System.out.println("Done");
	}

	public void init(@NonNull String[] args) throws IOException, SQLException {
		val cli = new JCommander(this, args);
		cli.setProgramName("AuditLogReportGenerator");
		
		if (help) {
			cli.usage();
			System.exit(0);
		}
		
		_service = new ReportGeneratorService(_outputDir, _consoleFlag);
		_service.init(_schemaFile);
	}

	public void shutdown() {
		_service.shutdownDb();
	}
	
	public void loadReferenceData() {
		System.out.println("Fetching Object Metadata");
		// need to query Portal to distinguish between BAM's and VCF's
		retrieveObjectTypes();

		System.out.println("Fetching Token Digests");
		// need to query Auth server to be able to de-reference token MD5's (stored in audit logs) to user e-mails
		retrieveTokenDigests();
	}
	
	private void retrieveTokenDigests() {
		Map<String, TokenEntry> tokenDigests = Collections.<String, TokenEntry>emptyMap();
		if (_digestFile != null) {
			tokenDigests = loadLocalTokenDigests();
		} else {
			tokenDigests = fetchTokenDigests();	
		}
		
		val tokenList = new ArrayList<TokenEntry>(tokenDigests.values());
		_service.loadTokenDigests(tokenList);
	}

	private void loadAuditEvents() {
		System.out.println("Fetching Audit Events");
		val events = loadLogs();
		events.sort(new Comparator<AuditEvent>() {

			public int compare(AuditEvent o1, AuditEvent o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}

		});

		System.out.println(String.format("%d events loaded from file", events.size()));
		_service.loadEvents(events);
	}

	public Map<String, TokenEntry> loadLocalTokenDigests() {
		Map<String, TokenEntry> result = Collections.<String, TokenEntry>emptyMap();
		val digestFile = FileSystems.getDefault().getPath(_digestFile);
		
		try (val in = Files.newInputStream(digestFile)) {
			result = parseTokenDigests(in);
		} catch (IOException e) {
			log.error("Could not read from " + _digestFile, e);
      System.err.println(e);
		}
		return result;
	}
	
	public Map<String, TokenEntry> fetchTokenDigests() {
		val restTemplate = new BasicAuthRestTemplate("mgmt", "uC~ieg/eiF3x");

		val headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		val entity = new HttpEntity<String>("parameters", headers);

		ResponseEntity<byte[]> response = restTemplate.exchange("https://auth.icgc.org/token-digests", HttpMethod.GET, entity,
				byte[].class);
		val json = response.getBody();

		val bais = new ByteArrayInputStream(json);
		val tokenDigests = parseTokenDigests(bais);
		return tokenDigests;
	}

	public void retrieveObjectTypes() {
		int PAGE_SIZE = 100;
		int itemCount = fetchObjectCount();
		int pages = itemCount / PAGE_SIZE;
		for (int i = 0; i <= pages; i++) {
			fetchObjectTypes(PAGE_SIZE, (i * PAGE_SIZE) + 1);	
		}
		_service.getObjectTypeCounts();
	}

	public void generateReports() {
		for (val e : _reportTypes) {
			log.info("requesting: " + e.toString());
		}
		_service.generateReports(_reportTypes);
	}
	
	public void fetchObjectTypes(int pageSize, int startPos) {
		val restTemplate = new RestTemplate();

		try {
			val constructedQuery = UriUtils.encodeQuery(
					String.format(PORTAL_OBJECT_QUERY, PORTAL_OBJECT_QUERY_FILTERS, pageSize, startPos), "UTF-8");

			ResponseEntity<String> response = restTemplate.getForEntity(constructedQuery, String.class);
			val json = response.getBody();

			val query = JsonQuery.compile(".hits[].fileCopies[] | select(.repoCode == \"aws-virginia\") | "
					+ "{ object_id: .repoFileId, file_format: .fileFormat, file_name: .fileName, file_size: .fileSize}");
			val doc = _mapper.readTree(json);
			val queryResults = query.apply(doc);

			val objectEntries = queryResults.stream().map(node -> {
				try {
					return _mapper.convertValue(node, ObjectEntry.class);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());

			_service.loadObjectData(objectEntries);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int fetchObjectCount() {
		val restTemplate = new RestTemplate();

		try {
			val constructedQuery = UriUtils.encodeQuery(String.format(PORTAL_OBJECT_QUERY, PORTAL_OBJECT_QUERY_FILTERS, 1, 1),
					"UTF-8");

			ResponseEntity<String> response = restTemplate.getForEntity(constructedQuery, String.class);
			val json = response.getBody();

			val objectMapper = new ObjectMapper();
			// actually it is a Map instance with maps-fields within
			val jsonObj = objectMapper.readValue(json, Object.class);
			val totalItems = PropertyUtils.getProperty(jsonObj, "pagination.total");

			return (Integer) totalItems;
		} catch (Exception e) {
			// so many exceptions can get thrown
			throw new RuntimeException(e);
		}
	}

	public List<AuditEvent> loadLogs() {
		val events = new ArrayList<AuditEvent>();
		val dir = FileSystems.getDefault().getPath(_logDirectory);
		try {
			val stream = Files.newDirectoryStream(dir, "*.audit.*.log");
			int fcount = 0;
			for (val path : stream) {
				System.out.print(path.getFileName());
				events.addAll(parseLogFile(path));
				fcount += 1;
			}
			stream.close();
			System.out.println(String.format("%d files processed", fcount));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return events;
	}

	public List<AuditEvent> parseLogFile(Path logFile) {
		List<AuditEvent> result = new ArrayList<AuditEvent>();
		try {
			val file = new RandomAccessFile(logFile.toFile(), "r");
			String line = null;
			int i = 0;
			while ((line = file.readLine()) != null) {
				val event = parseLine(line);
				if (event != null) {
					result.add(event);
				}
				if (++i % 10 == 0) {
					System.out.print(".");
				}
				if (i % 800 == 0) {
					System.out.println();
				}
			}
			file.close();
			System.out.println();
		} catch (IOException exc) {
			System.out.println(exc);
			System.exit(1);
		}
		return result;
	}

	public Map<String, TokenEntry> parseTokenDigests(InputStream stream) {
		val mapper = new ObjectMapper();
		Digests d = null;
		try {
			d = mapper.readValue(stream, Digests.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		val result = new HashMap<String, TokenEntry>();
		if (d != null) {
			for (val token : d.getDigests()) {
				result.put(token.getTokenMD5(), token);
			}
		}
		return result;
	}

	public AuditEvent parseLine(String logLine) {
		val bits = logLine.split(" ", 8);

		// check expected log line length
		if (bits.length < 8) {
			return null;
		}

		// Log Message Type
		val logType = bits[3];
		if (!logType.equalsIgnoreCase("INFO")) {
			return null;
		}

		// Timestamp
		val dt = bits[0] + " " + bits[1];
		val timestamp = parseDateTimestamp(dt);

		String objectId = "";
		String token = "";
		Long size = null;

		// not used at this time
		// val generatingClass = bits[5];

		// log message
		val info = bits[7];
		int idPos = info.indexOf(OBJECT_ID_SIG);
		if (idPos > 0) {
			objectId = info.substring(idPos + OBJECT_ID_SIG.length());
			objectId = (objectId.substring(0, objectId.indexOf(' ')));
		}

		int tokenPos = info.indexOf(TOKEN_SIG);
		if (tokenPos > 0) {
			token = info.substring(tokenPos + TOKEN_SIG.length());
			token = token.substring(0, token.indexOf(' '));
		}

		// Check for newer addition of client version
		String clientVersion = "";
		int clientVerPos = info.indexOf(CLIENT_VERSION_SIG);
		if (clientVerPos > 0) {
			clientVersion = info.substring(clientVerPos + CLIENT_VERSION_SIG.length()).trim();
		}

		// Check for newer addition of source ip
		// process this so we can omit from size-value processing below
		String sourceIp = "";
		int sourceIpPos = info.indexOf(SOURCE_IP_SIG);
		if (sourceIpPos > 0) {
			sourceIp = clientVerPos > 0 ? info.substring(sourceIpPos + SOURCE_IP_SIG.length(), clientVerPos).trim() : info
					.substring(sourceIpPos + SOURCE_IP_SIG.length()).trim();
		}

		int sizeOfPos = info.indexOf(SIZE_OF_SIG);
		if (sizeOfPos > 0) {
			// determine whether we want substring to source IP or to end of line
			// ***** assuming we will never encounter log lines that have size of and client version, but no source IP
			String sizeOf = sourceIpPos > 0 ? info.substring(sizeOfPos + SIZE_OF_SIG.length(), sourceIpPos).trim() : info
					.substring(sizeOfPos + SIZE_OF_SIG.length()).trim();
			size = Long.parseLong(sizeOf);
		}

		AuditEvent event = null;
		EventType eventType = null;
		val controller = bits[5];
		if (controller != null) {
			if (controller.contains("Download")) {
				eventType = EventType.DOWNLOAD;
			} else if (controller.contains("Upload")) {
				eventType = EventType.UPLOAD;
			}
		}

		if ((controller == null) || (eventType == null)) {
			System.out.println("Unrecognized log entry. Could not identify Controller class (expected in position 5)");
			return null;
		}

		if (size == null) {
			event = new AuditEvent(timestamp, objectId, token, sourceIp, clientVersion, EventType.DOWNLOAD);
		} else {
			event = new AuditEvent(timestamp, objectId, token, size, sourceIp, clientVersion, EventType.UPLOAD);
		}

		return event;
	}

	public DateTime parseDateTimestamp(String timestamp) {
		val dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSS");
		val ts = DateTime.parse(timestamp, dtf);
		return ts;
	}

	
	public void test() {
		val objectData = new HashMap<String, ObjectEntry>();
		try {
			val resource = getClass().getResource("/all_obj.txt");
			val testfile = new File(resource.getFile());
			// val json = FileUtils.readFileToString(testfile);

			val query = JsonQuery.compile(".hits[].fileCopies[] | select(.repoCode == \"aws-virginia\") | "
					+ "{ object_id: .repoFileId, file_format: .fileFormat, file_name: .fileName, file_size: .fileSize}");

			val doc = _mapper.readTree(testfile);

			val queryResults = query.apply(doc);

			val objectEntries = queryResults.stream().map(node -> {
				try {
					return _mapper.convertValue(node, ObjectEntry.class);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());

			objectEntries.stream().forEach(obj -> objectData.put(obj.getId(), obj));
			System.out.println();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
