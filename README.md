If Twitter is a 'command-line social network' with a simple text interface, Scriptus is shell scripting.

Scriptus creates a simple JavaScript environment for programming interactions between people and programs on Twitter. Scripts are run inside a simple UNIX-like process model.

Scriptus programs are designed to run for days, months or years, waiting for user input or waking as necessary.

Scriptus uses Rhino and serialisable continuations to make long-term persistence of program state possible, and can uses either an in-memory, filesystem or AWS-backed datastore.

For more, see docs/.

You need JDK 6 or more recent and Maven 2 to build Scriptus. 
"mvn clean install" will give you a WAR file that can be deployed in any standard servlet container.
"mvn jetty:run" will immediately run Scriptus on port 8080.

The license is GPL v2 or above.