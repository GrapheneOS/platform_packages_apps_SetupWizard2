# Guidelines

- this package will mostly contain android components which aren't related to `view`
- these components would ideally require declarations inside AndroidManifest.xml
- exceptions can be a receiver which could be registered at runtime and used only inside let's say
  WelcomeActions. In that case it would probably be better to keep that nearer to WelcomeActions (
  encourages cohesion and separation of concerns).