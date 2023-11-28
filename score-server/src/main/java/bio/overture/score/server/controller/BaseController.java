package bio.overture.score.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
public class BaseController {

  @Autowired
  ApplicationContext appCon;

  @RequestMapping(method = RequestMethod.GET, value = "/profile")
  public String getProfile() {
    return appCon.getBean("activeStorageProfile", String.class);
  }





}
