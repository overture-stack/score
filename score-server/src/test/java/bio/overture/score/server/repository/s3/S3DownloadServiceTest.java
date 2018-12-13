package bio.overture.score.server.repository.s3;

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class S3DownloadServiceTest {

    private final String objectId = "45dfcd17-8e80-53fc-b400-cc8b583dae05";

    private S3DownloadService s3DownloadService = new S3DownloadService();

    private MetadataService mockService;

    private MetadataEntity metadataEntity;

    @Before
    public void set_up(){
        mockService = mock(MetadataService.class);
        s3DownloadService.setMetadataService(mockService);
        metadataEntity = MetadataEntity.builder()
                .id(objectId)
                .fileName("file_1")
                .access("open")
                .gnosId("an1")
                .projectCode("project")
                .build();

        when(mockService.getAnalysisStateForMetadata(metadataEntity)).thenReturn("UNPUBLISHED");
    }

    @Test
    public void test_if_unpublished_file_triggers_error() {
        val throwable = catchThrowable(() -> s3DownloadService.checkAnalysisState(metadataEntity));
        assertThat(throwable)
                .hasMessageContaining(
                        format("Critical Error: cannot complete download for objectId '%s' with ", objectId));
    }

    @Test
    public void verify_if_download_unpublished_objectId_is_blocked() {
        when(mockService.getAnalysisStateForMetadata(metadataEntity)).thenReturn("UNPUBLISHED");
        when(mockService.getEntity(objectId)).thenReturn(metadataEntity);

        val throwable1 = catchThrowable(() -> s3DownloadService.download(objectId, 0, -1, false));
        assertThat(throwable1).isExactlyInstanceOf(NotRetryableException.class);
    }

}
