/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bio.overture.score.server.metadata;

import bio.overture.score.server.exception.IdNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import bio.overture.score.server.security.UserRestTemplate;
import org.springframework.web.client.RestTemplate;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class MetadataService {
  private final RestTemplate restTemplate = new UserRestTemplate();

  @Value("${metadata.url}")
  private String metadataUrl;

  private static final String ANALYSIS_STATE = "analysisState";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public MetadataEntity getEntity(@NonNull String id) {
    log.debug("using " + metadataUrl + " for MetaData server");
    try {
      return restTemplate.getForEntity(metadataUrl + "/entities/" + id, MetadataEntity.class).getBody();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == NOT_FOUND) {
        throw new IdNotFoundException(format("Entity %s is not registered on the server.", id));
      }

      log.error("Unexpected response code {} while getting ID {}", e.getStatusCode(), id);

      throw e;
    }
  }

  public String getAnalysisStateForMetadata(@NonNull MetadataEntity metadataEntity){
    val studyId = getStudyId(metadataEntity);
    val analysisId = getAnalysisId(metadataEntity);
    try{
      return readAnalysisState(studyId, analysisId);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == NOT_FOUND || e.getStatusCode() == BAD_REQUEST) {
        throw new IdNotFoundException(
            format("The analysis '%s' with studyId '%s' is not registered on the metadata server. Message: %s",
            analysisId, studyId, e.getResponseBodyAsString()));
      }
      log.error("Unexpected response code {} while getting AnalysisId {} for StudyId {}",
          e.getStatusCode(), analysisId, studyId);
      throw e;
    }
  }

  public static String getAnalysisId(MetadataEntity metadataEntity){
    return metadataEntity.getGnosId();
  }

  private static String getStudyId(MetadataEntity metadataEntity){
    return metadataEntity.getProjectCode();
  }

  @SneakyThrows
  private String readAnalysisState(@NonNull String studyId, @NonNull String analysisId  ){
    val response = restTemplate.getForObject(getAnalysis(studyId,analysisId), String.class);
    val jsonResponse = OBJECT_MAPPER.readTree(response);
    return parseAnalysisState(jsonResponse);
  }

  private String getAnalysis(String studyId, String analysisId){
    return format("%s/studies/%s/analysis/%s", metadataUrl, studyId, analysisId);
  }

  private static String parseAnalysisState(JsonNode response){
    checkArgument(response.has(ANALYSIS_STATE),
      "Could not parse '%s' from SONG server response: %s",
      ANALYSIS_STATE, response.textValue());
    return response.path(ANALYSIS_STATE).textValue();
  }
}

