package br.com.wpivotto.main;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import br.com.wpivotto.filebox.index.Configs;
import br.com.wpivotto.filebox.index.IndexScheduler;

@ApplicationScoped
public class DocsWatcher implements Runnable {

	private IndexScheduler scheduler;
	private Configs configs;
	private final Logger logger = Logger.getLogger(DocsWatcher.class.getName());
	
	@Inject
	public DocsWatcher(IndexScheduler scheduler, Configs configs) {
		this.scheduler = scheduler;
		this.configs = configs;
	}
	
	private final Set<Path> created = new LinkedHashSet<>();
	private final Set<Path> updated = new LinkedHashSet<>();
	private final Set<Path> deleted = new LinkedHashSet<>();

	private volatile boolean appIsRunning = true;
	private final int pollmillis = 100;
	private WatchService ws;

	public void shutdown() {
		this.appIsRunning = false;
	}

	public void run() {
		
		try (WatchService autoclose = ws) {

			while (appIsRunning) {

				boolean hasPending = created.size() + updated.size() + deleted.size() > 0;
	
				WatchKey wk = hasPending ? ws.poll(pollmillis, TimeUnit.MILLISECONDS) : ws.take();
				
				if (wk != null) {
					
					for (WatchEvent<?> event : wk.pollEvents()) {
						Path parent = (Path) wk.watchable();
						Path eventPath = (Path) event.context();
						storeEvent(event.kind(), parent.resolve(eventPath));
					}
					
					boolean valid = wk.reset();
					if (!valid) {
						logger.log(Level.INFO, "Check the path, dir may be deleted " + wk);
					}
				}

				logger.log(Level.INFO, "PENDING: cre=" + created.size() + " mod=" + updated.size() + " del=" + deleted.size());

				if (wk == null && hasPending) {
					scheduler.schedule(deleted, created, updated);
					deleted.clear();
					created.clear();
					updated.clear();
				}
			}
			
		} catch (InterruptedException e) {
			logger.log(Level.INFO, "Watch was interrupted, sending final updates");
			scheduler.schedule(deleted, created, updated);
			
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	private void register(String path) {

		try {
			
			
			Path dir = Paths.get(path);
			
			if (this.ws == null) {
				ws = dir.getFileSystem().newWatchService();
			}
	
			Kind<?>[] kinds = { StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE };
			dir.register(ws, kinds);
			
			logger.log(Level.INFO, "Watching file updates at " + path);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	
	private void storeEvent(Kind<?> kind, Path path) {
		
		boolean cre = false;
		boolean mod = false;
		boolean del = kind == StandardWatchEventKinds.ENTRY_DELETE;

		if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
			mod = deleted.contains(path);
			cre = !mod;
		} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			cre = created.contains(path);
			mod = !cre;
		}
		
		addOrRemove(created, cre, path);
		addOrRemove(updated, mod, path);
		addOrRemove(deleted, del, path);
		
	}

	private void addOrRemove(Set<Path> set, boolean add, Path path) {
		if (add)
			set.add(path);
		else
			set.remove(path);
	}

	public void setup() {
		register(configs.getDocsFolder());
		run();
	}


}
