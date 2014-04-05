# Usage: `TrustManagerBuilder`

Android supports SSL "out of the box". That support works best with
`HttpsURLConnection` and [Square's OkHttp](http://square.github.io/okhttp/).
It will support a reasonable set of certificate authorities for validating
the SSL certificates you make on your HTTPS request.

However, it does not handle *all* scenarios "out of the box", and that's
where `TrustManagerBuilder` comes in. `TrustManagerBuilder` makes it easy to
create custom certificate validation rules, to handle things like self-signed
certificates, custom certificate authorities, and the like.

## Using With HTTP Stacks

To use `TrustManagerBuilder`, create an instance, configure it using a set
of builder-style methods (described below), and then call `build()` or
`buildArray()`. `build()` returns an instance of `TrustManager`. `buildArray()`
returns that same instance wrapped in a one-element `TrustManager[]`, for convenience,
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

## Configuring a TrustManagerBuilder

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

- `memorize()`: this tells `TrustManagerBuilder` to only accept SSL certificates
approved by the user

- `addAll()`: this tells `TrustManagerBuilder` to add other trust managers that
you may have implemented yourself or obtained from other libraries

In addition, `or()` tells `TrustManagerBuilder` to logically OR any subsequent configuration
with whatever came previously in the build, while `and()` indicates that subsequent
configuration should be logically AND-ed with whatever came previously.

## Scenarios

All of that will make a bit more sense if we look at some candidate scenarios.

### You Want To Use a Self-Signed Certificate

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

### You Want To Use a Private Certificate Authority

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

### You Want to Detect Other Man-In-The-Middle Attacks

If you cannot use a self-signed certificate, you can still help detect man-in-the-middle
attacks, through certificate memorization.

The idea here is that even if we cannot tell, absolutely, whether a given certificate
is genuine or from an attacker, we can detect *differences in certificates over time*.
So, if the user has been seeing certificate A, and now all of a sudden receives
certificate B instead, there are two main possibilities:

1. The HTTPS server changed certificates for legitimate reasons

2. An attacker is providing an alternative certificate

So, what we do is check certificates against a roster that the user has approved
before. If the newly-received certificate is not in that roster, we fail the HTTPS
request, but raise a custom exception so that your code can detect this case and
ask the user for approval to proceed.

This requires a bit more configuration and management than much of the rest
of `TrustManagerBuilder`. There is a dedicated demo project in the `demoMemo/`
directory of the repository that demonstrates how to use certificate memorization.
The sections that follow explain how to set up certificate memorization.

Note: the certificate memorization employed by this library is inspired by,
though substantially different than, the `MemorizingTrustManager` from
[this GitHub project](https://github.com/ge0rg/MemorizingTrustManager). The underlying
trust manager in the library that implements certificate memorization is
also called `MemorizingTrustManager`, simply because it is the most likely name
for a `TrustManager` implementing certificate memorization.

#### Configuring the TrustManagerBuilder

In addition to other builder methods, you can call `memorize()` on a
`TrustManagerBuilder` to indicate that you want to enable certificate memorization.
This method takes an instance of a `MemorizingTrustManager.Options` object
to configure how memorization works.

The constructor for `MemorizingTrustManager.Options` takes three parameters:

1. A `Context`, used only for the duration of the constructor itself -- this `Context`
is not retained after the constructor returns.

2. A `String` representing a relative path to a directory, inside of `getFilesDir()`,
for working files for certificate memorization. This directory will be created for
you if it does not already exist.

3. A `String` that is the password to use for the `KeyStore` that will hold the
memorized certificates.

While `MemorizingTrustManager.Options` offers a builder-style API for configuring
the options, no other methods are required beyond the constructor for basic use.

The `memorize()` call should be towards the end of the configuration, after an
`and()` call, to say "we will accept SSL certificates that match other criteria
*and* are ones that are memorized".

```java
options=
    new MemorizingTrustManager.Options(this, "memorize", "snicklefritz");

try {
  builder=
      new TrustManagerBuilder(this).useDefault().and().memorize(options);
}
catch (Exception e) {
  // do something useful
}
```

#### Detecting Memorization Exceptions

When you perform an HTTPS operation, you may get an `SSLHandshakeException`. That
should wrap another exception, retrieved via `getCause()`.

If the wrapped exception is `CertificateNotMemorizedException`, that means that
we found a certificate that matched your other criteria (e.g., valid root CA), but
was not found in the memorized roster of certificates. The
`CertificateNotMemorizedException` contains the certificate chain, retrieved
via `getCertificateChain()`.

At this point, you should tell the user that the request failed because of the
unrecognized certificate, and ask the user how to proceed. You are welcome to use
the contents of the certificate chain to provide technical details regarding
the unrecognized certificates to the user, if you so choose.

Bear in mind that your users may be non-technical. Throwing up a dialog with
a lot of SSL gibberish may not be effective. Instead, ideally, you should steer
them for how to contact somebody who can indicate if it is safe to proceed.

There are three possible avenues that the user could take:

1. The user could elect to not proceed, under the premise that the SSL communications
may be compromised. How you handle that is up to you.

2. The user could elect to proceed, but not remember this certificate for very long
("Allow once").

3. The user could elect to proceed, with you remembering the certificate for a long
time, if the certificate is thought to be valid.

#### Memorizing the Certificate

In cases #2 and #3 above, you can call methods on your `TrustManagerBuilder` to
update the memorized roster with the certificate chain that had failed previously.
Use `allowCertOnce()` to remember the certificate for the lifetime of your process,
and use `memorizeCert()` to remember the certificate indefinitely. Both of these
methods take the `X509Certificate` array that is the certificate chain that you got
from calling `getCertificateChain()` on the `CertificateNotMemorizedException`.

After you do this, you can re-try your request that failed due to the unrecognized
certificate, and it should succeed.

#### Clearing the Certificate Roster

At any point, you can call `clearMemorizedCerts()` on the `TrustManagerBuilder`
to get rid of the certificate roster. Pass `false` as the parameter to only get
rid of the "transient" certificates (i.e., the ones you registered via `allowCertOnce()`).
Pass `true` as the parameter to get rid of both the transient and the persistent
certificates (i.e., the ones you registered via `memorizeCert()`).

#### Notes on Threading

Because certificate memorization involves reading from and writing to files, setup
and use of the `TrustManagerBuilder` should be performed on a background thread. Of
course, your network I/O should be on a background thread, anyway.

If you will have several threads that are all performing HTTPS operations, they
should *share* a `TrustManagerBuilder` instance, so that there is a central spot
for updating the certificate roster. The `MemorizingTrustManager` should be thread-safe;
please file issues if you run into threading-related problems.

#### Using Trust-on-First-Use

The default behavior of certificate memorization is to fail on every unrecognized
certificate. The downside of this is that the user will immediately get a failure
the first time the user uses the app, because no certificates will have been memorized
by that point.

You have three courses of action to handle this:

1. Live with it.

2. Manage it yourself, by determining when you believe it is safe to memorize the
certificate without user involvement.

3. Call `trustOnFirstUse()` on the `MemorizingTrustManager.Options` object when you create
it, to indicate that the *first* unrecognized certificate should be memorized automatically,
with failures being reported for all subsequent unrecognized certificates.

#### About CertificateMemorizationException

If you encounter a `CertificateMemorizationException` &mdash; in a crash log, for
example &mdash; that indicates that the library encountered some problem when attempting
to save a certificate that is being saved automatically via the trust-on-first-use
feature. This exception is unlikely to occur.

