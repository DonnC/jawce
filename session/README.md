# JAWCE Session
A JAWCE session interface and default implementations for the JAWCE engine

## Session Implementations
- File based session manager
- LMDB backed session manager
- Caffeine cache based session manager


## Examples
Refer to the [tests folder here](src/test/java/zw/co/dcl/jawce/session)


```java
// file based
FileBasedSessionManager session = new FileBasedSessionManager();

// lmdb based - uses Singleton pattern
// initialized as LmdbSessionManager(mapSizeInBytes, bufferSizeInBytes)
// LmdbSessionManager session = LmdbSessionManager.getInstance(10_485_760L, 1024);

var user1Session =  session.session("user1");

// add data to user session
user1Session.save("user1", "key1", "value1");

// save props
user1Session.saveProp("user1", "propKey", "propValue");

// get data
String result = user1Session.get("user1", "key1", String.class);

// save to global
user1Session.saveGlobal("globalKey", "globalValue");
```
