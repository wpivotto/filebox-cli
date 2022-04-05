package br.com.wpivotto.filebox.index;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;

@ApplicationScoped
public class Configs {

  private Configuration configs;
  private FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
  private final Logger logger = Logger.getLogger(Configs.class.getName());

  public Configs() {
    setup();
  }

  public void setup() {
    try {
      File path = load();
      Parameters params = new Parameters();
      builder =
          new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class);
      builder.configure(
          params
              .properties()
              .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
              .setFileName(path.getAbsolutePath()));
      configs = builder.getConfiguration();
      logger.log(Level.INFO, "Loading configurations at " + path.getAbsolutePath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String get(String key) {
    return configs.getString(key);
  }

  public int getInt(String key) {
    return configs.getInt(key);
  }

  public int getInt(String key, int fallback) {
    try {
      return configs.getInt(key);
    } catch (Exception e) {
      return fallback;
    }
  }

  public void remover(String key) {
    configs.clearProperty(key);
  }

  public File getFile(String fileName) {
    try {
      File arquivo = new File(getJarPath() + "/configs/" + fileName);
      if (arquivo.exists()) return arquivo;
      URL resource = Configs.class.getResource("/configs/" + fileName);
      return Paths.get(resource.toURI()).toFile();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Erro ao ler arquivo: " + fileName);
    }
  }

  public List<String> getStopwords() {
    return getContent("stopwords.txt");
  }

  public List<String> getDelimiters() {
    return getContent("delimiters.txt");
  }

  private List<String> getContent(String filename) {
    Scanner scanner = null;
    List<String> lines = new ArrayList<String>();
    try {
      File file = getFile(filename);
      scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        lines.add(scanner.nextLine());
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (scanner != null) scanner.close();
    }
    return lines;
  }

  public String getIndexFolder() {
    return get("index.folder");
  }

  public String getDocsFolder() {
    return get("docs.folder");
  }

  public boolean highlightEnabled() {
    return true;
  }

  private File load() throws URISyntaxException {
    try {
      File file = new File(getJarPath() + "/configs/configs.properties");
      if (file.exists()) return file;
      URL resource = Configs.class.getResource("/configs/configs.properties");
      return Paths.get(resource.toURI()).toFile();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] loadImage(String img) {
    try {
      File file = new File(getJarPath() + "/configs/img/" + img);
      if (file.exists()) return Files.readAllBytes(file.toPath());
      URL resource = Configs.class.getResource("/configs/img/" + img);
      return Files.readAllBytes(Paths.get(resource.toURI()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getJarPath() {
    try {
      File file =
          new File(
              Configs.class
                  .getProtectionDomain()
                  .getCodeSource()
                  .getLocation()
                  .toURI()
                  .normalize()
                  .getPath());
      String path = file.getParentFile().getAbsolutePath();
      return URLDecoder.decode(path, "UTF-8");
    } catch (Exception e) {
      return "";
    }
  }

  public int getThreadPoolSize() {
    return getInt("thread.pool.size", Runtime.getRuntime().availableProcessors());
  }

  public Integer getHttpPort() {
    return getInt("http.port", 9999);
  }

  public Set<Integer> getCustomDelimiters() {
    try {
      List<String> delimiters = getContent("delimiters.txt");
      Set<Integer> chars = new HashSet<>();
      for (String delimiter : delimiters) {
        char c = delimiter.charAt(0);
        chars.add((int) c);
      }
      return chars;
    } catch (Exception e) {
      e.printStackTrace();
      return new HashSet<>();
    }
  }

  public Path getImageLocation(String img) {
    String location = get("images.folder") + File.separator + img;
    return isValidPath(location)
        ? Paths.get(location).normalize().toAbsolutePath()
        : getImageLocation("not_found.png");
  }

  private boolean isValidPath(String path) {
    try {
      return Paths.get(path).toFile().exists();
    } catch (Exception ex) {
      return false;
    }
  }
}
