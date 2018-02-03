# Bojiti
A web crawler framework.  Very much in development

## Structure
Bojiti is split into several Java 9 modules.  These modules are used to load
in `Downloader` and `Parser` services using the `ServiceLoader` API.  The
current implementation relies solely on Jigsaw's `uses`/`provides` syntax to
load these services in, although in the future a traditional `services`-style
loading mechanism may be included for compatibility with older JDKs.

Locally, this project is built and developed using IntelliJ IDEA, but the
*.iml files and .idea directories are omitted.  The projects can, however, be
recreated using any IDE that supports Maven using the pom.xml files for each
module.  Some modules have Maven dependencies.
