package bio.overture.score.client.util;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor
public class CsvParser<T> {

  private final Class<T> type;
  private final Character columnSep;


  @SneakyThrows
  public List<T> parseFile(File file){
    val mapper =  new CsvMapper();
    mapper.addMixIn(type, type);
    val schema = mapper.schemaFor(type)
        .withHeader()
        .withColumnSeparator(columnSep);

    return mapper
        .readerFor(type)
        .with(schema)
        .<T>readValues(file)
        .readAll();
  }

}
