package br.com.wpivotto.main;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.se.events.ContainerInitialized;

import br.com.wpivotto.filebox.api.ApiServer;

@ApplicationScoped
public class Main {

  @Inject private ApiServer server;
  @Inject private DocsWatcher watcher;

  public void startup(@Observes ContainerInitialized event) {
    setupLogger(Level.INFO);
    server.start();
    watcher.setup();
  }

  private void setupLogger(Level level) {
    System.setProperty(
        "java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    Logger.getLogger("br.com.wpivotto").setLevel(level);
  }
}
