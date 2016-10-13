package org.icgc.dcc.storage.report.core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.h2.tools.Server;
import org.icgc.dcc.storage.report.model.ReportRow;
import org.icgc.dcc.storage.report.model.log.AuditEvent;
import org.icgc.dcc.storage.report.model.log.EventType;
import org.icgc.dcc.storage.report.model.reference.ObjectEntry;
import org.icgc.dcc.storage.report.model.reference.TokenEntry;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Slf4j
public class AuditReportDAO {
	private EmbeddedDatabase _db;

	public AuditReportDAO() {
	}

	public void init(@NonNull String schemaFileName) throws SQLException {
		val builder = new EmbeddedDatabaseBuilder();
		_db = builder.setType(EmbeddedDatabaseType.H2).addScript(schemaFileName).build();

		_db.getConnection().setAutoCommit(true);
		// Server webServer = Server.createWebServer("-webAllowOthers","-webPort","8082").start();
	}

	public void insertTokens(List<TokenEntry> authEntries) {
		val template = insertTemplate();

		template.batchUpdate("insert into USERS (ACCESS_TOKEN, USER_NAME, IS_DELETED) " + " values (?, ?, ?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						val entry = authEntries.get(i);
						ps.setString(1, entry.getTokenMD5());
						ps.setString(2, entry.getUserName());
						ps.setBoolean(3, entry.isDeleted());
					}

					@Override
					public int getBatchSize() {
						return authEntries.size();
					}
				});
	}

	public void insertEvents(@NonNull List<AuditEvent> events) {
		val template = insertTemplate();

		template.batchUpdate(
				"insert into AUDIT_EVENTS(EVENT_TS, EVENT_TYPE, OBJECT_ID, ACCESS_TOKEN) " + " values (?, ?, ?, ?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						val event = events.get(i);
						ps.setDate(1, new java.sql.Date(event.getTimestamp().toDate().getTime()));
						ps.setString(2, event.getEventType().toString());
						ps.setString(3, event.getObjectId());
						ps.setString(4, event.getTokenDigest());
					}

					@Override
					public int getBatchSize() {
						return events.size();
					}
				});
	}

	public void insertObjects(@NonNull List<ObjectEntry> objectEntries) {
		val template = insertTemplate();

		template.batchUpdate("insert into DCC_OBJECTS (ID, FILE_FORMAT, FILE_NAME, FILE_SIZE) " + " values (?, ?, ?, ?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						val entry = objectEntries.get(i);
						ps.setString(1, entry.getId());
						ps.setString(2, entry.getFileFormat());
						ps.setString(3, entry.getFileName());
						ps.setLong(4, entry.getFileSize());
					}

					@Override
					public int getBatchSize() {
						return objectEntries.size();
					}
				});
	}

	protected JdbcTemplate insertTemplate() {
		val template = new JdbcTemplate(_db);
		try {
			template.getDataSource().getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return template;
	}

	public Map<String, Long> getObjectTypes() {
		Map<String, Long> result = new HashMap<String, Long>();
		val template = new JdbcTemplate(_db);
		List<Map<String, Object>> counts = template
				.queryForList("select FILE_FORMAT, count(*) AS FILE_COUNT from DCC_OBJECTS group by FILE_FORMAT");

		for (val f : counts) {
			result.put((String)f.get("FILE_FORMAT"), (Long)f.get("FILE_COUNT"));
			//System.out.println(f.get("FILE_FORMAT") + ": " + f.get("FILE_COUNT"));
		}

		return result;
	}

	public int count() {
		val template = new JdbcTemplate(_db);
		val count = template.queryForObject("select count(*) from AUDIT_EVENTS", Integer.class);
		return count;
	}

	public List<ReportRow> countByDate() {
		val template = new JdbcTemplate(_db);

		List<ReportRow> results = template.query(
				"select EVENT_DATE, EVENT_TYPE, E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, O.FILE_FORMAT, COUNT(1) as COUNTS "
						+ "from AUDIT_EVENTS as E join DCC_OBJECTS as O on E.OBJECT_ID = O.ID "
						+ "left outer join USERS as U on E.ACCESS_TOKEN = U.ACCESS_TOKEN "
						+ "group by EVENT_DATE, EVENT_TYPE, E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, O.FILE_FORMAT "
						+ "order by EVENT_DATE, EVENT_TYPE, U.USER_NAME, O.FILE_FORMAT", 
						new RowMapper<ReportRow>() {
					public ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
						val row = new ReportRow();
						row.setEventDate(rs.getDate(1));
						row.setEventType(rs.getString(2));
						row.setAccessToken(rs.getString(3));
						row.setUserName(rs.getString(4));
						row.setDeleted(rs.getBoolean(5));
						row.setFileFormat(rs.getString(6));
						row.setCount(rs.getInt(7));
						return row;
					}
				});
		return results;
	}

	public List<ReportRow> countByDate(@NonNull EventType activityType) {
		val template = new NamedParameterJdbcTemplate(_db);
		val namedParms = new MapSqlParameterSource("event_type", activityType.toString());

		List<ReportRow> results = template
				.query(
						"select EVENT_DATE, E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, O.FILE_FORMAT, COUNT(1) as COUNTS "
							  + " from AUDIT_EVENTS as E join DCC_OBJECTS as O on E.OBJECT_ID = O.ID "
							  + " left outer join USERS as U on E.ACCESS_TOKEN = U.ACCESS_TOKEN "
								+ " where EVENT_TYPE = :event_type "
							  + " group by EVENT_DATE, E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, O.FILE_FORMAT "
								+ " order by EVENT_DATE, U.USER_NAME, O.FILE_FORMAT", namedParms, new RowMapper<ReportRow>() {
							public ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
								val row = new ReportRow();
								row.setEventDate(rs.getDate(1));
								row.setAccessToken(rs.getString(2));
								row.setUserName(rs.getString(3));
								row.setDeleted(rs.getBoolean(4));
								row.setFileFormat(rs.getString(5));
								row.setCount(rs.getInt(6));
								return row;
							}
						});
		return results;
	}

	public List<ReportRow> countByDateByTypeByUser() {
		val template = new NamedParameterJdbcTemplate(_db);

		List<ReportRow> results = template
				.query(
						"select E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, EVENT_DATE, EVENT_TYPE, E.OBJECT_ID, O.FILE_FORMAT, COUNT(1) as COUNTS "
								+ " from AUDIT_EVENTS as E join DCC_OBJECTS as O on E.OBJECT_ID = O.ID "
								+ " left outer join USERS as U on E.ACCESS_TOKEN = U.ACCESS_TOKEN "
								+ " group by E.ACCESS_TOKEN, U.USER_NAME, U.IS_DELETED, EVENT_DATE, EVENT_TYPE, E.OBJECT_ID, O.FILE_FORMAT " 
								+ " order by U.USER_NAME,  EVENT_DATE, EVENT_TYPE, O.FILE_FORMAT", new RowMapper<ReportRow>() {
							public ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
								val row = new ReportRow();
								row.setAccessToken(rs.getString(1));
								row.setUserName(rs.getString(2));
								row.setDeleted(rs.getBoolean(3));
								row.setEventDate(rs.getDate(4));
								row.setEventType(rs.getString(5));
								row.setObjectId(rs.getString(6));
								row.setFileFormat(rs.getString(7));
								row.setCount(rs.getInt(8));
								return row;
							}
						});
		return results;
	}

	public List<ReportRow> countByObject() {
		val template = new JdbcTemplate(_db);

		List<ReportRow> results = template
				.query(
						"select EVENT_DATE, EVENT_TYPE, OBJECT_ID, O.FILE_FORMAT, COUNT(1) as COUNTS "
						+ " from AUDIT_EVENTS as E " 
						+ " join DCC_OBJECTS as O on E.OBJECT_ID = O.ID "
						+ " group by EVENT_DATE, EVENT_TYPE, E.OBJECT_ID, O.FILE_FORMAT",
						new RowMapper<ReportRow>() {
							public ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
								val row = new ReportRow();
								row.setEventDate(rs.getDate(1));
								row.setEventType(rs.getString(2));
								row.setObjectId(rs.getString(3));
								row.setFileFormat(rs.getString(4));
								row.setCount(rs.getInt(5));
								return row;
							}
						});
		return results;
	}

	public List<ReportRow> countByObjectByUser() {
		val template = new JdbcTemplate(_db);

		List<ReportRow> results = template.query(
				"select OBJECT_ID, O.FILE_FORMAT, E.ACCESS_TOKEN, U.USER_NAME, EVENT_TYPE, COUNT(1) as COUNTS "
				+ " from AUDIT_EVENTS as E join DCC_OBJECTS as O on E.OBJECT_ID = O.ID "
				+ " left outer join USERS as U on E.ACCESS_TOKEN = U.ACCESS_TOKEN "
				+ " group by OBJECT_ID, O.FILE_FORMAT, E.ACCESS_TOKEN, U.USER_NAME, EVENT_TYPE " 
				+ " order by OBJECT_ID, O.FILE_FORMAT, U.USER_NAME, EVENT_TYPE DESC", new RowMapper<ReportRow>() {
					public ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
						val row = new ReportRow();
						row.setObjectId(rs.getString(1));
						row.setFileFormat(rs.getString(2));
						row.setAccessToken(rs.getString(3));
						row.setUserName(rs.getString(4));
						row.setEventType(rs.getString(5));
						row.setCount(rs.getInt(6));
						return row;
					}
				});
		return results;
	}

	public void verify() {
		try {
			Server.startWebServer(_db.getConnection());
		} catch (SQLException sqle) {
			throw new RuntimeException(sqle);
		}
	}

	public void shutdown() {
		if (_db != null) {
			_db.shutdown();
		}
	}

}
