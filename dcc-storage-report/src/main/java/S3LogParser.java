

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.val;

import com.hiramsoft.commons.jsalparser.JSalParser;
import com.hiramsoft.commons.jsalparser.S3LogEntry;

public class S3LogParser {

	public static void main(String[] args) {
		val parser = new S3LogParser();
		try {
			val is = new FileInputStream("/Users/ayang/Downloads/2016-01-06-18-22-12-5BF91F998ADBDDB9.txt");
			parser.test(is);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void test(InputStream logstream) throws IOException {
    
    List<S3LogEntry> entries = JSalParser.parseS3Log(logstream);

    for (int i=0; i<entries.size(); i++) {
      val entry = entries.get(i);
      System.out.println(entry.getTime());
      System.out.println(entry.getBucket());
      // getTime() returns a JODA DateTime object,
      // so Java prints:
      // 2014-08-27T20:20:05.000+00:00
    }
	}
}
