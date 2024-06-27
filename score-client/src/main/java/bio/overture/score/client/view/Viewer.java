package bio.overture.score.client.view;

import static java.util.Objects.isNull;

import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.slicing.SamFileBuilder;
import bio.overture.score.client.transport.NullSourceSeekableHTTPStream;
import htsjdk.samtools.CRAMFileReader;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.SeekableStream;
import java.io.File;
import java.net.URL;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class Viewer {
  File referenceFile = null;

  public Viewer(File referenceFile) {
    this.referenceFile = referenceFile;
  }

  public static SamInputResource getFileResource(@NonNull File bamFile, File baiFile) {
    if (isNull(baiFile)) {
      return SamInputResource.of(bamFile);
    } else {
      return SamInputResource.of(bamFile).index(baiFile);
    }
  }

  public static SamInputResource getStreamResource(
      @NonNull SeekableStream inputStream, SeekableStream indexStream) {
    if (isNull(indexStream)) {
      return SamInputResource.of(inputStream);
    } else {
      return SamInputResource.of(inputStream).index(indexStream);
    }
  }

  public static SeekableStream openInputStream(URL url) {
    return new NullSourceSeekableHTTPStream(url);
  }

  public static SeekableStream openIndexStream(URL url) {
    return new NullSourceSeekableHTTPStream(url);
  }

  @SneakyThrows
  public SamFileBuilder getBuilder(@NonNull File sequenceFile, File indexFile) {
    val entity = new Entity();
    entity.setFileName(sequenceFile.toString());
    val reference = new ReferenceSource(referenceFile);
    val resource = getFileResource(sequenceFile, indexFile);
    val builder =
        new SamFileBuilder().entity(entity).samInput(resource).cramReferenceSource(reference);
    val reader = builder.createSamReader();
    builder.reader(reader);
    return builder;
  }

  @SneakyThrows
  public SamFileBuilder getBuilder(
      @NonNull SeekableStream inputStream, SeekableStream indexStream, boolean isCram) {
    val resource = getStreamResource(inputStream, indexStream);
    val builder = new SamFileBuilder().samInput(resource);

    if (isCram) {
      // Since CRAM is a compressed form of BAM file that saves space by only recording
      // the differences between an individual BAM file and a standard reference file.
      // we need to have the reference file in order to decode the CRAM file.
      if (referenceFile == null) {
        throw new RuntimeException(
            "CRAM file type detected, an indexed (fai) reference file (fa, fasta) must be provided. Please specify via --reference-file.");
      }
      val reference = new ReferenceSource(referenceFile);
      val primitiveReader =
          new CRAMFileReader(
              inputStream, indexStream, reference, ValidationStringency.DEFAULT_STRINGENCY);
      val reader = new SamReader.PrimitiveSamReaderToSamReaderAdapter(primitiveReader, resource);
      return builder.reader(reader);
    }
    val reader = builder.createSamReader();
    return builder.reader(reader);
  }
}
