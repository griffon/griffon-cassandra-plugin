/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class CassandraGriffonPlugin {
    // the plugin version
    String version = '1.2.1'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.3.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [lombok: '0.5.0']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-cassandra-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Cassandra support'
    String description = '''
The Cassandra plugin enables lightweight access to [Cassandra][1] datastores
using [JDBC][2] + [CQL][3]. This plugin does NOT provide domain classes nor
dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * CassandraConfig.groovy - contains the database definitions.
 * BootstrapCassandra.groovy - defines init/destroy hooks for data to be
   manipulated during app startup/shutdown.

A new dynamic method named `withCql` will be injected into all controllers,
giving you access to a `groovy.sql.Sql` object, with which you'll be able to make
calls to the database. Remember to make all database calls off the UI thread
otherwise your application may appear unresponsive when doing long computations
inside the UI thread.

This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withCql { databaseName, database -> ... }
            withCql('internal') { databaseName, database -> ... }
        }
    }
    
The following list enumerates all the variants of the injected method

 * `<R> R withCql(Closure<R> stmts)`
 * `<R> R withCql(CallableWithArgs<R> stmts)`
 * `<R> R withCql(String databaseName, Closure<R> stmts)`
 * `<R> R withCql(String databaseName, CallableWithArgs<R> stmts)`

These methods are also accessible to any component through the singleton
`griffon.plugins.cassandra.CassandraConnector`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `CassandraEnhancer.enhance(metaClassInstance, cassandraProviderInstance)`.

Configuration
-------------
### CassandraAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.cassandra.CassandraAware`. This transformation injects the
`griffon.plugins.cassandra.CassandraContributionHandler` interface and default
behavior that fulfills the contract.

### Dynamic method injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.cassandra.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.cassandra.CassandraContributionHandler`.

### Events

The following events will be triggered by this addon

 * CassandraConnectStart[config, databaseName] - triggered before connecting to the database
 * CassandraConnectEnd[databaseName, database] - triggered after connecting to the database
 * CassandraDisconnectStart[config, databaseName, database] - triggered before disconnecting from the database
 * CassandraDisconnectEnd[config, databaseName] - triggered after disconnecting from the database

### Multiple Stores

The config file `CassandraConfig.groovy` defines a default database block. As the name
implies this is the database used by default, however you can configure named databases
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    databases {
        internal {
            driverClassName = 'org.apache.cassandra.cql.jdbc.CassandraDriver'
            url = 'jdbc:cassandra://localhost:9160/internal_dev'
            pool {
                maxWait = 60000
                maxIdle = 5
                maxActive = 8
            }
        }
    }

This block can be used inside the `environments()` block in the same way as the
default database block is used.

### Configuration Storage

The plugin will load and store the contents of `CassandraConfig.groovy` inside the
application's configuration, under the `pluginConfig` namespace. You may retrieve
and/or update values using

    app.config.pluginConfig.cassandra

### Connect at Startup

The plugin will attempt a connection to the default database at startup. If this
behavior is not desired then specify the following configuration flag in
`Config.groovy`

    griffon.cassandra.connect.onstartup = false

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/cassandra][4]

Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`CassandraEnhancer.enhance(metaClassInstance, cassandraProviderInstance)` where
`cassandraProviderInstance` is of type `griffon.plugins.cassandra.CassandraProvider`.
The contract for this interface looks like this

    public interface CassandraProvider {
        <R> R withCql(Closure<R> closure);
        <R> R withCql(CallableWithArgs<R> callable);
        <R> R withCql(String databaseName, Closure<R> closure);
        <R> R withCql(String databaseName, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyCassandraProvider implements CassandraProvider {
        public <R> R withCql(Closure<R> closure) { null }
        public <R> R withCql(CallableWithArgs<R> callable) { null }
        public <R> R withCql(String databaseName, Closure<R> closure) { null }
        public <R> R withCql(String databaseName, CallableWithArgs<R> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            CassandraEnhancer.enhance(service.metaClass, new MyCassandraProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@CassandraAware` then usage
of `CassandraEnhancer` should be avoided at all costs. Simply set `cassandraProviderInstance`
on the service instance directly, like so, first the service definition

    @griffon.plugins.cassandra.CassandraAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.cassandraProvider = new MyCassandraProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-cassandra-compile-x.y.z.jar`, with locations

 * dsdl/cassandra.dsld
 * gdsl/cassandra.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][5] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][5] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/cassandra-<version>/dist/griffon-cassandra-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:\
        griffon-lombok-compile-<version>.jar:griffon-cassandra-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@CassandraAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][6]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@CassandraAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][5] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-cassandra-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/cassandra-<version>/dist/griffon-cassandra-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@CassandraAware`.


[1]: http://www.cassandra.org
[2]: http://code.google.com/a/apache-extras.org/p/cassandra-jdbc/
[3]: http://www.datastax.com/docs/1.0/dml/using_cql
[4]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/cassandra
[5]: /plugin/lombok
[6]: http://netbeans.org/kb/docs/java/annotations-lombok.html
'''
}
