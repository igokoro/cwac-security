CWAC Security: Helping You Help Your Users Defend Their Data
============================================================

This project contains utility code related to Android security
measures.

At present, it contains:

- a `PermissionUtils` class with a `checkCustomPermissions()`
static method, to help you detect if another app has defined your
custom permissions before your app was installed

- a `TrustManagerBuilder` to help you create a custom `TrustManager`,
describing what sorts of SSL certificates you want to support in your
HTTPS operations

This Android library project is 
[available as a JAR](https://github.com/commonsguy/cwac-security/releases)
or as an artifact for use with Gradle. To use that, add the following
blocks to your `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://repo.commonsware.com.s3.amazonaws.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:security:0.2.+'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

Usage: checkCustomPermissions()
------------------------------
Custom permissions in Android are "first one in wins". In other
words, whatever app first has a `<permission>` element for a
given `android:name` gets to define, for all subsequent apps,
what the details are for that permission. And, courtesy of Android's
rules for informing users about permissions, the app that
defined the permission can hold the permission without the user's
knowledge.

This has some security implications, which are covered in greater
detail [in this paper](PERMS.md).

The `checkCustomPermissions()` method is designed to help you detect
if another app has defined the same custom permissions that you are
defining. Typically, developers expect to be the first one to define
the custom permission, but that may not be true, with consequences
for the developers and their users.

Calling `checkCustomPermissions()` is easy: just pass it a `Context`,
such as your launcher `Activity`.

What it returns is a `HashMap<PackageInfo, ArrayList<PermissionLint>>`,
which will require some explanation.

What `checkCustomPermissions()` does is find all of the custom
permissions in your app that you have declared via `<permission>`
elements. Then, it scans all other apps on the device, finding all
*their* custom permissions, and sees if there is a match on the
permission name (`android:name` attribute).

Each entry in the `HashMap` represents one app that has redefined
one or more of your custom permissions, keyed by
[the `PackageInfo` object](https://developer.android.com/reference/android/content/pm/PackageInfo.html)
describing that application. Each permission that
has been so redefined will be in the `ArrayList`. So, if you define
two custom permissions, but some other app only redefined one, there
will only be one entry in the `ArrayList`.

Each `PermissionLint` in the `ArrayList` contains the following
`public` fields:

- `PermissionInfo perm` providing details of the permission
*as declared in the other app*

- `boolean wasDowngraded`, which will be `true` if you declared
the permisison to be `signature`, but the other app declared
it to be `normal` or `dangerous`

- `boolean wasUpgraded`, which will be `true` if you declared
the permission to be `normal` or `dangerous`, but the other
app declared it to be `signature`

- `boolean proseDiffers`, which will be `true` if, for the
user's configured device locale, the label or description of
the other app's edition of this permission differs from your
edition of this permission

Hence, if all three boolean fields are `false`, the permission
in the other app is functionally identical to your own definition,
at least for this user and this locale.

The expectation is that you would call `checkCustomPermissions()`
on the first run of your app after installation. If you get back
an empty `HashMap`, then you can continue your first run normally.
If you get back a non-empty `HashMap`, you can decide what to do
with the information, including:

- warning the user about possible data leakage to other apps

- sending information about the pre-defined permission to your
servers, so you can track possible malware attacks targeting
your application and users

Usage: `TrustManagerBuilder`
----------------------------
Android supports SSL "out of the box". That support works best with
`HttpsURLConnection` and [Square's OkHttp](http://square.github.io/okhttp/).
It will support a reasonable set of certificate authorities for validating
the SSL certificates you make on your HTTPS request.

However, it does not handle *all* scenarios "out of the box", and that's
where `TrustManagerBuilder` comes in. `TrustManagerBuilder` makes it easy to
create custom certificate validation rules, to handle things like self-signed
certificates, custom certificate authorities, and the like.

To use `TrustManagerBuilder`, create an instance, configure it using a set
of builder-style methods (described below), and then call `build()` or
`buildArray()`. `build()` returns an instance of `TrustManager`. `buildArray()`
retuns that same instance wrapped in a one-element `TrustManager[]`, for convenience,
as many SSL-related APIs expect a `TrustManager[]` rather than a `TrustManager`.
You can then supply that `TrustManager[]` to `HttpsURLConnection`:

```java
TrustManagerBuilder builder=new TrustManagerBuilder();

// configure builder here

SSLContext ssl=SSLContext.getInstance("TLS")

ssl.init(null, builder.buildArray(), null);

HttpsURLConnection conn=(HttpsURLConnection)new URL(url).openConnection();

conn.setSSLSocketFactory(ssl.getSocketFactory());

// conn.getInputStream() and other work to process the HTTPS call
```

or `OkHttp`:

```java
TrustManagerBuilder builder=new TrustManagerBuilder();

// configure builder here

SSLContext ssl=SSLContext.getInstance("TLS")

ssl.init(null, builder.buildArray(), null);

OkHttpClient okHttpClient=new OkHttpClient();

okHttpClient.setSslSocketFactory(ssl.getSocketFactory());

HttpURLConnection conn=okHttpClient.open(new URL(url));

// conn.getInputStream() and other work to process the HTTPS call
```

There are two `TrustManagerBuilder` constructors: a zero-argument constructor
(shown above) and a one-parameter constructor, taking a `Context`. Use the one-parameter
constructor if you plan on using the builder-style methods that take a raw resource
ID or a path into `assets/` as parameters.

Of course, the real work is done in the `// configure builder here` parts, using
the following builder-style methods:

- `useDefault()`: this tells `TrustManagerBuilder` to use the system default `TrustManager` for
validating incoming SSL certificates

- `selfSigned()`: this tells `TrustManagerBuilder` to allow a specific self-signed
SSL certificate, based upon a supplied keystore file and password
 
- `allowCA()`: this tells `TrustManagerBuilder` to accept certificates signed by
a custom certificate authority, based upon a supplied certificate file

- `denyAll()`: this tells `TrustManagerBuilder` to reject all certificates (mostly
for testing purposes)

In addition, `or()` tells `TrustManagerBuilder` to logically OR any subsequent configuration
with whatever came previously in the build, while `and()` indicates that subsequent
configuration should be logically AND-ed with whatever came previously.

### Scenarios

All of that will make a bit more sense if we look at some candidate scenarios.

#### You Want To Use a Self-Signed Certificate

As [Moxie Marlinspike points out](http://www.thoughtcrime.org/blog/authenticity-is-broken-in-ssl-but-your-app-ha/),
one way to avoid having your app be the victim of a man-in-the-middle (MITM)
attack due to a hijacked certificate authority (CA) is to simply not use a certificate
authority. Those are designed for use by general-purpose clients (e.g., Web browsers)
hitting general-purpose servers (e.g., Web servers). In the case where you control
both the client *and* the server, you don't need a CA.

`selfSigned()` will help with that. The simple form of the method takes two parameters.
The first parameter is either:

- a `File` pointing to a keystore for your self-signed certificate on the local file system,
- an `int` raw resource ID, if you wish to package the keystore in your app in `res/raw/`, or
- a `String` pointing to a relative path in `assets/` where you have placed your keystore

The second parameter is a `char[]` for the password for the keystore. If you are
dynamically retrieving that password (e.g., the user types it in), you can clear out
the `char[]` (e.g., set all elements in the array to `x`), to quickly get rid of the
password from memory. If your password is more static, just call `toCharArray()` on a
`String` to get the `char[]` that you need.

The default `selfSigned()` expect that keystore to be in BKS format; if your keystore
is in some other format supported by Android's edition of `KeyStore`, use the three-parameter
version of `selfSigned()` that takes the `KeyStore` format name as the last parameter.

If you want to *only* support a specific self-signed certificate, you could set up
a `TrustManagerBuilder` as follows:

```java
new TrustManagerBuilder(this).selfSigned(R.raw.selfsigned, "foobar".toCharArray());
```

(where `this` is a `Context`, like the `IntentService` in which you are making
a SSL-encrypted Web service call)

Here, the BKS-formatted keystore would reside in `res/raw/selfsigned.bks` (though
the file extension could vary).

If you want to support more than one self-signed certificate &mdash; such as when you
plan on switching your old certificate to a new one &mdash; you could do:

```java
new TrustManagerBuilder(this)
  .selfSigned(R.raw.selfsigned, "foobar".toCharArray())
  .or()
  .selfSigned(R.raw.selfsigned2, "snicklefritz".toCharArray());
```

If you want to use a single `TrustManagerBuilder` for both your self-signed scenario
and regular CA-based certificates, you could do:

```java
new TrustManagerBuilder(this)
  .selfSigned(R.raw.selfsigned, "foobar".toCharArray())
  .or()
  .useDefault();
```

#### You Want To Use a Private Certificate Authority

Larger organizations might set up their own CA for signing their own
certificates. Think of this as self-signed certificates on an industrial scale.

[Google's documentation](https://developer.android.com/training/articles/security-ssl.html#UnknownCa)
shows how to handle this case, using a root CA certificate file published by the
organization (in their case, the University of Washington, whose possibly
unwitting assistance in this area is graciously acknowledged). But `TrustManagerBuilder`
makes it a bit easier:

```java
new TrustManagerBuilder(getContext()).allowCA("uwash-load-der.crt")
```

In this case, the certificate file is stored in `assets/uwash-load-der.crt`.

As with `selfSigned()`, `allowCA()`'s first parameter can be either:

- a `File` pointing to a certificate on the local file system,
- an `int` raw resource ID, if you wish to package the certificate in your app in `res/raw/`, or
- a `String` pointing to a relative path in `assets/` where you have placed your certificate

By default, the one-parameter version of `allowCA()` assumes an X.509 certificate
file. If your certificate file is in some other format that is supported by Android's
edition of the `CertificateFactory` class, you can use the two-parameter version
of `allowCA()` that takes the format name as a `String` in the second parameter.

And, of course, `allowCA()` can be combined with the others as well, such as a
configuration that supports the default certificate authorities or a custom one:

```java
new TrustManagerBuilder(this)
  .allowCA("uwash-load-der.crt")
  .or()
  .useDefault();
```

Dependencies
------------
This project has no dependencies. It is tested and supported on API Level 8 and
higher. It may well work on older devices, though that is unsupported and untested.
If you determine that
the library (not the demos) do not work on an older-yet-relevant
version of Android, please
file an [issue](https://github.com/commonsguy/cwac-security/issues).

Also note that testing of `TrustStoreBuilder`
has only been done using `HttpsURLConnection` and `OkHttp`. It should work with
`HttpClient` and other stacks.

Version
-------
This is version v0.2.0 of this module, meaning it is rather new.

Demo
----
In the `demoA/` sub-project you will find an application that uses
`checkCustomPermissions()` to see if some other app has already
defined a custom permission. The `demoB/` sub-project does not
use CWAC-Security, but defines that permission, so that you can
verify that `demoA` works as expected.

There is no demo app for `TrustStoreBuilder` at this time. If you are
aware of a public server that either uses a self-signed certificate or
a private certificate authority, one for which a demo app might make sense,
and one where the maintainer of the server will not mind, please
file an [issue](https://github.com/commonsguy/cwac-security/issues).

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with `commonsware` and `android`. Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-security/issues).
Be certain to include complete steps for reproducing the issue.

Do not ask for help via Twitter.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.

Release Notes
-------------
- v0.2.0: added `TrustManagerBuilder` and supporting classes
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

