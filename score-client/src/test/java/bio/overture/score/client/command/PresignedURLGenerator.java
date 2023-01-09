/**
 * Adapted from https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURLJavaSDK.html
 */

package bio.overture.score.client.command;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.URL;

@AllArgsConstructor
public class PresignedURLGenerator {
  private String endpoint;
  private String credentialsFile;

  public URL generateUrl(String bucketName, String objectKey) throws IOException {
    String signingRegion="";
    try {
      AmazonS3 s3Client = AmazonS3ClientBuilder
        .standard()
        .withCredentials(new ProfileCredentialsProvider(credentialsFile, "test"))
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint,signingRegion))
        .build();

      // Set the presigned URL to expire after one hour.
      java.util.Date expiration = new java.util.Date();
      long expTimeMillis = expiration.getTime();
      expTimeMillis += 1000 * 60 * 60;
      expiration.setTime(expTimeMillis);

      // Generate the presigned URL.
      GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucketName, objectKey)
          .withMethod(HttpMethod.GET)
          .withExpiration(expiration);
      URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
      return(url);
    } catch (AmazonServiceException e) {
      // The call was transmitted successfully, but Amazon S3 couldn't process
      // it, so it returned an error response.
      e.printStackTrace();
    } catch (SdkClientException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      e.printStackTrace();
    }
    return null;
  }
}