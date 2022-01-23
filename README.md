# KiORM - Simple and powerful MySQL library for tiny projects

> Notice: KiORM is still in SNAPSHOT state. The code is not tested, there is no Javadoc, the API may change unexpectedly. If that doesn't bother you, welcome! 😎

KiORM is simple MySQL library built on top of JDBC, that was originally created to build Minecraft plugins, but can be used anywhere. It provides some basic data serialization/deserialization functionality that makes it easy to map classes to SQL tables and backwards. Note, that it does not cover SQL relations mapping. KiORM was build to make database interactions require as less code as possible, so you don't need to worry about drivers, etc. Just make sure, that [mysql-connector-java](https://mvnrepository.com/artifact/mysql/mysql-connector-java) is included to your runtime and enjoy coding 🧑‍💻

![Java 8](https://img.shields.io/badge/java%20version-8%2b-orange)
![Open issues](https://img.shields.io/github/issues-raw/Rikonardo/KiORM)

💼 **This readme contains full library documentation/tutorial!**

## Install
❗ **SNAPSHOT! Not for production use!**

Gradle:
```groovy
repositories {
    maven {
        name = 'rikonardo-repository-snapshots'
        url = 'https://maven.rikonardo.com/snapshots/'
    }
}

dependencies {
    implementation 'com.rikonardo.kiorm:KiORM:1.0.0-SNAPSHOT'
}
```
Maven:
```xml
<project>
    <repositories>
        <repository>
            <id>rikonardo-repository-snapshots</id>
            <name>Rikonardo's repo (snapshots)</name>
            <url>https://maven.rikonardo.com/snapshots/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.rikonardo.kiorm</groupId>
            <artifactId>KiORM</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

If you are using it in Minecraft plugin, you can just shade this library into your jar using gradle Shadow plugin

## Documentation

| Content                                                         |
|-----------------------------------------------------------------|
| **1. [Basic usage](#basic-usage)**                              |
| **2. [SQL closures](#sql-closures)**                            |
| **3. [Computed fields](#computed-fields)**                      |
| **4. [Serializer classes](#serializer-classes)**                |
| **5. [Creating tables](#creating-tables)**                      |
| **6. [Raw SQL queries](#raw-sql-queries)**                      |
| **7. [Table/Field name modifiers](#tablefield-name-modifiers)** |
| **8. [Schema caching](#schema-caching)**                        |
| **9. [Usage with Lombok](#usage-with-lombok)**                  |

### Basic usage
Interact with database is as easy as write:
```java
// Specify table name here
@Document("players")
class Player {
    // String, passed to @Field will be used as field name in database
    @Field("name") public String name;
    @Field("score") public int score;
}
class Main {
    public static void main(String[] args) {
        // Connecting to database
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        // Create table based on class structure
        database.createTableIfNotExist(Player.class);
        // Create player object
        Player p = new Player();
        p.name = "SatoshiNakamoto";
        p.score = 100;
        // Save it do database
        database.insert(p).exec();
    }
}
```
But in the most time, we would want to have unique ID for every object in our database. It is pretty easy to do, just add numeric field with two additional annotations:
```java
@Document("players")
class Player {
    @Field("id") @PrimaryKey @AutoIncrement public float id;
    @Field("name") public String name;
    @Field("score") public int score;
}
class Main {
    public static void main(String[] args) {
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        database.createTableIfNotExist(Player.class);
        Player p = new Player();
        p.name = "SatoshiNakamoto";
        p.score = 100;
        database.insert(p).exec();
        // KiORM would automatically fill ID field after insert operation
        System.out.println(p.id);
        p.score += 200;
        // Documents with ID fields (or just with primary keys) can be used in update and delete queries
        database.update(p).exec();
    }
}
```
Notice, that @PrimaryKey can be used without `@AutoIncrement`, but `@AutoIncrement` requires `@PrimaryKey` to be set on field.
You can also use a composite primary key by specifying `@PrimaryKey` on multiple fields.

List of supported field types and their SQL names: `String` (TEXT), `boolean` (BOOLEAN), `byte` (TINYINT), `short` (SMALLINT), `int` (INT), `long` (BIGINT), `float` (FLOAT), `double` (DOUBLE), `byte[]` (BLOB).

> Important: KiORM only serializes fields marked with `@Field`. Even if field is private, KiORM will access it, so you don't need to think about how access modifiers affects data mapping.

> Important: KiORM requires document class to have public no-args constructor in order to be used in SELECT queries. By default, java implicitly creates it for you, but when you create your own constructor, the implicit one disappears, and you need to add it manually by writing `public ClassName() {}`.

### SQL closures
KiORM provides pretty simple java api for Where, OrderBy, etc. clauses. Here is an example:
```java
class Main {
    public static void main(String[] args) {
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        // Getting list of players, which score is >= 100
        List<Player> players = database
                .select(Player.class)
                .where(Where.gte("score", 100))
                .exec();
    }
}
```
It can also do more complicated queries. For SELECT there are `.where()`, `.order()` and `.limit()` clauses available.
```java
class Main {
    public static void main(String[] args) {
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        List<Player> players = database
            .select(Player.class)
            .where(
                Where.and(
                    Where.eq("alive", true),
                    Where.gte("score", 100),
                    Where.or(
                        Where.between("health", 10, 50),
                        Where.neq("type", "zombie")
                    )
                )
            )
            .order(
                Order.asc("score"),
                Order.desc("health")
            )
            .limit(10)
            .exec();
    }
}
```
INSERT, UPDATE and DELETE queries does not support any clauses. There also COUNT query, which supports `.where()` clause and can be used to count documents in table:
```java
class Main {
    public static void main(String[] args) {
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        long players = database
            .count(Player.class)
            .where(Where.gte("score", 100))
            .exec();
        System.out.println(players);
    }
}
```
All clauses are optional, you can run query like this to get all players:
```java
class Main {
    public static void main(String[] args) {
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        List<Player> players = database.select(Player.class).exec();
    }
}
```

### Computed fields
Sometimes we need to go beyond primitive types and store something more complicated, like JSON or player UUID. KiORM provides two different way to achieve this, and computed fields is the first one.
```java
@Document("items")
class Item {
    @Field("id") @PrimaryKey @AutoIncrement public float id;
    @Field("type") public String type;
    @Field("durability") public int durability;
    
    public Player owner;

    @Field("owner")
    private String getPlayerId() {
        return this.owner.getUUID();
    }
    
    @Field("owner")
    private void setPlayerId(String uuid) {
        this.owner = Player.fromUUID(uuid);
    }
}
```
This code allows you to interact only with real Player instance, while in stored as uuid string. `getPlayerId` and `setPlayerId` are private, so they won't distract you during development.

Computed fields always consists of two methods - a getter and a setter, returning/accepting one of the values, supported by the database (unless you use `@Serializer` along with a computed field, but more on that later).

Computed fields can also be primary key and even auto increment:
```java
@Document("items")
class Item {
    public CoolIdClass id;

    @PrimaryKey
    @AutoIncrement
    @Field("id")
    private long getPlayerId() {
        return this.id.getAsLong();
    }
    
    @Field("id")
    private void setPlayerId(long id) {
        this.id = new CoolIdClass(id);
    }
}
```
You may notice, that we specified `@PrimaryKey` and `@AutoIncrement` at getter, but in fact it does not matter. You can specify them both at setter, or even specify one annotation at getter and another at setter. KiORM would merge them.

### Serializer classes
Remember I said about two ways of complicated data serialization? Here it is!
Computed fields are good when you have one or two of them, but if you have two fields of the same type, then you would want to write serialization/deserialization code separately and reuse it at multiple fields.

This is where the `FieldSerializer<R, S>` interface comes. It has two generic types (R and S), where R means the real type and S is the storage type.
Just create a class that implements this interface and pass it in the `@Serializer` annotation on the desired field:
```java
class CoolIdSerializer implements FieldSerializer<CoolIdClass, Long> {
    @Override
    public Long serialize(CoolIdClass id) {
        return id.getAsLong();
    }

    @Override
    public CoolIdClass deserialize(Long id) {
        return new CoolIdClass(id);
    }
}

@Document("items")
class Item {
    @Serializer(CoolIdSerializer.class)
    @PrimaryKey @AutoIncrement @Field("id") public CoolIdClass id;
}
```
As you see, we are using `Long` instead of `long` here. KiORM would automatically care about object <-> primitive converting.

### Creating tables
As was shown earlier, you can easily create new table by calling `database.createTableIfNotExist(YourDocumentHere.class)`, but it is actually just combination of two other methods, which can be used separately: `database.checkIfTableExists(YourDocumentHere.class)` and `database.createTable(YourDocumentHere.class)`.
Currently, there are no table schema verification, if your code created mysql table, and then you changed its schema in the code, you may get errors.

### Raw SQL queries
While using all advantages of KiORM, you still can use raw SQL queries for things, that KiORM can't do. All you need to get JDBC connection and execute any queries is write `database.getConnection()`

### Table/Field name modifiers
Sometimes we need to specify table name in runtime (for example get it from configuration). Here name modifiers comes:
```java
class Main {
    public static void main(String[] args) {
        Map<Class<?>, String> tablesPrefixes = new HashMap<>();
        tables.put(Items.class, "new_");
        
        KiORM database = new KiORM("jdbc:mysql://root@127.0.0.1:3306/dbname");
        database.setTableNameModifier((name, type) -> tables.get(type) + name);
        database.createTableIfNotExist(Items.class); // Will create table with name "new_items"
    }
}
```
There is absolutely same way for modifying field names (`database.setFieldNameModifier((name, type) -> ...)`).

> Important: Due to schema caching, this lambda will be invoked only once. If you need to update table names, you can clear cache by calling `KiORM.clearSchemaCache()`

### Schema caching
By default, KiORM caches schemas after first parsing. The cache key consists of three things - the document class, the tableNameModifier lambda, and the fieldNameModifier lambda.

You can completely disable schema cache by adding `-Dkiorm.disableDocumentSchemaCache="true"` to Java launch args, or by calling `System.setProperty("kiorm.disableDocumentSchemaCache", "true")` in runtime.

You also can clear all cache by calling `KiORM.clearSchemaCache()`, or even access whole cache list by calling `DocumentParser.getCache()`

### Usage with Lombok
KiORM was designed to be used with [Lombok](https://projectlombok.org). You don't have to use it, but it greatly simplifies the writing of models, and allows you to get such beautiful schemas:
```java
@NoArgsConstructor // Creating no-args constructor for KiORM
@AllArgsConstructor // Creating constructor with all fields
@Document("items")
class Item {
    @Getter @PrimaryKey @AutoIncrement @Field("id") private long id; // ID without setter
    
    @Getter @Setter @Field("durability") private float durability;
    
    @Getter @Setter @Field("count") private int count;
    
    @Getter @Setter @Field("rarity") private int rarity;
    
    @Getter @Setter @Field("name") private String name;
    
    @Serializer(PlayerIdSerializer.class)
    @Getter @Setter @Field("owner") private Player owner;
}
```

## Epilogue
I'm developing this project during using it in other personal/work projects, so it will be maintained, and I hope, will grow up into full alternative of big and complicated ORMs for tiny/average projects.

I would be very pleased if you supported my work financially. You can do it on the LiberaPay or using Monero cryptocurrency

[![LibraPay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/Rikonardo/donate)

XMR: 42d4gWUUk9c79Vkg9CnU7z4VJci7UebTRf3B3FMNnPADFbKUqFZYMJ1S5xeK9vSV2b7gmsmZisS2XJQpW3pVwifrKc9PSN5