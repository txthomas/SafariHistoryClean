# Safari History Cleaner
This is programm helps you to clean your macOS Safari history for selectable url or regular expressions.
Criteria are stored in a seperat text file.

Project is build with maven. Just create a new project and run:
```
$ mvn clean
$ mvn package
```

To execute:

`java -jar SafariHistoryClean-0.5.0-jar-with-dependencies.jar -d "/Users/.../Library/Safari/History.db" -s searchExpressions.txt`

Search file:

Each line of the search file is read as one regex statement and checked against the saved urls in Safari history. The file should look like this:

```
www\.google\.com
www\.google.*
.*google.*
```
It is preferred to use the last pattern to match all entries with given domain.