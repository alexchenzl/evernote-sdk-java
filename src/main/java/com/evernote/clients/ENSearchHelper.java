/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.thrift.TException;

/**
 * Provides helper methods to search notes in different note stores.
 * 
 *
 * @author alexchenzl
 */
public class ENSearchHelper {

  private final ENClientFactory clientFactory;
  private final NoteStoreClient personalClient;
  private NoteStoreClient businessClient;
  // cache ENLinkedNotebookHelper objects for re-use
  private final Map<String, ENLinkedNotebookHelper> linkedNotebookHelpers =
      new HashMap<String, ENLinkedNotebookHelper>();

  public ENSearchHelper(ENClientFactory factory, NoteStoreClient client) {
    if (factory == null || client == null) {
      throw new IllegalArgumentException("All arguments must not be null!");
    }
    this.clientFactory = factory;
    this.personalClient = client;
  }

  /**
   * Searches personal notes in the user's account. It will not search shared notebook or
   * business notebook that the user has joined
   * 
   * @param search
   * @return
   * @throws EDAMUserException
   * @throws EDAMSystemException
   * @throws EDAMNotFoundException
   * @throws TException
   */
  public List<NotesMetadataList> findPersonalNotes(final SearchParam search)
      throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
    if (search == null) {
      return null;
    }
    return findNotesMetadata(search, personalClient, search.getNoteFilter());
  }

  /**
   * Searches notes in a specified LinkedNotebook. The specified LinkedNotebook is
   * corresponding to either a normal shared notebook or a business notebook the user has
   * joined.
   * 
   * @param search
   * @param linkedNotebook
   * @return
   * @throws EDAMUserException
   * @throws EDAMSystemException
   * @throws EDAMNotFoundException
   * @throws TException
   */
  public List<NotesMetadataList> findNotesInLinkedNotebook(final SearchParam search,
      final LinkedNotebook linkedNotebook) throws EDAMUserException, EDAMSystemException,
      EDAMNotFoundException, TException {

    if (search == null || linkedNotebook == null) {
      return null;
    }
    ENLinkedNotebookHelper linkedNotebookHelper = getLinkedNotebookHelper(linkedNotebook);
    if (linkedNotebookHelper == null) {
      return null;
    }
    String notebookGuid = linkedNotebookHelper.getCorrespondingNotebookGuid();

    // create a deep copy so that we don't touch the initial search request values
    NoteFilter noteFilter = new NoteFilter(search.getNoteFilter());
    noteFilter.setNotebookGuid(notebookGuid);

    return findNotesMetadata(search, linkedNotebookHelper.getSharedClient(), noteFilter);
  }

  /**
   * Searches notes within those shared notebooks that the user has joined that are
   * business notebooks in the business that the user is currently a member of.
   * 
   * @param search
   * @return
   * @throws EDAMUserException
   * @throws EDAMSystemException
   * @throws EDAMNotFoundException
   * @throws TException
   */
  public List<NotesMetadataList> findBusinessNotes(final SearchParam search)
      throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
    if (search == null) {
      return null;
    }
    getBusinessClient();
    return findNotesMetadata(search, businessClient, search.getNoteFilter());
  }

  private List<NotesMetadataList> findNotesMetadata(SearchParam search,
      NoteStoreClient client, NoteFilter filter) throws EDAMUserException,
      EDAMSystemException, EDAMNotFoundException, TException {

    List<NotesMetadataList> result = new ArrayList<NotesMetadataList>();
    final int maxNotes = search.getMaxNotes();
    int offset = search.getOffset();

    int remaining = maxNotes - offset;
    while (remaining > 0) {
      NotesMetadataList notesMetadata =
          client.findNotesMetadata(filter, offset, maxNotes, search.getResultSpec());
      offset = notesMetadata.getStartIndex() + notesMetadata.getNotesSize();
      remaining = notesMetadata.getTotalNotes() - offset;
      result.add(notesMetadata);
    }
    return result;

  }

  private NoteStoreClient getBusinessClient() throws TException, EDAMUserException,
      EDAMSystemException {
    if (businessClient == null) {
      ENBusinessNotebookHelper businessNotebookHelper =
          clientFactory.createBusinessNotebookHelper();
      businessClient = businessNotebookHelper.getBusinessClient();
    }
    return businessClient;
  }

  private ENLinkedNotebookHelper getLinkedNotebookHelper(LinkedNotebook linkedNotebook)
      throws EDAMUserException, EDAMSystemException, TException, EDAMNotFoundException {
    if (linkedNotebook != null) {
      ENLinkedNotebookHelper helper = linkedNotebookHelpers.get(linkedNotebook.getGuid());
      if (helper == null) {
        helper = clientFactory.createLinkedNotebookHelper(linkedNotebook);
        if (helper != null) {
          linkedNotebookHelpers.put(linkedNotebook.getGuid(), helper);
        }
      }
      return helper;
    }
    return null;
  }

  /**
   * A wrapper of search parameters
   * 
   */
  public static class SearchParam {
    private NoteFilter noteFilter;
    private NotesMetadataResultSpec resultSpec;
    private int offset;
    private int maxNotes;

    public SearchParam() {
      this.offset = 0;
      this.maxNotes = 256;
    }

    public NoteFilter getNoteFilter() {
      if (noteFilter == null) {
        noteFilter = new NoteFilter();
        noteFilter.setOrder(NoteSortOrder.UPDATED.getValue());
      }
      return noteFilter;
    }

    public void setNoteFilter(NoteFilter noteFilter) {
      this.noteFilter = noteFilter;
    }

    public NotesMetadataResultSpec getResultSpec() {
      if (resultSpec == null) {
        resultSpec = new NotesMetadataResultSpec();
        resultSpec.setIncludeTitle(true);
        resultSpec.setIncludeNotebookGuid(true);
      }
      return resultSpec;
    }

    public void setResultSpec(NotesMetadataResultSpec resultSpec) {
      this.resultSpec = resultSpec;
    }

    public int getOffset() {
      return offset;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }

    public int getMaxNotes() {
      return maxNotes;
    }

    public void setMaxNotes(int maxNotes) {
      this.maxNotes = maxNotes;
    }
  }

}
