# RavenDB Client for Java

## Installation

### Maven

```
<dependency>
  <groupId>net.ravendb</groupId>
  <artifactId>ravendb</artifactId>
  <version>5.0.1</version>
</dependency>
```


### Gradle
```
implementation 'net.ravendb:ravendb:5.0.1'
```

## Documentation

Please find the official documentation on [RavenDB Documentation](https://ravendb.net/docs/article-page/5.0/java) page.

## Bug tracker

You can report bug/feature: [http://issues.hibernatingrhinos.com/issues/RDBC](http://issues.hibernatingrhinos.com/issues/RDBC)


## Getting started

1. Initialize document store (you should have one DocumentStore instance per application)
```java
DocumentStore store = new DocumentStore("http://live-test.ravendb.net", "databaseName");
store.initialize();
```
2. Open a session
```java
try (IDocumentSession session = store.openSession()) {
    // here goes your code
}
```
3. Call `saveChanges()` once you're done:
```java
User user = session.load(User.class, "users/1-A");
user.setPassword("new password");
session.saveChanges();
// data is persisted 
// you can proceed e.g. finish web request
```

## CRUD example

### Storing documents
```java
Product product = new Product();
product.setTitle("iPhone X");
product.setPrice(999.99);
product.setCurrency("USD");
product.setStorage(64);
product.setManufacturer("Apple");
product.setInStock(true);

session.store(product, "products/");
System.out.println(product.getId()); // products/1-A
session.saveChanges();
```

### Loading documents
```java
Product product = session.load(Product.class, "products/1-A");
System.out.println(product.getTitle());   // iPhone X
System.out.println(product.getId());      // Products/1-A
```

### Loading documents with includes
```java
try (IDocumentSession session = store.openSession()) {
    // users/1
    // {
    //      "name": "John",
    //      "kids": ["users/2", "users/3"]
    // }
    User user1 = session.include("kids")
        .load(User.class, "users/1");
    // Document users/1 is going to be pulled along
    // with docs referenced in "kids" field within a single request

    User user2 = session.load(User.class, "users/2"); // this won't call server again
    assertThat(user1)
        .isNotNull();
    assertThat(user2)
        .isNotNull();
    assertThat(session.advanced().getNumberOfRequests())
        .isEqualTo(1);
}

```
### Updating documents
```java
Product product = session.load(Product.class, "products/1-A");
product.setInStock(false);
product.setLastUpdate(new Date());
session.saveChanges();
// ...
product = session.load(Product.class, "products/1-A");
System.out.println(product.getInStock());     // false
System.out.println(product.getLastUpdate())   // 2018-06-05T13:52:31.633Z
```

### Deleting documents
1. Using entity
```java
Product product = session.load(Product.class, "products/1-A");
session.delete(product);
session.saveChanges();

product = session.load(Product.class, "products/1-A");
assertThat(product)
    .isNull();
```

2. Using document ID
```java
session.delete("products/1-A");
```

## Querying documents
1. Use `query()` session method:

By collection:
```java
IDocumentQuery<Product> query = session.query(Product.class);
```

By index name:
```java
IDocumentQuery<Product> query = session.query(Product.class, Products_ByCategory.class);
```

2. Build up the query - apply conditions, set ordering etc. Query supports chaining calls:
```java
query
    .waitForNonStaleResults()
    .usingDefaultOperator(QueryOperator.AND)
    .whereEquals("manufacturer", "Apple")
    .whereEquals("inStock", true)
    .whereBetween("lastUpdate", lastYearDate, new Date())
    .orderBy("price");
```

3. Finally, you may get query results:
```java
List<Product> products = query.toList();
// ...
Product firstOne = query.first(); // gets first result
// ...
Product single = query.single();  // gets single result
```

### DocumentQuery methods overview

#### selectFields() - projections using a single field
```java

// RQL
// from users select name
List<String> userNames = session.query(User.class)
    .selectFields(String.class, "name")
    .toList();
// John,Stefanie,Thomas
```

#### selectFields() - projections using multiple fields
```java
// RQL
// from users select name, age
List<NameAndAge> result = session.query(User.class)
    .selectFields(NameAndAge.class)
    .toList();
// [ { name: 'John', age: 30, id: 'users/1-A' },
//   { name: 'Stefanie', age: 25, id: 'users/2-A' },
//   { name: 'Thomas', age: 25, id: 'users/3-A' } ]
```

#### distinct()
```java
// RQL
// from users select distinct age
session.query(User.class)
    .selectFields(String.class, "age")
    .distinct()
    .toList(); // [ 25, 30 ]
```

#### whereEquals() / whereNotEquals()
```java
// RQL
// from users where age = 30
session.query(User.class)
    .whereEquals("age", 30)
    .toList() 
// [ User {
//    name: 'John',
//    age: 30,
//    kids: [...],
//    registeredAt: 2017-11-10T23:00:00.000Z } ]
```

#### whereIn()
```java
// RQL
// from users where name in ("John", "Thomas")
session.query(User.class)
    .whereIn("name", Arrays.asList("John", "Thomas"))
    .toList()
// [ User {
//     name: 'John',
//     age: 30,
//     registeredAt: 2017-11-10T23:00:00.000Z,
//     kids: [...],
//     id: 'users/1-A' },
//   User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' } ]
```

#### whereStartsWith() / whereEndsWith()
```java
// RQL
// from users where startsWith(name, 'J')
session.query(User.class)
    .whereStartsWith("name", "J")
    .toList();
// [ User {
//    name: 'John',
//    age: 30,
//    kids: [...],
//    registeredAt: 2017-11-10T23:00:00.000Z } ]
```

#### whereBetween()
```java
// RQL
// from users where registeredAt between '2016-01-01' and '2017-01-01'
session.query(User.class)
    .whereBetween("registeredAt", lastYearStartDate, new Date())
    .toList()
// [ User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' } ]
```

#### whereGreaterThan() / whereGreaterThanOrEqual() / whereLessThan() / whereLessThanOrEqual()
```java
// RQL
// from users where age > 29
session.query(User.class)
    .whereGreaterThan("age", 29)
    .toList();
// [ User {
//   name: 'John',
//   age: 30,
//   registeredAt: 2017-11-10T23:00:00.000Z,
//   kids: [...],
//   id: 'users/1-A' } ]
```

#### whereExists()
Checks if the field exists.
```java
// RQL
// from users where exists("age")
session.query(User.class)
    .whereExists("kids")
    .toList();
// [ User {
//   name: 'John',
//   age: 30,
//   registeredAt: 2017-11-10T23:00:00.000Z,
//   kids: [...],
//   id: 'users/1-A' } ]
```

#### containsAny() / containsAll()
```java
// RQL
// from users where kids in ('Mara')
session.query(User.class)
    .containsAll("kids", Arrays.asList("Mara", "Dmitri"))
    .toList();
// [ User {
//   name: 'John',
//   age: 30,
//   registeredAt: 2017-11-10T23:00:00.000Z,
//   kids: ["Dmitri", "Mara"]
//   id: 'users/1-A' } ]
```

#### search()
Performs full-text search.
```java
// RQL
// from users where search(kids, 'Mara')
session.query(User.class)
    .search("kids", "Mara Dmitri")
    .toList();
// [ User {
//   name: 'John',
//   age: 30,
//   registeredAt: 2017-11-10T23:00:00.000Z,
//   kids: ["Dmitri", "Mara"]
//   id: 'users/1-A' } ]
```

#### openSubclause() / closeSubclause()
```java
// RQL
// from users where exists(kids) or (age = $p0 and name != $p1)
session.query(User.class)
    .whereExists("kids")
    .orElse()
    .openSubclause()
        .whereEquals("age", 25)
        .whereNotEquals("name", "Thomas")
    .closeSubclause()
    .toList(); 
// [ User {
//     name: 'John',
//     age: 30,
//     registeredAt: 2017-11-10T23:00:00.000Z,
//     kids: ["Dmitri", "Mara"]
//     id: 'users/1-A' },
//   User {
//     name: 'Stefanie',
//     age: 25,
//     registeredAt: 2015-07-29T22:00:00.000Z,
//     id: 'users/2-A' } ]
```

#### not()
```java
// RQL
// from users where age != 25
session.query(User.class)
    .not()
    .whereEquals("age", 25)
    .toList();
// [ User {
//   name: 'John',
//   age: 30,
//   registeredAt: 2017-11-10T23:00:00.000Z,
//   kids: ["Dmitri", "Mara"]
//   id: 'users/1-A' } ]
```

#### andAlso() / orElse()
```java
// RQL
// from users where age != 25
session.query(User.class)
    .whereExists("kids")
    .orElse()
    .whereLessThan("age", 30)
    .toList();
//  [ User {
//     name: 'John',
//     age: 30,
//     registeredAt: 2017-11-10T23:00:00.000Z,
//     kids: [ 'Dmitri', 'Mara' ],
//     id: 'users/1-A' },
//   User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' },
//   User {
//     name: 'Stefanie',
//     age: 25,
//     registeredAt: 2015-07-29T22:00:00.000Z,
//     id: 'users/2-A' } ]
```

#### usingDefaultOperator()
Sets default operator (which will be used if no `andAlso()` / `orElse()` was called. Just after query instantiation, `OR` is used as default operator. Default operator can be changed only adding any conditions.

#### orderBy() / randomOrdering()
```java
// RQL
// from users order by age
session.query(User.class)
    .orderBy("age") // .randomOrdering()
    .toList();
// [ User {
//     name: 'Stefanie',
//     age: 25,
//     registeredAt: 2015-07-29T22:00:00.000Z,
//     id: 'users/2-A' },
//   User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' },
//   User {
//     name: 'John',
//     age: 30,
//     registeredAt: 2017-11-10T23:00:00.000Z,
//     kids: [ 'Dmitri', 'Mara' ],
//     id: 'users/1-A' } ]
```
#### take()
Limits the number of result entries to `count`.
```java
// RQL
// from users order by age
session.query(User.class)
    .orderBy("age")
    .take(2)
    .toList();
// [ User {
//     name: 'Stefanie',
//     age: 25,
//     registeredAt: 2015-07-29T22:00:00.000Z,
//     id: 'users/2-A' },
//   User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' } ]
```

#### skip()
Skips first `count` results.
```javascript
// RQL
// from users order by age
session.query(User.class)
    .orderBy("age")
    .take(1)
    .skip(1)
    .toList();
// [ User {
//     name: 'Thomas',
//     age: 25,
//     registeredAt: 2016-04-24T22:00:00.000Z,
//     id: 'users/3-A' } ]
```

#### Getting query statistics
To obtain query statistics use `statistics()` method.
```java
Reference<QueryStatistics> statsRef = new Reference<>();
List<User> users = session.query(User.class)
    .whereGreaterThan("age", 29)
    .statistics(statsRef)
    .toList();
// QueryStatistics {
//   isStale: false,
//   durationInMs: 744,
//   totalResults: 1,
//   skippedResults: 0,
//   timestamp: 2018-09-24T05:34:15.260Z,
//   indexName: 'Auto/users/Byage',
//   indexTimestamp: 2018-09-24T05:34:15.260Z,
//   lastQueryTime: 2018-09-24T05:34:15.260Z,
//   resultEtag: 8426908718162809000 }
```

#### toList() / first() / single() / count()
`toList()` - returns all results

`first()` - first result

`single()` - first result, throws error if there's more entries

`count()` - returns the count of the results (not affected by `take()`)

### Attachments

#### Store attachments
```java
User doc = new User();
doc.setName("John");

// track entity
session.store(doc);

// get read stream or buffer to store
FileInputStream fileStream = new FileInputStream("../photo.png")

// store attachment using entity
session.advanced().attachments()
    .store(doc, "photo.png", fileStream, "image/png");

// OR using document ID
session.advanced().attachments()
    .store(doc.getId(), "photo.png", fileStream, "image/png");

session.saveChanges();
```

#### Get attachments

```java
try (CloseableAttachmentResult closeableAttachmentResult 
    = session.advanced().attachments().get(documentId, "photo.png")) {
    // closeableAttachmentResult.getDetails() contains information about the attachemnt
    //     { 
    //       name: 'photo.png',
    //       documentId: 'users/1-A',
    //       contentType: 'image/png',
    //       hash: 'MvUEcrFHSVDts5ZQv2bQ3r9RwtynqnyJzIbNYzu1ZXk=',
    //       changeVector: '"A:3-K5TR36dafUC98AItzIa6ow"',
    //       size: 4579 
    //     }
    InputStream is = closeableAttachmentResult.getData();
    // is contains attachment data
}
```

#### Check if attachment exists

```java
session.advanced().attachments().exists(doc.getId(), "photo.png");
// true

session.advanced().attachments().exists(doc.getId(), "not_there.avi");
// false
```

#### Get attachment names

```java
// use a loaded entity to determine attachments' names
AttachmentName[] names = session.advanced().attachments().getNames(doc);
// [ { name: 'photo.png',
//     hash: 'MvUEcrFHSVDts5ZQv2bQ3r9RwtynqnyJzIbNYzu1ZXk=',
//     contentType: 'image/png',
//     size: 4579 } ]
```

### Bulk Insert

```java
// create bulk insert instance using DocumentStore instance
try (BulkInsertOperation bulkInsert = store.bulkInsert()) {

    // insert your documents
    for (String name: Arrays.asList("Anna", "Maria", "Miguel", "Emanuel", "Dayanara", "Aleida")) {
        User user = new User();
        user.setName(name);
        bulkInsert.store(user);
    }

    // auto flush by closing bulkInsert operation
}
// User { name: 'Anna', id: 'users/1-A' }
// User { name: 'Maria', id: 'users/2-A' }
// User { name: 'Miguel', id: 'users/3-A' }
// User { name: 'Emanuel', id: 'users/4-A' }
// User { name: 'Dayanara', id: 'users/5-A' }
// User { name: 'Aleida', id: 'users/6-A' }
```

### Changes API
Listen for database changes e.g. document changes.

```java
CleanCloseable subscription = store
    .changes()
    .forAllDocuments()
    .subscribe(Observers.create(change -> {
        System.out.println(change.getType() + " on document " + change.getId());
    }));

// close when you are done
subscription.close();
```

### Streaming

#### Stream documents with ID prefix
```java
 try (CloseableIterator<StreamResult<User>> stream 
    = session.advanced().stream(User.class, "users/")) {
    while (stream.hasNext()) {
        StreamResult<User> next = stream.next();
        User document = next.getDocument();
        // do something with document
    }
}
```

#### Stream query results
```java
// create a query
IDocumentQuery<User> query = session.query(User.class)
    .whereGreaterThan("age", 29);

Reference<StreamQueryStatistics> statsRef = new Reference<>();
                
try (CloseableIterator<StreamResult<User>> stream 
    = session.advanced().stream(query, statsRef)) {
    while (stream.hasNext()) {
        StreamResult<User> item = stream.next();
        User user = item.getDocument();
    }
}
```

### Revisions
NOTE: Please make sure revisions are enabled before trying one of the below.

```java
User user = new User();
user.setName("Marcin");
user.setAge(30);
user.setPet("users/4");

try (IDocumentSession session = store.openSession()) {
    session.store(user, "users/1");
    session.saveChanges();

    // modify the document to create a new revision
    user.setName("Roman");
    user.setAge(40);

    session.saveChanges();
}


// get revisions
List<User> revisions = session.advanced()
    .revisions()
    .getFor(User.class, "users/1");
// [ { name: 'Roman',
//     age: 40,
//     pet: 'users/4',
//     '@metadata': [Object],
//     id: 'users/1' },
//   { name: 'Marcin',
//     age: 30,
//     pet: 'users/4',
//     '@metadata': [Object],
//     id: 'users/1' } ]
```

### Suggestions
```java
// users collection
// [ User {
//     name: 'John',
//     age: 30,
//     registeredAt: 2017-11-10T23:00:00.000Z,
//     kids: [Array],
//     id: 'users/1-A' },

// and a static index like:
public static class UsersIndex extends AbstractIndexCreationTask {
    public UsersIndex() {
        map = "from user in docs.users select new { user.name }";
        suggestion("name");
    }
}

// ...
try (IDocumentSession session = store.openSession()) {
    Map<String, SuggestionResult> results = session.query(User.class, UsersIndex.class)
        .suggestUsing(x -> x.byField("name", "Jon"))
        .execute();
}
// { name: { name: 'name', suggestions: [ 'john' ] } }
```

### Advanced patching
```java
session.advanced().increment("users/1", "age", 1);
// increments *age* field by 1

session.advanced().patch("users/1", "underAge", false);
// sets *underAge* field to *false*

session.saveChanges();
```


## Working with secured server

```java
// load certificate
KeyStore clientStore = KeyStore.getInstance("PKCS12");
clientStore.load(new FileInputStream("c:\\ravendb\\client-cert.pfx"), "passwordToPfx".toCharArray());

try (DocumentStore store = new DocumentStore())  {
    store.setCertificate(clientStore);
    store.setDatabase("Northwind");
    store.setUrls(new String[]{ "https://my_secured_raven" });

    store.initialize();

    // do your work here
}
```
