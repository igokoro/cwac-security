# The Custom Permission Problem

Android, since the beginning, has offered both platform permissions
(defined by the framework) and custom permissions (defined by apps).

Unfortunately, custom permissions have some undocumented limitations
that make them intrinsically risky. Here, we will walk through some
scenarios and show where the problems arise, plus discuss how to
mitigate them as best we can.

## Scenarios

All of the following scenarios focus on three major app profiles.

App A is an app that defines a custom permission in its manifest,
such as:

```xml
<permission
	android:name="com.commonsware.cwac.security.demo.OMG"
	android:description="@string/perm_desc"
	android:label="@string/perm_label"
	android:protectionLevel="normal"/>
```

App A also defends a component using the `android:permission`
attribute, referencing the custom permission:

```xml
<provider
	android:name="FileProvider"
	android:authorities="com.commonsware.cwac.security.demo.files"
	android:exported="true"
	android:grantUriPermissions="false"
	android:permission="com.commonsware.cwac.security.demo.OMG">
	<grant-uri-permission android:path="/test.pdf"/>
</provider>
```

App B has a `<uses-permission>` element to declare to the user that
it wishes to access components defended by that permission:

```xml
<uses-permission android:name="com.commonsware.cwac.security.demo.OMG"/>
```

App C has the same `<uses-permission>` element. The difference is
that App B *also* has the `<permission>` element, just as App A
does, albeit with different descriptive information (e.g.,
`android:description`) and, at times, a different protection level.

All three apps are signed with different signing keys, because
most likely they are from different developers.

So, to recap:

- A defines a permission and uses it for defense
- B defines the same permission and requests to hold it
- C just requests to hold this permission

With all that in mind, let's walk through some possible scenarios,
focusing on two questions:

1. What is the user told, when the app is installed through
normal methods (i.e., not via **`adb`**), regarding this
permission?

2. What access, if any, does App B or App C have to the `ContentProvider`
from App A?

### The Application SDK Case (A, Then C)

Suppose the reason why App A has defined a custom permission is because
it wants third-party apps to have the ability to access its secured
components... but only with user approval. By defining a custom
permission, and having third-party apps request that permission,
the user should be informed about the requested permission and can
make an informed decision.

Conversely, if an app tries to access a secured component but
has not requested the permission, the access attempt should fail.

App C has requested the custom permission via the `<uses-permission>`
element. If the permission -- defined by App A -- has an
`android:protectionLevel` of `normal` or `dangerous`, the user
will be informed about the requested permission at install time. If
the user continues with the installation, App C can access the
secured component.

If, however, the `android:protectionLevel` is `signature`, the user
is not informed about the requested permission at install time, as
the system can determine on its own whether or not the permission
should be granted. In this case, App A and App C are signed with
different signing keys, so Android silently ignores the permission
request. If the user continues with installation, then App C tries
to access App A's secured component, App C crashes with a
`SecurityException`.

In other words, this all works as expected.

### The Application SDK Problem Case (C, Then A)

However, in many cases, there is nothing forcing the user to install
App A before App C. This is particularly true for publicly-distributed
apps on common markets, like the Play Store.

When the user installs App C, the user is not informed about the
request for the custom permission, presumably because that permission has not
yet been defined. If the user later installs App A, App C is not
retroactively granted the permission, and so App C's attempts
to use the secured component fail.

This works as expected, though it puts a bit of a damper on custom
permissions. One way to work around this would be for the user to
uninstall App C, then install it again (with App A already installed).
This returns us to the original scenario from the preceding section.
However, if the user has data in App C, losing that data may be a problem
(as in a "let's give App C, or perhaps App A, one-star ratings on
the Play Store" sort of problem).

### The Peer Apps Case, Part One (A, Then B)

Suppose now we augment our SDK-consuming app (formerly App C) to
declare the same permission that App A does, in an attempt to allow
the two apps to be installed in either order. That is what App B is:
the same app as App C, but where it has the same `<permission>`
element as does App A in its manifest.

This scenario is particularly important where both apps could be
of roughly equal importance to the user. In cases where App C is
some sort of plugin for App A, it is not unreasonable for the
author of App A to require App A to be installed first. But, if
Twitter and Facebook wanted to access components of each others' apps,
it would be unreasonable for either of those firms to mandate that
their app must be installed first. After all, if Twitter wants to
be installed first, and Facebook wants to be installed first, one
will be disappointed.

If the user installs App A (the app defending a component with
the custom permission) before App B, the user will be notified at
install time about App B's request for this permission. Notably,
the information shown on the installation security screen will
contain App A's description of the permission. And, if the user goes
ahead and installs App B, App B can indeed access App A's secured
component, since it was granted permission by the user.

Once again, everything is working as expected. Going back to the
two questions:

1. The user is informed when App B or App C requests the permission
defined by App A.

2. App B and App C can hold that permission if and only if they meet the
requirements of the protection level

### The Peer Apps Case, Part Two (B, Then A)

What happens if we reverse the order of installation? After all, if
App A and App B are peers, from the standpoint of the user, there
is roughly a 50% chance that the user will install App B before
App A.

Here is where things go off the rails.

**The user is not informed about App B's request for the custom permission.**

The user will be informed about any platform permissions that the
app requests via other `<uses-permission>` elements. If there are none,
the user is told that App B requests no permissions... despite the
fact that it does.

When the user installs App A, the same thing occurs. Of course, since
App A does not have a `<uses-permission>` element, this is not all
that surprising.

However, at this point, **even though the user was not informed**, App B
holds the custom permission and can access the secured component.

This is bad enough when both parties are ethical. App B could be
a piece of malware, though, designed to copy the data from App A,
ideally without the user's knowledge. And, if App B is installed before
App A, that would happen.

So, going to the two questions:

1. The user is **not** informed about App B's request for the permission...

2. ...but App B gets it anyway and can access the secured component

### The Downgraded-Level Malware Case (B, Then A, Again)

You might think that the preceding problem would only be for
`normal` or `dangerous` protection levels. If App A defines
a permission as requiring a matching `signature`, and App A marks a
component as being defended by that permission, Android must require
the signature match, right?

Wrong.

The behavior is identical to the preceding case. Android does
not use the *defender's* protection level. It uses the *definer's*
protection level, meaning the protection level of whoever was installed
first and had the `<permission>` element.

So, if App A has the custom permission defined as `signature`, and
App B has the custom permission defined as `normal`, if App B is
installed first, the behavior is as shown in the preceding section:

1. The user is **not** informed about App B's request for the permission...

2. ...but App B gets it anyway and can access the secured component,
despite the signatures not matching

### The Peer Apps Case With a Side Order of C

What happens if we add App C back into the mix? Specifically, what
if App B is installed first, then App A, then App C?

When App C eventually gets installed, the user is prompted for the
custom permission that App C requests via `<uses-permission>`.
However, the description that the user sees is from App B, the one
that first defined the custom `<permission>`. Moreover, the
protection level is whatever App B defined it to be. So if App B
downgraded the protection level from App A's intended `signature`
to be `normal`, App C can hold that permission and access the
secured App A component, even if it is signed by another signing key.

Not surprisingly, the same results occur if you install App B,
then App C, then App A.

## Behavior Analysis

TBD

## Risk Assessment

TBD

## Mitigation

TBD

## Acknowledgements

TBD

