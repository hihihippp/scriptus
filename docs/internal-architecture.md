
#Scriptus: application architecture

The Scriptus codebase can be divided into several sections.

The goal of the architecture is to run script processes, persisting them in the datastore between API calls, and allowing them to interact with users via a transport.

The datastore object is responsible for any short- or long-term persistence of Script processes.

The transport object is used by the script process (via the API) to interact with users.

The transport and datastore have three separate implementations each, and which one is used at runtime is configured in `ScriptusConfig`
 and the property file backing it. Scriptus is reloaded when the settings are changed via the admin interface.

The process scheduler is responsible for executing script processes, and administering the task scheduler used for waking sleeping processes and managing timeouts.

The process scheduler has two subordinate classes, which are responsible for the waking and global per-process locking.

The `ScriptProcess` object contains the logic used to execute the scripts, persist the process as a continuation, and interact with the API.

The API itself is implemented in the `ScriptusAPI` object, which serves as the "global" object of the JavaScript environment, in much the same way that "window" does in a browser.

The API calls are implemented using a visitor pattern. Each call has an object e.g. the class Fork, which overrides `visit()` to execute the specific call logic.

The `visit` method takes a facade object that exposes almost all Scriptus internals to the API call implementation code.

The facade is also useful for overriding functionalities in test-cases to ensure they are invoked and work as intended.

##Testing

JUnit is used for all the tests.

`Testcase_ScriptusBasics` runs a battery of tests they checks all the API calls and ensures they work correctly.

`Testcase_ScriptusDAO` runs the tests that enforce the contract of the datastore implementations.