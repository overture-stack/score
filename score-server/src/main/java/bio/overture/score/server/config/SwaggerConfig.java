package bio.overture.score.server.config;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SwaggerConfig {

  @Value("${server.version:1.0}")
  private String serverVersion;

  @Value("${swagger.alternateUrl:/swagger}")
  @Getter
  private String alternateSwaggerUrl;

  // default is empty
  @Value("${swagger.host:}")
  private String swaggerHost;

  // default is empty
  @Value("${swagger.basePath:}")
  private String basePath;

  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .apis(basePackage("bio.overture.score.server.controller"))
        .build()
        .host(swaggerHost)
        .pathProvider(
            new RelativePathProvider(null) {
              @Override
              public String getApplicationBasePath() {
                return basePath;
              }
            });
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("Score API")
        .description("Score API reference for developers.")
        .version(serverVersion)
        .build();
  }
}
