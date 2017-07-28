# [SJPU Settings Provider]

## Abstract

Settings Provider implements a simple binding from .properties files to Java objects.  
It support tracking file content changes with automatic thread-safe substitution of new data in runtime.

## Description
Data object is declared with interface with getters. During data parsing 

By default mapping of values from .properties file works as follows: camel-hump property name (for example `myFieldName`) associated with 
dot-separated property name (`my.field.name`). Accessing to data could be declared as getter (`getMyFieldName()`) as well as 
method named by field name (`myFieldName()`)

Default mapping could be overriding by `@PropertyName` annotation. 

## Examples

#### Simple usage:
1. A .properties file with data (**user.properties**)  
![user.properties](docs/user-properties.png)
1. Declare interface for mapping data from .properties file **UserSettings.java**  
![UserSettings.java](docs/UserInterface-java.png)
1. Load settings from Java code. Settings could be loaded from resource, file or by URL **SimpleExample.java**   
![SimpleExample.java](docs/SimpleExample-java.png) 
2. Output:  
```
Loaded settings: UserSettings [password (password) = "MyPassword"; email (email) = "anon@example.org"; username (username) = "Anonymous"]
```

#### Optional fields
By default all the properties are defined as mandatory. This means if there is no mapping from data object interface 
field to property whole object could not be constructed. There are two ways to deal with it:
1. Add `@Optional` annotation to a field - this will allow to return `null` value. (not applicable to primitives)
2. Add `@DefaultValue` annotation to a field - if mapping is not found a value from annotation will be used.

Here is an example to show both variants.

1. Data object interface (**HostSettings.java**):  
![HostSettings.java](docs/HostSettings-java.png)
2. Properties files for test:
**all-set.properties**  
![all-set.properties](docs/all-set-properties.png)  
**default-port.properties**  
![default-port.properties](docs/default-port-properties.png)  
**optional-host.properties**   
![optional-host.properties](docs/optional-host-properties.png)  
3. Demonstration class **OptionalExample.java**
![OptionalExample.java](docs/OptionalExample-java.png)   
4. Outputs:
```
All set: HostSettings [targetPort (target.port) = "443"; targetHost (target.host) = "localhost"]  
Default port: HostSettings [targetPort (target.port) = "80"; targetHost (target.host) = "localhost"]  
Optional host: HostSettings [targetPort (target.port) = "443"; targetHost (target.host) = "null"]  
localhost:443
```

#### More examples
... will be shown later ...