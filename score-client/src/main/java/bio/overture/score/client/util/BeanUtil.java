package bio.overture.score.client.util;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

// utility class to inject beans dynamically at runtime

@Component
public class BeanUtil<T> {

  @Autowired ApplicationContext appContext;

  // this method injects beans dynamically based on score-server active profile
  public Object getBeanForProfile(Class cl) {
    String profile = appContext.getBean("storageProfile", String.class);

    Set<String> beans = appContext.getBeansOfType(cl).keySet();
    return appContext.getBean(
        beans.stream()
            .filter(s -> s.equals(profile + cl.getSimpleName()))
            .collect(Collectors.toList())
            .get(0),
        cl);
  }
}
