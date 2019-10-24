# simple-maven-file-filtering
Library for applying simple file filtering (ie expanding of variables) in text files to be used with maven plugins.

## Examples

Use the following call of `FilterUtils.filterFile` to expand only project-related 
variables (like `${project.name}` or `${project.version}`):
  
```java
import com.github.fracpete.simplemavenfilefiltering.FilterUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import java.nio.file.Path;

Model model = ...;  // the Maven model that your plugin has access to 
Log log = ...;      // the log instance from your plugin
Path source = ...;  // the source file with the variables unexpanded
Path target = ...;  // the target file to write with the variables expanded   

FilterUtils.filterFile(log, source, target, model);
```

If you want to replace other variables, e.g., parameters form your own
plugin, then you can supply a map of variables:
  
```java
import com.github.fracpete.simplemavenfilefiltering.FilterUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import java.nio.file.Path;
import java.utils.Map;
import java.utils.HashMap;

Model model = ...;  // the Maven model that your plugin has access to 
Log log = ...;      // the log instance from your plugin
Path source = ...;  // the source file with the variables unexpanded
Path target = ...;  // the target file to write with the variables expanded   
Map<String, String> vars = new HashMap<>();
vars.put("myvar1", "some_value");
vars.put("something", "else");

FilterUtils.filterFile(log, source, target, model, vars);
```

If you have to use a variable prefix/suffix pair other than the 
default `${` and `}`, you can supply these using the following methods:

* `filterFile(Log log, Path input, Path output, Model model, String varPrefix, String varSuffix)`
* `filterFile(Log log, Path input, Path output, Model model, Map<String, String> additional, String varPrefix, String varSuffix)`


## Maven

Use the following dependency to include the library in your Maven plugin:
```xml
    <dependency>
      <groupId>com.github.fracpete</groupId>
      <artifactId>simple-maven-file-filtering</artifactId>
      <version>0.0.1</version>
    </dependency>
```
