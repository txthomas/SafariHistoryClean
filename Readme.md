# Safari History Cleaner
This program helps you to clean your macOS Safari history and recent closed tabs for selectable url or regular expressions.
Criteria are stored in a seperat text file.

Project is build with maven and Java 1.8. Just create a new maven project and run:
```
$ mvn clean
$ mvn package
```

To execute:

```
$ java -jar SafariHistoryClean-0.5.0-jar-with-dependencies.jar
        -d "/Users/.../Library/Safari/History.db"
        -p "/Users/.../Library/Safari/RecentlyClosedTabs.plist"
        -s searchExpressions.txt
```
The parameter for database and plist are required, if no `-s` parameter is applied the program uses the `searchExpressions.txt` in same folder as the executable.

Search file:

Each line of the search file is read as one regex statement and checked against the saved urls in Safari history. The file should look like this:

```
www\.google\.com
www\.google.*
.*google.*
^([^\/]*)(\/{0}|\/{2})([^\/]*)google.*$
```

It is preferred to use the last pattern to match all entries with given domain and not just a key word inside url.

Go to eg. _https://regex101.com_ for evaluation of your regular expressions.

**Caution:** In some rarely situations, if browser is opened before program has finished, the database can be damaged and Safari has problems to quit. Just delete the database file and restart Safari.

_(Code is quite a little bit messy... It will be fixed at ongoing progress.)_

External libs:
- sqlite-jdbc 3.15.1
- dd-plist 1.16