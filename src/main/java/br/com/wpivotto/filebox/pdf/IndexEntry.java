package br.com.wpivotto.filebox.pdf;

import org.apache.commons.io.FilenameUtils;

public class IndexEntry {

	private Long id;
	private String path;
	private String page;
	private String content;
	private String type;
	private String reference;

	public static final String ID = "id";
	public static final String PATH = "title";
	public static final String PAGE = "page";
	public static final String CONTENT = "content";
	public static final String TYPE = "type";

	public IndexEntry(Long id, String path, String page, String content, String tags, String areas) {
		this.id = id;
		this.path = path;
		this.page = page;
		this.content = content;
		this.type = FilenameUtils.getExtension(path);
	}

	public IndexEntry() {

	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setPath(String path) {
		this.path = path;
		this.type = FilenameUtils.getExtension(path);
	}

	public void setPage(String page) {
		this.page = page;
	}
	
	public void setPage(Integer page) {
		this.page = page + "";
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public String getContent() {
		return content;
	}

	public String getPage() {
		return page;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getReference() {
		if (reference == null) return "";
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		return "IndexEntry [id=" + id + ", path=" + path + ", page=" + page + ", content=" + content + ", type=" + type + "]";
	}

	

}
