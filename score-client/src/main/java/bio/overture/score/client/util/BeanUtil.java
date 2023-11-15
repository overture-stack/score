package bio.overture.score.client.util;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BeanUtil<T> {

  @Autowired
  ApplicationContext appContext;

   public Object getBean(Class cl){

     List<String> profileList = appContext.getBean("profiles", List.class);
      if(profileList.isEmpty()){
        profileList.add("collaboratory");
      }

     Set<String> beans = appContext.getBeansOfType(cl).keySet();

     return appContext.getBean(beans.stream()
         .filter(s -> profileList.contains(s.substring(0, s.indexOf('U'))))
         .collect(Collectors.toList())
         .get(0), cl);
  }
}
