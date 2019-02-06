package bio.overture.score.client.view;

import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.slicing.SamFileBuilder;
import bio.overture.score.client.transport.NullSourceSeekableHTTPStream;
import htsjdk.samtools.CRAMFileReader;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.SeekableStream;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.net.URL;

public class Viewer {
  File referenceFile=null;

  public Viewer(File referenceFile) {
    this.referenceFile=referenceFile;
  }

  public static SamInputResource getFileResource(File bamFile, File baiFile) {
    if (baiFile != null) {
      return SamInputResource.of(bamFile).index(baiFile);
    } else {
      return SamInputResource.of(bamFile);
    }
  }

  public static SeekableStream openInputStream(URL url) {
    return new NullSourceSeekableHTTPStream(url);
  }

  public static SeekableStream openIndexStream(URL url) {
    return new NullSourceSeekableHTTPStream(url);
  }

  @SneakyThrows
  public SamFileBuilder getBuilder(File sequenceFile, File indexFile) {
    val entity = new Entity();
    entity.setFileName(sequenceFile.toString());
    val reference=new ReferenceSource(referenceFile);
    val resource = getFileResource(sequenceFile, indexFile);
    val builder = new SamFileBuilder()
        .entity(entity)
        .samInput(resource)
        .cramReferenceSource(reference);
    val reader = builder.createSamReader();
    builder.reader(reader);
    return builder;
  }

  @SneakyThrows
  public SamFileBuilder getBuilder(SeekableStream inputStream, SeekableStream indexStream, boolean isCram) {
    val resource = SamInputResource.of(inputStream).index(indexStream);
    val builder = new SamFileBuilder().samInput(resource);

    if (isCram) {
      // Since CRAM is a compressed form of BAM file that saves space by only recording
      // the differences between an individual BAM file and a standard reference file.
      // we need to have the reference file in order to decode the CRAM file.
      val reference = new ReferenceSource(referenceFile);
      val primitiveReader = new CRAMFileReader(inputStream, indexStream,reference, ValidationStringency.DEFAULT_STRINGENCY);
      val reader = new SamReader.PrimitiveSamReaderToSamReaderAdapter(
        primitiveReader, resource);
      return builder.reader(reader);
    }
    val reader = builder.createSamReader();
    return builder.reader(reader);
  }
}


