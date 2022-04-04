package br.com.wpivotto.filebox.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.lucene.index.IndexWriter;

@ApplicationScoped
public class IndexScheduler {
	
	private DocIndexer index;
	private ExecutorService executorService;
	private final Logger logger = Logger.getLogger(IndexScheduler.class.getName());
	
	@Inject
	public IndexScheduler(DocIndexer index, Configs configs) {
		this.index = index;
		this.executorService = Executors.newFixedThreadPool(configs.getThreadPoolSize());
	}
	
	public void schedule(Set<Path> deleted, Set<Path> created, Set<Path> updated) {
		executorService.submit(new IndexTask(deleted, created, updated));
	}
	
	private class IndexTask implements Runnable {
		
		private final Set<Path> created;
		private final Set<Path> updated;
		private final Set<Path> deleted;
		
		private IndexTask(Set<Path> deleted, Set<Path> created, Set<Path> updated) {
			this.deleted = new LinkedHashSet<Path>(deleted);
			this.created = new LinkedHashSet<Path>(created);
			this.updated = new LinkedHashSet<Path>(updated);
		}

		@Override
		public void run() {
			
			final IndexWriter writer = index.buildWriter();
			
			try {
				
				CountDownLatch latch = new CountDownLatch(deleted.size() + created.size() + updated.size());
				
				for (Path path : deleted) {
					logger.log(Level.INFO, "INDEX - DELETE DOCUMENT REQUEST: " + path);
					executorService.submit(() -> {
						index.delete(writer, path);
						latch.countDown();
					});
				}
				
				for (Path path : updated) {
					logger.log(Level.INFO, "INDEX - UPDATE DOCUMENT REQUEST: " + path);
					executorService.submit(() -> {
						index.update(writer, path);
						latch.countDown();
					});
				}
				
				for (Path path : created) {
					logger.log(Level.INFO, "INDEX - CREATE DOCUMENT REQUEST: " + path);
					executorService.submit(() -> {
						index.update(writer, path);
						latch.countDown();
					});
				}
				
				latch.await();
			
				writer.commit();
				
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (writer != null && writer.hasUncommittedChanges()) {
						writer.rollback();
					}
				} catch (IOException e1) {}
			} finally {
				
				try {
					if (writer != null)  {
						writer.close();
					}
				} catch (IOException e) {}
				
			}
			
		}
		
	}
	
	

}
