package net.ravendb.abstractions.indexing;

import java.util.ArrayList;
import java.util.List;


public class MergeSuggestions {
  private IndexDefinition mergedIndex = new IndexDefinition();

  private List<String> canMerge = new ArrayList<>();

  private String collection = "";

  private List<String> canDelete = new ArrayList<>();

  private String surpassingIndex = "";

  public List<String> getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(List<String> canDelete) {
    this.canDelete = canDelete;
  }

  public String getSurpassingIndex() {
    return surpassingIndex;
  }

  public void setSurpassingIndex(String surpassingIndex) {
    this.surpassingIndex = surpassingIndex;
  }

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public List<String> getCanMerge() {
    return canMerge;
  }

  public void setCanMerge(List<String> canMerge) {
    this.canMerge = canMerge;
  }

  public IndexDefinition getMergedIndex() {
    return mergedIndex;
  }

  public void setMergedIndex(IndexDefinition mergedIndex) {
    this.mergedIndex = mergedIndex;
  }

}
