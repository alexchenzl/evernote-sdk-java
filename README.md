Evernote SDK for Java
==========================================

Evernote API version 1.25


Overview
--------
This SDK contains wrapper code used to call the Evernote Cloud API from Java applications.

For Android-specific code and samples, see the [Evernote SDK for Android](http://www.github.com/evernote/evernote-sdk-android).

The SDK also contains two samples. The code in `sample/oauth` demonstrates the basic use of the SDK. The code in `sample/client` also demonstrates the basic use of API, but uses developer tokens instead of OAuth for authentication.

JavaDocs for the SDK are available at http://dev.evernote.com/documentation/reference/javadoc/.

Changes in version 1.25.2
-----------------------

* Added a class ENClientFactory and deprecated ClientFactory
* Added a class ENLinkedNotebookHelper and deprecated LinkedNoteStoreClient
* Added a class ENBusinessNotebookHelper and deprecated BusinessNoteStoreClient
* Added a search helper class ENSearchHelper
* Added ENHTMLHelper to help developers download Note as HTML
* Added ENHTMLToENMLHelper to help developers transform HTML to ENML easily

Prerequisites
-------------
In order to use the Cloud API, you need to obtain an API key from http://dev.evernote.com/documentation/cloud. You'll also find full API documentation on that page.

In order to run the sample code, you need a user account on the sandbox service where you will do your development. Sign up for an account at https://sandbox.evernote.com/Registration.action

In order to run the client client sample code, you need a developer token. Developer tokens make it easy to learn your way around the API without needing to worry about OAuth. Get one at https://sandbox.evernote.com/api/DeveloperToken.action

Including the SDK in your project
---------------------------------

The easiest way to incorporate the SDK into your Java project is to use Maven. If you're using Maven already, simply add a new dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.evernote</groupId>
    <artifactId>evernote-api</artifactId>
    <version>1.25.2</version>
</dependency>
```

If you'd prefer to build the SDK yourself, it's as simple as running

```bash
$ mvn package
```

You'll find `evernote-sdk-1.25.1.jar` in the target directory after the build completes. This single JAR contains everything needed to use the API.

Sample Code - Client
------------------------
The code in `sample/client/` demonstrates the basics of using the Evernote API, using developer tokens instead of OAuth to simplify the authentication process while you're learning. Real applications that support multiple users need to use OAuth.

1. Build the SDK library

    ```bash
    $ mvn package
    ```
1. Open `sample/client/src/main/com/evernote/EDAMDemo.java`
1. Scroll down to the top of the `EDAMDemo` class and fill in your Evernote developer token.
1. On the command line, run the following command to build the project:

    ```bash
    $ cd sample/client
    $ mvn package
    ```

1. On the command line, run the following command to execute the sample app:

    ```bash
    $ java -jar target/EDAMDemo-jar-with-dependencies.jar
    ````

Sample Code - OAuth
-----------------------
Real applications use OAuth to authenticate to the Evernote service. At the end of the OAuth flow you'll have an authentication token that you can use to access the Cloud API in the same way that the developer token is used in the client sample code. The code in `sample/oauth` demonstrate the OAuth authentication process.

1. Open the file `sample/oauth/src/main/webapp/index.jsp`
1. Fill in your Evernote API consumer key and secret
1. Build the sample project:

    ```bash
    $ cd sample/oauth
    $ mvn package
    ```

1. Deploy `sample/oauth/target/EDAMWebTest.war` to your servlet container (e.g. Tomcat)
1. Load the web application in your browser (e.g. `http://localhost:8080/EDAMWebTest`)

Sample Code - Scala / Play
--------------------------
Real applications use OAuth to authenticate to the Evernote service with Scala / Play framework.  You need to install [Scala](http://www.scala-lang.org/) and [Play](http://www.playframework.com/) first.

1. Open the file `app/controllers/Evernote.scala`
1. Fill in your Evernote API consumer key and secret
1. Run project with `play run`
1. Load the web application in your browser (e.g. `http://localhost:9000`)

Sample Code - Clojure
--------------------------

A user-community Clojure wrapper to the Evernote Java SDK is available at https://github.com/mikebroberts/clojurenote . The library provides OAuth authentication, read/write capabilities (using an OAuth access token, or developer token) and ENML to HTML translation. A sample compojure / ring app is also provided to show how OAuth workflow can be implemented.
