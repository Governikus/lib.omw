
This is the OMAPI wrapper library

Overview

The Open Mobile API (OMAPI) alone provides only functionality to access terminals (readers)
and cards connected to this terminals. This wrapper adds functionality to find an appropriate
card and handles "extended length APDU to ENVELOPE/GET RESPONSE commands" as well as logical
channels and encoding/escaping issues. Additionally some basic adaptions for specific
implementations of applets are addressed here, too.

Build

To build this module just install maven or open an IDE of your choice and either do
"mvn install" or use your IDE to install the project to your local repository. If you
are using Windows 10 and WSL to develop on Windows and build on Linux please remember
that your Linux user repository will be used for the build of the app. So you'll have
to "mvn install" within WSL.
