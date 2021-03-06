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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.SharedNotebook;
import com.evernote.thrift.TException;

/**
 * Provides several helper methods to access business notebooks and notes.
 * <p>
 * It's NOT thread safe.
 * 
 * @author alexchenzl
 * 
 */
public class ENBusinessNotebookHelper {
  private final NoteStoreClient businessClient;
  private final NoteStoreClient personalClient;
  private final String businessUsername;
  private final String businessUserShardId;

  /**
   * @param businessClient The note store client referencing the business note store.
   * @param personalClient The personal note store client
   * @param businessUsername The name of the business user.
   * @param businessUserShardId The shard ID of the business user.
   */
  public ENBusinessNotebookHelper(NoteStoreClient businessClient,
      NoteStoreClient personalClient, String businessUsername,
      String businessUserShardId) {

    if (businessClient == null || personalClient == null || businessUsername == null
        || businessUserShardId == null) {
      throw new IllegalArgumentException("All arguments must not be null!");
    }

    this.businessClient = businessClient;
    this.personalClient = personalClient;
    this.businessUsername = businessUsername;
    this.businessUserShardId = businessUserShardId;
  }

  public NoteStoreClient getPersonalClient() {
    return personalClient;
  }

  /**
   * @return The note store client referencing the business note store.
   */
  public NoteStoreClient getBusinessClient() {
    return businessClient;
  }

  /**
   * @return The business user name.
   */
  public String getBusinessUsername() {
    return businessUsername;
  }

  /**
   * @return The shard ID for this business user.
   */
  public String getBusinessUserShardId() {
    return businessUserShardId;
  }

  /**
   * @return A list of {@link LinkedNotebook}s, which all have a business ID.
   */
  public List<LinkedNotebook> listBusinessNotebooks() throws EDAMUserException,
      EDAMSystemException, TException, EDAMNotFoundException {
    List<LinkedNotebook> businessNotebooks = new ArrayList<LinkedNotebook>(personalClient
        .listLinkedNotebooks());
    Iterator<LinkedNotebook> iterator = businessNotebooks.iterator();
    while (iterator.hasNext()) {
      if (!isBusinessNotebook(iterator.next())) {
        iterator.remove();
      }
    }
    return businessNotebooks;
  }

  /**
   * @param notebook The business notebook to be created.
   * @return The new created {@link LinkedNotebook}, which has an business ID.
   */
  public LinkedNotebook createBusinessNotebook(Notebook notebook) throws TException,
      EDAMUserException, EDAMSystemException, EDAMNotFoundException {

    Notebook newCreatedNotebook = businessClient.createNotebook(notebook);

    List<SharedNotebook> sharedNotebooks = newCreatedNotebook.getSharedNotebooks();
    SharedNotebook sharedNotebook = sharedNotebooks.get(0);

    LinkedNotebook linkedNotebook = new LinkedNotebook();
    linkedNotebook.setShareKey(sharedNotebook.getShareKey());
    linkedNotebook.setShareName(newCreatedNotebook.getName());
    linkedNotebook.setUsername(businessUsername);
    linkedNotebook.setShardId(businessUserShardId);

    return personalClient.createLinkedNotebook(linkedNotebook);
  }

  /**
   * @return {@code true} if this linked notebook has a business ID.
   */
  public static boolean isBusinessNotebook(LinkedNotebook linkedNotebook) {
    return linkedNotebook.isSetBusinessId();
  }

}
