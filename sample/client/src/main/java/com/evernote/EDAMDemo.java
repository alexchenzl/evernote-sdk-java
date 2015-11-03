/*
 * Evernote API sample code, structured as a simple command line application that
 * demonstrates several API calls.
 * 
 * To compile (Unix): javac -classpath ../../target/evernote-api-*.jar EDAMDemo.java
 * 
 * To run: java -classpath ../../target/evernote-api-*.jar EDAMDemo
 * 
 * Full documentation of the Evernote API can be found at
 * http://dev.evernote.com/documentation/cloud/
 */
package com.evernote;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ENBusinessNotebookHelper;
import com.evernote.clients.ENClientFactory;
import com.evernote.clients.ENHTMLHelper;
import com.evernote.clients.ENHTMLToENMLHelper;
import com.evernote.clients.ENLinkedNotebookHelper;
import com.evernote.clients.ENSearchHelper;
import com.evernote.clients.ENSearchHelper.SearchParam;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Data;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;
import com.evernote.edam.type.Tag;
import com.evernote.enml.ResourceFetcher;
import com.evernote.enml.SimpleResourceFetcher;
import com.evernote.enml.converter.HTMLNodeHandler;
import com.evernote.thrift.TException;
import com.evernote.thrift.transport.TTransportException;

public class EDAMDemo {

  /***************************************************************************
   * You must change the following values before running this sample code *
   ***************************************************************************/

  // Real applications authenticate with Evernote using OAuth, but for the
  // purpose of exploring the API, you can get a developer token that allows
  // you to access your own Evernote account. To get a developer token, visit
  // https://sandbox.evernote.com/api/DeveloperToken.action
  private static final String AUTH_TOKEN = "your developer token";

  // Evernote has three separated services:
  // EvernoteService.SANDBOX: https://sandbox.evernote.com
  // EvernoteService.PRODUCTION: https://www.evernote.com
  // EvernoteService.YINXIANG : https://app.yinxiang.com (Evernote China service)
  //
  // For more information to support both Evernote and Evernote China service in your
  // application, please refer to https://dev.evernote.com/doc/articles/bootstrap.php
  private static final EvernoteService service = EvernoteService.SANDBOX;

  /***************************************************************************
   * You shouldn't need to change anything below here to run sample code *
   ***************************************************************************/
  private static ENClientFactory factory;
  private static boolean isBusinessUser = false;

  /**
   * Console entry point.
   */
  public static void main(String args[]) throws Exception {
    String token = System.getenv("AUTH_TOKEN");

    if (token == null) {
      token = AUTH_TOKEN;
    }
    if ("your developer token".equals(token)) {
      System.err.println("Please fill in your developer token");
      System.err
          .println(
              "To get a developer token, go to https://sandbox.evernote.com/api/DeveloperToken.action");
      return;
    }

    EDAMDemo demo = new EDAMDemo(token);
    try {

      NoteStoreClient noteStore = factory.createNoteStoreClient();
      demo.listNotebooks(noteStore);
      Note createdNote = demo.createNote(noteStore);
      demo.updateNoteTag(noteStore, createdNote.getGuid());

      LinkedNotebook linkedNotebook = null;
      List<LinkedNotebook> list = demo.listPersonalLinkedNotebook(noteStore);
      if (list != null && list.size() > 0) {
        for (LinkedNotebook lnb : list) {
          if (lnb.getShareKey() != null) {
            linkedNotebook = lnb;
          }
        }
      }
      if (linkedNotebook != null) {
        // create note in LinkedNotebook
        demo.createNoteInLinkedNotebook(linkedNotebook);
      }

      if (isBusinessUser) {
        LinkedNotebook bizLinkedNotebook = demo.createBusinessNotebook();
        demo.createNoteInBusinessNotebook(bizLinkedNotebook);
      }

      // search notes in business store
      demo.search(noteStore, linkedNotebook);

      // Clip a web page
      demo.clipWebPage(noteStore);

      // download a note's content as HTML, and save it as a local file
      demo.downloadNoteAsHtmlFile(createdNote.getGuid());

    } catch (EDAMUserException e) {
      // These are the most common error types that you'll need to
      // handle
      // EDAMUserException is thrown when an API call fails because a
      // paramter was invalid.
      if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
        System.err.println("Your authentication token is expired!");
      } else if (e.getErrorCode() == EDAMErrorCode.INVALID_AUTH) {
        System.err.println("Your authentication token is invalid!");
      } else if (e.getErrorCode() == EDAMErrorCode.QUOTA_REACHED) {
        System.err.println("Your authentication token is invalid!");
      } else {
        System.err.println("Error: " + e.getErrorCode().toString() + " parameter: "
            + e.getParameter());
      }
    } catch (EDAMSystemException e) {
      System.err.println("System error: " + e.getErrorCode().toString());
    } catch (TTransportException t) {
      System.err.println("Networking error: " + t.getMessage());
    }
  }

  /**
   * Intialize UserStore and NoteStore clients. During this step, we authenticate with the
   * Evernote web service. All of this code is boilerplate - you can copy it straight into
   * your application.
   */
  public EDAMDemo(String token) throws Exception {
    // Set up the UserStore client and check that we can speak to the server
    EvernoteAuth evernoteAuth = new EvernoteAuth(service, token);
    factory = new ENClientFactory(evernoteAuth);
    UserStoreClient userStore = factory.createUserStoreClient();

    boolean versionOk =
        userStore.checkVersion("Evernote EDAMDemo (Java)",
            com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
            com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
    if (!versionOk) {
      System.err.println("Incompatible Evernote client protocol version");
      System.exit(1);
    }

    if (userStore.isBusinessUser()) {
      isBusinessUser = true;
      System.out.println("You are a business user of "
          + userStore.getUser().getBusinessUserInfo().getBusinessName());
    }
    System.out.println();
  }

  private void listNotebooks(NoteStoreClient noteStore) throws Exception {
    System.out.println("Listing notebookss:");
    List<Notebook> notebooks = noteStore.listNotebooks();
    for (Notebook notebook : notebooks) {
      System.out.println("\tNotebook: " + notebook.getName());
    }
    System.out.println();

  }

  /**
   * Create a new note containing a little text and the Evernote icon.
   */
  private Note createNote(NoteStoreClient noteStore) throws Exception {

    // Finally, send the new note to Evernote using the createNote method
    // The new Note object that is returned will contain server-generated
    // attributes such as the new note's unique GUID.
    Note createdNote = noteStore.createNote(buildNote(null));
    String newNoteGuid = createdNote.getGuid();

    System.out.println("Successfully created a new note with GUID: " + newNoteGuid);
    System.out.println();
    return createdNote;
  }

  private Resource buildResource(String filename, String mime) throws Exception {
    Resource resource = new Resource();
    resource.setData(readFileAsData(filename));
    resource.setMime(mime);

    ResourceAttributes attributes = new ResourceAttributes();
    attributes.setFileName(filename);
    resource.setAttributes(attributes);
    return resource;
  }

  private Note buildNote(String notebookGuid) throws Exception {
    // To create a new note, simply create a new Note object and fill in
    // attributes such as the note's title.
    Note note = new Note();

    // Please make sure there's no blank characters on both ends of the title string.
    // Blank characters in ENML includes whitespace, tab, non-breaking space, \t \n and
    // other Unicode control characters
    String title = "Test note from Java SDK Demo";
    note.setTitle(title);

    String filename = "/enlogo.png";
    String mime = "image/png";

    // In order to add an image or other kinds of files into a note, you have to create a
    // Resource object first
    Resource resource = buildResource(filename, mime);

    // Now, add the new Resource to the note's list of resources
    note.addToResources(resource);

    // To display the Resource as part of the note's content, include an
    // <en-media>
    // tag in the note's ENML content. The en-media tag identifies the
    // corresponding
    // Resource using the MD5 hash.
    String hashHex = bytesToHex(resource.getData().getBodyHash());

    // The content of an Evernote note is represented using Evernote Markup
    // Language
    // (ENML). The full ENML specification can be found in the Evernote API
    // Overview
    // at http://dev.evernote.com/documentation/cloud/chapters/ENML.php
    String content =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
            + "<en-note>" + "<p>This is EDAMDemo</p>"
            + "<span style=\"color:green;\">Here's the Evernote logo:</span><br/>"
            + "<en-media type=\"image/png\" hash=\"" + hashHex + "\"/>" + "</en-note>";
    note.setContent(content);
    note.setNotebookGuid(notebookGuid);
    return note;
  }

  /**
   * Update the tags assigned to a note. This method demonstrates how only modified fields
   * need to be sent in calls to updateNote.
   */
  private void updateNoteTag(NoteStoreClient noteStore, String newNoteGuid)
      throws Exception {
    // When updating a note, it is only necessary to send Evernote the
    // fields that have changed. For example, if the Note that you
    // send via updateNote does not have the resources field set, the
    // Evernote server will not change the note's existing resources.
    // If you wanted to remove all resources from a note, you would
    // set the resources field to a new List<Resource> that is empty.

    // If you are only changing attributes such as the note's title or tags,
    // you can save time and bandwidth by omitting the note content and
    // resources.

    // In this sample code, we fetch the note that we created earlier,
    // including
    // the full note content and all resources. A real application might
    // do something with the note, then update a note attribute such as a
    // tag.
    Note note = noteStore.getNote(newNoteGuid, true, true, false, false);

    // Do something with the note contents or resources...

    // Now, update the note. Because we're not changing them, we unset
    // the content and resources. All we want to change is the tags.
    note.unsetContent();
    note.unsetResources();

    // We want to apply the tag "TestTag"
    note.addToTagNames("TestTag");

    // Now update the note. Because we haven't set the content or resources,
    // they won't be changed.
    noteStore.updateNote(note);
    System.out.println("Successfully added tag to existing note");

    // To prove that we didn't destroy the note, let's fetch it again and
    // verify that it still has 1 resource.
    note = noteStore.getNote(newNoteGuid, false, false, false, false);
    System.out.println("After update, note has " + note.getResourcesSize()
        + " resource(s)");
    System.out.println("After update, note tags are: ");
    for (String tagGuid : note.getTagGuids()) {
      Tag tag = noteStore.getTag(tagGuid);
      System.out.println("* " + tag.getName());
    }

    System.out.println();
  }

  /**
   * Helper method to read the contents of a file on disk and create a new Data object.
   */
  private Data readFileAsData(String fileName) throws Exception {
    // Read the full binary contents of the file
    InputStream in = getClass().getResourceAsStream(fileName);

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    byte[] block = new byte[10240];
    int len;
    while ((len = in.read(block)) >= 0) {
      byteOut.write(block, 0, len);
    }
    in.close();
    byte[] body = byteOut.toByteArray();

    // Create a new Data object to contain the file contents
    Data data = new Data();
    data.setSize(body.length);
    data.setBodyHash(MessageDigest.getInstance("MD5").digest(body));
    data.setBody(body);

    return data;
  }

  /**
   * Helper method to convert a byte array to a hexadecimal string.
   */
  public static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte hashByte : bytes) {
      int intVal = 0xff & hashByte;
      if (intVal < 0x10) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(intVal));
    }
    return sb.toString();
  }

  /**
   * It will exclude business LinkedNotebook
   * 
   */
  private List<LinkedNotebook> listPersonalLinkedNotebook(NoteStoreClient noteStore)
      throws EDAMUserException, EDAMNotFoundException, EDAMSystemException, TException {

    List<LinkedNotebook> list = noteStore.listLinkedNotebooks();

    if (list != null && list.size() > 0) {
      Iterator<LinkedNotebook> it = list.iterator();
      while (it.hasNext()) {
        LinkedNotebook lnb = it.next();
        if (ENBusinessNotebookHelper.isBusinessNotebook(lnb)) {
          it.remove();
        }
      }
    }

    if (list == null || list.size() == 0) {
      System.out
          .println("You have never joined any notebooks that others shared with you");
    } else {
      System.out.println("Your LinkedNotebook list:");
      int idx = 1;
      for (LinkedNotebook lnb : list) {
        System.out.println("\t" + idx + "\t" + lnb.getShareName());
        idx++;
      }
    }
    System.out.println();
    return list;
  }

  private Note createNoteInLinkedNotebook(LinkedNotebook linkedNotebook)
      throws Exception {

    ENLinkedNotebookHelper helper = factory.createLinkedNotebookHelper(linkedNotebook);
    if (helper.isNotebookWritable()) {
      // Get guid of corresponding Notebook
      String notebookGuid = helper.getCorrespondingNotebookGuid();
      Note note = buildNote(notebookGuid);
      note = helper.createNoteInLinkedNotebook(note);

      System.out.println("Successfully created a note " + note.getGuid()
          + " in linked notebook " + linkedNotebook.getShareName());
      System.out.println();
      return note;
    }

    System.out.println("This notebook is not writable.");
    System.out.println();
    return null;
  }

  private LinkedNotebook createBusinessNotebook() throws Exception {
    ENBusinessNotebookHelper businessHelper = factory.createBusinessNotebookHelper();
    Notebook notebook = new Notebook();
    notebook.setName("TestBizNotebook");
    LinkedNotebook linkedNotebook = businessHelper.createBusinessNotebook(notebook);

    System.out
        .println(
            "Successfully created a new business notebook, its LinkedNotebook GUID is "
                + linkedNotebook.getGuid());
    System.out.println();
    return linkedNotebook;
  }

  // The process is the same as creating notes in LinkedNotebook in fact
  private Note createNoteInBusinessNotebook(LinkedNotebook linkedNotebook)
      throws Exception {

    ENLinkedNotebookHelper helper = factory.createLinkedNotebookHelper(linkedNotebook);
    String notebookGuid = helper.getCorrespondingNotebookGuid();
    Note note = buildNote(notebookGuid);
    note = helper.createNoteInLinkedNotebook(note);

    System.out.println("Successfully created a note " + note.getGuid()
        + " in business notebook " + linkedNotebook.getShareName());
    System.out.println();
    return note;
  }

  private void search(NoteStoreClient noteStore, LinkedNotebook linkedNotebook)
      throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {

    // At first, create the NoteFilter
    // About the grammer of search query, please refer to
    // http://dev.evernote.com/documentation/cloud/chapters/Searching_notes.php
    NoteFilter filter = new NoteFilter();
    String query = "EDAMDemo";
    filter.setWords(query);
    filter.setOrder(NoteSortOrder.UPDATED.getValue());
    filter.setAscending(false);

    // Secondly, create the NotesMetadataResultSpec
    NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
    resultSpec.setIncludeTitle(true);
    resultSpec.setIncludeUpdated(true);
    resultSpec.setIncludeAttributes(true);

    // Thirdly create a SearchParam object to wrapp all parameters
    SearchParam search = new SearchParam();
    search.setNoteFilter(filter);
    search.setResultSpec(resultSpec);
    search.setMaxNotes(10);

    // create a search helper
    ENSearchHelper helper = new ENSearchHelper(factory, noteStore);

    // Search personal notes
    List<NotesMetadataList> result = helper.findPersonalNotes(search);
    System.out.println("findPersonalNotes Result:");
    printSearchResult(result);
    System.out.println();

    if (linkedNotebook != null) {
      // search in LinkedNotebook
      result = helper.findNotesInLinkedNotebook(search, linkedNotebook);
      System.out.println("findNotesInLinkedNotebook Result:");
      printSearchResult(result);
      System.out.println();
    }

    // search in Business Notebook
    if (isBusinessUser) {
      result = helper.findBusinessNotes(search);
      System.out.println("findNotesInLinkedNotebook Result:");
      printSearchResult(result);
      System.out.println();
    }

  }

  private void printSearchResult(List<NotesMetadataList> result) {
    if (result == null || result.size() == 0) {
      System.out.println("Nothing");
    } else {
      for (NotesMetadataList list : result) {
        int idx = 1;
        for (NoteMetadata data : list.getNotes()) {
          System.out.println("\t" + idx + "\tGuid:" + data.getGuid() + "\tTitle:"
              + data.getTitle());
          idx++;
        }
      }
    }
    System.out.println();
  }

  /**
   * 
   * Because HTMLToENMLHelper can not process pseudo classes and pseudo elements, we need
   * to implement a customized node handler to clip blogs on blog.evernote.com, so that we
   * can get a better layout.
   * 
   */
  final class EvernoteBlogNodeHandler implements HTMLNodeHandler {

    public void initialize() {
      // Don't need initialization here
    }

    public boolean process(Node node, ResourceFetcher fetcher) {
      if (node instanceof Element) {
        Element element = (Element) node;
        String className = element.className();
        if (className != null) {
          // Class top and bottom use :after to clear float, but it can not be handled by
          // the converter. So we can add a div tag to implement the same effect in ENML
          if (className.equals("top") || className.equals("bottom")) {
            element.after("<div style=\"clear:both;\"></div>");
          } else if (className.contains("premium-callout")) {
            // False means the node should be removed from the DOM tree, but do NOT
            // remove it here
            return false;
          }
        }
      }
      // True means this element needs further process
      return true;
    }

    public String extractKeywords(Document doc) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private Note clipWebPage(NoteStoreClient noteStore) throws EDAMUserException,
      EDAMSystemException, EDAMNotFoundException, TException, IOException {

    // The SimpleResourceFetcher is designed just for demonstration. You should implement
    // your own ResourceFetcher. If you are developing a server application, a connection
    // pool should be used to implement this interface. If you are developing an Android
    // application, OkHttpClient may be a good choice to use.

    ResourceFetcher fetcher = new SimpleResourceFetcher();

    // If you want to add a customized process step before the built-in transformation
    // steps, please implement your own HTMLElementHandler.

    HTMLNodeHandler handler = new EvernoteBlogNodeHandler();

    ENHTMLToENMLHelper helper = factory.createHTMLToENMLHelper(fetcher, handler);

    String url =
        "https://blog.evernote.com/blog/2015/07/13/astronaut-ron-garans-space-inspired-lessons-on-collaboration/";

    // use a CSS like selector to clip specified components of this web page. For more
    // information, please refer to
    // http://jsoup.org/cookbook/extracting-data/selector-syntax
    //
    // Here we only want to clip the main content of this page

    String selector = "section.post";
    Note note = helper.buildNoteFromURL(url, selector);
    note = noteStore.createNote(note);
    System.out.println("Content of this page " + url + " \nis saved to a new note "
        + note.getGuid());
    System.out.println();
    return note;
  }

  /**
   * ENML content of a Note will be downloaded as an HTML snippet. Resources in this note
   * will also be downloaded to local disk
   * 
   */
  private void downloadNoteAsHtmlFile(String noteGuid) throws TTransportException,
      EDAMUserException, EDAMSystemException, TException, IOException {

    ResourceFetcher fetcher = new SimpleResourceFetcher();

    ENHTMLHelper helper = factory.createHTMLHelper(fetcher);

    String html = helper.downloadNoteAsHtml(noteGuid, "", "");

    if (html != null) {
      String filename = "demo.html";
      File file = new File(filename);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(html);
      bw.close();
      System.out.println("The content of this note " + noteGuid + " was saved as file "
          + file.getAbsoluteFile());
      System.out.println();
    }
  }

}
