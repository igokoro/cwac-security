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
	android:protectionLevel="signature"/>
```

(the `android:protectionLevel` value will vary among scenarios, but
the rest of the `<permission>` element will remain constant)

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

So, to recap:

- A defines a permission and uses it for defense
- B defines the same permission and requests to hold it
- C just requests to hold this permission

With all that in mind, let's walk through some possible scenarios,
focusing on two questions:

1. What is the user told, when the app is installed, regarding this
permission?

2. What access, if any, does App B or App C have to the `ContentProvider`
from App A?


