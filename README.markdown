CWAC Security: Helping You Help Your Users Defend Their Data
============================================================

This project contains utility code related to Android security
measures.

At present, it contains:

- a `PermissionUtils` class with a `checkCustomPermissions()`
static method, to help you detect if another app has defined your
custom permissions before your app was installed

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
    compile 'com.commonsware.cwac:security:0.1.+'
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

Dependencies
------------
This project has no dependencies and should work on most versions of Android, though
it is only being tested on API Level 15+. If you determine that
the library (not the demos) do not work on an older-yet-relevant
version of Android, please
file an [issue](https://github.com/commonsguy/cwac-security/issues).

Version
-------
This is version v0.1.0 of this module, meaning it is rather new.

Demo
----
In the `demoA/` sub-project you will find an application that uses
`checkCustomPermissions()` to see if some other app has already
defined a custom permission. The `demoB/` sub-project does not
use CWAC-Security, but defines that permission, so that you can
verify that `demoA` works as expected.

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
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

