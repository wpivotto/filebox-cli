# Filebox


![image](https://user-images.githubusercontent.com/170504/161623100-a536d209-0455-42bb-ab31-65dc01b1ff7e.png)

## Features

* Easy integration, just write the files in the right folder
* Index documents on demand 
  - Asynchronous indexing
  - Automatically handles adding, changing and deleting documents
* Embedded web server with search API
* Aggregates result into a single pdf file
* Serves images with resizing support

## Basic Options

- `index.folder`: Apache Lucene index destination
- `docs.folder`: Folder where to get the documents to index
- `images.folder`: Folder where to get the images to serve 
- `highlight.enabled`: Highlight search matches in the output file
- `http.port`: The port that the embedded web server will listen on when running to handle clients

## Tokenization/Indexing Options

- `stopwords.txt`: list of stop words (used by Lucene StopFilter)
- `delimiters.txt`: list of custom word delimiters (used in tokenization)

## Running Locally

```java
mvn clean exec:java -Dexec.mainClass="br.com.wpivotto.Main"
```

## Build Windows Executable

```java
mvn clean package
```

## Basic Usage

1. Put any text file inside the configured folder. `Ex: C:\Filebox\Docs`
2. The system will extract the text from the document and index it automatically
3. Open a browser window and access the url http:\\\\localhost:`PORT`\\search?q=`TERM`
4. All search results will be grouped into a single PDF document and made available for download

## API Usage

**Search Query**
----
  Returns a single PDF file with all document matches (See Lucene Query Syntax)

* **URL**

 `/search?q=[query]`

* **Method:**

  `GET`
  
*  **URL Params**

   **Required:**
 
   `q=[string]`

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** Complete data stream of the file contents.

* **Sample Call:**

 `GET http://localhost:8099/search?q=test`

## Use Cases

* search in electrical projects
* search in instruction manuals
* search in calibration reports
* search in piping and instrumentation diagrams (P&ID)
* show pictures in embedded browsers (ActiveX) on SCADA Systems 

## TODO

- [x] Handle text orientation in tokenizer 
- [ ] Support for pattern replace (CAD files tend to export words without spacing)
 
