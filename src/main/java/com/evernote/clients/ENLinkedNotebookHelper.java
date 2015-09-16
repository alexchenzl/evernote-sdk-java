/*
 * Copyright 2015 Evernote Corporation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.evernote.clients;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.SharedNotebook;
import com.evernote.edam.userstore.PublicUserInfo;
import com.evernote.thrift.TException;

/**
 * Provides several helper methods for LinkedNotebook.
 * <p>
 * It's NOT thread safe.
 * 
 * @author alexchenzl
 * 
 */
public class ENLinkedNotebookHelper {
  private final NoteStoreClient sharedClient;
  private final LinkedNotebook linkedNotebook;
  private final PublicUserInfo publicUserInfo;
  private String correspondingNotebookGuid;

  /**
   * @param client {@link NoteStoreClient} object of the note store that owns the
   *          corresponding Notebook of this LinkedNotebook.
   * @param linkedNotebook The desired linked notebook.
   *
   */
  public ENLinkedNotebookHelper(NoteStoreClient client, LinkedNotebook linkedNotebook) {
    if (client == null || linkedNotebook == null) {
      throw new IllegalArgumentException("All arguments must not be null!");
    }
    this.sharedClient = client;
    this.linkedNotebook = linkedNotebook;
    this.publicUserInfo = null;
  }

  /**
   * If the LinkedNotebook is a public Notebook, please use this constructor
   * 
   * @param client {@link NoteStoreClient} object of the note store that owns the
   *          corresponding Notebook of this LinkedNotebook.
   * @param linkedNotebook The desired linked notebook.
   * @param publicUserInfo public user information of the user who owns this public
   *          notebook
   * 
   */
  public ENLinkedNotebookHelper(NoteStoreClient client, LinkedNotebook linkedNotebook,
      PublicUserInfo publicUserInfo) {
    if (client == null || linkedNotebook == null || publicUserInfo == null) {
      throw new IllegalArgumentException("All arguments must not be null!");
    }
    this.sharedClient = client;
    this.linkedNotebook = linkedNotebook;
    this.publicUserInfo = publicUserInfo;
  }

  /**
   * @return The note store client referencing the shared notebook's note store.
   */
  public NoteStoreClient getSharedClient() {
    return sharedClient;
  }

  /**
   * @return The LinkedNotebook
   */
  public LinkedNotebook getLinkedNotebook() {
    return linkedNotebook;
  }

  /**
   * @param note The new note.
   * @return The new created note from the server.
   */
  public Note createNoteInLinkedNotebook(Note note) throws EDAMUserException,
      EDAMSystemException, TException, EDAMNotFoundException {
    String guid = getCorrespondingNotebookGuid();
    note.setNotebookGuid(guid);
    return sharedClient.createNote(note);
  }

  /**
   * @return The GUID of the corresponding notebook for this linked notebook.
   */
  public String getCorrespondingNotebookGuid() throws TException, EDAMUserException,
      EDAMSystemException, EDAMNotFoundException {
    if (correspondingNotebookGuid == null) {
      if (publicUserInfo != null) {
        getCorrespondingNotebook();
      } else {
        SharedNotebook sharedNotebook = sharedClient.getSharedNotebookByAuth();
        correspondingNotebookGuid = sharedNotebook.getNotebookGuid();
      }
    }
    return correspondingNotebookGuid;
  }

  /**
   * @return The corresponding notebook for this linked notebook.
   */
  public Notebook getCorrespondingNotebook() throws TException, EDAMUserException,
      EDAMSystemException, EDAMNotFoundException {
    if (publicUserInfo != null) {
      Notebook notebook = sharedClient.getPublicNotebook(publicUserInfo.getUserId(),
          linkedNotebook.getUri());
      correspondingNotebookGuid = notebook.getGuid();
      return notebook;
    } else {
      String guid = getCorrespondingNotebookGuid();
      return sharedClient.getNotebook(guid);
    }
  }

  /**
   * @return {@code true} if this linked notebook is writable.
   */
  public boolean isNotebookWritable() throws EDAMUserException, TException,
      EDAMSystemException, EDAMNotFoundException {
    Notebook notebook = getCorrespondingNotebook();
    return !notebook.getRestrictions().isNoCreateNotes();
  }

  /**
   * @return {@code true} if this linked notebook is a public LinkedNotebook.
   */
  public boolean isPublic() {
    if (linkedNotebook.getUri() != null) {
      return true;
    }
    return false;
  }

}
