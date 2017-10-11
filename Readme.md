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
$ java -jar SafariHistoryClean-0.7.0-jar-with-dependencies.jar
        -d "/Users/.../Library/Safari/History.db"
        -p "/Users/.../Library/Safari/RecentlyClosedTabs.plist"
        -s "{executionPath}/searchExpressions.txt"
```
All parameters are optional. If nothing is applied the default values are as listed above.

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

### Caution:
In some situations the database can be damaged and Safari isn't able to read the database and quits.
For this reason a backup file (History_backup.db) is created. Just delete the database file, rename the backup and restart Safari or complete delete database file if it is not needed.
Still investigating the issue and searching for a solution...

####Update:
It seems, that since Safari 11 the database won't be unreadable anymore. Further observations required! Stay tuned...

### Disclaimer:
Use this program at your own risk. I'm not responsible if something goes wrong.

_(Code is quite a little bit messy... It will be fixed at ongoing progress.)_

External libs:
- sqlite-jdbc 3.20.1
- dd-plist 1.16