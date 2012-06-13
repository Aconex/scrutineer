Analyses a secondary stream of information against a known point-of-truth and reports inconsistencies.

The Why
=======

When you have a Lucene-based index of substantial size, say many hundreds of millions of records, what you want is confidence
that your index is correct. In many cases, people use Solr/ElasticSearch/Compass to index their central database, mongodb,
hbase etc so the index is a secondary storage of data.

How do you know if your index is accurate? Can you just reindex 500 million documents anytime you like? (That's the Aliens: "Nuke the site from
Orbit... It's the only way to be sure" approach). No, if there _ARE_ inconsistencies in your index, then you want to:

* find the items that are incorrect (and only them)
* do it fast

Scrutineer has been designed with this in mind, it can find any inconsistencies in your index fast.


How does this work?
===================

Scrutineer relies on your data having 2 core properties:

* an ID - a unique identifier for your object
* a Version - something stored in your primary datastore for that object that represents the temporal state of that object

The Version property is commonly used in an Optimistic Locking pattern. If you store the ID & Version information in your
secondary store (say, Solr/ElasticSearch) then you can always compare for any given item whether the version in secondary store is up
to date.

Scrutineer takes a stream from your primary, and a stream from your secondary store, presumes they are sorted identically (more
on that later) and walks the streams doing a merge comparison. It detects 4 states:

1. Both items are identical (yay!)
2. An ID is missing from the secondary stream (A missed add?  maybe that index message you sent to Solr/ElasticSearch never made it, anyway, it's not there)
3. An ID was detected in the secondary, but wasn't in the primary stream (A missed delete?  something was deleted on the primary, but the secondary never got the memo)
4. The ID exists in both streams, but the Version values are inconsistent (A missed update?  similar to the missed add, this time perhaps an update to a row in your DB never made it to Solr/ElasticSearch)

Example
=======
Here's an example, 2 streams in sorted order, one from the Database (your point-of-truth), 
and one from ElasticSearch (the one you're checking) with the <ID>:<VERSION> for each side:

<table border="1">
  <tr><th>Database</th><th>ElasticSearch</th></tr>
  <tr><td>1:12345</td><td>1:12345</td></tr>
  <tr><td>2:23455</td><td>3:84757</td></tr>
  <tr><td>3:84757</td><td>4:98765</td></tr>
  <tr><td>4:98765</td><td>5:38475</td></tr>
  <tr><td>6:34666</td><td>6:34556</td></tr>
</table>

Scrutineer picks up that:

* ID '2' is missing from ElasticSearch
* ID '5' was deleted from the database at some point, but ElasticSearch still has it in there
* ID '6' is visible in ElasticSearch but appears to have the wrong version

Running Scrutineer
==================

The very first thing you'll need to do is get your JDBC Driver jar and place it in the 'lib' directory of the unpacked
package.  We already have a JTDS driver in there if you're using SQL Server (that's just what we use).

    bin/scrutineer \
                --jdbcURL=jdbc:jtds:sqlserver://mydbhost/mydb  \
                --jdbcDriverClass=net.sourceforge.jtds.jdbc.Driver \
                --jdbcUser=itasecret \
                --jdbcPassword=itsasecret   \
                --sql="select id,version from myobjecttype order by cast(id as varchar(100))" \
                --clusterName=mycluster \
                --indexName=myindex \
                --query="_type:myobjecttype" \
                --numeric

*Note:* if you're weirded out about that '...cast(...)' then don't worry, we'll explain that shortly.

* **jdbcURL** – Standard JDBC URL you would use for your app to connect to your database
* **jdbcDriverClass** - Fully qualified class name of your JDBC Driver (don't forget to put your JDBC Driver jar in the lib directory as said above!)
* **jdbcUser** - user account to access your JDBC Database
* **jdbcPassword** -- password required for the user credentials
* **sql** - The SQL used to generate a lexicographical stream of ID & Version values (in that column order)
* **clusterName** - this is your ElasticSearch cluster name used to autodetect and connect to a node in your cluster
* **indexName** - the name of the index on your ElasticSearch cluster
* **query** - A query_parser compatible search query that returns all documents in your ElasticSearch index relating to the SQL query you're using
  Since it is common for an index to contain a type-per-db-table you can use the "_type:<type>" search query to filter for all values for that type.
* **numeric** - use this if your query returns results numerically ordered 


Output
======
Scrutineer writes any inconsistencies direct to Standard Error, in a well-defined, tab-separated format for easy parsing to feed into a
system to reindex/cleanup.  If we use the Example scenario above, this is what Scrutineer would print out:


    NOTINSECONDARY    2    23455
    MISMATCH    6    34666    secondaryVersion=34556
    NOTINPRIMARY    5    38475

The general format is:

   **FailureType**\t**ID**\t**VERSION**\t**Optional:Additional Info**

### NOTINSECONDARY
This means you are missing this item in your secondary and you should reindex/re-add to your secondary stream

### MISMATCH
This means the version of the object stored in the secondary is not the same information as the primary, and you should reindex

### NOTINPRIMARY
The object was removed from the Primary store, but the secondary still has it.  You should remove this item from your secondary.

Scrutineer does _not_ report when items match, we'll presume you're just fine with that...

Sorting
=======

__*VERY IMPORTANT*__: Scrutineer relies on both streams to be sorted using an identical mechanism. It
requires input streams to be in lexicographical (default) or numerical (indicate using `--numeric`) sort order.

ElasticSearch
=============

Since Aconex uses ElasticSearch, Scrutineer supports ES out of the box, but it would not be difficult for others to integrate
a Solr stream and wire something up. Happy to take Pull Requests!

What are the 'best practices' for using Scrutineer?
===================================================

The authors of Scrutineer, Aconex, index content from a JDBC data source and index using ElasticSearch.  We do the following:

* In the database table of the object being indexed we add an Insert/Update trigger to populate a 'lastupdated' timestamp column as our Version property
* When we index into ElasticSearch, we set the Version property of the item using the VersionType.EXTERNAL setting.  
* We create an SQL Index on this tuple so these 2 fields can be retrieved from the database very fast


Assumptions
===========

* Your Version property is Long compatible.  You can use java.sqlTimestamps column types too as a Version (that's what we do)
* Aconex is DB->ElasticSearch centric at the moment.  We've tried to keep things loosely coupled, so it should be 
simple to add further integration points for other Primary & Secondary sources (HBase, MongoDB, Solr). 

Building
========
Scrutineer is a Maven project, which really _should_ just build right out of the box if you have Maven installed.  Just type:

    mvn package

And you should have a Tarball in the 'target' sub-directory.

Submitting Pull Requests
========================

First, Please add unit tests!

Second, Please add integration tests!

Third, We have tightened up the quality rule set for CheckStyle, PMD etc pretty hard.  Before you issue a pull request, please run:

    mvn verify

which will run all quality checks.  Sorry to be super-anal, but we just like Clean Code.

Roadmap
=======

* Scrutineer currently only runs in a single thread based on a single stream.  
It would be good to provide a 'manifest' to Scrutineer to outline a set of stream verifications to perform, perhaps one for each type
you have so that your multi-core system can perform multiple stream comparisons in parallel.

* Incremental checking – Right now Scrutineer checks the whole thing, BUT if you are using timestamp-based versions, there's no reason 
it couldn't only check objects that were changed after the last known full verification.  This would require one to keep track of
deletes on the primary stream (perhaps an OnDelete Trigger in your SQL database) so that IDs that were deleted in the primary
stream after the last full check could be detected correctly.

* Obviously we'd love to have a Solr implementation here, we hope the community can help here.
