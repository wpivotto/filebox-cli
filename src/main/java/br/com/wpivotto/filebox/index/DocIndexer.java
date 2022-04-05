package br.com.wpivotto.filebox.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import br.com.wpivotto.filebox.pdf.IndexEntry;

@ApplicationScoped
public class DocIndexer {

  private Configs configs;
  private Directory dir;
  private ContentExtractor extractor;

  @Inject
  public DocIndexer(Configs configs, ContentExtractor extractor) {
    this.configs = configs;
    this.extractor = extractor;
  }

  public IndexWriter buildWriter() {
    try {
      Analyzer analyzer = new CustomAnalyzer(configs);
      IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_45, analyzer);
      config.setWriteLockTimeout(5000);
      config.setOpenMode(OpenMode.CREATE_OR_APPEND);
      return new IndexWriter(getIndexDir(), config);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private boolean indexDoc(IndexWriter writer, File upload) {

    try {

      ByteArrayInputStream source = extractor.getContent(upload);

      int pageNo = 0;

      List<String> content = extractor.getText(source);

      for (String text : content) {
        IndexEntry entry = new IndexEntry();
        Long id = (long) upload.getName().hashCode() + (pageNo + 1);
        entry.setId(id);
        entry.setPath(upload.getName());
        entry.setPage(pageNo + 1);
        entry.setContent(text);
        createIndex(writer, entry);
        pageNo++;
      }

      return true;

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public Directory getIndexDir() {
    try {
      if (dir == null) {
        dir = FSDirectory.open(new File(configs.getIndexFolder()));
      }
      return dir;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createIndex(IndexWriter writer, IndexEntry entry) throws IOException {
    writer.deleteDocuments(new Term(IndexEntry.ID, entry.getId().toString()));
    Document doc = new Document();
    doc.add(new StringField(IndexEntry.ID, entry.getId().toString(), Store.YES));
    doc.add(new StringField(IndexEntry.PATH, entry.getPath(), Store.YES));
    doc.add(new StringField(IndexEntry.PAGE, entry.getPage(), Store.YES));
    doc.add(new StringField(IndexEntry.TYPE, entry.getType(), Store.YES));
    doc.add(new TextField(IndexEntry.CONTENT, entry.getContent(), Store.YES));
    writer.addDocument(doc);
  }

  public boolean deleteAll() {
    IndexWriter writer = null;
    try {
      writer = buildWriter();
      writer.deleteAll();
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      try {
        if (writer != null) writer.close();
      } catch (IOException e) {
      }
    }
  }

  public boolean delete(String file) {
    IndexWriter writer = null;
    try {
      writer = buildWriter();
      writer.deleteDocuments(new Term(IndexEntry.PATH, file));
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      try {
        if (writer != null) writer.close();
      } catch (IOException e) {
      }
    }
  }

  public boolean delete(IndexWriter writer, Path path) {
    try {
      writer.deleteDocuments(new Term(IndexEntry.PATH, path.toAbsolutePath().toString()));
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean update(IndexWriter writer, Path path) {
    try {
      indexDoc(writer, path.toFile());
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
