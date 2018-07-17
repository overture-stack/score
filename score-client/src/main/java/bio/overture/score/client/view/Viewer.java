package bio.overture.score.client.view;

import bio.overture.score.client.command.PresignedUrls;
import bio.overture.score.client.command.ViewCommand;
import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.slicing.SamFileBuilder;
import bio.overture.score.client.transport.NullSourceSeekableHTTPStream;
import com.google.common.collect.Lists;
import htsjdk.samtools.CRAMFileReader;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.readers.LineIterator;
import lombok.SneakyThrows;
import lombok.val;
import org.icgc.dcc.common.core.util.VersionUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bio.overture.score.client.cli.Parameters.checkParameter;

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

  public static InputStream openInputStream(URL url) {
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
    val builder=new SamFileBuilder().samInput(resource).cramReferenceSource(reference);
    return builder;
  }

  @SneakyThrows
  public SamFileBuilder getBuilder(InputStream inputStream, SeekableStream indexStream, boolean isCram) {
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


