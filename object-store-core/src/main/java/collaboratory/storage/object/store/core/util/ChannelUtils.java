package collaboratory.storage.object.store.core.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

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
}