# Guidelines

- this package will contain all the possible `actions`.
- an `action` can either make changes to the system and refresh `data` accordingly, or just change
  data locally.
- an action should not directly try to manipulate `view` elements (view is anything that is visible
  to user).
