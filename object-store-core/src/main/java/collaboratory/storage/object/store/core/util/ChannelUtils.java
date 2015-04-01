package collaboratory.storage.object.store.core.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;

import com.google.api.client.util.IOUtils;

public final class ChannelUtils {

  public static void channelCopy(final ReadableByteChannel src,
      final WritableByteChannel dest) throws IOException {
    final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (src.read(buffer) != -1) {
      buffer.flip();
      dest.write(buffer);
      buffer.compact();
    }
    // EOF will leave buffer in fill state
    buffer.flip();
    // make sure the buffer is fully drained.
    while (buffer.hasRemaining()) {
      dest.write(buffer);
    }
  }

  public static void UploadObject(File upload, URL url) throws IOException {
    UploadObject(upload, url, 0, upload.length());
  }

  // TODO: http://codereview.stackexchange.com/questions/45819/httpurlconnection-response-code-handling
  public static String UploadObject(File upload, URL url, long offset, long length) throws IOException
  {
    System.out.println(String.format("URL: %s", url));
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setFixedLengthStreamingMode(length);
    FileInputStream fis = new FileInputStream(upload);
    WritableByteChannel toChannel = Channels.newChannel(connection.getOutputStream());
    fis.getChannel().transferTo(offset, length, toChannel);
    fis.close();
    toChannel.close();
    Map<String, List<String>> map = connection.getHeaderFields();
    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      System.out.println("Key : " + entry.getKey()
          + " ,Value : " + entry.getValue());
    }

    if (connection.getResponseCode() == 200) {
      if (connection.getHeaderField("ETag").isEmpty()) {
        throw new IOException("no etag found in the header: " + connection.getResponseMessage());
      }
      return connection.getHeaderField("ETag").replaceAll("^\"|\"$", "");
    } else {
      throw new IOException("fail to upload: " + connection.getResponseMessage());
    }

  }

  public static String GetObject(URL url) throws IOException
  {
    System.out.println(String.format("URL: %s", url));
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try {
      connection.setDoOutput(true);
      connection.setRequestMethod("GET");
      ByteArrayOutputStream response = new ByteArrayOutputStream();
      IOUtils.copy(connection.getInputStream(), response);
      Map<String, List<String>> map = connection.getHeaderFields();
      for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        System.out.println("Key : " + entry.getKey()
            + " ,Value : " + entry.getValue());
      }

      if (connection.getResponseCode() == 200) {
        return response.toString();
      } else {
        throw new IOException("fail to upload: " + connection.getResponseMessage());
      }
    } finally {
      connection.disconnect();
    }

  }
}