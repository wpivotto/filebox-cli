package br.com.wpivotto.filebox.index;

import java.util.Date;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Joiner;

public class Document {

  private String name;
  private String path;
  private long size;
  private Date lastModified;
  private Set<String> tags;
  private Set<String> areas;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = new Date(lastModified);
  }

  public String getByteCount() {
    return FileUtils.byteCountToDisplaySize(size);
  }

  public String getExtension() {
    return FilenameUtils.getExtension(name);
  }

  public String getPreviewClass() {
    boolean pdf = getExtension().equalsIgnoreCase("pdf");
    return pdf ? "" : "btn-disabled";
  }

  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  public String getTagList() {
    return Joiner.on(",").join(tags);
  }

  public Set<String> getAreas() {
    return areas;
  }

  public void setAreas(Set<String> areas) {
    this.areas = areas;
  }

  public String getAreaList() {
    return Joiner.on(",").join(areas);
  }
}
