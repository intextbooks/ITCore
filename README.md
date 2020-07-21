# ITCore (Intelligent Textbooks Core)
ITCore processes PDF textbooks and produces a TEI knowledge model of the textbook, which contains: structural (chapter and subchapters), content (textual), and domain terms (index terms) information.

## Web Application
Visit https://intextbooks.science.uu.nl/ to access the web application version of this project.

## Installation
The code is shared as a maven project.

The lib/ folder contains the jar dependencies that are not part of the maven repository.

The data/ folder contains some external models and files that are necessary for the creation of the knowledge models.

The src/config.xml file contains all the properties that need to be configured before running the code.

The src/intextbooks_db.sql file contains the database schema for the project. MySQL is required.

### Python dependencies
The project requires Python >= 3.7. The following libraries should be installed: nltk >= 3.5, spacy >= 2.3.2 (with the "en_core_web_sm" model), and jep >= 3.9.0. In the configuration file, use the property *jepLibraryPath* to set the path to the Jep's built C library in your local machine. See: https://github.com/ninia/jep/wiki/FAQ#how-do-i-fix-unsatisfied-link-error-no-jep-in-javalibrarypath

### DBpedia
THe enrichment of terms usign DBpedia requires a local copy of DBpedia. The required properties should be ingested into a TDB store (Apache Jena). The require properties are:

-----------------------------------------------------
| property                                            |
=======================================================
| <http://dbpedia.org/ontology/wikiPageWikiLink>      |
| <http://dbpedia.org/ontology/wikiPageRedirects>     |
| <http://dbpedia.org/ontology/abstract>              |
| <http://purl.org/dc/terms/subject>                  |
| <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>   |
| <http://www.w3.org/2004/02/skos/core#prefLabel>     |
| <http://www.w3.org/2004/02/skos/core#broader>       |
| <http://dbpedia.org/ontology/wikiPageDisambiguates> |
| <http://www.w3.org/2004/02/skos/core#related>       |
-------------------------------------------------------
In the configuration file, use the property *tdbDirectory* to set the path to the TDB store.

## Usage
Use the *TextbookProcessor.processFullTextbook* method to process a pdf file and create the knowledge model of the textbook. See the src/examples/ProcessingExample.java file.

## License
ITCore is licensed under the Apache 2.0 license. The licenses file contains a description of the dependencies and their licenses.

## Cite
If you find this code useful in your research, please consider citing:

@inproceedings{alpizar2020-pdf,
  author={Alpizar-Chacon, Isaac and Sosnovsky, Sergey},
  title={Order out of Chaos: Construction of Knowledge Models from PDF Textbooks},
  booktitle={Proceedings of the ACM Symposium on Document Engineering 2020},
  year={2020}
}

@inproceedings{alpizar2019-web,
 author = {Alpizar-Chacon, Isaac and Sosnovsky, Sergey},
 title = {Expanding the Web of Knowledge: one Textbook at a Time},
 booktitle = {Proceedings of the 30th on Hypertext and Social Media},
 series = {HT '19},
 year = {2019},
 location = {Hof, Germany},
 publisher = {ACM},
 address = {New York, NY, USA}
} ;
