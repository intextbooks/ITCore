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

## Usage
Use the *TextbookProcessor.processFullTextbook* method to process a pdf file and create the knowledge model of the textbook. See the src/examples/ProcessingExample.java file.

## License
ITCore is licensed under the Apache 2.0 license. The licenses file contains a description of the dependencies and their licenses.

## Cite
If you find this code useful in your research, please consider citing:

@inproceedings{alpizar2019-1,
 author = {Alpizar-Chacon, Isaac and Sosnovsky, Sergey},
 title = {Expanding the Web of Knowledge: one Textbook at a Time},
 booktitle = {Proceedings of the 30th on Hypertext and Social Media},
 series = {HT '19},
 year = {2019},
 location = {Hof, Germany},
 publisher = {ACM},
 address = {New York, NY, USA}
} ;
