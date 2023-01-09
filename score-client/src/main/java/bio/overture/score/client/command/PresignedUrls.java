package bio.overture.score.client.command;

import lombok.AllArgsConstructor;

import java.net.URL;

@AllArgsConstructor
public class PresignedUrls {
  URL file;
  URL index;
}
