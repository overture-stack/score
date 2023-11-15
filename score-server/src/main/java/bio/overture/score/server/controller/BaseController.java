package bio.overture.score.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
public class BaseController {

    @Autowired
    Environment environment;

  @RequestMapping(method = RequestMethod.GET, value = "/profile")
  public List<String> getProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) final String accessToken) {
      return Arrays.asList(environment.getActiveProfiles());
    }

}
