package br.com.wpivotto.filebox.pdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchMatches implements Iterable<SearchMatch> {

  private List<SearchMatch> matches;

  public SearchMatches(List<SearchMatch> matches) {
    this.matches = matches;
  }

  public SearchMatches() {}

  public void print() {
    for (SearchMatch match : matches) {
      System.out.println(match);
    }
  }

  public List<SearchMatch> values() {
    return matches;
  }

  public Map<String, List<SearchMatch>> groupedByDoc() {

    Map<String, List<SearchMatch>> response = new HashMap<>();

    for (SearchMatch match : matches) {

      List<SearchMatch> list = response.get(match.getPath());

      if (list == null) {
        list = new ArrayList<>();
        response.put(match.getPath(), list);
      }

      list.add(match);
    }

    return response;
  }

  public Set<String> paths() {

    Set<String> paths = new HashSet<>();

    for (SearchMatch match : matches) {
      paths.add(match.getPath());
    }

    return paths;
  }

  public List<SearchMatch> sortedValues() {
    List<SearchMatch> response = new ArrayList<>(matches);
    Collections.sort(response);
    return response;
  }

  public List<SearchMatch> groupedByPage() {

    Map<String, SearchMatch> map = new HashMap<>();
    List<SearchMatch> response = new ArrayList<>();

    for (SearchMatch match : matches) {
      String id = match.getPath() + match.getPage();
      map.put(id, match);
    }

    for (SearchMatch match : map.values()) {
      response.add(match);
    }

    Collections.sort(response);

    return response;
  }

  public int count() {
    if (matches == null) return 0;
    return matches.size();
  }

  public boolean found() {
    return matches != null && matches.size() > 0;
  }

  @Override
  public Iterator<SearchMatch> iterator() {
    return matches.iterator();
  }
}
