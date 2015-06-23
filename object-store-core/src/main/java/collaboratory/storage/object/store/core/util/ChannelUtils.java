package collaboratory.storage.object.store.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import collaboratory.storage.object.store.core.model.DataChannel;

/**
 * @deprecated
 */
@Deprecated
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

  public static String UploadObject(File upload, URL url, long offset, long length) throws IOException
  {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setFixedLengthStreamingMode(length);
    FileInputStream fis = new FileInputStream(upload);
    WritableByteChannel toChannel = Channels.newChannel(connection.getOutputStream());
    fis.getChannel().transferTo(offset, length, toChannel);
    fis.close();
    toChannel.close();

    if (connection.getResponseCode() == 200) {
      if (connection.getHeaderField("ETag").isEmpty()) {
        throw new IOException("no etag found in the header: " + connection.getResponseMessage());
      }
      return connection.getHeaderField("ETag").replaceAll("^\"|\"$", "");
    } else {
      throw new IOException("fail to upload: " + connection.getResponseMessage());
    }
  }

  public static String UploadObject(DataChannel channel, URL url) throws IOException
  {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setFixedLengthStreamingMode(channel.getLength());

    channel.writeTo(connection.getOutputStream());

    if (connection.getResponseCode() == 200) {
      if (connection.getHeaderField("ETag").isEmpty()) {
        throw new IOException("no etag found in the header: " + connection.getResponseMessage());
      }
      return connection.getHeaderField("ETag").replaceAll("^\"|\"$", "");
    } else {
      throw new IOException("fail to upload: " + connection.getResponseMessage());
    }
  }
}